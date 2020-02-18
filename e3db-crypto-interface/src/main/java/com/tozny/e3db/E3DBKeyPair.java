package com.tozny.e3db;

public class E3DBKeyPair {
  private byte[] publicKey;
  private byte[] privateKey;

  public E3DBKeyPair(byte[] publicKey, byte[] privateKey) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public byte[] getPrivateKey() {
    return privateKey;
  }
}
