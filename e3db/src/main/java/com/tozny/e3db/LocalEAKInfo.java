package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocalEAKInfo implements EAKInfo {
  private static final ObjectMapper mapper = new ObjectMapper();

  private final String key;
  private final String publicKey;
  private final UUID authorizerId;
  private final UUID signerId;
  private final String signerSigningKey;

  public LocalEAKInfo(String key, String publicKey, UUID authorizerId, UUID signerId, String signerSigningKey) {
    this.key = key;
    this.publicKey = publicKey;
    this.authorizerId = authorizerId;
    this.signerId = signerId;
    this.signerSigningKey = signerSigningKey;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getPublicKey() {
    return publicKey;
  }

  @Override
  public UUID getAuthorizerId() {
    return authorizerId;
  }

  @Override
  public UUID getSignerId() {
    return signerId;
  }

  @Override
  public String getSignerSigningKey() {
    return signerSigningKey;
  }

  /**
   * Encode the EAK as a JSON document, suitable for storage.
   * @return
   */
  public String encode() {
    Map<String, String> doc = new HashMap<>();

    doc.put("authorizer_id", getAuthorizerId().toString());
    doc.put("authorizer_public_key", getPublicKey());
    doc.put("signer_id", getSignerId().toString());
    doc.put("signer_signing_key", getSignerSigningKey());
    doc.put("eak", getKey());

    try {
      return mapper.writeValueAsString(doc);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decode an LocalEAKInfo document, previously encoded with
   * {@link #encode()}.
   *
   * @param doc
   * @return
   * @throws IOException
   */
  public static EAKInfo decode(String doc) throws IOException {
    JsonNode info = mapper.readTree(doc);

    UUID authorizerId = UUID.fromString(info.get("authorizer_id").asText());
    String authorizerPublicKey = info.get("authorizer_public_key").asText();
    UUID signerId = UUID.fromString(info.get("signer_id").asText());
    String signerSigningKey = info.get("signer_signing_key").asText();
    String key = info.get("eak").asText();

    return new LocalEAKInfo(key, authorizerPublicKey, authorizerId, signerId, signerSigningKey);
  }
}
