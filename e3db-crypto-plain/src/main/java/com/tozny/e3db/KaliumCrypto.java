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

package com.tozny.e3db;

import com.tozny.e3db.crypto.*;
import org.abstractj.kalium.NaCl;
import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.keys.KeyPair;

import java.util.Arrays;

import static com.tozny.e3db.Checks.*;
import static org.abstractj.kalium.NaCl.Sodium.*;

class KaliumCrypto implements Crypto {
  private final static Random random = new Random();
  private final static int ED25519_SEEDBYTES = 32;

  @Override
  public CipherWithNonce encryptSecretBox(byte[] message, byte[] key) {
    checkNotNull(message, "message");
    checkNotEmpty(key, "key");
    byte[] nonce = random.randomBytes(CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES);
    return new CipherWithNonce(new SecretBox(key).encrypt(nonce, message), nonce);
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
    Box box = new Box(publicKey, privateKey);
    byte[] nonce = random.randomBytes(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_NONCEBYTES);
    return new CipherWithNonce(box.encrypt(nonce, message), nonce);
  }

  @Override
  public byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey) {
    checkNotNull(message, "message");
    checkNotNull(publicKey, "publicKey");
    checkNotNull(privateKey, "privateKey");
    Box box = new Box(publicKey, privateKey);
    return box.decrypt(message.getNonce(), message.getCipher());
  }

  @Override
  public byte[] getPublicKey(byte[] privateKey) {
    checkNotEmpty(privateKey, "privateKey");
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

  @Override
  public byte[] signature(byte [] message, byte[] signingKey) {
    checkNotNull(message, "message");
    checkNotNull(signingKey, "signingKey");

    byte[] combined = new byte[CRYPTO_SIGN_ED25519_BYTES + message.length];
    int result = NaCl.sodium().crypto_sign_ed25519(combined, null, message, message.length, signingKey);

    if(result != 0)
      throw new RuntimeException("crypto_sign_ed25519: " + result);

    return Arrays.copyOf(combined, CRYPTO_SIGN_ED25519_BYTES);
  }

  @Override
  public boolean verify(Signature signature, byte[] message, byte[] publicSigningKey) {
    checkNotNull(signature, "bytes");
    checkNotNull(message, "message");
    checkNotNull(publicSigningKey, "publicSigningKey");

    byte[] combined = new byte[signature.bytes.length + message.length];
    System.arraycopy(signature.bytes, 0, combined, 0, signature.bytes.length);
    System.arraycopy(message, 0, combined, signature.bytes.length, message.length);
    return NaCl.sodium().crypto_sign_ed25519_open(new byte[message.length], null, combined, combined.length, publicSigningKey) == 0;
  }

  @Override
  public byte[] newPrivateSigningKey() {
    byte[] seed = new byte[ED25519_SEEDBYTES];
    byte[] publicSigningKey = new byte[CRYPTO_SIGN_ED25519_PUBLICKEYBYTES];
    byte[] privateSigningKey = new byte[CRYPTO_SIGN_ED25519_SECRETKEYBYTES];

    NaCl.sodium().randombytes(seed, seed.length);
    int result = NaCl.sodium().crypto_sign_ed25519_seed_keypair(publicSigningKey, privateSigningKey, seed);

    if(result != 0)
      throw new RuntimeException("crypto_sign_ed25519_seed_keypair: " + result);

    return privateSigningKey;
  }

  @Override
  public byte[] getPublicSigningKey(byte[] privateKey) {
    checkNotNull(privateKey, "privateKey");

    byte[] publicSigningKey = new byte[CRYPTO_SIGN_ED25519_PUBLICKEYBYTES];
    // From libsodium's crypto_sign_ed25519_sk_to_pk (see
    // https://github.com/jedisct1/libsodium/blob/1a3b474f7f6e6c01c56f2b3f53c1d30ed74e54ef/src/libsodium/crypto_sign/ed25519/sign_ed25519.c)

    System.arraycopy(privateKey, ED25519_SEEDBYTES, publicSigningKey, 0, publicSigningKey.length);
    return publicSigningKey;
  }
}
