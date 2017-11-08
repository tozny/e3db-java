package com.tozny.e3db;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

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
