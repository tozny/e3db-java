package com.tozny.e3db;

import okio.ByteString;

public class CipherWithNonce {
  private final byte[] cipher;
  private final byte[] nonce;

  public CipherWithNonce(byte[] cipher, byte[] nonce) {
    this.cipher = cipher;
    this.nonce = nonce;
  }

  public byte[] getCipher() {
    return cipher;
  }

  public byte[] getNonce() {
    return nonce;
  }

  public String toMessage() {
    return new StringBuilder(ByteString.of(cipher).base64Url())
      .append(".")
      .append(ByteString.of(nonce).base64Url())
      .toString();
  }

  public static CipherWithNonce decode(String message) {
    int splitAt = message.indexOf('.');

    if(splitAt == -1)
      throw new IllegalArgumentException("Can't decode ciphertext.");

    return new CipherWithNonce(ByteString.decodeBase64(message.substring(0, splitAt)).toByteArray(),
      ByteString.decodeBase64(message.substring(splitAt + 1)).toByteArray());
  }
}
