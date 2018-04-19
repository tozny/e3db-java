package com.tozny.e3db.crypto;

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * 
 * Copyright (c) 2018 
 * 
 * All rights reserved.
 * 
 * e3db-java
 * 
 * Created by Lilli Szafranski on 4/6/18.
 * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


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
