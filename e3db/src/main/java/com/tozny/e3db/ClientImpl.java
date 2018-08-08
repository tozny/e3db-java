/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3db;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.*;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import okio.ByteString;
import retrofit2.Retrofit;

import javax.net.ssl.*;

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
 * <p>The {@link Registration#register(String, String, String, ResultHandler)} method can be used to
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
 * updated with {@link #update(UpdateMeta, RecordData, Map, ResultHandler)}, and deleted with {@link #delete(UUID, String, ResultHandler)}.
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
 * <h1>Authorizers</h1>
 * This client can allow another client to share records on its behalf. That is, a client that writes records (the "writer") can allow
 * another client (the "authorizer") to share those records with any other client (the "reader"). This de-couples the production of
 * data from sharing the data.
 *
 * To grant authorizer permission, this client should call the {@link #addAuthorizer(UUID, String, ResultHandler)}) method. Permission is granted per record type; there is
 * no facility for sharing all records written by a client. The writer <b>must</b> call {@code addAuthorizer} before the authorizer can share records
 * on the writer's behalf.
 *
 * Permission to share records can be revoked by either of the {@code removeAuthorizer} methods. It is possible to remove permission for <b>all</b>
 * record types written by this client.
 *
 * Use the {@link #getAuthorizers(ResultHandler)} method to see all the client's authorized to share records on behalf of this client. Similarly,
 * as an authorizer, use the {@link #getAuthorizedBy(ResultHandler)} method to see all the writers that have given permission for this client
 * to share records.
 *
 * <h1>Sharing, cont'd</h1>
 * There is no difference between an authorizer sharing records on behalf of this client, or this client sharing records directly with
 * a reader. Removing the authorizer's permission to share records does <b>not</b> remove any sharing relationships set up by that
 * authorizer. The writer must remove each sharing relationship explicitly.
 *
 * To share records as an authorizer, use the {@link #shareOnBehalfOf(WriterId, String, UUID, ResultHandler)} method. Calling this method will grant permission
 * to reader specified to read the records written by the writer, of the given type.
 *
 * <h1>Local Encryption &amp; Decryption</h1>
 * The client instance has the ability to encrypt records for local storage, and to decrypt locally-stored records. Locally-encrypted records are
 * always encrypted with a key that can be shared with other clients via E3DB's sharing mechanism.
 *
 * <p>Local encryption (and decryption) requires two steps:
 *
 * <oL>
 *   <li>Create a 'writer key' (for encryption) or obtain a 'reader key' (for decryption).</li>
 *  <li>Call {@link #encryptRecord(String, RecordData, Map, EAKInfo)} (for a new document) or {@link #encryptExisting(LocalRecord, EAKInfo)}
 *  (for an existing {@link LocalRecord} instance); for decryption, call {@link #decryptExisting(EncryptedRecord, EAKInfo)}.</li>
 * </ol>
 *
 * <h1>Obtaining A Key for Local Encryption &amp; Decryption</h1>
 *
 * A writer key can be created by calling {@link #createWriterKey(String, ResultHandler)}; a 'reader key' can be obtained by calling
 * {@link #getReaderKey(UUID, UUID, String, ResultHandler)}. (Note that the client calling {@code getReaderKey} will only receive a key if the writer
 * of those records has given access to the calling client through the `share` operation.)
 *
 * <p>The 'writer key' and 'reader key' returned from the client are both {@link LocalEAKInfo} objects (an implementation of the {@link EAKInfo} interface), which can be persisted to storage via the {@link LocalEAKInfo#encode()}
 * method. Previously-persisted keys can be parsed using the {@link LocalEAKInfo#decode(String)} method.
 *
 * <p>However, note that {@code EAKInfo} objects are encrypted with the client's private key, meaning they cannot be decoded by any other client.
 *
 * <h1>Storing Encrypted Records Locally</h1>
 *
 * Use the {@link LocalEncryptedRecord#encode()} method to convert an encrypted record to a string for storage. You can use
 * the {@link LocalEncryptedRecord#decode(String)} method to convert an encoded record back to an {@code EncryptedRecord}
 * instance.
 *
 * <h1>Document Signing &amp; Verification</h1>
 *
 * <p>Every E3DB client created with this SDK is capable of signing
 * documents and verifying the signature associated with a document.
 * By attaching signatures to documents, clients can be confident in:
 *
 * <ul>
 *   <li>Document integrity - the document's contents have not been altered (because the signature will not match).</li>
 *   <li>Proof-of-authorship - The author of the document held the private signing key associated with the given public key
 *       when the document was created.</li>
 * </ul>
 *
 * <p>Use the {@link #sign(Signable)} method to sign a document. Note that {@link Record}, {@link LocalEncryptedRecord}, and {@link LocalRecord}
 * all implement the {@link Signable} interface, and thus can be signed.
 *
 * To verify a signed document, use the {@link #verify(SignedDocument, String)} method. Note that the {@link LocalEncryptedRecord} class
 * implements {@link SignedDocument} and thus always has a signature attached that can be verified.
 *
 * <h1>Reading &amp; Writing Large Files</h1>
 *
 * E3DB can compress and encrypt files for storage. Files are treated much like records, except the data for the file is
 * not included inline when downloading a record. Instead, a separate request for each file must be made to the {@link #readFile(UUID, File, ResultHandler)}
 * method.
 *
 * To write a file, use the {@link #writeFile(String, File, Map, ResultHandler)} method. The record type and plaintext meta
 * arguments behave the same as with normal records. The SDK will compress and encrypt the record in the same directory
 * that stores the plaintext file and will need at least twice as much free space as the size of the plaintext file. The
 * temporary encrypted file will always be deleted the upload finishes (or if an error occurs).
 *
 * To read a file, use the {@link #readFile(UUID, File, ResultHandler)} method (which is as yet not implemented). The SDK will
 * download the encrypted file to the same directory as the destination given. It will decrypt and decompress in-place, and write
 * the result to the destination file. Afterwards, the temporary encrypted file is deleted.
 */
public class ClientImpl implements Client {
  private static final OkHttpClient anonymousClient;
  private static final ObjectMapper mapper;
//  private static final okhttp3.logging.HttpLoggingInterceptor loggingInterceptor;
  private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");
  private static final MediaType APPLICATION_OCTET = MediaType.parse("application/octet-stream");
  private static final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
  private static final MediaType PLAIN_TEXT = MediaType.parse("text/plain");
  private static final Executor backgroundExecutor;
  private static final Executor uiExecutor;
  private static final String allowRead = "{\"allow\" : [ { \"read\": {} } ] }";
  private static final String denyRead = "{\"deny\" : [ { \"read\": {} } ] }";
  private static final String denyAuthorizer = "{\"deny\" : [ { \"authorizer\": {} } ] }";
  private static final String allowAuthorizer = "{\"allow\" : [ { \"authorizer\": {} } ] }";
  private final ConcurrentMap<EAKCacheKey, EAKEntry> eakCache = new ConcurrentHashMap<>();
  private final String apiKey;
  private final String apiSecret;
  private final UUID clientId;
  private final byte[] privateEncryptionKey;
  private final byte[] privateSigningKey;
  private final byte[] publicSigningKey;
  private final StorageAPI storageClient;
  private final ShareAPI shareClient;

  private static final Charset UTF8 = Charset.forName("UTF-8");

  static {
//    loggingInterceptor = new okhttp3.logging.HttpLoggingInterceptor(new okhttp3.logging.HttpLoggingInterceptor.Logger() {
//      @Override
//      public void log(String message) {
//        System.out.println(message);
//      }
//    }).setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY);
    anonymousClient = enableTLSv12(new OkHttpClient.Builder()
//      .addInterceptor(loggingInterceptor)
    ).build();

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
            thread.setName("E3DB background " + threadCount++);
            return thread;
          }
        });

    if (Platform.isAndroid()) {
      // Post results to UI thread
      uiExecutor = new Executor() {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable runnable) {
          handler.post(runnable);
        }
      };
    } else {
      // Post results to current thread (whatever that is)
      uiExecutor = new Executor() {
        @Override
        public void execute(Runnable runnable) {
          runnable.run();
        }
      };
    }

    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  /**
   * Enables TLS v1.2 when creating SSLSockets.
   * <p/>
   * For some reason, android supports TLS v1.2 from API 16, but enables it by
   * default only from API 20.
   * <p>
   *
   * Thanks to https://github.com/square/okhttp/issues/2372#issuecomment-244807676.
   *
   * @link https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
   * @see SSLSocketFactory
   */
  static class Tls12SocketFactory extends SSLSocketFactory {
    private static final String[] TLS_V12_ONLY = {TlsVersion.TLS_1_2.javaName()};

    final SSLSocketFactory delegate;

    public Tls12SocketFactory(SSLSocketFactory base) {
      this.delegate = base;
    }

    @Override
    public String[] getDefaultCipherSuites() {
      return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
      return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
      return patch(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
      return patch(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
      return patch(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      return patch(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
      return patch(delegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket patch(Socket s) {
      if (s instanceof SSLSocket) {
        ((SSLSocket) s).setEnabledProtocols(TLS_V12_ONLY);
      }
      return s;
    }
  }

  // From https://github.com/square/okhttp/issues/2372#issuecomment-244807676.
  private static OkHttpClient.Builder enableTLSv12(OkHttpClient.Builder client) {
    if (Platform.isAndroid() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < 21) {//Build.VERSION_CODES.LOLLIPOP_MR1) {
      try {
        // From javadoc for okhttp3.OkHttpClient.Builder.sslSocketFactory(javax.net.ssl.SSLSocketFactory, javax.net.ssl.X509TrustManager)
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
          throw new IllegalStateException("Unexpected default trust managers:"
                  + Arrays.toString(trustManagers));
        }
        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
        SSLContext sc = SSLContext.getInstance(TlsVersion.TLS_1_2.javaName());
        sc.init(null, new TrustManager[] { trustManager }, null);
        client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), trustManager);

        ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .build();

        List<ConnectionSpec> specs = Arrays.asList(cs,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT);
        client.connectionSpecs(specs);
      } catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException exc) {
        throw new RuntimeException(exc);
      }
    }

    return client;
  }

  ClientImpl(String apiKey, String apiSecret, UUID clientId, URI host, byte[] privateKey, byte[] privateSigningKey, CertificatePinner certificatePinner) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.clientId = clientId;
    this.privateEncryptionKey = privateKey;
    this.privateSigningKey = privateSigningKey;
    if(this.privateSigningKey != null)
      publicSigningKey = Platform.crypto.getPublicSigningKey(privateSigningKey);
    else
      publicSigningKey = null;

    OkHttpClient.Builder clientBuilder = enableTLSv12(new OkHttpClient.Builder()
//            .addInterceptor(loggingInterceptor)
            .addInterceptor(new TokenInterceptor(apiKey, apiSecret, host, certificatePinner)));

    if (certificatePinner != null)
      clientBuilder.certificatePinner(certificatePinner);

    Retrofit build = new Retrofit.Builder()
            .callbackExecutor(uiExecutor)
            .client(clientBuilder.build())
            .baseUrl(host.resolve("/").toString())
            .build();

    storageClient = build.create(StorageAPI.class);
    shareClient = build.create(ShareAPI.class);

  }

  ClientImpl(String apiKey, String apiSecret, UUID clientId, URI host, byte[] privateKey, byte[] privateSigningKey) {
    this(apiKey, apiSecret, clientId, host, privateKey, privateSigningKey, null);
  }

  private static class EAKCacheKey {
    private final UUID writerId;
    private final UUID userId;
    private final String type;

    private EAKCacheKey(UUID writerId, UUID userId, String type) {
      this.writerId = writerId;
      this.userId = userId;
      this.type = type;
    }

    public static EAKCacheKey fromMeta(ClientMeta meta) {
      return new EAKCacheKey(meta.writerId(), meta.userId(), meta.type());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      EAKCacheKey eakCacheKey = (EAKCacheKey) o;

      if (!writerId.equals(eakCacheKey.writerId)) return false;
      if (!userId.equals(eakCacheKey.userId)) return false;
      if (!type.equals(eakCacheKey.type)) return false;

      return true;
    }
    @Override
    public int hashCode() {
      int result = writerId.hashCode();
      result = 31 * result + userId.hashCode();
      result = 31 * result + type.hashCode();
      return result;
    }

  }

  private static class EAKEntry {
    private final byte[] ak;
    private final LocalEAKInfo eakInfo;

    private EAKEntry(byte[] ak, LocalEAKInfo eakInfo) {
      this.ak = ak;
      this.eakInfo = eakInfo;
    }

    public byte[] getAk() {
      return ak;
    }

    public LocalEAKInfo getEAK() {
      return eakInfo;
    }
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

  private static class AP implements AuthorizerPolicy {
    private final UUID authorizerId;
    private final UUID writerId;
    private final UUID userId;
    private final String recordType;
    private final UUID authorizedBy;

    public AP(UUID authorizerId, UUID writerId, UUID userId, String recordType, UUID authorizedBy) {
      this.authorizerId = authorizerId;
      this.writerId = writerId;
      this.userId = userId;
      this.recordType = recordType;
      this.authorizedBy = authorizedBy;
    }

    @Override
    public UUID authorizerId() {
      return authorizerId;
    }

    @Override
    public UUID writerId() {
      return writerId;
    }

    @Override
    public UUID userId() {
      return userId;
    }

    @Override
    public String recordType() {
      return recordType;
    }

    @Override
    public UUID authorizedBy() {
      return authorizedBy;
    }
  }

  private void onBackground(Runnable runnable) {
    backgroundExecutor.execute(runnable);
  }

  private LocalEncryptedRecord makeEncryptedRecord(EAKInfo eakInfo, Map<String, String> data, ClientMeta clientMeta) {
    if(ClientImpl.this.privateSigningKey == null)
      throw new IllegalStateException("Client must have a signing key to encrypt locally.");

    byte[] ak = decryptLocalEAKInfo(eakInfo);
    return new LocalEncryptedRecord(encryptObject(ak, data), clientMeta, sign(new LocalRecord(data, clientMeta)).signature());
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

  private static class R implements Record {
    private final Map<String, String> data;
    private final RecordMeta meta;

    public R(Map<String, String> data, RecordMeta meta) {
      this.data = data;
      this.meta = meta;
    }

    private static RecordMeta getRecordMeta(JsonNode rawMeta) throws ParseException {
      UUID recordId = UUID.fromString(rawMeta.get("record_id").asText());
      UUID writerId = UUID.fromString(rawMeta.get("writer_id").asText());
      UUID userId = UUID.fromString(rawMeta.get("user_id").asText());
      Date created = iso8601.parse(rawMeta.get("created").asText());
      Date lastModified = iso8601.parse(rawMeta.get("last_modified").asText());
      String version = rawMeta.get("version").asText();
      String type = rawMeta.get("type").asText();
      JsonNode plain = rawMeta.has("plain") ? rawMeta.get("plain") : mapper.createObjectNode();
      FileMeta file = rawMeta.has("file_meta") ? getFileMeta(rawMeta.get("file_meta")) : null;

      return new M(recordId, writerId, userId, version, created, lastModified, type, plain, file);
    }

    private static FM getFileMeta(JsonNode fileMeta) {
      return new FM(fileMeta.has("size") ? fileMeta.get("size").asLong() : null,
          fileMeta.has("file_url") ? fileMeta.get("file_url").asText() : null,
          fileMeta.has("file_name") ? fileMeta.get("file_name").asText() : null,
          fileMeta.has("checksum") ? fileMeta.get("checksum").asText() : null,
          fileMeta.has("compression") ? Compression.fromType(fileMeta.get("compression").asText()) : null);
    }

    public static R makeLocal(JsonNode rawMeta) {
      try {
        return new R(new HashMap<String, String>(), getRecordMeta(rawMeta));
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }

    private static Record makeLocal(byte[] accessKey, JsonNode rawMeta, JsonNode fields, byte[] signature, byte[] publicSigningKey) throws ParseException, E3DBVerificationException {
      RecordMeta meta = getRecordMeta(rawMeta);
      Map<String, String> encryptedFields = new HashMap<>();
      Iterator<String> keys = fields.fieldNames();
      while (keys.hasNext()) {
        String key = keys.next();
        encryptedFields.put(key, fields.get(key).asText());
      }

      R record = new R(decryptObject(accessKey, encryptedFields), meta);

      if (signature != null && publicSigningKey != null) {
        boolean verified = Platform.crypto.verify(new Signature(signature),
            record.toSerialized().getBytes(UTF8),
            publicSigningKey);

        if (!verified)
          throw new E3DBVerificationException(clientMeta(meta));
      }

      return record;
    }

    @Override
    public RecordMeta meta() {
      return meta;
    }

    @Override
    public Map<String, String> data() {
      return data;
    }

    @Override
    public String toSerialized() {
      return new LocalRecord(data, clientMeta(meta)).toSerialized();
    }
  }

  private static Record makeR(JsonNode rawMeta) throws ParseException {
    return R.makeLocal(rawMeta);
  }

  private static Record makeR(byte[] accessKey, JsonNode rawMeta, JsonNode fields, byte[] signature, byte[] publicSigningKey) throws ParseException, UnsupportedEncodingException, E3DBVerificationException {
    return R.makeLocal(accessKey, rawMeta, fields, signature, publicSigningKey);
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

  private static class TokenInterceptor implements Interceptor {
    private final URI host;
    private final AuthAPI authClient;
    private final String basic;
    private String token = null;
    private Date replaceAfter = new Date(0L);

    private TokenInterceptor(String apiKey, String apiSecret, URI host) {
      this(apiKey, apiSecret, host, null);
    }

    private TokenInterceptor(String apiKey, String apiSecret, URI host, CertificatePinner certificatePinner) {
      this.host = host;
      this.basic = new StringBuffer("Basic ").append(ByteString.of(new StringBuffer(apiKey).append(":").append(apiSecret).toString().getBytes(UTF8)).base64()).toString();

      OkHttpClient.Builder clientBuilder = enableTLSv12(new OkHttpClient.Builder());

      if (certificatePinner != null) {
        clientBuilder.certificatePinner(certificatePinner);
      }

      this.authClient = new Retrofit.Builder()
        .client(clientBuilder.build())
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

  private class SD<T extends Signable> implements SignedDocument<T> {
    private final T document;
    private final String signature;

    private SD(T document, String signature) {
      this.document = document;
      this.signature = signature;
    }

    @Override
    public String signature() {
      return signature;
    }

    @Override
    public T document() {
      return document;
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
      Record record;
      JsonNode queryRecord = currPage.get(currRow);
      JsonNode access_key = queryRecord.get("access_key");
      if(access_key != null && access_key.isObject()) {
        record = makeR(Platform.crypto.decryptBox(
                CipherWithNonce.decode(access_key.get("eak").asText()),
                decodeURL(access_key.get("authorizer_public_key").get("curve25519").asText()),
                privateEncryptionKey),
                queryRecord.get("meta"),
                queryRecord.get("record_data"),
                null,
                null
        );
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

  private static Map<String, String> encryptObject(byte[] accessKey, Map<String, String> fields)  {
    Map<String, String> encFields = new HashMap<>();
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      byte[] dk = Platform.crypto.newSecretKey();

      String encField = new StringBuilder(Platform.crypto.encryptSecretBox(dk, accessKey).toMessage()).append(".")
        .append(Platform.crypto.encryptSecretBox(entry.getValue().getBytes(UTF8), dk).toMessage()).toString();
      encFields.put(entry.getKey(), encField);
    }
    return encFields;
  }

  private static Map<String, String> decryptObject(byte[] accessKey, Map<String, String> record) {
    Map<String, String> decryptedFields = new HashMap<>();
    for(Map.Entry<String,String> entry : record.entrySet()) {
      ER er = new ER(entry.getValue());
      byte[] dk = Platform.crypto.decryptSecretBox(er.edk, accessKey);
      String value = new String(Platform.crypto.decryptSecretBox(er.ef, dk), UTF8);
      decryptedFields.put(entry.getKey(), value);
    }
    return decryptedFields;
  }

  private static List<AuthorizerPolicy> getAuthorizerPolicies(JsonNode list) {
    List<AuthorizerPolicy> authorizers = new ArrayList<>(list.size());
    for (int i = 0; i < list.size(); i += 1) {
      JsonNode item = list.get(i);
      authorizers.add(new AP(UUID.fromString(item.get("authorizer_id").asText()),
          UUID.fromString(item.get("writer_id").asText()),
          UUID.fromString(item.get("user_id").asText()),
          item.get("record_type").asText(),
          UUID.fromString(item.get("authorized_by").asText())));
    }
    return authorizers;
  }

  private byte[] getOwnAccessKey(String type) throws E3DBException, IOException {
    byte[] existingAK = getAccessKey(this.clientId, this.clientId, this.clientId, type);
    if(existingAK != null) {
      return existingAK;
    }
    else {
      // Create new AK
      try {
        setAccessKey(this.clientId, this.clientId, this.clientId, type, Platform.crypto.getPublicKey(this.privateEncryptionKey), Platform.crypto.newSecretKey(), this.clientId, this.publicSigningKey);
      }
      catch(E3DBConflictException ex) { }

      byte[] newAK = getAccessKey(this.clientId, this.clientId, this.clientId, type);
      if(newAK == null)
        throw new RuntimeException("Unable to create own AK for " + this.clientId + " and type '" + type + "'");

      return newAK;
    }
  }

  private void removeAccessKey(UUID writerId, UUID userId, UUID readerId, String type) throws IOException, E3DBException {
    retrofit2.Response<ResponseBody> response = storageClient.deleteAccessKey(writerId.toString(), userId.toString(), readerId.toString(), type).execute();
    if(response.code() != 204) {
      throw E3DBException.find(response.code(), response.message());
    }

    EAKCacheKey cacheEntry = new EAKCacheKey(writerId, userId, type);
    eakCache.remove(cacheEntry);
  }

  private byte[] getAccessKey(UUID writerId, UUID userId, UUID readerId, String type) throws E3DBException, IOException {
    EAKEntry eak = getEAK(writerId, userId, readerId, type);
    return eak == null ? null : eak.ak;
  }

  private EAKEntry getEAK(UUID writerId, UUID userId, UUID readerId, String type) throws IOException, E3DBException {
    EAKCacheKey cacheEntry = new EAKCacheKey(writerId, userId, type);
    EAKEntry cachedEak = eakCache.get(cacheEntry);

    if(cachedEak != null)
      return cachedEak;
    else {
      retrofit2.Response<ResponseBody> response = storageClient.getAccessKey(writerId.toString(), userId.toString(), readerId.toString(), type).execute();
      if (response.code() == 404) {
        return null;
      } else if (response.code() != 200) {
        throw E3DBException.find(response.code(), response.message());
      } else {
        JsonNode eakResponse = mapper.readTree(response.body().string());
        String eak = eakResponse.get("eak").asText();
        String publicKey = eakResponse.get("authorizer_public_key").get("curve25519").asText();
        UUID authorizerId = UUID.fromString(eakResponse.get("authorizer_id").asText());

        JsonNode signer_signing_key = eakResponse.get("signer_signing_key");
        UUID signerId = null;
        String signerPublicKey = null;
        if (signer_signing_key != null) {
          signerId = UUID.fromString(eakResponse.get("signer_id").asText());
          signerPublicKey = signer_signing_key.get("ed25519").asText();
        }

        byte[] ak = Platform.crypto.decryptBox(CipherWithNonce.decode(eak),
          decodeURL(publicKey),
          this.privateEncryptionKey);
        EAKEntry entry = new EAKEntry(ak, new LocalEAKInfo(eak, publicKey, authorizerId, signerId, signerPublicKey));
        eakCache.put(cacheEntry, entry);
        return entry;
      }
    }
  }

  private void setAccessKey(UUID writerId, UUID userId, UUID readerId, String type, byte[] readerKey, byte[] ak, UUID signerId, byte[] signerPublicKey) throws E3DBException, IOException {
    EAKCacheKey cacheEntry = new EAKCacheKey(writerId, userId, type);
    String encryptedAk = Platform.crypto.encryptBox(ak, readerKey, this.privateEncryptionKey).toMessage();

    Map<String, String> doc = new HashMap<>();
    doc.put("eak", encryptedAk);

    RequestBody body = RequestBody.create(APPLICATION_JSON, mapper.writeValueAsString(doc));
    retrofit2.Response<ResponseBody> response = storageClient.putAccessKey(writerId.toString(), userId.toString(), readerId.toString(), type, body).execute();
    if (response.code() != 201) {
      throw E3DBException.find(response.code(), response.message());
    }

    eakCache.put(cacheEntry, new EAKEntry(ak, new LocalEAKInfo(encryptedAk, encodeURL(readerKey), this.clientId, signerId, encodeURL(signerPublicKey))));
  }

  private byte[] decryptLocalEAKInfo(EAKInfo eakInfo) {
    return Platform.crypto.decryptBox(CipherWithNonce.decode(eakInfo.getKey()), decodeURL(eakInfo.getPublicKey()), this.privateEncryptionKey);
  }

  private static ClientMeta clientMeta(RecordMeta meta) {
    return new LocalMeta(meta.writerId(), meta.userId(), meta.type(), meta.plain());
  }

  private static String getMD5(File encryptedFile) throws NoSuchAlgorithmException, IOException {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    FileChannel channel = new FileInputStream(encryptedFile).getChannel();
    ByteBuffer buffer = ByteBuffer.wrap(new byte[Platform.crypto.getBlockSize()]);
    try {
      for (int amt = channel.read(buffer); amt != -1; amt = channel.read(buffer)) {
        buffer.flip();
        digest.update(buffer);
        buffer.clear();
      }
    } finally {
      channel.close();
    }

    return Base64.encode(digest.digest());
  }

  private static LocalEAKInfo toLocalEAK(EAKInfo eakInfo) {
    if(eakInfo instanceof LocalEAKInfo)
      return (LocalEAKInfo) eakInfo;
    else
      return new LocalEAKInfo(eakInfo.getKey(), eakInfo.getPublicKey(), eakInfo.getAuthorizerId(), eakInfo.getSignerId(), eakInfo.getSignerSigningKey());
  }

  private FileMeta makeFileMeta(String encryptedFileMD5, long encryptedFileSize, Compression compression, String fileName) {
    return new FM(encryptedFileSize, null, fileName, encryptedFileMD5, compression);
  }

  private Map<String, Object> makeRecordMetaMap(String type, Map<String, String> plain, FileMeta fileMeta) {
    Map<String, Object> meta = new HashMap<>();
    meta.put("writer_id", clientId.toString());
    meta.put("user_id", clientId.toString());
    meta.put("type", type.trim());

    if (plain != null)
      meta.put("plain", plain);

    if(fileMeta != null) {
      HashMap<String, Object> fm = new HashMap<>();
      fm.put("checksum", fileMeta.checksum());
      fm.put("compression", fileMeta.compression().getType());
      fm.put("size", fileMeta.size());
      fm.put("file_name", fileMeta.fileName());
      meta.put("file_meta", fm);
    }
    return meta;
  }

  private static class ClientInfo {
    private final byte[] curve25519;
    private final byte[] ed25519;
    private final UUID clientId;


    public ClientInfo(byte[] curve25519, byte[] ed25519, UUID clientId) {
      this.curve25519 = curve25519;
      this.ed25519 = ed25519;
      this.clientId = clientId;
    }

    public byte[] getCurve25519() {
      return curve25519;
    }

    public byte[] getEd25519() {
      return ed25519;
    }

    public UUID getClientId() {
      return clientId;
    }
  }

  private ClientInfo getClientInfo(UUID clientId) throws IOException, E3DBException {
    final retrofit2.Response<ResponseBody> clientInfo = shareClient.lookupClient(clientId).execute();
    if (clientInfo.code() == 404) {
      throw new E3DBClientNotFoundException(clientId.toString());
    } else if (clientInfo.code() != 200) {
      throw E3DBException.find(clientInfo.code(), clientInfo.message());
    }
    JsonNode info = mapper.readTree(clientInfo.body().string());
    String curve25519 = info.get("public_key").get("curve25519").asText();
    String ed25519;
    {
      if(info.get("signing_key") != null && info.get("signing_key").get("ed25519") != null)
        ed25519 = info.get("signing_key").get("ed25519").asText();
      else
        ed25519 = null;
    }
    String cId = info.get("client_id").asText();
    return new ClientInfo(Base64.decodeURL(curve25519), ed25519 == null ? null : Base64.decodeURL(ed25519), UUID.fromString(cId));
  }

  private void sharing(final String type, final UUID readerId, final UUID writerId, final ResultHandler<Void> handleResult) {
    onBackground(new Runnable() {
      public void run() {
        try {
          final byte[] readerKey = getClientInfo(readerId).getCurve25519();
          final byte[] writerSigningKey;
          {
            if(writerId.equals(ClientImpl.this.clientId)) {
              writerSigningKey = ClientImpl.this.publicSigningKey;
            }
            else {
              ClientInfo writerInfo = getClientInfo(writerId);
              writerSigningKey = writerInfo.getEd25519();
            }
          }

          try {
            byte[] ak = getAccessKey(writerId, writerId, ClientImpl.this.clientId, type);
            if(ak == null) {
              setAccessKey(writerId, writerId, ClientImpl.this.clientId, type, readerKey, Platform.crypto.newSecretKey(), writerId, writerSigningKey);
              ak = getAccessKey(writerId, writerId, ClientImpl.this.clientId, type);
            }
            setAccessKey(writerId, writerId, readerId, type, readerKey, ak, writerId, writerSigningKey);
          } catch (E3DBConflictException ex) {
            // no-op
          }

          retrofit2.Response<ResponseBody> shareResponse = shareClient.putPolicy(
              writerId.toString(),
              writerId.toString(),
              readerId.toString(),
              type,
              RequestBody.create(APPLICATION_JSON, allowRead)).execute();

          if (shareResponse.code() != 201)
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

  private void revoking(final String type, final UUID writerId, final UUID readerId, final ResultHandler<Void> handleResult) {
    onBackground(new Runnable() {
      public void run() {
        try {
          removeAccessKey(writerId, writerId, readerId, type);
          retrofit2.Response<ResponseBody> shareResponse = shareClient.putPolicy(
              writerId.toString(),
              writerId.toString(),
              readerId.toString(),
              type,
              RequestBody.create(APPLICATION_JSON, denyRead)).execute();

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
   * @deprecated Use {@link #generateKey()}  instead.
   *
   * @return A Base64URL-encoded string representing the new private key.
   */
  @Deprecated
  public static String newPrivateKey() {
    return encodeURL(Platform.crypto.newPrivateKey());
  }

  /**
   * Generates a private key that can be used for Curve25519
   * public-key encryption.
   *
   * <p>The associated public key can be retrieved using {@link #getPublicKey(String)}.
   *
   * @return A Base64URL-encoded string representing the new private key.
   *
   */
  public static String generateKey() {
    return encodeURL(Platform.crypto.newPrivateKey());
  }

  /**
   * Gets the public key for the given private key.
   *
   * <p>The private key must be a Base64URL-encoded string.
   *
   * The returned value represents the key as a Base64URL-encoded string.
   *
   * @param privateKey Curve25519 private key as a Base64URL-encoded string.
   * @return The public key portion of the private key, as a Base64URL-encoded string.
   */
  public static String getPublicKey(String privateKey) {
    checkNotEmpty(privateKey, "privateKey");
    byte[] arr = decodeURL(privateKey);
    checkNotEmpty(arr, "privateKey");
    return encodeURL(Platform.crypto.getPublicKey(arr));
  }

  /**
   * Generates a private key that can be used for Ed25519
   * public-key signatures.
   *
   * <p>The associated public key can be retrieved using {@link #getPublicKey(String)}.
   *
   * @return A Base64URL-encoded string representing the new private key.
   *
   */
  public static String generateSigningKey() {
    return encodeURL(Platform.crypto.newPrivateSigningKey());
  }

  /**
   * Gets the public signing key for the given private key.
   *
   * <p>The private key must be a Base64URL-encoded string.
   *
   * The returned value represents the key as a Base64URL-encoded string.
   *
   * @param privateSigningKey Ed25519 private key as a Base64URL-encoded string.
   * @return The public key portion of the private key, as a Base64URL-encoded string.
   */
  public static String getPublicSigningKey(String privateSigningKey) {
    checkNotEmpty(privateSigningKey, "privateSigningKey");
    byte[] arr = decodeURL(privateSigningKey);
    checkNotEmpty(arr, "privateSigningKey");
    return encodeURL(Platform.crypto.getPublicSigningKey(arr));
  }

  /**
   * The ID of this client.
   *
   * @return clientId.
   */
  @Override
  public UUID clientId() {
    return clientId;
  }


  /**
   * Write the given file to E3DB. Intended for files from 5MB up to 5GB in size. The contents of the file are
   * encrypted before being uploaded.
   *
   * @param type Type of the record. Cannot be {@code null} or blank.
   * @param file Path to the file to upload. Cannot be {@code null}.
   * @param plain Plaintext meta associated with the file. Can be {@code null}.
   * @param handleResult Handles the result of the write.
   */
  @Override
  public void writeFile(final String type, final File file, final Map<String, String> plain, final ResultHandler<RecordMeta> handleResult) {
    checkNotEmpty(type, "type");
    checkNotNull(file, "file");
    if(plain != null && plain.size() > 0)
      checkMap(plain, "plain");

    final File absFile = file.getAbsoluteFile();
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          final byte[] ownAK = getOwnAccessKey(type);
          File encryptedFile = Platform.crypto.encryptFile(absFile, ownAK);
          FileMeta fileMeta = makeFileMeta(getMD5(encryptedFile), encryptedFile.length(), Compression.RAW, absFile.getName());
          Map<String, Object> meta = makeRecordMetaMap(type, plain, fileMeta);

          Map<String, Object> record = new HashMap<>();
          record.put("meta", meta);
          record.put("data", new HashMap<>());

          EXIT: {
            retrofit2.Response<ResponseBody> postResponse = storageClient.writeFile(RequestBody.create(APPLICATION_JSON, mapper.writeValueAsString(record))).execute();
            if (postResponse.code() != 202) {
              uiError(handleResult, E3DBException.find(postResponse.code(), postResponse.message()));
              break EXIT;
            }

            final JsonNode pendingFile = mapper.readTree(postResponse.body().string());
            UUID pendingFileID = UUID.fromString(pendingFile.get("id").asText());
            String destURL = pendingFile.get("file_url").asText();

            try {
              Response putFileResponse = anonymousClient.newCall(new Request.Builder()
                                     .url(destURL)
                                     .header("Content-MD5", fileMeta.checksum())
                                     .put(RequestBody.create(APPLICATION_OCTET, encryptedFile))
                                     .build()).execute();
              if (putFileResponse.code() != 200) {
                uiError(handleResult, E3DBException.find(putFileResponse.code(), putFileResponse.body().string()));
                break EXIT;
              }

              retrofit2.Response<ResponseBody> commitResponse = storageClient.commitFile(pendingFileID.toString()).execute();
              if (commitResponse.code() != 201) {
                uiError(handleResult, E3DBException.find(commitResponse.code(), commitResponse.message()));
                break EXIT;
              }

              uiValue(handleResult, R.getRecordMeta(mapper.readTree(commitResponse.body().string()).get("meta")));
            } finally {
              encryptedFile.delete();
            }
          }
        } catch (Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Read the file associated with the given record from the server.
   *
   * @param recordId ID of the record. Cannot be {@code null}.
   * @param dest Destination to write the decrypted file to. If the file exists, it will be truncated.
   * @param handleResult Handles the result of the operation. If the operation completes successfully, the
   *                     destination will hold the contents of the unencrypted file.
   */
  @Override
  public void readFile(UUID recordId, File dest, ResultHandler<RecordMeta> handleResult) {
    throw new IllegalStateException("Not yet implemented.");
  }

  /**
   * Write a new record.
   *
   * @param type Describes the type of the record (e.g., "contact_info", "credit_card", etc.).
   * @param fields Values to encrypt and store.
   * @param plain Additional, user-defined metadata that will <b>NOT</b> be encrypted. Can be null.
   * @param handleResult Result of the operation. If successful, returns the newly written record .
   */
  @Override
  public void write(final String type, final RecordData fields, final Map<String, String> plain, final ResultHandler<Record> handleResult) {
    checkNotEmpty(type, "type");
    checkNotNull(fields, "fields");
    if(plain != null && plain.size() > 0)
      checkMap(plain, "plain");

    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          final byte[] ownAK = getOwnAccessKey(type);
          Map<String, String> encFields = encryptObject(ownAK, fields.getCleartext());
          Map<String, Object> meta = makeRecordMetaMap(type, plain, null);

          Map<String, Object> record = new HashMap<>();
          record.put("meta", meta);
          record.put("data", encFields);

          String content = mapper.writeValueAsString(record);
          final retrofit2.Response<ResponseBody> response = storageClient.writeRecord(RequestBody.create(APPLICATION_JSON, content)).execute();
          if (response.code() == 201) {
            JsonNode result = mapper.readTree(response.body().string());
            uiValue(handleResult,
                makeR(
                    ownAK,
                    result.get("meta"),
                    result.get("data"),
                    null,
                    ClientImpl.this.publicSigningKey));
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
   * <p>The {@code updateMeta} argument is only used to obtain
   * information about the record to replace. No metadata updates by
   * the client are allowed.
   * @param updateMeta Metadata describing the record to update. Can be obtained
   *                   from an existing {@link RecordMeta} instance using {@link LocalUpdateMeta#fromRecordMeta(RecordMeta)}.
   * @param fields Field names and values. Wrapped in a {@link
   *               RecordData} instance to prevent confusing with the
   *               {@code plain} parameter.
   * @param plain Any metadata associated with the record that will
   *              <b>NOT</b> be encrypted. If {@code null}, existing
   *              metadata will be removed.
   * @param handleResult If the update succeeds, returns the updated
   *                     record. If the update fails due to a version
   *                     conflict, the value passed to the {@link
   *                     ResultHandler#handle(Result)}} method return
   *                     an instance of {@link E3DBVersionException}
   *                     when {@code asError().error()} is called.
   */
  @Override
  public void update(final UpdateMeta updateMeta, final RecordData fields, final Map<String, String> plain, final ResultHandler<Record> handleResult) {
    checkNotNull(updateMeta, "updateMeta");
    checkNotNull(fields, "fields");
    if(plain != null && plain.size() > 0)
      checkMap(plain, "plain");

    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          UUID id = updateMeta.getRecordId();
          final byte[] ownAK = getOwnAccessKey(updateMeta.getType());
          Map<String, String> encFields = encryptObject(ownAK, fields.getCleartext());
          Map<String, Object> meta = makeRecordMetaMap(updateMeta.getType(), plain, null);

          Map<String, Object> fields = new HashMap<>();
          fields.put("meta", meta);
          fields.put("data", encFields);

          retrofit2.Response<ResponseBody> response = storageClient.updateRecord(id.toString(), updateMeta.getVersion(), RequestBody.create(APPLICATION_JSON, mapper.writeValueAsString(fields))).execute();
          if (response.code() == 409) {
            uiError(handleResult, new E3DBVersionException(id, updateMeta.getVersion()));
          } else if (response.code() == 200) {
            JsonNode result = mapper.readTree(response.body().string());
            uiValue(handleResult,
                    makeR(ownAK,
                            result.get("meta"),
                            result.get("data"),
                            null,
                            null));
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
  @Override
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
  @Override
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

          JsonNode result = mapper.readTree(response.body().string());
          JsonNode meta = result.get("meta");
          EAKEntry eak = getEAK(UUID.fromString(meta.get("writer_id").asText()),
                  UUID.fromString(meta.get("user_id").asText()),
                  clientId, meta.get("type").asText());

          if(eak == null) {
            uiError(handleResult, new E3DBUnauthorizedException("Can't read records of type " + meta.get("type").asText()));
            return;
          }

          uiValue(handleResult,
                  makeR(eak.ak, meta, result.get("data"), null, null));
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
  @Override
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
   * Share records written by the given writer with the given reader.
   *
   * <p>This method should be called by a client authorized to share on behalf of the given writer.</p>
   *
   * @param writer ID of the client that produces the records.
   * @param type The type of records to grant access to.
   * @param readerId ID of client which will be allowed to read the records.
   * @param handleResult If successful, returns no useful information (except that the operation
   *                     completed).
   */
  @Override
  public void shareOnBehalfOf(final WriterId writer, final String type, final UUID readerId, final ResultHandler<Void> handleResult) {
    checkNotNull(writer, "writer");
    checkNotNull(readerId, "readerId");
    checkNotEmpty(type, "type");

    sharing(type, readerId, writer.getWriterId(), handleResult);
  }

  /**
   * Give another client the ability to read records.
   *
   * <p>This operation gives read access for records of {@code type}, written by this client, to the
   * the recipient specified by {@code readerId}.
   *
   * @param type The type of records to grant access to.
   * @param readerId ID of client which will be allowed to read the records.
   * @param handleResult If successful, returns no useful information (except that the operation
   *                     completed).
   */
  @Override
  public void share(final String type, final UUID readerId, final ResultHandler<Void> handleResult) {
    checkNotEmpty(type, "type");
    checkNotNull(readerId, "readerId");

    sharing(type, readerId, ClientImpl.this.clientId, handleResult);
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
  @Override
  public void revoke(final String type, final UUID readerId, final ResultHandler<Void> handleResult) {
    checkNotEmpty(type, "type");
    checkNotNull(readerId, "readerId");

    revoking(type, clientId, readerId, handleResult);
  }

  /**
   * Remove permission for the given reader to read records written by the writer.
   *
   * <p>This method should be called by a client authorized to share on behalf of the writer.
   *
   * @param writerId Writer that produces the records
   * @param type Record type to revoke permission for.
   * @param readerId Reader who's permission will be revoked.
   * @param handleResult If successful, returns no useful information (except that the operation
   *                     completed).
   */
  @Override
  public void revokeOnBehalfOf(final WriterId writerId, final String type, final UUID readerId, final ResultHandler<Void> handleResult) {
    checkNotEmpty(type, "type");
    checkNotNull(readerId, "readerId");
    checkNotNull(writerId, "writerId");

    revoking(type, writerId.getWriterId(), readerId, handleResult);
  }

  /**
   * Get a list of record types shared with this client.
   *
   * <p>This operation lists all record types shared with this client, as well as the client (writer)
   * sharing those records.
   * @param handleResult If successful, returns a list of records types shared with this client. The resulting list may be empty but never null.
   */
  @Override
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
  @Override
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

  /**
   * Creates (or retrieves) a key that can be used to locally encrypt records.
   *
   * @param type Type of the records to encrypt.
   * @param handleResult Handle the LocalEAKInfo object retrieved.
   */
  @Override
  public void createWriterKey(final String type, final ResultHandler<LocalEAKInfo> handleResult) {
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          try {
            setAccessKey(ClientImpl.this.clientId,
                ClientImpl.this.clientId,
                ClientImpl.this.clientId,
                type,
                Platform.crypto.getPublicKey(ClientImpl.this.privateEncryptionKey),
                Platform.crypto.newSecretKey(),
                ClientImpl.this.clientId,
                ClientImpl.this.publicSigningKey);
          } catch (E3DBConflictException e) {
            // no-op
          }

          EAKEntry eak = getEAK(ClientImpl.this.clientId, ClientImpl.this.clientId, ClientImpl.this.clientId, type);
          if (eak == null)
            uiError(handleResult, new RuntimeException("Can't create writer key for " + type));
          else
            uiValue(handleResult, eak.eakInfo);
        } catch (Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Retrieve a key for reading a shared record.
   * @param writerId ID of the client that wrote the record.
   * @param userId ID of the user associated with the record (normally equal to {@code writerId}).
   * @param type Type of record that was shared.
   * @param handleResult Handle the LocalEAKInfo object retrieved.
   */
  @Override
  public void getReaderKey(final UUID writerId, final UUID userId, final String type, final ResultHandler<LocalEAKInfo> handleResult) {
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          EAKEntry eak = getEAK(writerId, userId, ClientImpl.this.clientId, type);
          if(eak == null)
            uiError(handleResult, new E3DBException("Access key not found."));
          else
            uiValue(handleResult, eak.eakInfo);
        }
        catch(Throwable e) {
          uiError(handleResult, e);
        }
      }
    });
  }

  /**
   * Decrypt a locally-encrypted record.
   *
   * @param record The record to decrypt.
   * @param eakInfo The key to use for decrypting.
   * @throws E3DBVerificationException Thrown when verification of the signature fails.
   * @return The decrypted record.
   */
  @Override
  public LocalRecord decryptExisting(EncryptedRecord record, EAKInfo eakInfo) throws E3DBVerificationException {
    if (eakInfo.getSignerSigningKey() == null || eakInfo.getSignerSigningKey().isEmpty())
      throw new IllegalStateException("eakInfo cannot be used to verify the record as it has no public signing key.");

    byte[] ak = decryptLocalEAKInfo(eakInfo);

    Map<String, String> plainRecord = decryptObject(ak, record.document().data());

    if(!verify(new SD<>(new LocalRecord(plainRecord, record.document().meta()),
        record.signature()), eakInfo.getSignerSigningKey()))
      throw new E3DBVerificationException(record.document().meta());

    return new LocalRecord(plainRecord, record.document().meta());
  }

  /**
   * Sign &amp; encrypt an existing record for local storage.
   *
   * @param record The record to encrypt.
   * @param eakInfo The key to use for encrypting.
   * @return The encrypted record.
   */
  @Override
  public LocalEncryptedRecord encryptExisting(LocalRecord record, EAKInfo eakInfo) {
    checkNotNull(record, "record");
    checkNotNull(eakInfo, "eakInfo");

    return makeEncryptedRecord(eakInfo, record.data(), record.meta());
  }

  /**
   * Sign &amp; encrypt a record for local storage.
   *
   * @param type The type of the record.
   * @param data Fields to encrypt.
   * @param plain Plaintext metadata which will be stored with the record.
   * @param eakInfo The key to use for encrypting.
   * @return The encrypted record.
   */
  @Override
  public LocalEncryptedRecord encryptRecord(String type, RecordData data, Map<String, String> plain, EAKInfo eakInfo) {
    checkNotNull(type, "type");
    checkNotNull(data, "data");
    checkNotNull(eakInfo, "eakInfo");
    checkMap(data.getCleartext(), "data.getCleartext()");

    if(plain != null && plain.size() > 0)
      checkMap(plain, "plain");

    return makeEncryptedRecord(eakInfo, data.getCleartext(), new LocalMeta(this.clientId, this.clientId, type, plain));
  }

  /**
   * Derives a signature using this client's private Ed25519 key and the document given.
   *
   * @param document The document to sign. Consider using {@link LocalRecord}, which implements the
   *                 {@link Signable} interface.
   * @param <T> T.
   * @return A {@link SignedDocument} instance, holding the document given and a signature
   * for it.
   */
  @Override
  public <T extends Signable> SignedDocument<T> sign(T document) {
    checkNotNull(document, "document");
    if(this.privateSigningKey == null)
      throw new IllegalStateException("Client must have a signing key.");

    return new SD<>(document, Base64.encodeURL(
      Platform.crypto.signature(
        document.toSerialized().getBytes(UTF8), this.privateSigningKey
      )
    ));
  }

  /**
   * Verifies that the signature attached to the document was:
   *
   * <ul>
   * <li>Produced by the holder of the private key associated with the {@code publicSigningKey} given.</li>
   * <li>Derived from the document given.
   * </ul>
   *
   * @param signedDocument Document and signature to verify.
   * @param publicSigningKey Public portion of the Ed25519 key used to produce the signature, as a Base64URL-encoded
   *                         string.
   * @return {@code true} if the signature matches; {@code false} otherwise.
   */
  @Override
  public boolean verify(SignedDocument signedDocument, String publicSigningKey) {
    checkNotNull(signedDocument, "signedDocument");
    checkNotNull(publicSigningKey, "publicSigningKey");

    String signature = signedDocument.signature();
    Signable document = signedDocument.document();

    checkNotNull(signature, "signature");
    checkNotNull(document, "document");

    return Platform.crypto.verify(new Signature(Base64.decodeURL(signature)), document.toSerialized().getBytes(UTF8), Base64.decodeURL(publicSigningKey));
  }

  /**
   * Adds an authorizer for records written by this client.
   *
   * <p>Calling this method will grant permission for the "{@code authorizer}" client to allow
   * <b>other</b> clients to read records of the given type, written by this client.
   *
   * <p>The authorizer client itself will not be able to read records; it will only be able to grant
   * permission for other clients to do so.
   *
   * @param authorizer ID of the authorizer. Canot be {@code null}.
   * @param recordType Record type to give authorizer ability to share. Canot be {@code null}.
   * @param handler Handles result of the call.
   */
  @Override
  public void addAuthorizer(final UUID authorizer, final String recordType, final ResultHandler<Void> handler) {
    checkNotNull(authorizer, "authorizer");
    checkNotEmpty(recordType, "recordType");

    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          ClientInfo authorizerInfo = getClientInfo(authorizer);
          byte[] ak = getOwnAccessKey(recordType);
          try {
            setAccessKey(ClientImpl.this.clientId, ClientImpl.this.clientId, authorizer, recordType, authorizerInfo.getCurve25519(), ak, ClientImpl.this.clientId, ClientImpl.this.publicSigningKey);
          }
          catch(E3DBConflictException e) {
            // safe to ignore, it means this client has already been authorized.
          }
          retrofit2.Response<ResponseBody> putResponse = shareClient.putPolicy(ClientImpl.this.clientId.toString(),
              ClientImpl.this.clientId.toString(),
              authorizer.toString(),
              recordType,
              RequestBody.create(APPLICATION_JSON, allowAuthorizer)
          ).execute();

          if(putResponse.code() != 201)
            throw E3DBException.find(putResponse.code(), putResponse.message() + ": unable to write policy: ");

          uiValue(handler, null);
        }
        catch(Throwable e) {
          uiError(handler, e);
        }
      }
    });
  }

  /**
   * Remove the given client's ability to share any record type written by this client.
   *
   * <p>This method removes the permission granted by {@link #addAuthorizer(UUID, String, ResultHandler)},
   * for all record types written by this client.
   *
   * <p>No error occurs if the client did not have permission in the first place.
   *
   * @param authorizer ID of the authorizer. Cannot be {@code null}.
   * @param handler Handles the result of the operation.
   */
  @Override
  public void removeAuthorizer(final UUID authorizer, final ResultHandler<Void> handler) {
    checkNotNull(authorizer, "authorizer");
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          retrofit2.Response<ResponseBody> deleteResponse = shareClient.deletePolicy(ClientImpl.this.clientId.toString(),
              ClientImpl.this.clientId.toString(),
              authorizer.toString()
          ).execute();

          if(deleteResponse.code() != 200)
            throw E3DBException.find(deleteResponse.code(), deleteResponse.message() + ": could not remove authorizer.");

          uiValue(handler, null);
        }
        catch(Throwable e) {
          uiError(handler, e);
        }
      }
    });
  }

  /**
   * Remove the given client's ability to share the specific type of record given.
   *
   * <p>This method removes the permission granted by {@link #addAuthorizer(UUID, String, ResultHandler)},
   * for the specific record type identified.
   *
   * <p>No error occurs if the client did not have permission in the first place.
   *
   * @param authorizer ID of the authorizer. Cannot be {@code null}.
   * @param recordType Type of record. Cannot be blank or {@code null}.
   * @param handler Handles the result of the operation.
   */
  @Override
  public void removeAuthorizer(final UUID authorizer, final String recordType, final ResultHandler<Void> handler) {
    checkNotNull(authorizer, "authorizer");
    checkNotEmpty(recordType, "recordType");

    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          retrofit2.Response<ResponseBody> putResponse = shareClient.putPolicy(ClientImpl.this.clientId.toString(),
              ClientImpl.this.clientId.toString(),
              authorizer.toString(),
              recordType,
              RequestBody.create(APPLICATION_JSON, denyAuthorizer)
          ).execute();

          if(putResponse.code() != 201)
            throw E3DBException.find(putResponse.code(), putResponse.message() + ": could not remove authorizer.");

          uiValue(handler, null);
        }
        catch(Throwable e) {
          uiError(handler, e);
        }
      }
    });
  }

  /**
   * Lists all the clients (and associated record types) that this client has authorized
   * to share on its behalf.
   *
   * <p>This method allows the client to inspect all other clients that have permission to
   * share records on this client's behalf.
   *
   * @param handler Handles result of the call.
   */
  @Override
  public void getAuthorizers(final ResultHandler<List<AuthorizerPolicy>> handler) {
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          retrofit2.Response<ResponseBody> getResponse = shareClient.getProxies().execute();
          if(getResponse.code() != 200)
            throw E3DBException.find(getResponse.code(), getResponse.message() + ": unable to get authorizers.");

          uiValue(handler, getAuthorizerPolicies(
              mapper.readTree(getResponse.body().byteStream())
          ));
        }
        catch(Throwable e) {
          uiError(handler, e);
        }
      }
    });
  }

  /**
   * Lists all writers (and associated record types) that have authorized this client to
   * share data on their behalf.
   *
   * <p>This method allows the client to inspect the list of data producer's that have
   * authorized the client to share records on their behalf.
   *
   * @param handler Handles result of the call.
   */
  @Override
  public void getAuthorizedBy(final ResultHandler<List<AuthorizerPolicy>> handler) {
    onBackground(new Runnable() {
      @Override
      public void run() {
        try {
          retrofit2.Response<ResponseBody> getResponse = shareClient.getGranted().execute();
          if(getResponse.code() != 200)
            throw E3DBException.find(getResponse.code(), getResponse.message() + ": unable to get authorized by list.");

          uiValue(handler, getAuthorizerPolicies(
              mapper.readTree(getResponse.body().byteStream())
          ));
        }
        catch(Throwable e) {
          uiError(handler, e);
        }
      }
    });
  }

  private static class FM implements FileMeta {
    private final Long size;
    private final String fileUrl;
    private final String fileName;
    private final String checksum;
    private final Compression compression;

    private FM(Long size, String fileUrl, String fileName, String checksum, Compression compression) {
      this.size = size;
      this.fileUrl = fileUrl;
      this.fileName = fileName;
      this.checksum = checksum;
      this.compression = compression;
    }

    @Override
    public String fileUrl() {
      return fileUrl;
    }

    @Override
    public String fileName() {
      return fileName;
    }

    @Override
    public String checksum() {
      return checksum;
    }

    @Override
    public Compression compression() {
      return compression;
    }

    @Override
    public Long size() {
      return size;
    }
  }

  /**
   * Immutable information about a given record.
   */
  private static class M implements RecordMeta {

    private final UUID recordId;
    private final UUID writerId;
    private final UUID userId;
    private final String version;
    private final Date created;
    private final Date lastModified;
    private final String type;
    private final JsonNode plain;
    private final FileMeta file;

    private volatile Map<String, String> plainMap = null;

    M(UUID recordId, UUID writerId, UUID userId, String version, Date created, Date lastModified, String type, JsonNode plain, FileMeta file) {
      this.recordId = recordId;
      this.writerId = writerId;
      this.userId = userId;
      this.version = version;
      this.created = created;
      this.lastModified = lastModified;
      this.type = type;
      this.plain = plain;
      this.file = file;
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

    public Map<String, String> plain() {
      // Source: Effective Java, 2nd edition.
      // From http://www.oracle.com/technetwork/articles/java/bloch-effective-08-qa-140880.html ("More Effective Java With Google's Joshua Bloch")
      Map<String, String> result = plainMap;
      if (result == null) {
        synchronized (this) {
          result = plainMap;
          if (result == null) {
            result = new HashMap<>();
            if (plain != null) {
              Iterable<Map.Entry<String, JsonNode>> entries = new Iterable<Map.Entry<String, JsonNode>>() {
                @Override
                public Iterator<Map.Entry<String, JsonNode>> iterator() {
                  return plain.fields();
                }
              };
              for (Map.Entry<String, JsonNode> entry : entries) {
                result.put(entry.getKey(), entry.getValue().asText());
              }
            }
            plainMap = result;
          }
        }
      }
      return result;
    }

    @Override
    public FileMeta file() {
      return file;
    }
  }
}
