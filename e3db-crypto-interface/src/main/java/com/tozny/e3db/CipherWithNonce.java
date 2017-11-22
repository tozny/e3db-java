package com.tozny.e3db;

import okio.ByteString;

import static com.tozny.e3db.Base64.decodeURL;
import static com.tozny.e3db.Base64.encodeURL;
import static com.tozny.e3db.Checks.*;

/**
 * Holds an encrypted message with the nonce value used
 * during encryption.
 */
public class CipherWithNonce {
  private final byte[] cipher;
  private final byte[] nonce;

  /**
   * Create a container holding an encrypted message and the nonce
   * used during encryption.
   *
   * @param cipher cipher.
   * @param nonce nonce.
   */
  public CipherWithNonce(byte[] cipher, byte[] nonce) {
    checkNotNull(cipher, "cipher");
    checkNotEmpty(nonce, "nonce");
    this.cipher = cipher;
    this.nonce = nonce;
  }

  /**
   * Get the encrypted message.
   *
   * @return cipher.
   */
  public byte[] getCipher() {
    return cipher;
  }

  /**
   * Get the nonce used during encryption of the message.
   *
   * @return nonce.
   */
  public byte[] getNonce() {
    return nonce;
  }

  /**
   * Convert the encrypted message and its nonce to a string with the format <i>MESSAGE64</i>.<i>NONCE64</i>, where
   * <i>MESSAGE64</i> is a Base64URL-encoded representation of the encrypted message, and <i>NONCE64</i> is a Base64URL-encoded
   * representation of the nonce.
   *
    * @return The encoded message and its nonce.
   */
  public String toMessage() {
    return new StringBuilder(encodeURL(cipher))
      .append(".")
      .append(encodeURL(nonce))
      .toString();
  }

  /**
   * Decode a string (in the same format as given by {@link #toMessage()}) into an encrypted
   * message and an associated nonce.
   * @param message The message to decode.
   * @return The encrypted message and its nonce.
   */
  public static CipherWithNonce decode(String message) {
    checkNotEmpty(message, "message");
    int splitAt = message.indexOf('.');

    if(splitAt == -1)
      throw new IllegalArgumentException("Can't decode ciphertext.");

    return new CipherWithNonce(decodeURL(message.substring(0, splitAt)),
      decodeURL(message.substring(splitAt + 1)));
  }
}
