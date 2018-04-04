package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tozny.e3db.Base64.decodeURL;
import static com.tozny.e3db.Base64.encodeURL;
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
 * <p>The {@code fromCredentials} method(s) can also be used create a {@code Config}
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
   * Curve25519 public key for this client, as a Base64URL-encoded string.
   */
  public final String publicKey;
  /**
   * Curve25519 private key for the client, as a Base64URL-encoded string.
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
  /**
   * Ed25519 public key for the client, as a Base64URL-encoded string. Can be {@code null}.
   */
  public final String publicSigningKey;
  /**
   * Ed25519 private key for the client, as a Base64URL-encoded string. Can be {@code null}.
   */
  public final String privateSigningKey;

  Config(String apiKey, String apiSecret, UUID clientId, String name, String host, String privateEncryptionKey, String privateSigningKey) {
    checkNotEmpty(apiKey, "apiKey");
    checkNotEmpty(apiSecret, "apiSecret");
    checkNotEmpty(host, "host");
    checkNotEmpty(privateEncryptionKey, "privateEncryptionKey");
    checkNotNull(clientId, "clientId");
    checkNotNull(name, "name");

    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.clientId = clientId;
    this.name = name;
    this.host = host;
    this.privateKey = privateEncryptionKey;
    this.publicKey = encodeURL(Platform.crypto.getPublicKey(decodeURL(this.privateKey)));
    this.privateSigningKey = privateSigningKey;

    if(this.privateSigningKey != null)
      this.publicSigningKey = encodeURL(Platform.crypto.getPublicSigningKey(decodeURL(this.privateSigningKey)));
    else
      this.publicSigningKey = null;
  }


  /**
   * Create a Config instance from the given credentials and private encryption key.
   *
   * @param creds Username (API key), password (API secret), and client ID.
   * @param privateEncryptionKey Curve25519 private key, as a Base64URL-encoded string.
   * @return A {@link Config} instance.
   */
  public static Config fromCredentials(ClientCredentials creds, String privateEncryptionKey) {
    checkNotNull(creds, "creds");
    checkNotEmpty(privateEncryptionKey, "privateEncryptionKey");

    return new Config(creds.apiKey(),
      creds.apiSecret(),
      creds.clientId(),
      creds.name(),
      "https://api.e3db.com",
      privateEncryptionKey,
      null
    );
  }

  /**
   * Create a Config instance from the given credentials, private encryption key and private
   * signing key.
   *
   * @param creds Username (API key), password (API secret), and client ID.
   * @param privateEncryptionKey Curve25519 private key, as a Base64URL-encoded string.
   * @param privateSigningKey Ed25519 private key, as a Base64URL-encoded string.
   * @return A {@link Config} instance.
   */
  public static Config fromCredentials(ClientCredentials creds, String privateEncryptionKey, String privateSigningKey) {
    checkNotNull(creds, "creds");
    checkNotEmpty(privateEncryptionKey, "privateEncryptionKey");
    checkNotEmpty(privateSigningKey, "privateSigningKey");

    return new Config(creds.apiKey(),
      creds.apiSecret(),
      creds.clientId(),
      creds.name(),
      "https://api.e3db.com",
      privateEncryptionKey,
      privateSigningKey
    );
  }

  /**
   * Create a config instance from the given JSON document.
   *
   * <p>The JSON document given in {@code doc} must be generated (at some previous time)
   * using the {@link #json()} method.
   *
   * @param doc JSON document representing a configuration.
   * @throws IOException ioException.
   * @return a {@link Config} instance.
   */
  public static Config fromJson(String doc) throws IOException {
    checkNotEmpty(doc, "doc");

    JsonNode info = mapper.readTree(doc);
    JsonNode api_key_id = info.get("api_key_id");
    JsonNode api_secret = info.get("api_secret");
    JsonNode client_id = info.get("client_id");
    JsonNode client_email = info.get("client_email");
    JsonNode api_url = info.get("api_url");
    JsonNode private_key = info.get("private_key");
    JsonNode public_key = info.get("public_key");
    JsonNode public_signing_key = info.get("public_signing_key");
    JsonNode private_signing_key = info.get("private_signing_key");
    JsonNode version = info.get("version");

    checkNotNull(api_key_id, "api_key_id");
    checkNotNull(api_secret, "api_secret");
    checkNotNull(client_id, "client_id");
    checkNotNull(client_email, "client_email");
    checkNotNull(api_url, "api_url");
    checkNotNull(private_key, "private_key");
    checkNotNull(public_key, "public_key");

    if(version != null && version.asInt() > 1) {
      checkNotNull(private_signing_key, "private_signing_key");
      checkNotNull(public_signing_key, "public_signing_key");
    }

    return new Config(api_key_id.asText(),
      api_secret.asText(),
      UUID.fromString(client_id.asText()),
      client_email == null ? "" : client_email.asText(),
      api_url.asText(),
      private_key.asText(),
      private_signing_key == null ? null : private_signing_key.asText()
    );
  }

  /**
   * Converts this instance to a JSON document.
   *
   * <p>The value returned contains a JSON document which can be passed to the
   * {@link #fromJson(String)} method to re-create this {@code Config} instance.
   *
   * @return JSON representation of this object.
   */
  public String json() {
    Map<String, Object> info = new HashMap<>();
    info.put("api_url", host);
    info.put("api_key_id", apiKey);
    info.put("api_secret", apiSecret);
    info.put("client_id", clientId.toString());
    info.put("client_email", name);
    info.put("public_key", publicKey);
    info.put("private_key", privateKey);
    info.put("version", 1);
    if(publicSigningKey != null) {
      info.put("public_signing_key", publicSigningKey);
      info.put("version", 2);
    }
    if(privateSigningKey != null) {
      info.put("private_signing_key", privateSigningKey);
      info.put("version", 2);
    }

    try {
      return mapper.writeValueAsString(info);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * TODO
   * @param helper
   * @param config
   */
  public static void saveConfigSecurely(ConfigStorageHelper helper, Config config) {
    if (Platform.isAndroid()) {
      helper.saveConfigSecurely(config.json());
    } else {
      throw new IllegalStateException("Method is only available for Android.");
    }
  }

  /**
   * TODO
   * @param helper
   * @return
   * @throws IOException
   */
  public static Config loadConfigSecurely(ConfigStorageHelper helper) throws IOException {
    if (Platform.isAndroid()) {
      return Config.fromJson(helper.loadConfigSecurely());
    } else {
      throw new IllegalStateException("Method is only available for Android.");
    }
  }
}
