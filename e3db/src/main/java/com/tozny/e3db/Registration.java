package com.tozny.e3db;

import okhttp3.CertificatePinner;

public interface Registration {
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
    void register(String token, String clientName, String host, CertificatePinner certificatePinner, ResultHandler<Config> handleResult);

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
    void register(String token, String clientName, String host, ResultHandler<Config> handleResult);

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
    void register(String token, String clientName, String publicKey, String publicSignKey, String host, ResultHandler<ClientCredentials> handleResult);

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
    void register(String token, String clientName, String publicKey, String publicSignKey, String host, CertificatePinner certificatePinner, ResultHandler<ClientCredentials> handleResult);
}
