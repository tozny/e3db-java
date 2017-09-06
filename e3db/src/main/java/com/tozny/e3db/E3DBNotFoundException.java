package com.tozny.e3db;

import java.util.UUID;

public class E3DBNotFoundException extends E3DBException {
  public final String recordId;

  public E3DBNotFoundException(UUID recordId) {
    super(recordId.toString() + " not found");
    this.recordId = recordId.toString();
  }
}
