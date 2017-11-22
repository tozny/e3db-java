package com.tozny.e3db;

/**
 * Indicates that the implementor will provide a deterministic serialization of
 * the document for signing purposes.
 */
public interface Signable {
  /**
   * A string representing the contents of the document. The value produced must
   * always identical for documents that are the "same."
   *
   * @return A string representing the document.
   */
  public String toSerialized();
}
