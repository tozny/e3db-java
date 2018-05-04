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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An implementation of {@link EAKInfo} to can persist to storage (using {@link #encode()}
 * and load from storage (using {@link #decode(String)}.
 */
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
  public static LocalEAKInfo decode(String doc) throws IOException {
    JsonNode info = mapper.readTree(doc);

    UUID authorizerId = UUID.fromString(info.get("authorizer_id").asText());
    String authorizerPublicKey = info.get("authorizer_public_key").asText();
    UUID signerId = UUID.fromString(info.get("signer_id").asText());
    String signerSigningKey = info.get("signer_signing_key").asText();
    String key = info.get("eak").asText();

    return new LocalEAKInfo(key, authorizerPublicKey, authorizerId, signerId, signerSigningKey);
  }
}
