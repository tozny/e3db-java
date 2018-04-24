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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.v4.content.PermissionChecker;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;


import javax.crypto.Cipher;
import java.security.*;

import static com.tozny.e3db.android.KeyAuthentication.KeyAuthenticationType.*;

class KeyStoreManager {

  private static void checkArgs(Context context, KeyAuthentication protection, KeyAuthenticator keyAuthenticator) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      if (protection.authenticationType() == FINGERPRINT || protection.authenticationType() == LOCK_SCREEN)
        throw new IllegalArgumentException(protection.authenticationType().toString() + " not supported below API 23.");
    }

    if (protection.authenticationType() == PASSWORD || protection.authenticationType() == FINGERPRINT || protection.authenticationType() == LOCK_SCREEN) {
      if (keyAuthenticator == null) {
        throw new IllegalArgumentException("KeyAuthenticator can't be null for key protection type: " + protection.authenticationType().toString());
      }
    }

    if (protection.authenticationType() == FINGERPRINT) {
      if (PermissionChecker.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PermissionChecker.PERMISSION_GRANTED ||
              !FingerprintManagerCompat.from(context).isHardwareDetected())
        throw new IllegalArgumentException(protection.authenticationType().toString() + " not currently supported.");
    }

    if (protection.authenticationType() == LOCK_SCREEN) {
      if (protection.validUntilSecondsSinceUnlock() < 1)
        throw new IllegalArgumentException("secondsSinceUnlock must be greater than 0.");
    }
  }

  interface AuthenticatedCipherHandler {
    void onAuthenticated(Cipher cipher) throws Throwable;
    void onCancel();
    void onError(Throwable e);
  }

  @SuppressLint("NewApi")
  static void getCipher(final Context context, final String identifier, final KeyAuthentication protection, final KeyAuthenticator keyAuthenticator,
                        final CipherManager.GetCipher cipherGetter, final AuthenticatedCipherHandler authenticatedCipherHandler) throws Throwable {

    checkArgs(context, protection, keyAuthenticator);

    final Cipher cipher;

    switch(protection.authenticationType()) {
      case NONE:
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
          authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, FSKSWrapper.getSecretKey(context, identifier, protection, null)));
        else
          authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(identifier, protection)));

        break;

      case FINGERPRINT:
        cipher = cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(identifier, protection));

        keyAuthenticator.authenticateWithFingerprint(new FingerprintManagerCompat.CryptoObject(cipher), new KeyAuthenticator.AuthenticateHandler() {
          @Override
          public void handleAuthenticated() {
            try {
              authenticatedCipherHandler.onAuthenticated(cipher);
            } catch (Throwable e) {
              authenticatedCipherHandler.onError(e);
            }
          }

          @Override
          public void handleCancel() {
            authenticatedCipherHandler.onCancel();
          }

          @Override
          public void handleError(Throwable e) {
            authenticatedCipherHandler.onError(e);
          }
        });

        break;

      case LOCK_SCREEN:
        try {
          cipher = cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(identifier, protection));
          authenticatedCipherHandler.onAuthenticated(cipher); /* If the user unlocked the screen within the timeout limit, then this is already authenticated. */

        } catch (InvalidKeyException e) {

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && e instanceof KeyPermanentlyInvalidatedException) {
            authenticatedCipherHandler.onError(e);

          } else {

            keyAuthenticator.authenticateWithLockScreen(new KeyAuthenticator.AuthenticateHandler() {
              @Override
              public void handleAuthenticated() {
                try {
                  getCipher(context, identifier, protection, keyAuthenticator, cipherGetter, authenticatedCipherHandler);

                } catch (Throwable e) {
                  authenticatedCipherHandler.onError(e);
                }
              }

              @Override
              public void handleCancel() {
                authenticatedCipherHandler.onCancel();
              }

              @Override
              public void handleError(Throwable e) {
                authenticatedCipherHandler.onError(e);
              }
            });
          }
        }

        break;

      case PASSWORD:
        keyAuthenticator.getPassword(new KeyAuthenticator.PasswordHandler() {

          @Override
          public void handlePassword(String password) throws UnrecoverableKeyException {
            try {
              authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, FSKSWrapper.getSecretKey(context, identifier, protection, password)));
            } catch (Throwable e) {

              if (e instanceof UnrecoverableKeyException) {
                throw (UnrecoverableKeyException) e;
              } else {
                authenticatedCipherHandler.onError(e);
              }
            }
          }

          @Override
          public void handleCancel() {
            authenticatedCipherHandler.onCancel();
          }

          @Override
          public void handleError(Throwable e) {
            authenticatedCipherHandler.onError(e);
          }
        });

        break;

      default:
        throw new IllegalStateException("Unhandled key protection: " + protection.authenticationType().toString());
    }
  }

  static void removeSecretKey(Context context, String identifier) throws Throwable {
    FSKSWrapper.removeSecretKey(context, identifier);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      AKSWrapper.removeSecretKey(identifier);
  }
}
