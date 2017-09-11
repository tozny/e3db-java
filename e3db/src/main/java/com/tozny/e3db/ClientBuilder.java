package com.tozny.e3db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import okio.ByteString;

import static com.tozny.e3db.Checks.*;

/**
 * Use this class to configure and create an E3DB client.
 *
 * <p>Two shortcut methods are available for creating a client:
 *
 * <ul>
 *   <li>{@link #fromConfig(Config)} -- Creates a client based on a {@link Config} object.</li>
 *   <li>{@link #fromCredentials(ClientCredentials, String)} -- Creates a client based on credentials and a private key.</li>
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
  private byte[] privateKey;

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
   * @param info
   * @return This instance.
   */
  public ClientBuilder fromConfig(Config info) {
    checkNotNull(info, "info");

    return this.setHost(info.host)
      .setClientId(info.clientId)
      .setApiKey(info.apiKey)
      .setApiSecret(info.apiSecret)
      .setPrivateKey(info.privateKey);
  }

  /**
   * Configure this {@code ClientBuilder} using the given credentials
   * and private key.
   * @param creds Credentials holding the username (API key), password (API secret) and client ID for the
   *              client.
   * @param privateKey A Base64URL-encoded string representing a Curve 25519 private key.
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
   * @param apiKey
   * @return This instance.
   */
  public ClientBuilder setApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  /**
   * Configure the password (API  secret) for this instance.
   *
   * @param apiSecret
   * @return This instance.
   */
  public ClientBuilder setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
    return this;
  }

  /**
   * Configure the Client ID for this instance.
   *
   * @param clientId
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
   * @param host
   * @return This instance.
   */
  public ClientBuilder setHost(String host) {
    this.host = URI.create(host);
    return this;
  }

  /**
   * Configure the private key for this instance
   *
   * @param privateKey A Base64URL-encoded string representing a Curve 25519 private key.
   * @return This instance.
   */
  public ClientBuilder setPrivateKey(String privateKey) {
    this.privateKey = ByteString.decodeBase64(privateKey).toByteArray();
    return this;
  }

  /**
   * Create an E3DB Client instance based on configured parameters.
   *
   * @return a configured Client instance.
   */
  public Client build() {
    checkState();
    return new Client(apiKey, apiSecret, clientId, host, privateKey);
  }
}
