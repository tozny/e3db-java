package com.tozny.e3db;

import static com.tozny.e3db.Checks.*;

/**
 * Holds the bytes representing an Ed25519 signature.
 */
public class Signature {
  public final byte[] bytes;

  public Signature(byte[] signature) {
    checkNotNull(signature, "bytes");
    this.bytes = signature;
  }
}
