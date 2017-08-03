package com.tozny.e3db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import okio.ByteString;

/**
 * Created by Justin on 8/28/2017.
 */
public class E3DBClientBuilder {
  private static final ObjectMapper mapper = new ObjectMapper();
  private String apiKey;
  private String apiSecret;
  private UUID clientId;
  private URI host = URI.create("https://api.e3db.com");
  private byte[] privateKey;

  public E3DBClientBuilder() {
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

  public E3DBClientBuilder fromClientInfo(ClientInfo info) {
    return this.setHost(info.host)
      .setClientId(info.clientId)
      .setApiKey(info.apiKey)
      .setApiSecret(info.apiSecret)
      .setPrivateKey(info.privateKey);
  }

  public E3DBClientBuilder fromJson(String credsJson, byte[] privateKey) {
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

  public E3DBClientBuilder setApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  public E3DBClientBuilder setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
    return this;
  }

  public E3DBClientBuilder setClientId(UUID clientId) {
    this.clientId = clientId;
    return this;
  }

  public E3DBClientBuilder setHost(String host) {
    this.host = URI.create(host);
    return this;
  }

  public E3DBClientBuilder setPrivateKey(String privateKey) {
    this.privateKey = ByteString.decodeBase64(privateKey).toByteArray();
    return this;
  }

  public E3DBClient build() {
    checkState();
    return new E3DBClient(apiKey, apiSecret, clientId, host, privateKey);
  }
}
