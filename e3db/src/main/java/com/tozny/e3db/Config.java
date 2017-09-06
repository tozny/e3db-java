package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tozny.e3db.Checks.*;

public class Config {
  private static final ObjectMapper mapper =  new ObjectMapper();
  public final String apiKey;
  public final String apiSecret;
  public final String privateKey;
  public final UUID clientId;
  public final String name;
  public final String host;
  private final String publicKey;

  public Config(String apiKey, String apiSecret, UUID clientId, String name, String host, String privateKey, String publicKey) {
    checkNotEmpty(apiKey, "apiKey");
    checkNotEmpty(apiSecret, "apiSecret");
    checkNotNull(clientId, "clientId");
    checkNotEmpty(name, "name");
    checkNotEmpty(host, "host");
    checkNotEmpty(privateKey, "privateKey");
    checkNotEmpty(publicKey, "publicKey");

    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.privateKey = privateKey;
    this.clientId = clientId;
    this.name = name;
    this.host = host;
    this.publicKey = publicKey;
  }

  public static Config fromCredentials(ClientCredentials creds, String host, String privateKey) {
    checkNotNull(creds, "creds");
    checkNotEmpty(host, "host");
    checkNotEmpty(privateKey, "privateKey");

    return new Config(creds.apiKey(), creds.apiSecret(), creds.clientId(), creds.name(), host, privateKey, creds.publicKey());
  }

  public static Config fromJson(String doc) throws IOException {
    checkNotEmpty(doc, "doc");
    JsonNode info = mapper.readTree(doc);
    return new Config(info.get("api_key_id").asText(),
      info.get("api_secret").asText(),
      UUID.fromString(info.get("client_id").asText()),
      info.get("client_email").asText(),
      info.get("api_url").asText(),
      info.get("private_key").asText(),
      info.get("public_key").asText());
  }

  public String json() {
    Map<String, String> info = new HashMap<>();
    info.put("api_url", host);
    info.put("api_key_id", apiKey);
    info.put("api_secret", apiSecret);
    info.put("client_id", clientId.toString());
    info.put("client_email", name);
    info.put("public_key", publicKey);
    info.put("private_key", privateKey);
    try {
      return mapper.writeValueAsString(info);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
