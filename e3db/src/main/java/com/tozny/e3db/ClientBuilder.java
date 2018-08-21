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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.UUID;

import okhttp3.CertificatePinner;
import okio.ByteString;

import static com.tozny.e3db.Checks.*;

/**
 * Use this class to configure and create an E3DB client.
 *
 * <p>Two shortcut methods are available for creating a client:
 *
 * <ul>
 *   <li>{@link #fromConfig(Config)} &mdash; Creates a client based on a {@link Config} object.</li>
 *   <li>{@link #fromCredentials(ClientCredentials, String)} &mdash; Creates a client based on credentials and a private key.</li>
 * </ul>
 *
 * <p>Otherwise, the following methods must be called before {@code build} will succeed:
 *
 * <ul>
 *   <li>{@link #setApiKey(String)}</li>
 *   <li>{@link #setApiSecret(String)}</li>
 *   <li>{@link #setClientId(UUID)}</li>
 *   <li>{@link #setPrivateKey(String)}</li>
 * </ul>
 */
public class ClientBuilder {
  private static final ObjectMapper mapper = new ObjectMapper();
  private String apiKey;
  private String apiSecret;
  private UUID clientId;
  private URI host = URI.create("https://api.e3db.com");
  private CertificatePinner certificatePinner = null;
  private byte[] privateKey;
  private byte[] privateSigningKey;

  public ClientBuilder() {
  }

  private void checkState() {
    checkNotEmpty(apiKey, "apiKey");
    checkNotEmpty(apiSecret, "apiSecret");
    checkNotNull(clientId, "clientId");
    checkNotNull(host, "host");
    checkNotNull(privateKey, "privateKey");
  }

  /**
   * Configure this object using the {@code info} argument.
   *
   * @param info info.
   * @return This instance.
   */
  public ClientBuilder fromConfig(Config info) {
    checkNotNull(info, "info");

    return this.setHost(info.host)
      .setClientId(info.clientId)
      .setApiKey(info.apiKey)
      .setApiSecret(info.apiSecret)
      .setPrivateKey(info.privateKey)
      .setPrivateSigningKey(info.privateSigningKey);
  }

  /**
   * Configure this {@code ClientBuilder} using the given credentials
   * and private key.
   * @param creds Credentials holding the username (API key), password (API secret) and client ID for the
   *              client.
   * @param privateKey A Base64URL-encoded string representing a Curve25519 private key.
   * @return This instance.
   */
  public ClientBuilder fromCredentials(ClientCredentials creds, String privateKey) {
    checkNotNull(creds, "creds");
    checkNotEmpty(privateKey, "privateKey");

    return this.setApiKey(creds.apiKey())
      .setApiSecret(creds.apiSecret())
      .setClientId(creds.clientId())
      .setPrivateKey(privateKey);
  }

  /**
   * Configure the username (API key) for this instance.
   *
   * @param apiKey apiKey.
   * @return This instance.
   */
  public ClientBuilder setApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  /**
   * Configure the password (API  secret) for this instance.
   *
   * @param apiSecret apiSecret.
   * @return This instance.
   */
  public ClientBuilder setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
    return this;
  }

  /**
   * Configure the Client ID for this instance.
   *
   * @param clientId clientId.
   * @return This instance.
   */
  public ClientBuilder setClientId(UUID clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Configure the host for this instance.
   *
   * Not normally necessary, defaults to {@code https://api.e3db.com}.
   * @param host host.
   * @return This instance.
   */
  public ClientBuilder setHost(String host) {
    this.host = URI.create(host);
    return this;
  }

  /**
   * Configure a certificate pinner for this instance.
   *
   * @param certificatePinner OkHttp CertificatePinner instance.
   *
   * @return This instance.
   */
  public ClientBuilder setCertificatePinner(CertificatePinner certificatePinner) {
    this.certificatePinner = certificatePinner;
    return this;
  }

  /**
   * Configure the private key for this instance
   *
   * @param privateKey A Base64URL-encoded string representing a Curve25519 private key.
   * @return This instance.
   */
  public ClientBuilder setPrivateKey(String privateKey) {
    this.privateKey = ByteString.decodeBase64(privateKey).toByteArray();
    return this;
  }

  public ClientBuilder setPrivateSigningKey(String privateSigningKey) {
    this.privateSigningKey = ByteString.decodeBase64(privateSigningKey).toByteArray();
    return this;
  }

  /**
   * Create an E3DB client based on configured parameters.
   *
   * @return a configured client.
   */
  public E3DBClient build() {
    checkState();
    if (certificatePinner == null)
      return new Client(apiKey, apiSecret, clientId, host, privateKey, privateSigningKey);
    else
      return new Client(apiKey, apiSecret, clientId, host, privateKey, privateSigningKey, certificatePinner);
  }
}
