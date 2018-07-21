package com.tozny.e3db;

import static com.tozny.e3db.KeyType.Curve25519;
import static com.tozny.e3db.KeyType.Ed25519;
import static com.tozny.e3db.KeyType.P384;

public enum CipherSuite {
  FIPS(P384, P384),
  Sodium(Curve25519, Ed25519);

  private final KeyType encryptionKey;

  private final KeyType signingKey;
  CipherSuite(KeyType encryptionKey, KeyType signingKey) {

    this.encryptionKey = encryptionKey;
    this.signingKey = signingKey;
  }

  public KeyType getSigningKey() {
    return signingKey;
  }

  public KeyType getEncryptionKey() {
    return encryptionKey;
  }
}
