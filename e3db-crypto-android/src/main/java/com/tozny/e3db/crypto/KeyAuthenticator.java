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

package com.tozny.e3db.crypto;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import java.security.UnrecoverableKeyException;

public abstract class KeyAuthenticator {
    private static final KeyAuthenticator noAuthentication = new KeyAuthenticator() {
        @Override
        void getPassword(PasswordAuthenticatorCallbackHandler handler) {
            throw new IllegalStateException("getPassword should not be called.");
        }

        @Override
        void authenticateWithLockScreen(DeviceLockAuthenticatorCallbackHandler handler) {
            throw new IllegalStateException("authenticateWithLockScreen should not be called.");
        }

        @Override
        void authenticateWithFingerprint(FingerprintManagerCompat.CryptoObject cryptoObject, DeviceLockAuthenticatorCallbackHandler handler) {
            throw new IllegalStateException("authenticateWithFingerprint should not be called.");
        }
    };

    interface PasswordAuthenticatorCallbackHandler {
        void handlePassword(String password) throws UnrecoverableKeyException;
        void handleCancel();
        void handleError(Throwable e);
    }

    interface DeviceLockAuthenticatorCallbackHandler {
        void handleAuthenticated();
        void handleCancel();
        void handleError(Throwable e);
    }

    abstract void getPassword(PasswordAuthenticatorCallbackHandler handler);

    // Make a note in docs that you can't call this if your activity has the 'noHistory' attribute set (via
    // AndroidManifest.xml). If you do, 'onActivityResult' is never called and the device credential flow
    // fails to finalize and call 'EnrollmentHandler#didCreateAccount'/'AuthorizationHandler#handleAuthorized'.
    @RequiresApi(api = Build.VERSION_CODES.M)
    abstract void authenticateWithLockScreen(DeviceLockAuthenticatorCallbackHandler handler);

    @RequiresApi(api = Build.VERSION_CODES.M)
    abstract void authenticateWithFingerprint(FingerprintManagerCompat.CryptoObject cryptoObject, DeviceLockAuthenticatorCallbackHandler handler);

    public static KeyAuthenticator defaultAuthenticator(Activity activity, String title) {
        return new DefaultKeyAuthenticator(activity, title);
    }

    public static KeyAuthenticator noAuthentication() {
        return noAuthentication;
    }
}
