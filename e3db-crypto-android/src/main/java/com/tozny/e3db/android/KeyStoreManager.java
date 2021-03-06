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
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.PermissionChecker;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;


import com.tozny.e3db.E3DBCryptoException;

import javax.crypto.Cipher;

import java.io.IOException;
import java.security.*;

import static com.tozny.e3db.android.KeyAuthentication.KeyAuthenticationType.*;

class KeyStoreManager {

  @SuppressLint({"MissingPermission", "NewApi"})
  private static void checkArgs(Context context, KeyAuthentication protection, KeyAuthenticator keyAuthenticator) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      if (protection.authenticationType() == BIOMETRIC|| protection.authenticationType() == BIOMETRIC_STRONG)
        throw new IllegalArgumentException(protection.authenticationType().toString() + " not supported below API 28.");
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      if (protection.authenticationType() == FINGERPRINT || protection.authenticationType() == LOCK_SCREEN)
        throw new IllegalArgumentException(protection.authenticationType().toString() + " not supported below API 23.");
    }

    if (protection.authenticationType() == PASSWORD || protection.authenticationType() == FINGERPRINT || protection.authenticationType() == LOCK_SCREEN || protection.authenticationType() == BIOMETRIC || protection.authenticationType() == BIOMETRIC_STRONG) {
      if (keyAuthenticator == null) {
        throw new IllegalArgumentException("KeyAuthenticator can't be null for key protection type: " + protection.authenticationType().toString());
      }
    }

    if (protection.authenticationType() == BIOMETRIC){
      if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        isFingerPrintAvailable(context, protection);
      } else {
        isBiometricAvailable(context, protection.authenticationType(), BiometricManager.Authenticators.BIOMETRIC_WEAK);
      }
    }


    if (protection.authenticationType() == BIOMETRIC_STRONG){
      if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        isFingerPrintAvailable(context, protection);
      } else {
        isBiometricAvailable(context, protection.authenticationType(), BiometricManager.Authenticators.BIOMETRIC_STRONG);
      }
    }

    if (protection.authenticationType() == FINGERPRINT) {
      isFingerPrintAvailable(context, protection);
    }

    if (protection.authenticationType() == LOCK_SCREEN) {
      if (protection.validUntilSecondsSinceUnlock() < 1)
        throw new IllegalArgumentException("secondsSinceUnlock must be greater than 0.");
    }
  }

  private static void isFingerPrintAvailable(Context context, KeyAuthentication protection) {
    if (PermissionChecker.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PermissionChecker.PERMISSION_GRANTED)
      throw new IllegalArgumentException(protection.authenticationType().toString() + " permission not granted.");

    // Some devices support fingerprint but the support library doesn't recognize it, so
    // we use the actual FingerprintManager here. (https://stackoverflow.com/a/45181416/169359)
    FingerprintManager mgr = context.getSystemService(FingerprintManager.class);
    if (mgr == null || ! mgr.isHardwareDetected())
      throw new IllegalArgumentException(protection.authenticationType().toString() + " hardware not available.");
  }

  @SuppressLint({"MissingPermission", "NewApi"})
  private static void isBiometricAvailable(Context context, KeyAuthentication.KeyAuthenticationType protection, int biometricType) {
    if (PermissionChecker.checkSelfPermission(context, Manifest.permission.USE_BIOMETRIC) != PermissionChecker.PERMISSION_GRANTED) {
      throw new IllegalArgumentException(protection.toString() + " permission not granted.");
    }
    BiometricManager mgr = BiometricManager.from(context);
    int canBiometricAuth = mgr.canAuthenticate(biometricType);
    if (mgr == null || canBiometricAuth != BiometricManager.BIOMETRIC_SUCCESS) {
      switch (canBiometricAuth) {
        case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
          throw new IllegalArgumentException(protection.toString() + " cannot be used because no biometrics are enrolled");
        case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
          throw new IllegalArgumentException(protection.toString() + " cannot be used because no biometric features available on this device.");
        case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
          throw new IllegalArgumentException(protection.toString() + " cannot be used because biometric features are currently unavailable.");
        default:
          throw new IllegalArgumentException(protection.toString() + " cannot be used");
      }
    }
  }

  interface AuthenticatedCipherHandler {
    void onAuthenticated(Cipher cipher);
    void onCancel();
    void onError(Throwable e);
  }

  @SuppressLint("NewApi")
  static void getCipher(final Context context, final String identifier, final KeyAuthentication protection, final KeyAuthenticator keyAuthenticator,
                        final CipherManager.GetCipher cipherGetter, final AuthenticatedCipherHandler authenticatedCipherHandler) {

      checkArgs(context, protection, keyAuthenticator);

      final Cipher cipher;

      switch(protection.authenticationType()) {
        case NONE:
          try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
              authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, FSKSWrapper.getSecretKey(context, identifier, protection, null)));
            else
              authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(identifier, protection)));
          } catch (InvalidKeyException | UnrecoverableKeyException | IOException | E3DBCryptoException e) {
            authenticatedCipherHandler.onError(e);
          }

          break;
        case BIOMETRIC_STRONG:
        case BIOMETRIC:
          try {
            cipher = cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(identifier, protection));
            keyAuthenticator.authenticateWithBiometric(new BiometricPrompt.CryptoObject(cipher), new KeyAuthenticator.AuthenticateHandler() {
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
          } catch (InvalidKeyException | UnrecoverableKeyException | IOException | E3DBCryptoException e) {
              authenticatedCipherHandler.onError(e);
            }
          break;
        case FINGERPRINT:
          try {
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

          } catch (InvalidKeyException | UnrecoverableKeyException | IOException | E3DBCryptoException e) {
            authenticatedCipherHandler.onError(e);
          }

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
          } catch (UnrecoverableKeyException | IOException | E3DBCryptoException e) {
            authenticatedCipherHandler.onError(e);
          }

          break;
        case PASSWORD:
          keyAuthenticator.getPassword(new KeyAuthenticator.PasswordHandler() {

            @Override
            public void handlePassword(String password) throws UnrecoverableKeyException {
              try {
                authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, FSKSWrapper.getSecretKey(context, identifier, protection, password)));
              } catch (InvalidKeyException | IOException | E3DBCryptoException e) {
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
        default:
          throw new IllegalStateException("Unhandled key protection: " + protection.authenticationType().toString());
      }

  }

  static void removeSecretKey(Context context, String identifier) throws E3DBCryptoException {
    FSKSWrapper.removeSecretKey(context, identifier);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      AKSWrapper.removeSecretKey(identifier);
  }
}
