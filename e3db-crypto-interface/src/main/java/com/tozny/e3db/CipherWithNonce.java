package com.tozny.e3db;

import okio.ByteString;

import static com.tozny.e3db.Base64.decodeURL;
import static com.tozny.e3db.Base64.encodeURL;
import static com.tozny.e3db.Checks.*;

public class CipherWithNonce {
  private final byte[] cipher;
  private final byte[] nonce;

  public CipherWithNonce(byte[] cipher, byte[] nonce) {
    checkNotNull(cipher, "cipher");
    checkNotEmpty(nonce, "nonce");
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
    return new StringBuilder(encodeURL(cipher))
      .append(".")
      .append(encodeURL(nonce))
      .toString();
  }

  public static CipherWithNonce decode(String message) {
    checkNotEmpty(message, "message");
    int splitAt = message.indexOf('.');

    if(splitAt == -1)
      throw new IllegalArgumentException("Can't decode ciphertext.");

    return new CipherWithNonce(decodeURL(message.substring(0, splitAt)),
      decodeURL(message.substring(splitAt + 1)));
  }
}
