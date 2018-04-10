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


import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import java.security.UnrecoverableKeyException;

public interface KeyAuthenticator {
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

    void getPassword(PasswordAuthenticatorCallbackHandler handler);

    // Make a note in docs that you can't call this if your activity has the 'noHistory' attribute set (via
    // AndroidManifest.xml). If you do, 'onActivityResult' is never called and the device credential flow
    // fails to finalize and call 'EnrollmentHandler#didCreateAccount'/'AuthorizationHandler#handleAuthorized'.
    @RequiresApi(api = Build.VERSION_CODES.M)
    void authenticateWithLockScreen(DeviceLockAuthenticatorCallbackHandler handler);

    @RequiresApi(api = Build.VERSION_CODES.M)
    void authenticateWithFingerprint(FingerprintManagerCompat.CryptoObject cryptoObject, DeviceLockAuthenticatorCallbackHandler handler);
}
