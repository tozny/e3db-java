package com.tozny.e3db;

import java.util.UUID;

/**
 * Indiicates the requested record could not be found.
 *
 * This exception can occur when attempting to read a record
 * that does not exist.
 */
public class E3DBNotFoundException extends E3DBException {
  public final String recordId;

  public E3DBNotFoundException(UUID recordId) {
    super(recordId.toString() + " not found");
    this.recordId = recordId.toString();
  }
}
