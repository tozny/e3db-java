package com.tozny.e3db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import okhttp3.CertificatePinner;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

import static com.tozny.e3db.Base64.encodeURL;
import static com.tozny.e3db.Checks.checkNotEmpty;
import static com.tozny.e3db.Checks.checkNotNull;

public class Registration {
    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    /**
     * Registers a new client. This method creates a new public/private key pair for the client
     * to use when encrypting and decrypting records.
     *
     * @param token Registration token obtained from the Tozny console at <a href="https://console.tozny.com">https://console.tozny.com</a>.
     * @param clientName Name of the client; for informational purposes only.
     * @param host Host to register with. Should be {@code https://api.e3db.com}.
     * @param certificatePinner OkHttp CertificatePinner instance to restrict which certificates and authorities are trusted.
     * @param handleResult Handles the result of registration. The {@link Config} value can be converted to JSON, written to
     *                     a secure location, and loaded later.
     */
    public void register(final String token, final String clientName, final String host, final CertificatePinner certificatePinner, final ResultHandler<Config> handleResult) {
        checkNotEmpty(token, "token");
        checkNotEmpty(clientName, "clientName");
        checkNotEmpty(host, "host");

        final byte[] privateKey = Platform.crypto.newPrivateKey();
        final String publicKey = encodeURL(Platform.crypto.getPublicKey(privateKey));

        final byte[] privateSigningKey = Platform.crypto.newPrivateSigningKey();
        final String publicSigningKey = encodeURL(Platform.crypto.getPublicSigningKey(privateSigningKey));

        ResultHandler<ClientCredentials> resultHandler = new ResultHandler<ClientCredentials>() {
            @Override
            public void handle(Result<ClientCredentials> r) {
                if (r.isError()) {
                    executeError(PredefinedExecutors.uiExecutor, handleResult, r.asError().other());
                }
                else {
                    final ClientCredentials credentials = r.asValue();
                    Config info = new Config(credentials.apiKey(), credentials.apiSecret(), credentials.clientId(), clientName, credentials.host(), encodeURL(privateKey),
                            encodeURL(privateSigningKey));
                    executeValue(PredefinedExecutors.uiExecutor, handleResult, info);
                }
            }
        };

        if (certificatePinner == null)
            register(token, clientName, publicKey, publicSigningKey, host, resultHandler);
        else
            register(token, clientName, publicKey, publicSigningKey, host, certificatePinner, resultHandler);
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
    public void register(final String token, final String clientName, final String host, final ResultHandler<Config> handleResult) {
        register(token, clientName, host, null, handleResult);
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
     *                  private key. Consider using {@link KeyGenerator#generateKey()} to generate a private key.
     * @param publicSignKey A Base64URL-encoded string representing the public signing key associated with the client. Should be based on a Ed25519
     *                      private key. Consider using {@link KeyGenerator#generateSigningKey()} to generate a private key.
     * @param host Host to register with. Should be {@code https://api.e3db.com}.
     * @param handleResult Handles the result of registration.
     */
    public void register(final String token, final String clientName, final String publicKey, final String publicSignKey, final String host, final ResultHandler<ClientCredentials> handleResult) {
        OkHttpClient client = OkHttpTLSv12.enable(new OkHttpClient.Builder()).build();

        register(token, clientName, publicKey, publicSignKey, host, client, handleResult);
    }

    /**
     * Registers a new client with a given public key.
     *
     * <p>This method does not create a private/public key pair; rather, the public key should be provided
     * by the caller.
     *
     * <p>This method does not create a certificate pin collectionl rather, the implementing application should
     * <a href="https://github.com/square/okhttp/wiki/HTTPS#certificate-pinning">implement</a> a {@code CertificatePinner}
     * instance and pass it.
     *
     * @param token Registration token obtained from the Tozny console at <a href="https://console.tozny.com">https://console.tozny.com</a>.
     * @param clientName Name of the client; for informational purposes only.
     * @param publicKey A Base64URL-encoded string representing the public key associated with the client. Should be based on a Curve25519
     *                  private key. Consider using {@link KeyGenerator#generateKey} to generate a private key.
     * @param publicSignKey A Base64URL-encoded string representing the public signing key associated with the client. Should be based on a Ed25519
     *                      private key. Consider using {@link KeyGenerator#generateSigningKey()} to generate a private key.
     * @param host Host to register with. Should be {@code https://api.e3db.com}.
     * @param certificatePinner OkHttp CertificatePinner instance to restrict which certificates and authorities are trusted.
     * @param handleResult Handles the result of registration.
     */
    public void register(final String token, final String clientName, final String publicKey, final String publicSignKey, final String host, final CertificatePinner certificatePinner, final ResultHandler<ClientCredentials> handleResult) {
        checkNotNull(certificatePinner, "certificatePinner");
        OkHttpClient client = OkHttpTLSv12.enable(new OkHttpClient.Builder()).certificatePinner(certificatePinner).build();

        register(token, clientName, publicKey, publicSignKey, host, client, handleResult);
    }

    /**
     * Abstract helper method to enable registration with a pre-configured client instance.
     *
     * @param token Registration token obtained from the Tozny console at <a href="https://console.tozny.com">https://console.tozny.com</a>.
     * @param clientName Name of the client; for informational purposes only.
     * @param publicKey A Base64URL-encoded string representing the public key associated with the client. Should be based on a Curve25519
     *                  private key. Consider using {@link KeyGenerator#generateKey()} to generate a private key.
     * @param publicSignKey A Base64URL-encoded string representing the public signing key associated with the client. Should be based on a Ed25519
     *                      private key. Consider using {@link KeyGenerator#generateSigningKey()} to generate a private key.
     * @param host Host to register with. Should be {@code https://api.e3db.com}.
     * @param anonymousClient OkHttpClient instance for making anonymous requests against the server.
     * @param handleResult Handles the result of registration.
     */
    private void register(final String token, final String clientName, final String publicKey, final String publicSignKey, final String host, final OkHttpClient anonymousClient, final ResultHandler<ClientCredentials> handleResult) {
        checkNotEmpty(token, "token");
        checkNotEmpty(clientName, "clientName");
        checkNotEmpty(publicKey, "publicKey");
        checkNotEmpty(publicSignKey, "publicSignKey");
        checkNotEmpty(host, "host");

        final RegisterAPI registerClient = new Retrofit.Builder()
                .callbackExecutor(PredefinedExecutors.uiExecutor)
                .client(anonymousClient)
                .baseUrl(URI.create(host).resolve("/").toString())
                .build().create(RegisterAPI.class);

        PredefinedExecutors.backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> publicKeyInfo = new HashMap<>();
                    publicKeyInfo.put("curve25519", publicKey);

                    Map<String, String> publicSignKeyInfo = new HashMap<>();
                    publicSignKeyInfo.put("ed25519", publicSignKey);

                    Map<String, Object> clientInfo = new HashMap<>();
                    clientInfo.put("name", clientName);
                    clientInfo.put("public_key", publicKeyInfo);
                    clientInfo.put("signing_key", publicSignKeyInfo);

                    Map<String, Object> registerInfo = new HashMap<>();
                    registerInfo.put("token", token);
                    registerInfo.put("client", clientInfo);

                    final retrofit2.Response<ResponseBody> response = registerClient.register(RequestBody.create(APPLICATION_JSON, mapper.writeValueAsString(registerInfo))).execute();
                    if (response.code() != 201) {
                        executeError(PredefinedExecutors.uiExecutor, handleResult, E3DBException.find(response.code(), response.message()));
                    } else {
                        final String doc = response.body().string();
                        JsonNode creds = mapper.readTree(doc);
                        final String apiKey = creds.get("api_key_id").asText();
                        final String apiSecret = creds.get("api_secret").asText();
                        final UUID clientId = UUID.fromString(creds.get("client_id").asText());
                        final String name = creds.get("name").asText();
                        final String publicKey = creds.get("public_key").get("curve25519").asText();
                        final String signingKey = creds.get("signing_key").get("ed25519").asText();
                        final boolean enabled = creds.get("enabled").asBoolean();
                        final ClientCredentials clientCredentials = new CC(apiKey, apiSecret, clientId, name, publicKey, publicSignKey, host, enabled);
                        executeValue(PredefinedExecutors.uiExecutor, handleResult, clientCredentials);
                    }
                } catch (final Throwable e) {
                    executeError(PredefinedExecutors.uiExecutor, handleResult, e);
                }
            }
        });
    }

    private static class CC implements ClientCredentials {
        private final String apiKey;
        private final String apiSecret;
        private final UUID clientId;
        private final String name;
        private final String publicKey;
        private final String publicSignKey;
        private final URI host;

        private final boolean enabled;

        public CC(String apiKey, String apiSecret, UUID clientId, String name, String publicKey, String publicSignKey, String host, boolean enabled) {
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
            this.clientId = clientId;
            this.name = name;
            this.publicKey = publicKey;
            this.publicSignKey = publicSignKey;
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
        public String publicSignKey() {
            return publicSignKey;
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
}
