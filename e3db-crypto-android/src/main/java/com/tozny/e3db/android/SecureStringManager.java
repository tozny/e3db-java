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


import javax.crypto.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

class SecureStringManager {

  static boolean secureStringExists(Context context, String fileName) throws Throwable {
    return new File(FileSystemManager.getEncryptedDataFilePath(context, fileName)).exists();
  }

  static void deleteStringFromSecureStorage(Context context, String fileName) throws Throwable {
    if (new File(FileSystemManager.getEncryptedDataFilePath(context, fileName)).exists()) {
      File file = new File(FileSystemManager.getEncryptedDataFilePath(context, fileName));
      file.delete();
    }
  }

  static void saveStringToSecureStorage(Context context, String fileName, String string, Cipher cipher) throws Throwable {
    CipherOutputStream cipherOutputStream = null;

    try {
      cipherOutputStream =
          new CipherOutputStream(new FileOutputStream(FileSystemManager.getEncryptedDataFilePath(context, fileName)), cipher);

      cipherOutputStream.write(string.getBytes("UTF-8"));

    } finally {
      if (cipherOutputStream != null) cipherOutputStream.close();
    }
  }

  static String loadStringFromSecureStorage(Context context, String fileName, Cipher cipher) throws Throwable {
    ArrayList<Byte> values = new ArrayList<>();

    CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(FileSystemManager.getEncryptedDataFilePath(context, fileName)), cipher);

    int nextByte;
    while ((nextByte = cipherInputStream.read()) != -1) {
      values.add((byte) nextByte);
    }

    byte[] bytes = new byte[values.size()];
    for (int i = 0; i < values.size(); i++) {
      bytes[i] = values.get(i);
    }

    return new String(bytes, "UTF-8");
  }
}

