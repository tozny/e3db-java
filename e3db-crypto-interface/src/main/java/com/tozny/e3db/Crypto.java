package com.tozny.e3db;

/**
 * Provides cryptographic operations necessary for interacting with E3DB. Android and
 * plain Java implementations of this class are automatically included in each respective
 * client library (see <a href="package-summary.html#package.description">the package overview</a> for
 * instructions on referencing this library from each type of application).
 */
public interface Crypto {
  /**
   * Encrypts a message using the secret key given.
   *
   * @param message Bytes to encrypt. Can be empty but not null.
   * @param key Secret key to encrypt with.  Must be a 32-byte array. To generate a key, use {@link #newSecretKey()}.
   *
   * @return The encrypted message and a nonce used during encryption.
   */
  CipherWithNonce encryptSecretBox(byte[] message, byte [] key);

  /**
   * Decrypt a message encrypted with a secret key.
   *
   * @param message Message to decrypt, along with the nonce used during encryption.
   * @param key Secret key for decryption. Should be a byte array previously generated with {@link #newSecretKey()}.
   * @return The decrypted bytes, or throws an exception if decryption fails.
   */
  byte[] decryptSecretBox(CipherWithNonce message, byte [] key);

  /**
   * Encrypt a message using public-key cryptography.
   * @param message Bytes to encrypt. Can be empty but never null.
   * @param publicKey The public key of the recipient. A 32-byte array.
   * @param privateKey Private key of the sender. A 32-byte array. Private keys can be generated
   *                   with {@link #newPrivateKey()}.
   * @return The encrypted message plus a nonce used during encryption.
   */
  CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey);

  /**
   * Decrypt a message for a given recipient.
   *
   * @param message Message to decrypt, plus a nonce used during encryption.
   * @param publicKey Public key of the sender (for authentication). A 32-byte array.
   * @param privateKey Private key of the recipient. A 32-byte array. Private keys can be generated
   *                   with {@link #newPrivateKey()}.
   * @return The decrypted message, or throws if decryption fails.
   */
  byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey);

  /**
   * Extract the public key from a given private key.
   * @param privateKey A 32-byte array. Private keys can be generated withw {@link #newPrivateKey()}).
   * @return The public key, or throws if the argument does not represent a valid private key.
   */
  byte[] getPublicKey(byte[] privateKey);

  /**
   * Create a new private key for use with {@link #encryptBox(byte[], byte[], byte[])} and {@link #decryptBox(CipherWithNonce, byte[], byte[])}. The associated public key can be
   * extracted from the returned key using {@link #getPublicKey(byte[])}.
   * @return A new private key.
   */
  byte[] newPrivateKey();

  /**
   * Generate a new secret key for use with {@link #encryptSecretBox(byte[], byte[])} and {@link #decryptSecretBox(CipherWithNonce, byte[])}.
   * @return A new secret key.
   */
  byte[] newSecretKey();
}