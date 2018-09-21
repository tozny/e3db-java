package com.tozny.e3db;

import okhttp3.CertificatePinner;

/**
 * A client capable of registering further clients. Intended for mock testing, where the static {@link Client#register(String, String, String, ResultHandler)}
 * methods are problematic.
 *
 * Not for normal use; prefer the static methods.
 * @return
 */
public class RegistrationClient implements E3DBRegistrationClient {
  @Override
  public void register(String token, String clientName, String host, CertificatePinner certificatePinner, ResultHandler<Config> handleResult) {
    Client.register(token, clientName, host, certificatePinner, handleResult);
  }

  @Override
  public void register(String token, String clientName, String host, ResultHandler<Config> handleResult) {
    Client.register(token, clientName, host, handleResult);
  }
}
