package com.tozny.e3db;

/**
 * Thrown when a record fails verification during decryption.
 */
public class E3DBVerificationException extends E3DBException {
  private final RecordMeta meta;

  public E3DBVerificationException(RecordMeta meta) {
    super("Document failed verification.");
    this.meta = meta;
  }

  public E3DBVerificationException(RecordMeta meta, Exception inner) {
    super("Document failed verification.", inner);
    this.meta = meta;
  }

  public E3DBVerificationException(RecordMeta meta, String message, Exception inner) {
    super(message, inner);
    this.meta = meta;
  }
}
