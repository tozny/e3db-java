package com.tozny.e3db;

import java.util.UUID;

/**
 * Indicates the requested record could not be found.
 *
 * This exception can occur when attempting to read a record
 * that does not exist.
 */
public class E3DBNotFoundException extends E3DBException {
  /**
   * ID of Record that could not be found.
   */
  public final UUID recordId;

  public E3DBNotFoundException(UUID recordId) {
    super(recordId.toString() + " not found");
    this.recordId = recordId;
  }
}
