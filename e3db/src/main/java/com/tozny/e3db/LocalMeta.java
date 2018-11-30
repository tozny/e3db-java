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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static com.tozny.e3db.Checks.checkNotNull;

/**
 * Represents metadata about a record.
 */
public class LocalMeta implements ClientMeta {
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  private final UUID writerId;
  private final UUID userId;
  private final String type;
  private final Map<String, String> plain;

  /**
   * Create instance.
   *
   * @param writerId ID of the writer.
   * @param userId ID of the user whom this record is about (typically equal to {@code writerId}).
   * @param type Type of the record.
   * @param plain Plaintext metadata about the record. Can be {@code null}.
   */
  public LocalMeta(UUID writerId, UUID userId, String type, Map<String, String> plain) {
    checkNotNull(writerId, "writerId");
    checkNotNull(userId, "userId");
    checkNotNull(type, "type");

    this.writerId = writerId;
    this.userId = userId;
    this.type = type;
    this.plain = plain;
  }

  @Override
  public UUID writerId() {
    return writerId;
  }

  @Override
  public UUID userId() {
    return userId;
  }

  @Override
  public String type() {
    return type;
  }
  @Override
  public Map<String, String> plain() {
    return plain;
  }

  @Override
  public String toSerialized() throws JsonProcessingException {
    SortedMap<String, Object> clientMeta = new TreeMap<>();
    clientMeta.put("writer_id", writerId().toString());
    clientMeta.put("user_id", userId().toString());
    clientMeta.put("type", type());
    clientMeta.put("plain", plain() == null ?
                                new TreeMap<String, String>() :
                                new TreeMap<>(plain()));

      return mapper.writeValueAsString(clientMeta);

  }

  public static LocalMeta fromRecordMeta(RecordMeta m) {
    return new LocalMeta(m.writerId(), m.userId(), m.type(), m.plain());
  }
}
