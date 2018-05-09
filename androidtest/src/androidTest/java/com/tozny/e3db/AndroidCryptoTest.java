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

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.internal.runner.listener.InstrumentationResultPrinter;
import com.tozny.e3db.crypto.*;
import junit.framework.Assert;
import org.junit.Test;
import org.libsodium.jni.crypto.Random;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.UUID;

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

  @Test
  public void testGCM() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException {
    Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
    KeyGenerator aes = KeyGenerator.getInstance("AES");
    aes.init(256);
    SecretKey key = aes.generateKey();
    SecureRandom random = new SecureRandom();
    byte[] iv = new byte[12];
    random.nextBytes(iv);

    byte[] plainbytes = "hello".getBytes("UTF-8");
    AlgorithmParameterSpec params = getParams(iv);
    encryptCipher.init(Cipher.ENCRYPT_MODE, key, params);
    byte[] encrypted = encryptCipher.doFinal(plainbytes);

    {
      Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
      decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
      byte[] decrypted = decryptCipher.doFinal(encrypted);

      org.junit.Assert.assertArrayEquals("Could not decrypt.", plainbytes, decrypted);
    }

    try {
      Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
      decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
      byte[] badEncrypted = Arrays.copyOf(encrypted, encrypted.length);
      badEncrypted[badEncrypted.length - 1] = (byte) (badEncrypted[badEncrypted.length - 1] + 1);
      decryptCipher.doFinal(badEncrypted);
      fail("Should not decrypt altered bytes.");
    }
    catch(BadPaddingException e) {
      // success
    }

    try {
      Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
      decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
      byte[] badEncrypted = Arrays.copyOf(encrypted, encrypted.length);
      badEncrypted[0] = (byte) (badEncrypted[0] + 1);
      decryptCipher.doFinal(badEncrypted);
      fail("Should not decrypt altered bytes.");
    }
    catch(BadPaddingException e) {
      // success
    }
  }

  @Test
  public void testGCMAKS() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException {
    if(Build.VERSION.SDK_INT >= 23) {
      byte[] plainbytes = "hello".getBytes("UTF-8");
      String keystoreAlias = UUID.randomUUID().toString();

      KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(keystoreAlias,KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
         .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
         .setKeySize(256)
         .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
         .build();
      KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
      keyGenerator.init(spec);
      SecretKey key = keyGenerator.generateKey();

      Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
      encryptCipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] encrypted = encryptCipher.doFinal(plainbytes);
      byte[] iv = encryptCipher.getIV();
      AlgorithmParameterSpec params = getParams(iv);
      {
        Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        org.junit.Assert.assertArrayEquals("Could not decrypt.", plainbytes, decrypted);
      }

      try {
        Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
        byte[] badEncrypted = Arrays.copyOf(encrypted, encrypted.length);
        badEncrypted[badEncrypted.length - 1] = (byte) (badEncrypted[badEncrypted.length - 1] + 1);
        decryptCipher.doFinal(badEncrypted);
        fail("Should not decrypt altered bytes.");
      }
      catch (BadPaddingException e) {
        // success
      }

      try {
        Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
        byte[] badEncrypted = Arrays.copyOf(encrypted, encrypted.length);
        badEncrypted[0] = (byte) (badEncrypted[0] + 1);
        decryptCipher.doFinal(badEncrypted);
        fail("Should not decrypt altered bytes.");
      } catch (BadPaddingException e) {
        // success
      }
    }
  }

  private AlgorithmParameterSpec getParams(byte[] iv) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
      return new GCMParameterSpec(128, iv);
    else
      return new IvParameterSpec(iv);
  }

}
