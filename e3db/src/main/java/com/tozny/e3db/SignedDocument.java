package com.tozny.e3db;

/**
 * Represents a document and its signature.
 *
 * @param <T> The document for which a signature was calculated.
 */
public interface SignedDocument<T extends Signable> {
  /**
   * The Ed25519 signature over the document in {@link #document()}, as a Base64URL-encoded
   * string.
   *
   * @return signature.
   */
  public String signature();

  /**
   * Returns the original document from which {@link #signature()} is
   * derived.
   *
   * @return document.
   */
  public T document();
}
