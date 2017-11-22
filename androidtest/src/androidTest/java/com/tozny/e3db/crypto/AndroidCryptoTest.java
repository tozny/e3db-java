package com.tozny.e3db.crypto;

import com.tozny.e3db.Crypto;
import com.tozny.e3db.Signature;
import com.tozny.e3db.crypto.AndroidCrypto;

import org.junit.Test;
import org.libsodium.jni.crypto.Random;
import static org.libsodium.jni.Sodium.*;

import static junit.framework.Assert.*;

public class AndroidCryptoTest {
  private final Crypto crypto = new AndroidCrypto();
  private final Random random = new Random();

  @Test
  public void testValidSignature() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = random.randomBytes(100_000);
    byte[] signature = crypto.signature(document, privateSigningKey);
    assertTrue("Unable to verify signature on document.", crypto.verify(new Signature(signature), document, publicSigningKey));
  }

  @Test
  public void testInvalidSignature() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = random.randomBytes(100_000);
    byte[] signature = random.randomBytes(crypto_sign_bytes());

    assertFalse("Verification should fail with wrong signature.", crypto.verify(new Signature(signature), document, publicSigningKey));
    assertFalse("Verification should fail with zero signature.", crypto.verify(new Signature(new byte[crypto_sign_bytes()]), document, publicSigningKey));
    assertFalse("Verification should fail with empty signature.", crypto.verify(new Signature(new byte[0]), document, publicSigningKey));
  }

  @Test
  public void testWrongPublicKey() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] wrongSigningKey = random.randomBytes(crypto_sign_publickeybytes());
    byte[] document = random.randomBytes(100_000);

    byte[] signature = crypto.signature(document, privateSigningKey);
    assertFalse("Verification should fail with wrong public key.", crypto.verify(new Signature(signature), document, wrongSigningKey));
    assertFalse("Verification should fail with zero public key.", crypto.verify(new Signature(signature), document, new byte[crypto_sign_publickeybytes()]));
    assertFalse("Verification should fail with empty public key.", crypto.verify(new Signature(signature), document, new byte[0]));
  }

  @Test
  public void testWrongDocument() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);
    byte[] document = random.randomBytes(100_000);
    byte[] wrongDocument = random.randomBytes(100_000);
    byte[] signature = crypto.signature(document, privateSigningKey);

    assertFalse("Verification should fail with wrong document.", crypto.verify(new Signature(signature), wrongDocument, publicSigningKey));
    assertFalse("Verification should fail with zero document.", crypto.verify(new Signature(signature), new byte[100_000], publicSigningKey));
    assertFalse("Verification should fail with empty document.", crypto.verify(new Signature(signature), new byte[0], publicSigningKey));
  }
}
