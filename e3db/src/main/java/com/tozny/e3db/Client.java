package com.tozny.e3db;

import android.os.Handler;
import android.os.Looper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tozny.e3db.crypto.AndroidCrypto;
import com.tozny.e3db.crypto.KaliumCrypto;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.ByteString;
import retrofit2.Retrofit;

import static com.tozny.e3db.Base64.decodeURL;
import static com.tozny.e3db.Base64.encodeURL;
import static com.tozny.e3db.Checks.*;

/**
 * Implements interactions with E3DB.
 *
 * <p>This class provides all communication with E3DB. Use it to read, write, update, delete and
 * query records. It can also be used to add and remove sharing rules (through the {@code share} and {@code revoke}
 * methods).
 *
 * <h1>Asynchronous Result Handling</h1>
 * Most methods in this class have a {@code void} return type, and instead return results via a callback argument of type {@link ResultHandler}.
 * All E3DB network communication occurs on a background thread (created by the class). Results are delivered via the callback argument given. On Android,
 * results are delivered on the UI thread. On all other platforms, results are delivered on the same background thread that performed the E3DB operation.
 *
 * <p>Note that no E3DB operations have a defined timeout &mdash; your application is responsible for setting timeouts and performing appropriate action.
 *
 * <h2><i>ResultHandler</i> &amp; <i>Result</i> Values</h2>
 * The {@link ResultHandler} callback accepts a {@link Result} value, which signals whether an error occurred or if the operation completed
 * successfully. The {@link Result#isError()} method will return {@code true} if some error
 * occurred. If not, {@link Result#asValue()} will give the value of the operation that occurred.
 *
 * <p>More details are available on the documentation for {@link Result}.
 *
 * <h1>Registering a Client</h1>
 * Registration requires a "token," which associates each
 * client with your Tozny account. Before using the SDK, go to <a href="https://console.tozny.com">https://console.tozny.com</a>,
 * create an account, and go to
 * the "Manage Clients" section. Click the "Create Token" button under
 * the "Client Registration Tokens" heading. Use the token value created to register
 * new clients. Note that this value is not
 * meant to be secret and is safe to embed in your app.
 *
 * <p>The {@link #register(String, String, String, ResultHandler)} method can be used to
 * register a client. After registration, save the resulting credentials for use later.
 *
 * <h1>Creating a Client</h1>
 * Use the {@link ClientBuilder} class to create a {@link Client} instance from stored credentials. The
 * convenience method {@link ClientBuilder#fromConfig(Config)} makes it easy to load all client
 * information from one location. The
 * {@link Config} class also provides methods for converting credentials to and from JSON.
 *
 * <p>However, <b>ALWAYS</b> be sure to store client credentials securely. Those are the username,
 * password, and private key used by the {@link Client} instance!
 *
 * <h1>Write, Update and Delete Records</h1>
 *
 * Records can be written with the {@link #write(String, RecordData, Map, ResultHandler)} method,
 * updated with {@link #update(RecordMeta, RecordData, Map, ResultHandler)}, and deleted with {@link #delete(UUID, String, ResultHandler)}.
 *
 * <h1>Reading &amp; Querying Records</h1>
 *
 * You can read a single record with the {@link #read(UUID, ResultHandler)} method.
 *
 * <p>Multiple records (matching some criteria) can be read using the {@link #query(QueryParams, ResultHandler)} method. You must pass a {@link QueryParams} instance
 * to specify the selection critera for records; use the {@link QueryParamsBuilder} object to build the query.
 *
 * <h2>Pagination</h2>
 * Query result pagination depends on the {@link QueryResponse#last()} value, which returns an index indicating the last record
 * in a given page of results. Pass the {@code last()} value obtained to the {@link QueryParamsBuilder#setAfter}) method to
 * obtain records following the last record.
 *
 * <h1>Sharing</h1>
 * Sharing allows one client to give read access to a set of that client's records to another client, without compromising end-to-end encryption. To share
 * records, a client must know the ID of the client they wish to share with. The SDK does not provide support for looking up client IDs - you will have to
 * implement such support out of band.
 *
 * <p>To share records, use the {@link #share(String, UUID, ResultHandler)} method. To remove sharing access, use the {@link #revoke(String, UUID, ResultHandler)}} method.
 *
 */
public class Client {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");
  private static final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
  private static final MediaType PLAIN_TEXT = MediaType.parse("text/plain");
  private static final Executor backgroundExecutor;
  private static final Executor uiExecutor;
  private static final Crypto crypto;
  private static final String allow = "{\"allow\" : [ { \"read\": {} } ] }";
  private static final String deny = "{\"deny\" : [ { \"read\": {} } ] }";

  static {
    backgroundExecutor = new ThreadPoolExecutor(1,
      Runtime.getRuntime().availableProcessors(),
      30,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<Runnable>(10),
      new ThreadFactory() {
        private int threadCount = 1;
        @Override
        public Thread newThread(Runnable runnable) {
          final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
          thread.setDaemon(true);
          thread.setName("E3DB background "+ threadCount++);
          return thread;
        }
      });

    boolean isAndroid = false;
    try {
      Class.forName("android.os.Build");
      isAndroid = true;
    } catch (ClassNotFoundException ignored) {
    }

    if (isAndroid) {
      crypto = new AndroidCrypto();
      // Post results to UI thread
      uiExecutor = new Executor() {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable runnable) {
          handler.post(runnable);
        }
      };
    } else {
      crypto = new KaliumCrypto();
      // Post results to current thread (whatever that is)
      uiExecutor = new Executor() {
        @Override
        public void execute(Runnable runnable) {
          runnable.run();
        }
      };
    }
  }

  private final String apiKey;
  private final String apiSecret;

  private final UUID clientId;
  private final byte[] privateKey;
  private final StorageAPI storageClient;
  private final ShareAPI shareClient;

  Client(String apiKey, String apiSecret, UUID clientId, URI host, byte[] privateKey) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.clientId = clientId;
    this.privateKey = privateKey;

    Retrofit build = new Retrofit.Builder()
      .callbackExecutor(this.uiExecutor)
      .client(new OkHttpClient.Builder()
        .addInterceptor(new TokenInterceptor(apiKey, apiSecret, host))
        .build())
      .baseUrl(host.resolve("/").toString())
      .build();

    storageClient = build.create(StorageAPI.class);
    shareClient = build.create(ShareAPI.class);
  }

  private static class CC implements ClientCredentials {
    private final String apiKey;
    private final String apiSecret;
    private final UUID clientId;
    private final String name;
    private final String publicKey;
    private final URI host;
    private final boolean enabled;

    public CC(String apiKey, String apiSecret, UUID clientId, String name, String publicKey, String host, boolean enabled) {
      this.apiKey = apiKey;
      this.apiSecret = apiSecret;
      this.clientId = clientId;
      this.name = name;
      this.publicKey = publicKey;
      this.host = URI.create(host);
      this.enabled = enabled;
    }

    @Override
    public String apiKey() {
      return apiKey;
    }

    @Override
    public String apiSecret() {
      return apiSecret;
    }

    @Override
    public UUID clientId() {
      return clientId;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String publicKey() {
      return publicKey;
    }

    @Override
    public String host() {
      return host.toASCIIString();
    }

    @Override
    public boolean enabled() {
      return enabled;
    }
  }

  private void onBackground(Runnable runnable) {
    backgroundExecutor.execute(runnable);
  }

  private static <R> void executeError(Executor executor, final ResultHandler<R> handler, final Throwable e) {
    if (handler != null)
      executor.execute(new Runnable() {
        @Override
        public void run() {
          handler.handle(new ErrorResult<R>(e));
        }
      });
  }

  private static <R> void executeValue(Executor executor, final ResultHandler<R> handler, final R value) {
    if (handler != null)
      executor.execute(new Runnable() {
        @Override
        public void run() {
          handler.handle(new ValueResult<R>(value));
        }
      });
  }

  /**
   * Obtains a Tozny JWT, if necessary, and adds it to every request.
   */

  private <R> void uiError(final ResultHandler<R> handleError, final Throwable e) {
    if (handleError != null)
      uiExecutor.execute(new Runnable() {
        @Override
        public void run() {
          handleError.handle(new ErrorResult<R>(e));
        }
      });
  }
  private <R> void uiValue(final ResultHandler<R> handleResult, final R r) {
    if (handleResult != null)
      uiExecutor.execute(new Runnable() {
        @Override
        public void run() {
          handleResult.handle(new ValueResult<R>(r));
        }
      });
  }

  private static class ER {
    public final CipherWithNonce edk; // encrypted data key
    public final CipherWithNonce ef; // encrypted field

    public ER(String quad) {
      int split = quad.indexOf(".", quad.indexOf(".") + 1);
      edk = CipherWithNonce.decode(quad.substring(0, split));
      ef = CipherWithNonce.decode(quad.substring(split + 1));
    }
  }

  private static RecordMeta getRecordMeta(JsonNode rawMeta) throws ParseException, JsonProcessingException {
    UUID recordId = UUID.fromString(rawMeta.get("record_id").asText());
    UUID writerId = UUID.fromString(rawMeta.get("writer_id").asText());
    UUID userId = UUID.fromString(rawMeta.get("user_id").asText());
    Date created = iso8601.parse(rawMeta.get("created").asText());
    Date lastModified = iso8601.parse(rawMeta.get("last_modified").asText());
    String version = rawMeta.get("version").asText();
    String type = rawMeta.get("type").asText();

    String plain;
    if(rawMeta.has("plain")) {
      JsonNode plain1 = rawMeta.get("plain");
      if(plain1.isNull()) {
        plain = null;
      }
      else
        plain = mapper.writeValueAsString(plain1);
    }
    else
      plain = null;

    return new M(recordId, writerId, userId, version, created, lastModified, type, plain);
  }

  private static R makeR(JsonNode rawMeta) throws ParseException, JsonProcessingException {
    return new R(new HashMap<String, String>(), getRecordMeta(rawMeta));
  }

  private static R makeR(byte[] accessKey, JsonNode rawMeta, JsonNode fields, Crypto crypto) throws ParseException, UnsupportedEncodingException, JsonProcessingException {
    RecordMeta meta = getRecordMeta(rawMeta);
    Map<String, String> results = decryptObject(accessKey, fields, crypto);
    return new R(results, meta);
  }

  private static class M implements RecordMeta {
    private final UUID recordId;
    private final UUID writerId;
    private final UUID userId;
    private final String version;
    private final Date created;
    private final Date lastModified;
    private final String type;
    private final String plain;

    private M(UUID recordId, UUID writerId, UUID userId, String version, Date created, Date lastModified, String type, String plain) {
      this.recordId = recordId;
      this.writerId = writerId;
      this.userId = userId;
      this.version = version;
      this.created = created;
      this.lastModified = lastModified;
      this.type = type;
      this.plain = plain;
    }

    public UUID recordId() {
      return recordId;
    }

    public UUID writerId() {
      return writerId;
    }

    public UUID userId() {
      return userId;
    }

    public String version() {
      return version;
    }

    public Date created() {
      return created;
    }

    public Date lastModified() {
      return lastModified;
    }

    public String type() {
      return type;
    }

    public String plain() {
      return plain;
    }

  }
  private static class R implements Record {

    private final Map<String, String> data;

    private final RecordMeta meta;

    public R(Map<String, String> data, RecordMeta meta) {
      this.data = data;
      this.meta = meta;
    }

    @Override
    public RecordMeta meta() {
      return meta;
    }

    @Override
    public Map<String, String> data() {
      return data;
    }
  }

  private static class QR implements QueryResponse {

    private final ArrayList<Record> records;

    private final long lastIdx;

    private QR(ArrayList<Record> records, long lastIdx) {
      this.records = records;
      this.lastIdx = lastIdx;
    }
    @Override
    public List<Record> records() {
      return records;
    }
    @Override
    public long last() {
      return lastIdx;
    }
  }

  private QueryResponse doSearchRequest(QueryParams params) throws IOException, E3DBException, ParseException {
    Map<String, Object> searchRequest = new HashMap<>();

    if(params.after > 0)
      searchRequest.put("after_index", params.after);

    if(params.count != -1)
      searchRequest.put("count", params.count);

    if(params.includeData != null)
      searchRequest.put("include_data", params.includeData.booleanValue());

    if(params.writerIds != null)
      searchRequest.put("writer_ids", makeArray(params.writerIds));

    if(params.includeAllWriters != null)
      searchRequest.put("include_all_writers", params.includeData.booleanValue());

    if(params.userIds != null)
      searchRequest.put("user_ids", makeArray(params.userIds));

    if(params.recordIds != null)
      searchRequest.put("record_ids", makeArray(params.recordIds));

    if (params.types != null)
      searchRequest.put("content_types", params.types.toArray());

    String content = mapper.writeValueAsString(searchRequest);
    RequestBody queryRequest = RequestBody.create(APPLICATION_JSON, content);
    retrofit2.Response<ResponseBody> execute = storageClient.query(queryRequest).execute();
    if (execute.code() != 200)
      throw E3DBException.find(execute.code(), execute.message());

    JsonNode results = mapper.readTree(execute.body().string());

    JsonNode currPage = results.get("results");
    long nextIdx = results.get("last_index").asLong();

    ArrayList<Record> records = new ArrayList<>(currPage.size());
    for (int currRow = 0; currRow < currPage.size(); currRow++) {
      R record;
      JsonNode queryRecord = currPage.get(currRow);
      JsonNode access_key = queryRecord.get("access_key");
      if(access_key != null && access_key.isObject()) {
        record = makeR(crypto.decryptBox(
            CipherWithNonce.decode(access_key.get("eak").asText()),
            decodeURL(access_key.get("authorizer_public_key").get("curve25519").asText()),
            privateKey
          ),
          queryRecord.get("meta"),
          queryRecord.get("record_data"),
          crypto);
      }
      else {
        record = makeR(queryRecord.get("meta"));
      }
      records.add(record);
    }

    return new QR(records, nextIdx);
  }

  private String[] makeArray(List writerIds) {
    String[] objects = new String[writerIds.size()];
    int idx = 0;
    for(Object id : writerIds) {
      objects[idx++] = id.toString();
    }
    return objects;
  }

  private static Map<String, String> encryptObject(byte[] accessKey, Map<String, String> fields, Crypto crypto) throws UnsupportedEncodingException {
    Map<String, String> encFields = new HashMap<>();
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      byte[] dk = crypto.newSecretKey();

      String encField = new StringBuilder(crypto.encryptSecretBox(dk, accessKey).toMessage()).append(".")
        .append(crypto.encryptSecretBox(entry.getValue().getBytes("UTF-8"), dk).toMessage()).toString();
      encFields.put(entry.getKey(), encField);
    }
    return encFields;
  }

  private static Map<String, String> decryptObject(byte[] accessKey, JsonNode record, Crypto crypto) throws UnsupportedEncodingException {
    Map<String, String> decryptedFields = new HashMap<>();
    Iterator<String> keys = record.fieldNames();
    while (keys.hasNext()) {
      String key = keys.next();
      ER er = new ER(record.get(key).asText());
      byte[] dk = crypto.decryptSecretBox(er.edk, accessKey);
      String value = new String(crypto.decryptSecretBox(er.ef, dk), "UTF-8");
      decryptedFields.put(key, value);
    }
    return decryptedFields;
  }
  private static class TokenInterceptor implements Interceptor {
    private final URI host;

    private final AuthAPI authClient;
    private final String basic;

    private String token = null;

    private Date replaceAfter = new Date(0L);
    private TokenInterceptor(String apiKey, String apiSecret, URI host) {
      this.host = host;
      try {
        this.basic = new StringBuffer("Basic ").append(ByteString.of(new StringBuffer(apiKey).append(":").append(apiSecret).toString().getBytes("UTF-8")).base64()).toString();
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      authClient = new Retrofit.Builder()
        .baseUrl(host.resolve("/").toString())
        .build()
        .create(AuthAPI.class);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
      Request req = chain.request();
      if (req.header("Authorization") != null) {
        return chain.proceed(req);
      } else {
        if (replaceAfter.before(new Date())) {
          retrofit2.Response<ResponseBody> response = authClient.getToken(basic, RequestBody.create(PLAIN_TEXT, "grant_type=client_credentials")).execute();
          if (response.code() != 200)
            throw new IOException("Unable to renew token.");

          JsonNode token = mapper.readTree(response.body().string());
          Calendar c = Calendar.getInstance();
          c.add(Calendar.SECOND, Math.max(60, Math.min(15 * 60, token.get("expires_in").asInt() - 60)));
          replaceAfter = c.getTime();
          this.token = token.get("access_token").asText();
        }

        return chain.proceed(req.newBuilder().addHeader("Authorization", "Bearer " + token).build());
      }
    }

  }

  private byte[] getOwnAccessKey(String type) throws E3DBException, IOException {
    byte[] ak = getAccessKey(this.clientId, this.clientId, this.clientId, type);
    if (ak == null) {
      // Write new AK
      ak = crypto.newSecretKey();
      setAccessKey(this.clientId, this.clientId, this.clientId, type, crypto.getPublicKey(this.privateKey), ak);
    }

    return ak;
  }

  private void removeAccessKey(UUID writerId, UUID userId, UUID readerId, String type) throws IOException, E3DBException {
    retrofit2.Response<ResponseBody> response = storageClient.deleteAccessKey(writerId.toString(), userId.toString(), readerId.toString(), type).execute();
    if(response.code() != 204) {
      throw E3DBException.find(response.code(), response.message());
    }
  }

  private byte[] getAccessKey(UUID writerId, UUID userId, UUID readerId, String type) throws E3DBException, IOException {
    // TODO: cache AKs.
    retrofit2.Response<ResponseBody> response = storageClient.getAccessKey(writerId.toString(), userId.toString(), readerId.toString(), type).execute();
    if (response.code() == 404) {
      return null;
    } else if (response.code() == 200) {
      JsonNode eakResponse = mapper.readTree(response.body().string());
      byte[] ak = crypto.decryptBox(CipherWithNonce.decode(eakResponse.get("eak").asText()),
        decodeURL(eakResponse.get("authorizer_public_key").get("curve25519").asText()),
        this.privateKey);
      return ak;
    }
    else
      throw E3DBException.find(response.code(), response.message());
  }

  private void setAccessKey(UUID writerId, UUID userId, UUID readerId, String type, byte[] readerKey, byte[] ak) throws E3DBException, IOException {
    Map<String, String> eak = new HashMap<>();
    eak.put("eak", crypto.encryptBox(ak, readerKey, this.privateKey).toMessage());

    RequestBody body = RequestBody.create(APPLICATION_JSON, mapper.writeValueAsString(eak));
    retrofit2.Response<ResponseBody> response = storageClient.putAccessKey(writerId.toString(), userId.toString(), readerId.toString(), type, body).execute();
    if (response.code() != 201) {
      throw E3DBException.find(response.code(), response.message());
    }
  }

  /**
   * Generates a private key that can be used for Curve25519
   * public-key encryption.
   *
   * <p>The associated public key can be retrieved using {@link #getPublicKey(String)}.
   *
   * The returned value represents the key as a Base64URL-encoded string.
   */
  public static String newPrivateKey() {
    byte [] key = crypto.newPrivateKey();
    return encodeURL(key);
  }

  /**
   * Gets the public key for the given private key.
   *
   * <p>The private key must be a Base64URL-encoded string.
   *
   * The returned value represents the key as a Base64URL-encoded string.
   */
  public static String getPublicKey(String privateKey) {
    checkNotEmpty(privateKey, "privateKey");
    byte[] arr = decodeURL(privateKey);
    checkNotEmpty(arr, "privateKey");
    return encodeURL(crypto.getPublicKey(arr));
  }

  /**
   * The ID of this client.
   */
  public UUID clientId() {
    return clientId;
  }

  /**
   * Registers a new client. This method creates a new public/private key pair for the client
   * to use when encrypting and decrypting records.
   *
   * @param token Registration token obtained from the Tozny console at <a href="https://console.tozny.com">https://console.tozny.com</a>.
   * @param clientName Name of the client; for informational purposes only.
   * @param host Host to register with. Should be {@code https://api.e3db.com}.
   * @param handleResult Handles the result of registration. The {@link Config} value can be converted to JSON, written to
   *                     a secure location, and loaded later.
   */
  public static void register(final String token, final String clientName, final String host, final ResultHandler<Config> handleResult) {
    checkNotEmpty(token, "token");
    checkNotEmpty(clientName, "clientName");
    checkNotEmpty(host, "host");

    final byte[] privateKey = crypto.newPrivateKey();
    final String publicKey = encodeURL(crypto.getPublicKey(privateKey));

    register(token, clientName, publicKey, host, new ResultHandler<ClientCredentials>() {
      @Override
      public void handle(Result<ClientCredentials> r) {
        if (r.isError()) {
          executeError(uiExecutor, handleResult, r.asError().other());
        }
        else {
          final ClientCredentials credentials = r.asValue();
          Config info = new Config(credentials.apiKey(), credentials.apiSecret(), credentials.clientId(), clientName, credentials.host(), encodeURL(privateKey),
            publicKey);
          executeValue(uiExecutor, handleResult, info);
        }
      }
    });
  }

  /**
   * Registers a new client with a given public key.
   *
   * <p>This method does not create a private/public key pair; rather, the public key should be provided
   * by the caller.
   *
   * @param token Registration token obtained from the Tozny console at <a href="https://console.tozny.com">https://console.tozny.com</a>.
   * @param clientName Name of the client; for informational purposes only.
   * @param publicKey A Base64URL-encoded string representing the public key associated with the client. Should be based on a Curve25519
   *                  private key. Consider using {@link #newPrivateKey()} to generate a private key.
   * @param host Host to register with. Should be {@code https://api.e3db.com}.
   * @param handleResult Handles the result of registration.
   */
  public static void register(final String token, final String clientName, final String publicKey, final String host, final ResultHandler<ClientCredentials> handleResult) {
    checkNotEmpty(token, "token");
    checkNotEmpty(clientName, "clientName");
    checkNotEmpty(publicKey, "publicKey");
    checkNotEmpty(host, "host");

    final RegisterAPI registerClient = new Retrofit.Builder()
      .callbackExecutor(uiExecutor)
      .client(new OkHttpClient.Builder()
        .build())
      .baseUrl(URI.create(host).resolve("/").toString())
      .build().create(RegisterAPI.class);

    backgroundExecutor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          Map<String, String> publicKeyInfo = new HashMap<>();
          publicKeyInfo.put("curve25519", publicKey);

          Map<String, Object> clientInfo = new HashMap<>();
          clientInfo.put("name", clientName);
          clientInfo.put("public_key", publicKeyInfo);

          Map<String, Object> registerInfo = new HashMap<>();
          registerInfo.put("token", token);
          registerInfo.put("client", clientInfo);

          final retrofit2.Response<ResponseBody> response = registerClient.register(RequestBody.create(APPLICATION_JSON, mapper.writeValueAsString(registerInfo))).execute();
          if (response.code() != 201) {
            executeError(uiExecutor, handleResult, E3DBException.find(response.code(), response.message()));
          } else {
            final String doc = response.body().string();
            JsonNode creds = mapper.readTree(doc);
            final String apiKey = creds.get("api_key_id").asText();
            final String apiSecret = creds.get("api_secret").asText();
            final UUID clientId = UUID.fromString(creds.get("client_id").asText());
            final String name = creds.get("name").asText();
            final String publicKey = creds.get("public_key").get("curve25519").asText();
            final boolean enabled = creds.get("enabled").asBoolean();
            final ClientCredentials clientCredentials = new CC(apiKey, apiSecret, clientId, name, publicKey, host, enabled);
            executeValue(uiExecutor, handleResult, clientCredentials);
          }
        } catch (final Throwable e) {
          executeError(uiExecutor, handleResult, e);
        }
      }
    });
  }

  /**
   * Write a new record.
   *
   * @param type Describes the type of the record (e.g., "contact_info", "credit_card", etc.).
   * @param fields Values to encrypt and store.
   * @param plain A JSON document giving additional, user-defined metadata that will
   *              <b>NOT</b> be encrypted. Can be {@code null}. If not {@code null}, must be a JSON
   *              object.
   * @param handleResult Result of the operation. If successful, returns the newly written record .
   */
  public void write(final String type, final RecordData fields, String plain, final ResultHandler<Record> handleResult) {
    checkNotEmpty(type, "type");
    checkNotNull(fields, "fields");
    final JsonNode plainJson;
    try {
      if(plain != null) {
        plainJson = mapper.readTree(plain);
        if(! plainJson.isObject())
          throw new IllegalArgumentException("elain must be an object; was " + plainJson.getNodeType());
      }
      else {
        plainJson = null;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid JSON document: plain", e);
    }

    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          if (fields == null) {
            uiError(handleResult, new IllegalArgumentException("data null"));
          }
          if (type == null || type.trim().length() == 0) {
            uiError(handleResult, new IllegalArgumentException("type null"));
          }
          final byte[] ownAK = getOwnAccessKey(type);
          Map<String, String> encFields = encryptObject(ownAK, fields.getCleartext(), crypto);

          Map<String, Object> meta = new HashMap<>();
          meta.put("writer_id", clientId.toString());
          meta.put("user_id", clientId.toString());
          meta.put("type", type.trim());

          if (plainJson != null)
            meta.put("plain", plainJson);

          Map<String, Object> record = new HashMap<>();
          record.put("meta", meta);
          record.put("data", encFields);

          String content = mapper.writeValueAsString(record);
          final retrofit2.Response<ResponseBody> response = storageClient.writeRecord(RequestBody.create(APPLICATION_JSON, content)).execute();
          if (response.code() == 201) {
            JsonNode result = mapper.readTree(response.body().string());
            uiValue(handleResult, makeR(ownAK, result.get("meta"), result.get("data"), crypto));
          } else {
            uiError(handleResult, E3DBException.find(response.code(), response.message()));
          }
        } catch (final Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Replace the given record with new data and plaintext metadata.
   *
   * <p>The {@code recordMeta} argument is only used to obtain
   * information about the record to replace. No metadata updates by
   * the client are allowed.
   *
   * @param recordMeta Metadata for the record being replaced
   * (obtained from a previous {@code write} or {@code query} call).
   * @param fields Field names and values. Wrapped in a {@link
   * RecordData} instance to prevent confusing with the {@code plain}
   * parameter.
   * @param plain A JSON document giving additional, user-defined metadata that will
   *              <b>NOT</b> be encrypted. Can be {@code null}. If not {@code null}, must be a JSON
   *              object.
   * @param handleResult If the update succeeds, returns the updated
   * record. If the update fails due to a version conflict, the value
   * passed to the {@link ResultHandler#handle(Result)}} method returns
   * an instance of {@link E3DBVersionException} when {@code
   * asError().error()} is called.
   */
  public void update(final RecordMeta recordMeta, final RecordData fields, String plain, final ResultHandler<Record> handleResult) {
    checkNotNull(recordMeta, "recordMeta");
    checkNotNull(fields, "fields");
    final JsonNode plainJson;
    try {
      if(plain != null) {
        plainJson = mapper.readTree(plain);
        if(! plainJson.isObject())
          throw new IllegalArgumentException("plain must be an object; was " + plainJson.getNodeType());
      }
      else {
        plainJson = null;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid JSON document: plain", e);
    }

    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          final byte[] ownAK = getOwnAccessKey(recordMeta.type());
          Map<String, String> encFields = encryptObject(ownAK, fields.getCleartext(), crypto);

          Map<String, Object> meta = new HashMap<>();
          meta.put("writer_id", clientId.toString());
          meta.put("user_id", clientId.toString());
          meta.put("type", recordMeta.type().trim());

          if (plainJson != null)
            meta.put("plain", plainJson);

          Map<String, Object> fields = new HashMap<>();
          fields.put("meta", meta);
          fields.put("data", encFields);

          retrofit2.Response<ResponseBody> response = storageClient.updateRecord(recordMeta.recordId().toString(), recordMeta.version(), RequestBody.create(APPLICATION_JSON, mapper.writeValueAsString(fields))).execute();
          if (response.code() == 409) {
            uiError(handleResult, new E3DBVersionException(recordMeta.recordId(), recordMeta.version()));
          } else if (response.code() == 200) {
            JsonNode result = mapper.readTree(response.body().string());
            uiValue(handleResult, makeR(ownAK, result.get("meta"), result.get("data"), crypto));
          }
          else {
            uiError(handleResult, E3DBException.find(response.code(), response.message()));
          }
        } catch (Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Deletes a given record.
   *
   * @param recordId ID of the record to delete. Obtained from {@link RecordMeta#recordId()}.
   * @param version Version associated with the record. Obtained from {@link RecordMeta#version()}.
   * @param handleResult If the deletion succeeds, returns no useful information. If the delete fails due to a version conflict, the value passed to the {@link ResultHandler#handle(Result)}} method return an instance of
   * {@link E3DBVersionException} when {@code asError().error()} is called.
   */
  public void delete(final UUID recordId, final String version, final ResultHandler<Void> handleResult) {
    checkNotNull(recordId, "recordId");
    checkNotEmpty(version, "version");

    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          retrofit2.Response<ResponseBody> response = storageClient.deleteRecord(recordId.toString(), version.toString()).execute();
          if (response.code() == 409) {
            uiError(handleResult, new E3DBVersionException(recordId, version));
          } else if(response.code() == 204) {
            uiValue(handleResult, null);
          }
          else {
            uiError(handleResult, E3DBException.find(response.code(), response.message()));
          }
        } catch (Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Read a record.
   *
   * @param recordId ID of the record to read. Especially useful with {@code query} results that do not include
   *                 the actual data.
   * @param handleResult If successful, return the record read.
   */
  public void read(final UUID recordId, final ResultHandler<Record> handleResult) {
    checkNotNull(recordId, "recordId");
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          final retrofit2.Response<ResponseBody> response = storageClient.getRecord(recordId.toString()).execute();
          if (response.code() != 200) {
            if (response.code() == 404)
              uiError(handleResult, new E3DBNotFoundException(recordId));
            else
              uiError(handleResult, E3DBException.find(response.code(), response.message()));
            return;
          }

          final JsonNode result = mapper.readTree(response.body().string());
          final JsonNode meta = result.get("meta");
          final byte[] key = getAccessKey(UUID.fromString(meta.get("writer_id").asText()),
            UUID.fromString(meta.get("user_id").asText()),
            clientId, meta.get("type").asText());
          try {
            uiValue(handleResult, makeR(key, meta, result.get("data"), crypto));
          } catch (ParseException e) {
            uiError(handleResult, e);
          } catch (UnsupportedEncodingException e) {
            uiError(handleResult, e);
          }
        } catch (final Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Get a list of records matching some criteria.
   *
   * <p>By default, results are limited to 50 records and do not include encrypted data (a separate
   * read must be made to get the data for a record).
   *
   * @param params The criteria to filter records by. Use the {@link QueryParamsBuilder} class to make this object.
   * @param handleResult If successful, returns a page of results. The {@link QueryResponse#last()} method can be used
   *                     in conjunction with the {@link QueryParamsBuilder#setAfter(long)} method to implement pagination.
   */
  public void query(final QueryParams params, final ResultHandler<QueryResponse> handleResult) {
    checkNotNull(params, "params");

    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          final QueryResponse queryResponse = doSearchRequest(params);
          uiValue(handleResult, queryResponse);
        } catch (final E3DBException e) {
          uiError(handleResult, e);
        } catch (final Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Give another client the ability to read records.
   *
   * <p>This operation gives read access for records of {@code type}, written by this client, to the
   * the recipient specified by {@code readerId}.
   *
   * @param type The type of records to grant access to.
   * @param readerId ID of client to give access to.
   * @param handleResult If successful, returns no useful information (except that the operation
   *                     completed).
   */
  public void share(final String type, final UUID readerId, final ResultHandler<Void> handleResult) {
    checkNotEmpty(type, "type");
    checkNotNull(readerId, "readerId");

    onBackground(new Runnable() {
      public void run() {
        try {
          if(getAccessKey(clientId, clientId, readerId, type) == null) {
            final retrofit2.Response<ResponseBody> clientInfo = shareClient.lookupClient(readerId).execute();
            if (clientInfo.code() == 404) {
              uiError(handleResult, new E3DBClientNotFoundException(readerId.toString()));
              return;
            } else if (clientInfo.code() != 200) {
              uiError(handleResult, E3DBException.find(clientInfo.code(), clientInfo.message()));
              return;
            }

            byte[] ak = getOwnAccessKey(type);
            JsonNode info = mapper.readTree(clientInfo.body().string());
            byte[] readerKey = decodeURL(info.get("public_key").get("curve25519").asText());
            setAccessKey(Client.this.clientId, Client.this.clientId, readerId, type, readerKey, ak);
          }

          retrofit2.Response<ResponseBody> shareResponse = shareClient.putPolicy(
            clientId.toString(),
            clientId.toString(),
            readerId.toString(),
            type,
            RequestBody.create(APPLICATION_JSON, allow)).execute();

          if(shareResponse.code() != 201)
            uiError(handleResult, E3DBException.find(shareResponse.code(), shareResponse.message()));
          else
            uiValue(handleResult, null);
        }
        catch(Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Remove permission for another client to read records.
   *
   * <p>This operation removes previously granted permission for the client specified by {@code readerId} to
   * read records, written by this client, of type {@code type}.
   *
   * @param type The type of records to remove access to.
   * @param readerId ID of client to remove access from.
   * @param handleResult If successful, returns no useful information (except that the operation
   *                     completed).
   */
  public void revoke(final String type, final UUID readerId, final ResultHandler<Void> handleResult) {
    checkNotEmpty(type, "type");
    checkNotNull(readerId, "readerId");

    onBackground(new Runnable() {
      public void run() {
        try {
          removeAccessKey(clientId, clientId, readerId, type);
          retrofit2.Response<ResponseBody> shareResponse = shareClient.putPolicy(
            clientId.toString(),
            clientId.toString(),
            readerId.toString(),
            type,
            RequestBody.create(APPLICATION_JSON, deny)).execute();

          if(shareResponse.code() != 201)
            uiError(handleResult, E3DBException.find(shareResponse.code(), shareResponse.message()));
          else
            uiValue(handleResult, null);
        } catch (Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Get a list of record types shared with this client.
   *
   * <p>This operation lists all record types shared with this client, as well as the client (writer)
   * sharing those records.
   * @param handleResult If successful, returns a list of records types shared with this client. The resulting list may be empty but never null.
   */
  public void getIncomingSharing(final ResultHandler<List<IncomingSharingPolicy>> handleResult) {
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          retrofit2.Response<ResponseBody> response = shareClient.getIncoming().execute();
          if(response.code() != 200) {
            uiError(handleResult, E3DBException.find(response.code(), response.message()));
            return;
          }

          JsonNode results = mapper.readTree(response.body().string());
          ArrayList<IncomingSharingPolicy> policies = new ArrayList<>(results.size());
          if(results.isArray()) {
            for(JsonNode policy : results) {
              String writer_name = policy.get("writer_name") == null ? "" : policy.get("writer_name").asText();
              String writer_id = policy.get("writer_id").asText();
              String record_type = policy.get("record_type").asText();
              policies.add(new IncomingSharingPolicy(UUID.fromString(writer_id), writer_name, record_type));
            }
          }

          uiValue(handleResult, policies);
        } catch (Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Get a list of record types shared by this client.
   *
   * <p>This operation returns a list of record types shared by this client, including the
   * client (reader) that the records are shared with.
   * @param handleResult If successful, returns a list of record types that this client has shared. The resulting list may be empty but will never be null.
   */
  public void getOutgoingSharing(final ResultHandler<List<OutgoingSharingPolicy>> handleResult) {
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          retrofit2.Response<ResponseBody> response = shareClient.getOutgoing().execute();
          if(response.code() != 200) {
            uiError(handleResult, E3DBException.find(response.code(), response.message()));
            return;
          }

          JsonNode results = mapper.readTree(response.body().string());
          ArrayList<OutgoingSharingPolicy> policies = new ArrayList<>(results.size());
          if(results.isArray()) {
            for(JsonNode policy : results) {
              String reader_name = policy.get("reader_name") == null ? "" : policy.get("reader_name").asText();
              String reader_id = policy.get("reader_id").asText();
              String record_type = policy.get("record_type").asText();
              policies.add(new OutgoingSharingPolicy(UUID.fromString(reader_id), reader_name, record_type));
            }
          }

          uiValue(handleResult, policies);
        } catch (Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }
}
