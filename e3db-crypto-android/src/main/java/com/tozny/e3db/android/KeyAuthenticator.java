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

import android.app.Activity;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import java.security.UnrecoverableKeyException;

/**
 * Provides an interface for authenticating with the user, by gathering a password,
 * asking for a fingerprint, or by entering a device PIN. The authentication method used
 * is determined by the value returned by the {@link KeyAuthentication#authenticationType()} method for
 * the {@link KeyAuthentication} instance associated with the configuration stored.
 *
 * <p>When authentication is needed, this class will call the appropriate method ({@code getPasssword},
 * etc.) with a {@code handler} argument. The implementation should gather the appropriate authentication, and then
 * call the {@code handle} method on the handler that was given.
 *
 * <p>Use the static method {@link #defaultAuthenticator(Activity, String)} to get an authenticator implementation
 * that can gather a password, collect a fingerprint, or ask the user to enter their lock screen PIN.
 *
 * <p>The static method {@link #noAuthentication()} can be used to get an authenticator that should not be called (useful
 * when the configuration is not protected by any special authentication).
 */
public abstract class KeyAuthenticator {
  private static final KeyAuthenticator noAuthentication = new KeyAuthenticator() {
    @Override
    public void getPassword(PasswordHandler handler) {
      throw new IllegalStateException("getPassword should not be called.");
    }

    @Override
    public void authenticateWithLockScreen(AuthenticateHandler handler) {
      throw new IllegalStateException("authenticateWithLockScreen should not be called.");
    }

    @Override
    public void authenticateWithFingerprint(FingerprintManagerCompat.CryptoObject cryptoObject, AuthenticateHandler handler) {
      throw new IllegalStateException("authenticateWithFingerprint should not be called.");
    }
  };

  /**
   * Receives the result of gathering a password from the user.
   */
  public interface PasswordHandler {
    void handlePassword(String password) throws UnrecoverableKeyException;
    void handleCancel();
    void handleError(Throwable e);
  }

  /**
   * Receives the result of authenticating a user using their fingerprint or
   * their lock screen PIN.
   */
  public interface AuthenticateHandler {
    void handleAuthenticated();
    void handleCancel();
    void handleError(Throwable e);
  }

  /**
   * No op.
   */
  public KeyAuthenticator() { }

  /**
   * Called when a password is needed to authenticate.
   *
   * @param handler For receiving the password gathered.
   */
  public abstract void getPassword(PasswordHandler handler);

  /**
   * Called then the user needs to enter the lock screen PIN. This will generally launch a system-provided activity,
   * and then return to the calling application.
   *
   * <p>You can't call this if your activity has the {@code noHistory} attribute set (via
   * {@code AndroidManifest.xml}). If you do, {@code onActivityResult} is never called and the device credential flow
   * fails to return to your activity.
   * @param handler For receiving the result of the authentcation.
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  public abstract void authenticateWithLockScreen(AuthenticateHandler handler);

  /**
   * Called when the user needs to present a fingerprint.
   * @param cryptoObject Contains a reference to the crypto operation that must be authenticated before it can be used.
   * @param handler For receiving the result of the authentication.
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  public abstract void authenticateWithFingerprint(FingerprintManagerCompat.CryptoObject cryptoObject, AuthenticateHandler handler);

  /**
   * Returns an instance that can gather a password, collect a fingerprint, or initiate the system flow for asking the
   * used to enter their lock screen PIN.
   *
   * @param activity The activity from which the authenticatoin will be launched. Note that if your activity has the {@code noHistory} attribute set (via
   * AndroidManifest.xml), the lock screen authentication method will fail. ({@code onActivityResult} is never called and the device credential flow
   * fails to return to your activity.)
   * @param title Title to use on the dialog for gathering a fingerprint.
   * @return Ibid.
   */
  public static KeyAuthenticator defaultAuthenticator(Activity activity, String title) {
    return new DefaultKeyAuthenticator(activity, title);
  }

  /**
   * Returns an instance that throws on any method call. Useful when saving, loading or removing configurations that
   * do not require any user authenticatoin (as the methdos are never called in that case).
   * @return Ibid.
   */
  public static KeyAuthenticator noAuthentication() {
    return noAuthentication;
  }
}
