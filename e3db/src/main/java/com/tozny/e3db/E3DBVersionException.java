package com.tozny.e3db;

import java.util.UUID;

/**
 * Indicates that a version conflict prevented an update or delete.
 */
public class E3DBVersionException extends E3DBException {
  /**
   * ID of the record which had the conflict.
   */
  public final UUID recordId;
  /**
   * Version of the record, according to the client.
   */
  public final String version;

  public E3DBVersionException(UUID recordId, String version) {
    super(recordId.toString() + " with version " + version + " could not be updated.");
    this.recordId = recordId;
    this.version = version;
  }
}
