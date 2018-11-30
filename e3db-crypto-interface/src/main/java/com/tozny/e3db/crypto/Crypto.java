/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3db.crypto;

import com.tozny.e3db.CipherSuite;
import com.tozny.e3db.CipherWithNonce;
import com.tozny.e3db.E3DBCryptoException;
import com.tozny.e3db.E3DBDecryptionException;
import com.tozny.e3db.E3DBEncryptionException;
import com.tozny.e3db.Signature;

import java.io.File;
import java.io.IOException;

/**
 * Provides cryptographic operations necessary for interacting with E3DB. Android and
 * plain Java implementations of this class are automatically included in each respective
 * client library.
 */
public interface Crypto {
  CipherSuite suite();

  /**
   * Encrypts a message using the secret key given.
   *
   * @param message Bytes to encrypt. Can be empty but not null.
   * @param key Secret key to encrypt with.  Must be a 32-byte array. To generate a key, use {@link #newSecretKey()}.
   *
   * @return The encrypted message and a nonce used during encryption.
   */
  CipherWithNonce encryptSecretBox(byte[] message, byte [] key) throws E3DBEncryptionException;

  /**
   * Decrypt a message encrypted with a secret key.
   *
   * @param message Message to decrypt, along with the nonce used during encryption.
   * @param key Secret key for decryption. Should be a byte array previously generated with {@link #newSecretKey()}.
   * @return The decrypted bytes, or throws an exception if decryption fails.
   */
  byte[] decryptSecretBox(CipherWithNonce message, byte [] key) throws E3DBDecryptionException;

  /**
   * Encrypt a message using public-key cryptography.
   *
   * @param message Bytes to encrypt. Can be empty but never null.
   * @param publicKey The public key of the recipient. A 32-byte array.
   * @param privateKey Private key of the sender. A 32-byte array. Private keys can be generated
   *                   with {@link #newPrivateKey()}.
   * @return The encrypted message plus a nonce used during encryption.
   */
  CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey) throws E3DBEncryptionException;

  /**
   * Decrypt a message for a given recipient.
   *
   * @param message Message to decrypt, plus a nonce used during encryption.
   * @param publicKey Public key of the sender (for authentication). A 32-byte array.
   * @param privateKey Private key of the recipient. A 32-byte array. Private keys can be generated
   *                   with {@link #newPrivateKey()}.
   * @return The decrypted message, or throws if decryption fails.
   */
  byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey) throws E3DBDecryptionException;

  /**
   * Extract the public key from a given private key.
   * @param privateKey A 32-byte array. Private keys can be generated withw {@link #newPrivateKey()}).
   * @return The public key, or throws if the argument does not represent a valid private key.
   */
  byte[] getPublicKey(byte[] privateKey) throws E3DBCryptoException;

  /**
   * Create a new private key for use with {@link #encryptBox(byte[], byte[], byte[])} and {@link #decryptBox(CipherWithNonce, byte[], byte[])}. The associated public key can be
   * extracted from the returned key using {@link #getPublicKey(byte[])}.
   * @return A new private key.
   */
  byte[] newPrivateKey() throws E3DBCryptoException;

  /**
   * Generate a new secret key for use with {@link #encryptSecretBox(byte[], byte[])} and {@link #decryptSecretBox(CipherWithNonce, byte[])}.
   * @return A new secret key.
   */
  byte[] newSecretKey();

  /**
   * Returns an Ed255119 private key that can be used for creating signatures.
   *
   * @return A new Ed25519 private key.
   */
  byte [] newPrivateSigningKey() throws E3DBCryptoException;

  /**
   * Extract the public key portion from a secret signing key.
   *
   * @param privateKey Ed25519 private key.
   * @return Public key portion of the private key.
   * @throws E3DBCryptoException
   */
  byte[] getPublicSigningKey(byte[] privateKey) throws E3DBCryptoException;

  /**
   * Creates an Ed25519 signature over the given message.
   *
   * @param message Message to sign.
   * @param signingKey Private Ed25519 key to use.
   * @return An Ed25519 signature.
   */
  byte[] signature(byte [] message, byte[] signingKey) throws E3DBCryptoException;

  /**
   * Verify a signature, given a document and public key.
   *
   * @param signature The Ed255199 signature to verify.
   * @param message Message used to generate the signature.
   * @param publicSigningKey Public portion of the Ed25519  key used to create the signature given.
   * @return {@code true} if the signatue was generated for the given document, using the private key
   * associated with the public key given. {@code false} otherwise.
   */
  boolean verify(Signature signature, byte[] message, byte[] publicSigningKey);

  /**
   * Encrypt the given file, using the key given, and return a
   * reference to the encrypted file.
   *
   * Enough temporary space must be available on the device to store
   * the encrypted file.
   *
   * @param file File to encrypt.
   * @param secretKey Key to encrypt with.
   * @return The encrypted file.
   */
  File encryptFile(File file, byte[] secretKey) throws IOException, E3DBCryptoException;

  /**
   * Decrypts the given file, assuming it was encrypted with the
   * given secret key.
   *
   * @param file Location of file to decrypt
   * @param secretKey Key to use when decrypting.
   * @param dest The destination to write the decrypted contents to. If this file exists, it will be truncated.
   * @throws IOException
   */
  void decryptFile(File file, byte[] secretKey, File dest) throws IOException, E3DBCryptoException;

  /**
   * Size of the block used when encrypting files (in bytes).
   * @return block size in bytes.
   */
  int getBlockSize();
}