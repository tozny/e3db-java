package com.tozny.e3db;

import java.util.UUID;

class E3DBVersionException extends E3DBException {
  public final UUID recordId;
  public final String version;

  public E3DBVersionException(UUID recordId, String version) {
    super(recordId.toString() + " with version " + version + " could not be updated.");
    this.recordId = recordId;
    this.version = version;
  }
}
