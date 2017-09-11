package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tozny.e3db.Checks.*;

/**
 * Holds all necessary information to create an E3DB client.
 *
 * <p>Use this class to save and load credentials and other information about
 * a given client. The {@link #json()} method will return a {@code String} containing
 * a JSON document that can be later used to re-create the {@code Config} object using
 * the {@link #fromJson(String)} static method.
 *
 * <p>Once a {@code Config} instance has been created, use the {@link ClientBuilder#fromConfig(Config)}
 * method to configure a {@code ClientBuilder} that can re-create the client.
 *
 * <p>The {@link #fromCredentials(ClientCredentials, String)} method can also be used create a {@code Config}
 * instance, if you do not want to use the built-in JSON document.</p>
 */
public class Config {
  private static final ObjectMapper mapper =  new ObjectMapper();
  /**
   * Username (API key) for the client.
   */
  public final String apiKey;
  /**
   * Password (API secret) for the client.
   */
  public final String apiSecret;
  /**
   * Curve 25519 private key for the client, as a Base64URL-encoded string.
   */
  public final String privateKey;
  /**
   * ID of the client.
   */
  public final UUID clientId;
  /**
   * Name of the client.
   */
  public final String name;
  /**
   * Host the client registered with.
   */
  public final String host;
  private final String publicKey;

  Config(String apiKey, String apiSecret, UUID clientId, String name, String host, String privateKey, String publicKey) {
    checkNotEmpty(apiKey, "apiKey");
    checkNotEmpty(apiSecret, "apiSecret");
    checkNotNull(clientId, "clientId");
    checkNotNull(name, "name");
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

  /**
   * Create a Config instance from the given credentials and private key.
   * @param creds Username (API key), password (API secret), and client ID.
   * @param privateKey Curve 25519 private key, as a Base64URL-encoded string.
   * @return
   */
  public static Config fromCredentials(ClientCredentials creds, String privateKey) {
    checkNotNull(creds, "creds");
    checkNotEmpty(privateKey, "privateKey");

    return new Config(creds.apiKey(), creds.apiSecret(), creds.clientId(), creds.name(), "https://api.e3db.com", privateKey, creds.publicKey());
  }

  /**
   * Create a config instance from the given JSON document.
   *
   * <p>The JSON document given in {@code doc} must be generated (at some previous time)
   * using the {@link #json()} method.
   *
   * @param doc
   * @return
   * @throws IOException
   */
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

  /**
   * Converts this instance to a JSON document.
   *
   * <p>The value returned contains a JSON document which can be passed to the
   * {@link #fromJson(String)} method to re-create this {@code Config} instance.
   * @return
   */
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
