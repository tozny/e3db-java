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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.spec.AlgorithmParameterSpec;


class CipherManager {

  interface GetCipher {
    Cipher getCipher(Context context, String identifier, SecretKey key) throws Throwable;
  }

  private static void saveInitializationVector(Context context, String fileName, byte[] bytes) throws Throwable {
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)));
      fos.write(bytes);
      fos.flush();

    } finally {
      if (fos != null) fos.close();
    }
  }

  private static byte[] loadInitializationVector(Context context, String fileName) throws Throwable {
    FileInputStream fis = null;
    byte[] bytes;

    try {
      File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
      int fileSize = (int) file.length();
      bytes = new byte[fileSize];
      fis = new FileInputStream(file);
      fis.read(bytes, 0, fileSize);

    } finally {
      if (fis != null) fis.close();
    }

    return bytes;
  }

  static void deleteInitializationVector(Context context, String fileName) throws Throwable {
    if (new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)).exists()) {
      File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
      file.delete();
    }
  }

  static class SaveCipherGetter implements GetCipher {

    @Override
    public Cipher getCipher(Context context, String identifier, SecretKey key) throws Throwable {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key);

      saveInitializationVector(context, identifier, cipher.getIV());

      return cipher;
    }
  }

  static class LoadCipherGetter implements GetCipher {
    private static int RECC_AUTH_TAG_LEN = 128;

    @Override
    public Cipher getCipher(Context context, String identifier, SecretKey key) throws Throwable {
      AlgorithmParameterSpec params;

      if (key.getClass().getSimpleName().equals("AndroidKeyStoreSecretKey") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        params = new GCMParameterSpec(RECC_AUTH_TAG_LEN, loadInitializationVector(context, identifier));
      else
        params = new IvParameterSpec(loadInitializationVector(context, identifier));

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, params);

      return cipher;
    }
  }

  static GetCipher saveCipherGetter() {
    return new SaveCipherGetter();
  }

  static GetCipher loadCipherGetter() {
    return new LoadCipherGetter();
  }
}
