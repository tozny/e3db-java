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
import android.util.Log;

import com.tozny.e3db.E3DBCryptoException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

class CipherManager {

  private static final String TAG = "CipherManager";

  private static void saveInitializationVector(Context context, String fileName, byte[] bytes) throws IOException {
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)));
      fos.write(bytes);
      fos.flush();

    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          Log.d(TAG, e.getMessage(), e);
        }
      }
    }
  }

  private static byte[] loadInitializationVector(Context context, String fileName) throws IOException{
    FileInputStream fis = null;
    byte[] bytes;

    try {
      File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
      int fileSize = (int) file.length();
      bytes = new byte[fileSize];
      fis = new FileInputStream(file);
      fis.read(bytes, 0, fileSize);

    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          Log.d(TAG, e.getMessage(), e);
        }
      }
    }

    return bytes;
  }

  static byte[] getRandomBytes(int numberOfBytes) {
    try {
      byte[] bytes = new byte[numberOfBytes];
      SecureRandom r = SecureRandom.getInstance("SHA1PRNG");
      r.nextBytes(bytes);
      return bytes;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  static void deleteInitializationVector(Context context, String fileName) {
    if (new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)).exists()) {
      File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
      file.delete();
    }
  }

  static IvParameterSpec createIV() {
    return new IvParameterSpec(getRandomBytes(12));
  }

  interface GetCipher {
    Cipher getCipher(Context context, String identifier, SecretKey key) throws InvalidKeyException, IOException, E3DBCryptoException;
  }

  static class SaveCipherGetter implements GetCipher {


    private final KeyAuthentication.KeyAuthenticationType protection;

    SaveCipherGetter(KeyAuthentication.KeyAuthenticationType protection) {
      this.protection = protection;
    }

    @Override
    public Cipher getCipher(Context context, String identifier, SecretKey key) throws InvalidKeyException, IOException, E3DBCryptoException {
      try {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
          cipher.init(Cipher.ENCRYPT_MODE, key, createIV());
        } else {
          switch (protection) {
            case BIOMETRIC_STRONG:
            case BIOMETRIC:
            case FINGERPRINT:
            case LOCK_SCREEN:
            case NONE:
              // These keys will use the Android Key Store, which does not allow a
              // provided IV.
              cipher.init(Cipher.ENCRYPT_MODE, key);
              break;
            case PASSWORD:
              cipher.init(Cipher.ENCRYPT_MODE, key, createIV());
              break;
            default:
              throw new IllegalArgumentException("Unrecognized protection type " + protection.name());
          }
        }

        saveInitializationVector(context, identifier, cipher.getIV());
        return cipher;
      } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException e) {
        throw new E3DBCryptoException(e);
      }
    }

  }

  static class LoadCipherGetter implements GetCipher {

    static int RECC_AUTH_TAG_BITS = 128;
    private final KeyAuthentication.KeyAuthenticationType protection;

    LoadCipherGetter(KeyAuthentication.KeyAuthenticationType protection) {
      this.protection = protection;
    }

    private AlgorithmParameterSpec makeIvSpec(byte[] iv) {
      if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return new IvParameterSpec(iv);
      }
      else {
        switch(protection) {
          case BIOMETRIC_STRONG:
          case BIOMETRIC:
          case FINGERPRINT:
          case LOCK_SCREEN:
          case NONE:
            // These keys will use the Android Key Store and only support GCMParameterSpec
            return new GCMParameterSpec(RECC_AUTH_TAG_BITS, iv);
          case PASSWORD:
            return new IvParameterSpec(iv);
          default:
            throw new IllegalArgumentException("Unrecognized protection type " + protection.name());
        }
      }
    }

    @Override
    public Cipher getCipher(Context context, String identifier, SecretKey key) throws InvalidKeyException, E3DBCryptoException, IOException {
      try {
        AlgorithmParameterSpec params = makeIvSpec(loadInitializationVector(context, identifier));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, params);
        return cipher;
      } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
        throw new E3DBCryptoException(e);
      }
    }
  }
}
