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

import java.util.*;

import static com.tozny.e3db.Checks.checkNotNull;

/**
 * Represents an E3DB record.
 */
public class LocalRecord implements Signable {
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  private final Map<String, String> data;
  private final ClientMeta meta;

  /**
   * Creates a representation of a Record suitable for signing or storing locally.
   *
   * @param data Data contained in the record. Cannot be {@code null}.
   * @param meta Data about the record. Cannot be {@code null}. Consider using {@link LocalMeta}.
   */
  public LocalRecord(Map<String, String> data, ClientMeta meta) {
    checkNotNull(data, "data");
    checkNotNull(meta, "meta");

    this.data = data;
    this.meta = meta;
  }

  public ClientMeta meta() {
    return meta;
  }

  public Map<String, String> data() {
    return data;
  }

  @Override
  public String toSerialized() {
    try {

      return meta.toSerialized() + mapper.writeValueAsString(data());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
