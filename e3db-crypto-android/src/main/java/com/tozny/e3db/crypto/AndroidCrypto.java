package com.tozny.e3db.crypto;

import com.tozny.e3db.CipherWithNonce;
import com.tozny.e3db.Crypto;
import com.tozny.e3db.Signature;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.crypto.Box;
import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.crypto.SecretBox;

import java.io.UnsupportedEncodingException;

import static com.tozny.e3db.Checks.*;

import static org.libsodium.jni.Sodium.*;

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
    int result = sodium.crypto_box_keypair(privateKey, publicKey);

    if(result != 0)
      throw new RuntimeException("crypto_box_keypair: " + result);

    return privateKey;
  }

  @Override
  public byte[] getPublicKey(byte[] privateKey) {
    checkNotEmpty(privateKey, "privateKey");
    byte[] publicKey = new byte[sodium.crypto_box_publickeybytes()];
    int result = sodium.crypto_scalarmult_base(publicKey, privateKey);

    if(result != 0)
      throw new RuntimeException("crypto_scalarmult_base: " + result);

    return publicKey;
  }

  @Override
  public byte[] newSecretKey() {
    return random.randomBytes(crypto_secretbox_keybytes());
  }

  @Override
  public byte[] signature(byte[] message, byte[] signingKey) {
    byte[] dst = new byte[sodium.crypto_sign_bytes()];
    int result = sodium.crypto_sign_detached(dst, new int[0], message, message.length, signingKey);

    if(result != 0)
      throw new RuntimeException("crypto_sign_detached: " + result);

    return dst;
  }

  @Override
  public boolean verify(Signature signature, byte[] message, byte[] publicSigningKey) {
    checkNotNull(signature, "signature");
    checkNotNull(message, "message");
    checkNotNull(publicSigningKey, "publicSigningKey");

    return sodium.crypto_sign_verify_detached(signature.bytes, message, message.length, publicSigningKey) == 0;
  }

  @Override
  public byte[] newPrivateSigningKey() {
    byte[] sk = new byte[crypto_sign_secretkeybytes()];
    byte[] pk = new byte[crypto_sign_publickeybytes()];
    int result = sodium.crypto_sign_keypair(pk, sk);

    if(result != 0)
      throw new RuntimeException("crypto_sign_keypair: " + result);

    return sk;
  }

  @Override
  public byte[] getPublicSigningKey(byte[] privateKey) {
    byte[] dst = new byte[crypto_sign_publickeybytes()];
    int result = crypto_sign_ed25519_sk_to_pk(dst, privateKey);

    if(result != 0)
      throw new RuntimeException("crypto_sign_ed25519_sk_to_pk: " + result);

    return dst;
  }
}
