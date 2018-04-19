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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents an E3DB record.
 */
public class LocalRecord implements Record {
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  private final Map<String, String> data;
  private final RecordMeta meta;

  @JsonProperty("rec_sig")
  private final String signature;

  /**
   * Creates a representation of a Record suitable for signing or storing locally..
   *
   * @param data Data contained in the record.
   * @param meta Data about the record. Consider using {@link LocalMeta}.
   */
  public LocalRecord(Map<String, String> data, RecordMeta meta) {
    this.data = data;
    this.meta = meta;
    this.signature = null;
  }

  /**
   * Creates a record with an associated signature.
   *
   * @param data Data contained in the record.
   * @param meta Data about the record. Consider using {@link LocalMeta}.
   */
  public LocalRecord(Map<String, String> data, RecordMeta meta, String signature) {
    this.data = data;
    this.meta = meta;
    this.signature = signature;
  }

  @Override
  public RecordMeta meta() {
    return meta;
  }

  @Override
  public Map<String, String> data() {
    return data;
  }

  @Override
  public String signature() {
    return signature;
  }

  @Override
  public Record document() {
    return this;
  }

  @Override
  public String toSerialized() {
    try {
      SortedMap<String, Object> clientMeta = new TreeMap<>();
      clientMeta.put("writer_id", meta().writerId().toString());
      clientMeta.put("user_id", meta().userId().toString());
      clientMeta.put("type", meta().type().toString());
      Map<String, String> plain = meta().plain();
      clientMeta.put("plain", plain == null ?
          new TreeMap<String, String>() :
          new TreeMap<String, String>(plain));

      String clientMetaSerial = mapper.writeValueAsString(clientMeta);
      String dataSerial = mapper.writeValueAsString(data());
      return new StringBuffer(clientMetaSerial.length() + dataSerial.length()).append(clientMetaSerial).append(dataSerial).toString();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
