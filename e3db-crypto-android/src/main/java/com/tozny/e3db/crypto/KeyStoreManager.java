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


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.v4.content.PermissionChecker;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;


import javax.crypto.Cipher;
import java.security.*;

import static com.tozny.e3db.crypto.KeyProtection.KeyProtectionType.FINGERPRINT;
import static com.tozny.e3db.crypto.KeyProtection.KeyProtectionType.LOCK_SCREEN;
import static com.tozny.e3db.crypto.KeyProtection.KeyProtectionType.PASSWORD;

public class KeyStoreManager {

    private static void checkArgs(Context context, KeyProtection protection, KeyAuthenticator keyAuthenticator) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (protection.protectionType() == FINGERPRINT || protection.protectionType() == LOCK_SCREEN)
                throw new IllegalArgumentException(protection.protectionType().toString() + " not supported below API 23.");
        }

        if (protection.protectionType() == PASSWORD || protection.protectionType() == FINGERPRINT || protection.protectionType() == LOCK_SCREEN) {
            if (keyAuthenticator == null) {
                throw new IllegalArgumentException("KeyAuthenticator can't be null for key protection type: " + protection.protectionType().toString());
            }
        }

        if (protection.protectionType() == FINGERPRINT) {
            if (PermissionChecker.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PermissionChecker.PERMISSION_GRANTED ||
                    !FingerprintManagerCompat.from(context).isHardwareDetected())
                throw new IllegalArgumentException(protection.protectionType().toString() + " not currently supported.");
        }

        if (protection.protectionType() == LOCK_SCREEN) {
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
    static void getCipher(final Context context, final String identifier, final KeyProtection protection, final KeyAuthenticator keyAuthenticator,
                          final CipherManager.GetCipher cipherGetter, final AuthenticatedCipherHandler authenticatedCipherHandler) throws Throwable {

        checkArgs(context, protection, keyAuthenticator);

        final Cipher cipher;

        switch(protection.protectionType()) {
            case NONE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, FSKSWrapper.getSecretKey(context, identifier, protection, null)));
                else
                    authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(identifier, protection)));

                break;

            case FINGERPRINT:
                cipher = cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(identifier, protection));

                keyAuthenticator.authenticateWithFingerprint(new FingerprintManagerCompat.CryptoObject(cipher), new KeyAuthenticator.DeviceLockAuthenticatorCallbackHandler() {
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

                        keyAuthenticator.authenticateWithLockScreen(new KeyAuthenticator.DeviceLockAuthenticatorCallbackHandler() {
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
                keyAuthenticator.getPassword(new KeyAuthenticator.PasswordAuthenticatorCallbackHandler() {

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
                throw new IllegalStateException("Unhandled key protection: " + protection.protectionType().toString());
        }
    }

    static void removeSecretKey(Context context, String identifier) throws Throwable {
        FSKSWrapper.removeSecretKey(context, identifier);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            AKSWrapper.removeSecretKey(identifier);
    }
}
