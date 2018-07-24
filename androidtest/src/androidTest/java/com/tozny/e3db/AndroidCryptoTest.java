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

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import com.goterl.lazycode.lazysodium.LazySodium;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.SodiumAndroid;
import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.interfaces.SecretStream;
import com.goterl.lazycode.lazysodium.interfaces.Sign;
import com.tozny.e3db.crypto.*;
import org.junit.Test;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static com.goterl.lazycode.lazysodium.LazySodium.toHex;
import static junit.framework.Assert.*;

public class AndroidCryptoTest {
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private final Crypto crypto = new AndroidCrypto();
  private final LazySodium lazySodium = new LazySodiumAndroid(new SodiumAndroid());

  private InputStream readFile(File encrypted) throws IOException {
    ByteArrayInputStream cipherIn;
    {
      byte [] cipher = new byte[new Long(encrypted.length()).intValue()];
      FileInputStream in = new FileInputStream(encrypted);
      try {
        in.read(cipher);
      } finally {
        in.close();
      }

      cipherIn = new ByteArrayInputStream(cipher);
    }
    return cipherIn;
  }

  private AlgorithmParameterSpec getParams(byte[] iv) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
      return new GCMParameterSpec(128, iv);
    else
      return new IvParameterSpec(iv);
  }

  @Test
  public void testValidSignature() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = lazySodium.randomBytesBuf(100_000);
    byte[] signature = crypto.signature(document, privateSigningKey);
    assertTrue("Unable to verify signature on document.", crypto.verify(new Signature(signature), document, publicSigningKey));
  }

  @Test
  public void testInvalidSignature() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = lazySodium.randomBytesBuf(100_000);
    byte[] signature = lazySodium.randomBytesBuf(Sign.BYTES);

    assertFalse("Verification should fail with wrong signature.", crypto.verify(new Signature(signature), document, publicSigningKey));
    assertFalse("Verification should fail with zero signature.", crypto.verify(new Signature(new byte[Sign.BYTES]), document, publicSigningKey));
    assertFalse("Verification should fail with empty signature.", crypto.verify(new Signature(new byte[0]), document, publicSigningKey));
  }

  @Test
  public void testWrongPublicKey() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] wrongSigningKey = lazySodium.randomBytesBuf(Sign.PUBLICKEYBYTES);
    byte[] document = lazySodium.randomBytesBuf(100_000);

    byte[] signature = crypto.signature(document, privateSigningKey);
    assertFalse("Verification should fail with wrong public key.", crypto.verify(new Signature(signature), document, wrongSigningKey));
    assertFalse("Verification should fail with zero public key.", crypto.verify(new Signature(signature), document, new byte[Sign.PUBLICKEYBYTES]));
    assertFalse("Verification should fail with empty public key.", crypto.verify(new Signature(signature), document, new byte[0]));
  }

  @Test
  public void testWrongDocument() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);
    byte[] document = lazySodium.randomBytesBuf(100_000);
    byte[] wrongDocument = lazySodium.randomBytesBuf(100_000);
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

    byte[] plainbytes = "hello".getBytes(UTF8);
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
      byte[] plainbytes = "hello".getBytes(UTF8);
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

  @Test
  public void testEncryptFile() throws IOException, SodiumException {
    File plain = File.createTempFile("test", ".txt");
    plain.deleteOnExit();
    String message = "Stately, plump Buck Mulligan came from the stairhead, bearing a bowl of\n" +
                         "lather on which a mirror and a razor lay crossed. A yellow dressinggown,\n" +
                         "ungirdled, was sustained gently behind him on the mild morning air.";
    FileOutputStream out = new FileOutputStream(plain);
    try {
      out.write(message.getBytes(UTF8));
    }
    finally {
      out.close();
    }

    byte[] secretKey = crypto.newSecretKey();
    File encrypted = crypto.encryptFile(plain, secretKey);
    assertTrue("Invalid encrypted file returned.", encrypted != null && encrypted.exists());
    assertTrue("Plain file not found.", plain.exists());

    InputStream cipherIn = readFile(encrypted);

    // Read version
    String field = new String(new byte[] { (byte) cipherIn.read() }, UTF8);
    String sep = new String(new byte[]{(byte) cipherIn.read()}, UTF8);

    assertEquals("Version not found in header", "3", field);
    assertEquals("Separator not fond", ".", sep);

    CipherWithNonce edkDecoded = null;
    {
      StringBuffer edk = new StringBuffer();
      int sepCnt = 0;
      while(sepCnt < 2) {
        String b = new String(new byte[]{(byte) cipherIn.read()}, UTF8);
        if (b.equalsIgnoreCase("."))
          sepCnt += 1;

        if (sepCnt < 2)
          edk.append(b);
      }

      // Read SecretStream header
      edkDecoded = CipherWithNonce.decode(edk.toString());
    }
    Log.d("AndroidCryptoTest", edkDecoded.toMessage());

    byte[] dataKey = crypto.decryptSecretBox(edkDecoded, secretKey);
    Log.d("AndroidCryptoTest", toHex(dataKey));
    byte[] header = new byte[SecretStream.HEADERBYTES];
    cipherIn.read(header);
    Log.d("AndroidCryptoTest", toHex(header));

    SecretStream.State state = lazySodium.cryptoSecretStreamInitPull(header, toHex(dataKey));
    ByteBuffer allPlainBytes = ByteBuffer.allocate((int) plain.length());
    {
      byte[] encBlock = new byte[65_636 + SecretStream.ABYTES];
      int amt = cipherIn.read(encBlock);
      byte[] plainTag = {0};
      while(amt != -1) {
        byte[] plainBytes = new byte[crypto.getBlockSize()];
        long[] plainLen = new long[1];
        lazySodium.cryptoSecretStreamPull(state, plainBytes, plainLen, plainTag, encBlock, amt, new byte[0], 0);
        assertTrue("Every message should have MESSAGE or FINAL tag. Got: " + plainTag[0], plainTag[0] == SecretStream.TAG_MESSAGE || plainTag[0] == SecretStream.TAG_FINAL);
        allPlainBytes.put(plainBytes, 0, amt - SecretStream.ABYTES);
        amt = cipherIn.read(encBlock);
      }
      assertEquals("Expected message to end with FINAL tag", SecretStream.TAG_FINAL, plainTag[0]);
    }

    {
      allPlainBytes.flip();
      byte[] decryptedMessage = new byte[allPlainBytes.limit()];
      allPlainBytes.get(decryptedMessage);
      String actual = new String(decryptedMessage, UTF8);
      assertEquals("Didnt decrypt", message, actual);
    }
  }

  @Test
  public void testRoundTripSecretStream() throws SodiumException {
    String message1 = "Hello";
    String message2 = "World";
    byte[] header = new byte[SecretStream.HEADERBYTES];

    String secretKey = lazySodium.cryptoSecretBoxKeygen();
    SecretStream.State encState = lazySodium.cryptoSecretStreamInitPush(header, secretKey);

    String c1 = lazySodium.cryptoSecretStreamPush(encState, message1, SecretStream.TAG_MESSAGE);
    String c2 = lazySodium.cryptoSecretStreamPush(encState, message2, SecretStream.TAG_MESSAGE);

    SecretStream.State decState = lazySodium.cryptoSecretStreamInitPull(header, secretKey);
    String decM1 = lazySodium.cryptoSecretStreamPull(decState, c1, new byte[1]);
    String decM2 = lazySodium.cryptoSecretStreamPull(decState, c2, new byte[1]);

    assertEquals(message1, decM1);
    assertEquals(message2, decM2);
  }

  @Test
  public void testRoundtripFile() throws IOException {
    byte [] plain = lazySodium.randomBytesBuf(100 + 1);
    File plainFile = File.createTempFile("e2e", "");
    plainFile.deleteOnExit();

    FileOutputStream plainOut = new FileOutputStream(plainFile, false);
    try {
      plainOut.write(plain);
    }
    finally {
      plainOut.close();
    }

    byte[] secretKey = crypto.newSecretKey();
    File encryptFile = crypto.encryptFile(plainFile, secretKey);
    encryptFile.deleteOnExit();

    File plainResultFile = File.createTempFile("e2e-", ".html");
    plainResultFile.deleteOnExit();

    crypto.decryptFile(encryptFile, secretKey, plainResultFile);
    FileInputStream expected = new FileInputStream(plainFile);
    FileInputStream actual = new FileInputStream(plainResultFile);
    try {
      int a, b, pos = 1;
      for(a = expected.read(), b = actual.read(); a != -1 && b != -1; a = expected.read(), b = actual.read(), pos++ ) {
        assertEquals("Files differed at position " + pos,  a, b);
      }
      assertTrue("Files not the same length.", a == -1 && a == b);
    }
    finally {
      expected.close();;
      actual.close();
    }
  }

  @Test
  public void testBlockSizeFile() throws IOException {
    byte [] plain = lazySodium.randomBytesBuf(Platform.crypto.getBlockSize());
    File plainFile = File.createTempFile("e2e", "");
    plainFile.deleteOnExit();

    FileOutputStream plainOut = new FileOutputStream(plainFile, false);
    try {
      plainOut.write(plain);
    }
    finally {
      plainOut.close();
    }

    byte[] secretKey = crypto.newSecretKey();
    File encryptFile = crypto.encryptFile(plainFile, secretKey);
    encryptFile.deleteOnExit();

    File plainResultFile = File.createTempFile("e2e-", ".html");
    plainResultFile.deleteOnExit();

    crypto.decryptFile(encryptFile, secretKey, plainResultFile);
    FileInputStream expected = new FileInputStream(plainFile);
    FileInputStream actual = new FileInputStream(plainResultFile);
    try {
      int a, b, pos = 1;
      for(a = expected.read(), b = actual.read(); a != -1 && b != -1; a = expected.read(), b = actual.read(), pos++ ) {
        assertEquals("Files differed at position " + pos,  a, b);
      }
      assertTrue("Files not the same length.", a == -1 && a == b);
    }
    finally {
      expected.close();;
      actual.close();
    }
  }

  @Test
  public void testSigningSameDocs() {
    byte [] doc = ("Stately, plump Buck Mulligan came from the stairhead, bearing a bowl of\n" +
                       "lather on which a mirror and a razor lay crossed. A yellow dressinggown,\n" +
                       "ungirdled, was sustained gently behind him on the mild morning air.").getBytes(UTF8);

    byte[] signingKey = crypto.newPrivateSigningKey();
    byte[] sig1 = crypto.signature(doc, signingKey);
    byte[] sig2 = crypto.signature(doc, signingKey);

    assertEquals("Same document signed two different times should be the same signature", Base64.encodeURL(sig1), Base64.encodeURL(sig2));
  }
}
