package com.tozny.e3db.crypto;

import com.tozny.e3db.CipherWithNonce;
import com.tozny.e3db.Crypto;

import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.keys.KeyPair;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_NONCEBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_KEYBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES;

public class KaliumCrypto implements Crypto {
  private static Random random = new Random();

  public static class Bar {}

  @Override
  public CipherWithNonce encryptSecretBox(byte[] message, byte[] key) {
    byte[] nonce = random.randomBytes(CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES);
    return new CipherWithNonce(new SecretBox(key).encrypt(nonce, message), nonce);
  }

  @Override
  public byte[] decryptSecretBox(CipherWithNonce message, byte[] key) {
    return new SecretBox(key).decrypt(message.getNonce(), message.getCipher());
  }

  @Override
  public byte[] decryptSecretBox(String cipher, byte[] key) {
    CipherWithNonce message = CipherWithNonce.decode(cipher);
    return new SecretBox(key).decrypt(message.getNonce(), message.getCipher());
  }

  @Override
  public CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey) {
    Box box = new Box(publicKey, privateKey);
    byte[] nonce = random.randomBytes(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_NONCEBYTES);
    return new CipherWithNonce(box.encrypt(nonce, message), nonce);
  }

  @Override
  public byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey) {
    Box box = new Box(publicKey, privateKey);
    return box.decrypt(message.getNonce(), message.getCipher());
  }

  @Override
  public byte[] decryptBox(String message, byte[] publicKey, byte[] privateKey) {
    return decryptBox(CipherWithNonce.decode(message), publicKey, privateKey);
  }

  @Override
  public byte[] getPublicKey(byte[] privateKey) {
    return new KeyPair(privateKey).getPublicKey().toBytes();
  }

  @Override
  public byte[] newPrivateKey() {
    return new KeyPair().getPrivateKey().toBytes();
  }

  @Override
  public byte[] newSecretKey() {
    return random.randomBytes(CRYPTO_SECRETBOX_XSALSA20POLY1305_KEYBYTES);
  }
}
