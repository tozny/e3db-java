package com.tozny.e3db;

public interface E3DBCrypto {
  /**
   * Encrypts the bytes given using the key given. key must be a 32-byte array. Returns a
   * a Base64-URL encoded string with the format EF.EFN (encrypted field/encrypted field nonce).
   *
   * @param message
   * @param key
   * @return A string containing two Base64 URL-encoded strings, separated by a period. The first
   *   portion is the encrypted message, the second is the nonce used during encryption.
   */
  CipherWithNonce encryptSecretBox(byte[] message, byte [] key);
  byte[] decryptSecretBox(CipherWithNonce message, byte [] key);
  byte[] decryptSecretBox(String message, byte [] key);

  CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey);
  byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey);
  byte[] decryptBox(String message, byte[] publicKey, byte[] privateKey);

  byte[] getPublicKey(byte[] privateKey);
  byte[] newPrivateKey();

  byte[] newSecretKey();
}