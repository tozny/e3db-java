package com.tozny.e3db;

import okhttp3.CertificatePinner;

/**
 * An interface for registration. Should only be used when mocking the E3DB client in unit tests.
 */
public interface E3DBRegistrationClient {
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
  void register(String token, String clientName, String host, CertificatePinner certificatePinner,  ResultHandler<Config> handleResult);
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
  void register( String token,  String clientName,  String host,  ResultHandler<Config> handleResult);
}
