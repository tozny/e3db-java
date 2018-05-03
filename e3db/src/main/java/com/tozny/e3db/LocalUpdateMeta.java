package com.tozny.e3db;

import java.util.UUID;

/**
 * Holds immutable metadata necessary to identify a record for
 * updating.
 */
public class LocalUpdateMeta implements UpdateMeta {
  private final String type;
  private final UUID recordId;
  private final String version;

  /**
   * @param type
   * @param recordId
   * @param version
   */
  public LocalUpdateMeta(String type, UUID recordId, String version) {
    this.type = type;
    this.recordId = recordId;
    this.version = version;
  }

  public static LocalUpdateMeta fromRecordMeta(RecordMeta meta) {
    return new LocalUpdateMeta(meta.type(), meta.recordId(), meta.version());
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public UUID getRecordId() {
    return recordId;
  }

  @Override
  public String getVersion() {
    return version;
  }
}
