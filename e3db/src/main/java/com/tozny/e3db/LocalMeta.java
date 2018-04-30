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

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.tozny.e3db.Checks.checkNotNull;

/**
 * Represents metadata about a record.
 */
public class LocalMeta implements RecordMeta {
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
  public UUID recordId() {
    throw new IllegalStateException("recordId not defined");
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
  public Date created() {
    throw new IllegalStateException("created not defined");
  }

  @Override
  public Date lastModified() {
    throw new IllegalStateException("lastModified not defined");
  }

  @Override
  public String version() {
    throw new IllegalStateException("version not defined");
  }

  @Override
  public String type() {
    return type;
  }
  @Override
  public Map<String, String> plain() {
    return plain;
  }
}
