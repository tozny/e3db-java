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

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

class AKSWrapper {
  @RequiresApi(api = Build.VERSION_CODES.M)
  private static void createSecretKeyIfNeeded(String alias, KeyAuthentication protection) {
    try {
      KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
      keyStore.load(null);

      if (!keyStore.containsAlias(alias)) {
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                                  .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                                  .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          builder.setInvalidatedByBiometricEnrollment(false);
        }

        switch(protection.authenticationType()) {
          case FINGERPRINT:
            builder.setUserAuthenticationRequired(true);
            break;

          case LOCK_SCREEN:
            builder.setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(protection.validUntilSecondsSinceUnlock());
            break;
          case PASSWORD:
            throw new IllegalArgumentException("info: Password protection not supported.");
          case NONE:
            break;
          default:
            throw new IllegalStateException("Unhandled protection type: " + protection.authenticationType());
        }

        KeyGenParameterSpec spec = builder.build();

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        keyGenerator.init(spec);
        keyGenerator.generateKey();
      }
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
  }

  @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.M)
  static SecretKey getSecretKey(String alias, KeyAuthentication protection) {
    try {
      createSecretKeyIfNeeded(alias, protection);

      KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
      keyStore.load(null);

      return (SecretKey) keyStore.getKey(alias, null);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException e) {
      throw new RuntimeException(e);
    }
  }

  static void removeSecretKey(String alias) {
    try {
      KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
      keyStore.load(null);

      if (keyStore.containsAlias(alias))
        keyStore.deleteEntry(alias);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new RuntimeException(e);
    }
  }
}
