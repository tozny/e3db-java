package com.tozny.e3db.crypto;

import com.tozny.e3db.CipherWithNonce;
import com.tozny.e3db.E3DBCrypto;

public class AndroidE3DBCrypto implements E3DBCrypto {
  @Override
  public CipherWithNonce encryptSecretBox(byte[] message, byte[] key) {
    return null;
  }

  @Override
  public byte[] decryptSecretBox(CipherWithNonce message, byte[] key) {
    return new byte[0];
  }

  @Override
  public byte[] decryptSecretBox(String message, byte[] key) {
    return new byte[0];
  }

  @Override
  public CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey) {
    return null;
  }

  @Override
  public byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey) {
    return new byte[0];
  }

  @Override
  public byte[] decryptBox(String message, byte[] publicKey, byte[] privateKey) {
    return new byte[0];
  }

  @Override
  public byte[] getPublicKey(byte[] privateKey) {
    return new byte[0];
  }

  @Override
  public byte[] newPrivateKey() {
    return new byte[0];
  }

  @Override
  public byte[] newSecretKey() {
    return new byte[0];
  }
}
