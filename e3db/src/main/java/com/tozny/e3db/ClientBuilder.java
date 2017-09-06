package com.tozny.e3db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import okio.ByteString;

import static com.tozny.e3db.Checks.*;

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
    if (apiKey == null)
      throw new IllegalStateException("apiKey null");
    if (apiSecret == null)
      throw new IllegalStateException("apiSecret null");
    if (clientId == null)
      throw new IllegalStateException("clientId null");
    if (host == null)
      throw new IllegalStateException("host null");
    if (privateKey == null)
      throw new IllegalStateException("privateKey null");
  }

  public ClientBuilder fromConfig(Config info) {
    checkNotNull(info, "info");
    return this.setHost(info.host)
      .setClientId(info.clientId)
      .setApiKey(info.apiKey)
      .setApiSecret(info.apiSecret)
      .setPrivateKey(info.privateKey);
  }

  public ClientBuilder fromJson(String credsJson, byte[] privateKey) {
    checkNotEmpty(credsJson, "credsJson");
    checkNotEmpty(privateKey, "privateKey");

    try {
      JsonNode creds = mapper.readTree(credsJson);
      apiKey = creds.get("api_key_id").asText();
      apiSecret = creds.get("api_secret").asText();
      clientId = UUID.fromString(creds.get("client_id").asText());
      host = URI.create(creds.get("api_url").asText());
      this.privateKey = privateKey;
      return this;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ClientBuilder setApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  public ClientBuilder setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
    return this;
  }

  public ClientBuilder setClientId(UUID clientId) {
    this.clientId = clientId;
    return this;
  }

  public ClientBuilder setHost(String host) {
    this.host = URI.create(host);
    return this;
  }

  public ClientBuilder setPrivateKey(String privateKey) {
    this.privateKey = ByteString.decodeBase64(privateKey).toByteArray();
    return this;
  }

  public Client build() {
    checkState();
    return new Client(apiKey, apiSecret, clientId, host, privateKey);
  }
}
