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

package com.tozny.e3db.android;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import org.junit.Test;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.Assert.*;

public class SecureStringManagerTest {

  private static final String TAG = "SecureStringManagerTest";

  private AlgorithmParameterSpec getParams(byte[] iv) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
      return new GCMParameterSpec(128, iv);
    else
      return new IvParameterSpec(iv);
  }

  private void writeFile(File path, byte[] modified) throws IOException {
    OutputStream os = new FileOutputStream(path, false);
    os.write(modified);
    os.close();
  }

  private byte[] readBytes(File path) throws IOException {
    Log.d(TAG, "Reading " + path.toString());
    FileInputStream in = new FileInputStream(path);
    ArrayList<Byte> arr = new ArrayList<>();
    byte[] tmp = new byte[16];
    for(int b = in.read(tmp); b != -1; b = in.read(tmp)) {
      Log.d(TAG, "Read " + b + " bytes.");
      for (int i = 0; i < b; i++)
        arr.add(tmp[i]);
    }
    byte[] result = new byte[arr.size()];
    for(int i = 0; i < arr.size(); i++)
      result[i] = arr.get(i).byteValue();

    return result;
  }

  @Test
  public void testSave() throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
    // Ensures that if we modify the encrypted file, it will no longer decrypt due because the mac is
    // incorrect.

    Context ctx = InstrumentationRegistry.getTargetContext();
    String filename = UUID.randomUUID().toString();
    String text = "foo";
    KeyGenerator aes = KeyGenerator.getInstance("AES");
    aes.init(256);
    SecretKey key = aes.generateKey();
    SecureRandom random = new SecureRandom();
    byte[] iv = new byte[12];
    random.nextBytes(iv);
    AlgorithmParameterSpec params = getParams(iv);

    {
      Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
      encryptCipher.init(Cipher.ENCRYPT_MODE, key, params);
      SecureStringManager.saveStringToSecureStorage(ctx, filename, text, encryptCipher);
    }

    File path = new File(FileSystemManager.getEncryptedDataFilePath(ctx, filename));
    byte[] encryptedContents = readBytes(path);
    try {
      byte[] modified = Arrays.copyOf(encryptedContents, encryptedContents.length);
      modified[modified.length - 1] = (byte) (modified[modified.length -1] + 1);
      writeFile(path, modified);
      Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
      decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
      SecureStringManager.loadStringFromSecureStorage(ctx, filename, decryptCipher);
      fail("Modified file should not decrypt.");
    }
    catch(IOException ex) {
      // success
    }

    try {
      byte[] modified = Arrays.copyOf(encryptedContents, encryptedContents.length);
      modified[0] = (byte) (modified[0] + 1);
      writeFile(path, modified);
      Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
      decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
      SecureStringManager.loadStringFromSecureStorage(ctx, filename, decryptCipher);
      fail("Modified file should not decrypt.");
    }
    catch(IOException ex) {
    }

    {
      writeFile(path, encryptedContents);
      Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
      decryptCipher.init(Cipher.DECRYPT_MODE, key, params);
      String result = SecureStringManager.loadStringFromSecureStorage(ctx, filename, decryptCipher);
      assertEquals("Text did not decrypt as expected.", text, result);
    }
  }
}
