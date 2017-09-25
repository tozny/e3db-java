package com.tozny.e3db.crypto;

import com.tozny.e3db.CipherWithNonce;
import com.tozny.e3db.Crypto;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.crypto.Box;
import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.crypto.SecretBox;

import static com.tozny.e3db.Checks.*;

import static org.libsodium.jni.Sodium.crypto_box_noncebytes;
import static org.libsodium.jni.Sodium.crypto_secretbox_keybytes;
import static org.libsodium.jni.Sodium.crypto_secretbox_noncebytes;
import static org.libsodium.jni.SodiumJNI.crypto_box_keypair;

public class AndroidCrypto implements Crypto {
  private final Sodium sodium;
  private final Random random;

  public AndroidCrypto() {
    // Make sure libsodium initialization occurs.
    sodium = SodiumInit.sodium;
    random = new Random();
  }

  private static class SodiumInit {
    // Static inner class as a singleton to make sure
    // Sodium library is initalized once and only once.
    public static Sodium sodium = NaCl.sodium();
  }

  @Override
  public CipherWithNonce encryptSecretBox(byte[] message, byte[] key) {
    checkNotNull(message, "message");
    checkNotEmpty(key, "key");
    byte[] nonce = random.randomBytes(crypto_secretbox_noncebytes());
    byte[] cipher = new SecretBox(key).encrypt(nonce, message);
    return new CipherWithNonce(cipher, nonce);
  }

  @Override
  public byte[] decryptSecretBox(CipherWithNonce message, byte[] key) {
    checkNotNull(message, "message");
    checkNotEmpty(key, "key");
    return new SecretBox(key).decrypt(message.getNonce(), message.getCipher());
  }

  @Override
  public CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey) {
    checkNotNull(message, "message");
    checkNotEmpty(publicKey, "publicKey");
    checkNotEmpty(privateKey, "privateKey");
    byte [] nonce = random.randomBytes(crypto_box_noncebytes());
    byte[] cipher = new Box(publicKey, privateKey).encrypt(nonce, message);
    return new CipherWithNonce(cipher, nonce);
  }

  @Override
  public byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey) {
    checkNotNull(message, "message");
    checkNotNull(publicKey, "publicKey");
    checkNotNull(privateKey, "privateKey");
    return new Box(publicKey, privateKey).decrypt(message.getNonce(), message.getCipher());
  }

  @Override
  public byte[] newPrivateKey() {
    byte[] privateKey = new byte[sodium.crypto_box_secretkeybytes()];
    byte[] publicKey = new byte[sodium.crypto_box_publickeybytes()];
    crypto_box_keypair(privateKey, publicKey);
    return privateKey;
  }

  @Override
  public byte[] getPublicKey(byte[] privateKey) {
    checkNotEmpty(privateKey, "privateKey");
    byte[] publicKey = new byte[sodium.crypto_box_publickeybytes()];
    sodium.crypto_scalarmult_base(publicKey, privateKey);
    return publicKey;
  }

  @Override
  public byte[] newSecretKey() {
    return random.randomBytes(crypto_secretbox_keybytes());
  }
}
