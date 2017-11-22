package com.tozny.e3db.crypto;

import com.tozny.e3db.CipherWithNonce;
import com.tozny.e3db.Crypto;
import com.tozny.e3db.Signature;

public class AndroidCrypto implements Crypto {
  @Override
  public CipherWithNonce encryptSecretBox(byte[] message, byte[] key) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] decryptSecretBox(CipherWithNonce message, byte[] key) {
    throw new IllegalStateException();
  }

  @Override
  public CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] getPublicKey(byte[] privateKey) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] newPrivateKey() {
    throw new IllegalStateException();
  }

  @Override
  public byte[] newSecretKey() {
    throw new IllegalStateException();
  }

  @Override
  public byte[] signature(byte [] message, byte[] signingKey) {
    throw new IllegalStateException();
  }

  @Override
  public boolean verify(Signature signature, byte[] message, byte[] publicSigningKey) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] newPrivateSigningKey() {
    throw new IllegalStateException();
  }

  @Override
  public byte[] getPublicSigningKey(byte[] privateKey) {
    throw new IllegalStateException();
  }
}
