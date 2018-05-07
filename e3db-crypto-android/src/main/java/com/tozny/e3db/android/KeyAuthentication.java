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

import android.support.v4.content.PermissionChecker;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.util.SparseArray;

/**
 * Determines how a key is protected when stored on the device. Fingerprint and
 * lock screen protection require Android 23+.
 */
public abstract class KeyAuthentication {

  public KeyAuthentication() { }

  /**
   * Amount of time after the user unlocks their phone that a key protected
   * by the lock screen PIN is 'unlocked'. After the timeout, using the key
   * requires that the user provide their PIN again. Set to 60 seconds.
   */
  public static final int DEFAULT_LOCK_SCREEN_TIMEOUT = 60;

  /**
   * Specifies the type of key authentication mechanisms supported.
   */
  public enum KeyAuthenticationType {
    /**
     * No authentication.
     */
    NONE,
    /**
     * Fingerprint authentication required.
     *
     * <p>See "Requiring User Authentication For Key Use" in the article "<a href="https://developer.android.com/training/articles/keystore.html">Android Keystore System</a>"
     * for details.
     */
    FINGERPRINT,
    /**
     * Lock scren PIN authentication required.
     *
     * <p>See "Requiring User Authentication For Key Use" in the article "<a href="https://developer.android.com/training/articles/keystore.html">Android Keystore System</a>"
     * for details.
     */
    LOCK_SCREEN,
    /**
     * Password authentication required.
     */
    PASSWORD;

    private static class Ords {
      private static final SparseArray<KeyAuthenticationType> ordMap;
      static {
        ordMap = new SparseArray<>();
        for(KeyAuthenticationType i : KeyAuthenticationType.values()) {
          ordMap.put(i.ordinal(), i);
        }
      }
    }

    static KeyAuthenticationType fromOrdinal(int ordinal) {
      KeyAuthenticationType keyAuthenticationType = Ords.ordMap.get(ordinal);
      if(keyAuthenticationType == null)
        throw new IllegalArgumentException("ordinal not found " + ordinal);
      return keyAuthenticationType;
    }
  }

  /**
   * Indicates the timeout for lock screen PIN authentication for the
   * key associated with this authentication method. Only valid when {@link #authenticationType()}
   * returns {@link KeyAuthenticationType#LOCK_SCREEN}.
   *
   * See "Requiring User Authentication For Key Use" in the article "<a href="https://developer.android.com/training/articles/keystore.html">Android Keystore System</a>"
   * for details.
   * @return Ibid.
   */
  public abstract int validUntilSecondsSinceUnlock();

  /**
   * Indicates what type of authentication is supported by this instance.
   * @return Ibid.
   */
  public abstract KeyAuthenticationType authenticationType();

  /**
   * Indicates if the device and SDK support the given type of key authentication.
   * @param ctx Application context.
   * @param authentication Authentication type.
   * @return {@code true} if the authencation type is supported; {@code false} otherwise.
   */
  @SuppressLint("MissingPermission")
  public static boolean protectionTypeSupported(Context ctx, KeyAuthenticationType authentication) {
    switch(authentication){
      case NONE:
      case PASSWORD:
        return true;
      case LOCK_SCREEN:
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
      case FINGERPRINT:
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          // Some devices support fingerprint but the support library doesn't recognize it, so
          // we use the actual FingerprintManager here. (https://stackoverflow.com/a/45181416/169359)
          FingerprintManager mgr = ctx.getSystemService(FingerprintManager.class);
          if (mgr != null) {
            return PermissionChecker.checkSelfPermission(ctx, Manifest.permission.USE_FINGERPRINT) == PermissionChecker.PERMISSION_GRANTED &&
                       mgr.isHardwareDetected() &&
                       mgr.hasEnrolledFingerprints();
          }
        }
        return false;
      default:
        throw new IllegalStateException("Unhandled authentication type: " + authentication);
    }
  }

  /**
   * Creates an instance for requiring fingerprint authentication.
   *
   * See "Requiring User Authentication For Key Use" in the article "<a href="https://developer.android.com/training/articles/keystore.html">Android Keystore System</a>"
   * for details.
   * @return Ibid.
   */
  public static KeyAuthentication withFingerprint() {
    return new KeyAuthentication() {
      @Override
      public int validUntilSecondsSinceUnlock() {
        throw new IllegalStateException();
      }

      @Override
      public KeyAuthenticationType authenticationType() {
        return KeyAuthenticationType.FINGERPRINT;
      }

      @Override
      public String toString() {
        return KeyAuthenticationType.FINGERPRINT.toString();
      }
    };
  }

  /**
   * Creates an instance requiring no authentication.
   * @return Ibid.
   */
  public static KeyAuthentication withNone() {
    return new KeyAuthentication() {
      @Override
      public int validUntilSecondsSinceUnlock() {
        throw new IllegalStateException();
      }

      @Override
      public KeyAuthenticationType authenticationType() {
        return KeyAuthenticationType.NONE;
      }

      @Override
      public String toString() {
        return KeyAuthenticationType.NONE.toString();
      }
    };
  }

  /**
   * Creates an instance requiring lock screen authentication (using the default
   * timeout of {@link #DEFAULT_LOCK_SCREEN_TIMEOUT}).
   *
   * See "Requiring User Authentication For Key Use" in the article "<a href="https://developer.android.com/training/articles/keystore.html">Android Keystore System</a>"
   * for details.
   * @return Ibid.
   */
  public static KeyAuthentication withLockScreen() {
    return withLockScreen(DEFAULT_LOCK_SCREEN_TIMEOUT);
  }

  /**
   * Creates an instance requiring lock screen authentication (using the given timeout).
   *
   * See "Requiring User Authentication For Key Use" in the article "<a href="https://developer.android.com/training/articles/keystore.html">Android Keystore System</a>"
   * for details.
   * @param timeoutSeconds Length of time for which lock screen authencation is valid.
   * @return Ibid.
   */
  public static KeyAuthentication withLockScreen(final int timeoutSeconds) {

    return new KeyAuthentication() {
      @Override
      public KeyAuthenticationType authenticationType() {
        return KeyAuthenticationType.LOCK_SCREEN;
      }

      @Override
      public int validUntilSecondsSinceUnlock() {
        return timeoutSeconds;
      }

      @Override
      public String toString() {
        return KeyAuthenticationType.LOCK_SCREEN.toString();
      }
    };
  }

  /**
   * Creates an instance requiring password authentication.
   * @return Ibid.
   */
  public static KeyAuthentication withPassword() {

    return new KeyAuthentication() {

      @Override
      public KeyAuthenticationType authenticationType() {
        return KeyAuthenticationType.PASSWORD;
      }

      @Override
      public int validUntilSecondsSinceUnlock() {
        throw new IllegalStateException();
      }

      @Override
      public String toString() {
        return "(Known) " + KeyAuthenticationType.PASSWORD.toString();
      }
    };
  }
}
