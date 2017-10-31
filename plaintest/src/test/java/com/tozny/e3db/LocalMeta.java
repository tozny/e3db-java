package com.tozny.e3db;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * A subset of record metadata, usable for local storage and signing.
 */
public class LocalMeta implements RecordMeta {
  private final Map<String, String> plain;
  private final String type;
  private final UUID writerId;

  public LocalMeta(Map<String, String> plain, String type, UUID writerId) {
    this.plain = plain;
    this.type = type;
    this.writerId = writerId;
  }

  @Override
  public UUID recordId() {
    throw new IllegalStateException();
  }

  @Override
  public UUID writerId() {
    return writerId;
  }

  @Override
  public UUID userId() {
    return writerId;
  }

  @Override
  public Date created() {
    throw new IllegalStateException();
  }

  @Override
  public Date lastModified() {
    throw new IllegalStateException();
  }

  @Override
  public String version() {
    throw new IllegalStateException();
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
