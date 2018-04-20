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
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

class FSKSWrapper {
  private static final String TAG = "KeyProvider";
  private static final String FSKS = "com.tozny.e3db.crypto.fsks";
  private static final String FSKS_LOC = "com.tozny.e3db.crypto.sys";

  private static volatile KeyStore fsKS;
  private static final Object keyStoreCreateLock = new Object();
  private final static Object keyStoreWriteLock = new Object();

  private static boolean fileExists(Context context, String privateFile) {
    File filesDir = context.getFilesDir();
    return new File(filesDir, privateFile).exists();
  }

  private static byte[] getRandomBytes(int numberOfBytes) throws Throwable {
    byte[] bytes = new byte[numberOfBytes];

    SecureRandom r = SecureRandom.getInstance("SHA1PRNG");
    r.nextBytes(bytes);
    return bytes;
  }

  private synchronized static String getPerf(Context context, String dir) throws Throwable {
    final int count = 65, bytes = 20;

    int r;
    byte[] s, buf;

    if (!fileExists(context, dir)) {
      // should only run once, the first time the keystore is accessed.
      buf = getRandomBytes(count);

      for (int i = 0; i + 1 < buf.length; i += 2) {
        byte a = buf[i];
        buf[i] = buf[i + (i % 2 == 1 ? 1 : i - (i - 1))];
        buf[i + (i % 2 == 1 ? i - (i - 1) : 1)] = a;
      }

      OutputStream output = context.openFileOutput(dir, Context.MODE_PRIVATE);
      try {
        output.write(buf);
      } finally {
        output.close();
      }

      r = SecureRandom.getInstance("SHA1PRNG").nextInt(1000) + 10000;
      s = getRandomBytes(bytes);

      SharedPreferences sharedPreferences = context.getSharedPreferences(dir, Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.putInt("r", r);
      editor.putString("s", Base64Util.encode(s));
      editor.apply();

    } else {
      buf = new byte[count];
      InputStream input = context.openFileInput(dir);

      try {
        if (input.read(buf) != count)
          throw new RuntimeException("Invalid perf log");
      } finally {
        input.close();
      }

      SharedPreferences sharedPreferences = context.getSharedPreferences(dir, Context.MODE_PRIVATE);
      r = sharedPreferences.getInt("r", -1);
      s = Base64Util.decode(sharedPreferences.getString("s", null));
    }

    for (int i = 0; i + 1 < buf.length; i += 2) {
      byte a = buf[i];
      buf[i] = buf[i + (i % 2 == 1 ? 1 : i - (i - 1))];
      buf[i + (i % 2 == 1 ? i - (i - 1) : 1)] = a;
    }

    return Base64Util.encode(
        SecretKeyFactory.getInstance("PBKDF2WITHHMACSHA1").generateSecret(
            new PBEKeySpec(Base64Util.encode(buf).toCharArray(), s, r, bytes * 8)
        ).getEncoded()
    );
  }

  private static KeyStore getFSKS(Context context) throws Throwable {
    if (fsKS == null) {
      synchronized (keyStoreCreateLock) {
        KeyStore result = fsKS;
        if (result == null) {
          try {
            result = KeyStore.getInstance("BKS");
            Log.d(TAG, "Keystore: " + result.getClass().getCanonicalName());
            if (fileExists(context, FSKS)) {
              // load key store
              FileInputStream kstore = context.openFileInput(FSKS);
              try {
                result.load(kstore, getPerf(context, FSKS_LOC).toCharArray());
              } finally {
                kstore.close();
              }
            } else {
              // create key store
              result.load(null, null);
              FileOutputStream out = context.openFileOutput(FSKS, Context.MODE_PRIVATE);
              try {
                result.store(out, getPerf(context, FSKS_LOC).toCharArray());
              } finally {
                out.close();
              }
            }

            fsKS = result;
          } catch (KeyStoreException | IOException | NoSuchAlgorithmException| CertificateException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }

    return fsKS;
  }

  private static void saveFSKS(Context context) throws Throwable {
    if (fsKS != null) {
      synchronized(keyStoreWriteLock) {
        try {
          OutputStream output = context.openFileOutput(FSKS, Context.MODE_PRIVATE);
          try {
            fsKS.store(output, getPerf(context, FSKS_LOC).toCharArray());
            Log.d(TAG, "Saved keystore.");
          } finally {
            output.close();
          }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
          Log.e(TAG, e.getMessage());
          throw new RuntimeException(e);
        }
      }
    }
  }

  private static KeyStore.ProtectionParameter getProtectionParameter(KeyAuthentication protection, String password) {
    if (protection.authenticationType() == KeyAuthentication.KeyAuthenticationType.PASSWORD) {
      if (password == null || password.trim().length() == 0)
        throw new IllegalArgumentException("password cannot be blank.");

      return new KeyStore.PasswordProtection(password.toCharArray());

    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN && protection.authenticationType() == KeyAuthentication.KeyAuthenticationType.NONE) {
      // On API 16, the KeyStore will throw when `setEntry` is called with a null ProtectionParam.
      // Passing a null password wrapped a PasswordProtection instance has the same effect as no password, however.
      return new KeyStore.PasswordProtection(null);

    } else {
      return null;

    }
  }

  private static void createSecretKeyIfNeeded(Context context, String alias, KeyAuthentication protection, String password) throws Throwable {

    KeyStore keyStore = getFSKS(context);

    if (!keyStore.containsAlias(alias)) {

      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      SecureRandom random = new SecureRandom();
      keyGen.init(random);
      SecretKey secretKey = keyGen.generateKey();

      keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), getProtectionParameter(protection, password));

      saveFSKS(context);
    }
  }

  static SecretKey getSecretKey(Context context, String alias, KeyAuthentication protection, String password) throws Throwable {
    createSecretKeyIfNeeded(context, alias, protection, password);

    KeyStore keyStore = getFSKS(context);

    return (SecretKey) keyStore.getKey(alias, (password == null) ? null : password.toCharArray());
  }

  static void removeSecretKey(Context context, String alias) throws Throwable {
    KeyStore keyStore = getFSKS(context);

    if (keyStore.containsAlias(alias)) {
      keyStore.deleteEntry(alias);
      saveFSKS(context);
    }
  }
}
