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


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import javax.crypto.Cipher;
import java.security.*;

import static com.tozny.e3db.crypto.KeyProtection.KeyProtectionType.FINGERPRINT;
import static com.tozny.e3db.crypto.KeyProtection.KeyProtectionType.LOCK_SCREEN;

public class KeyStoreManager {

    private final static String KEYSTORE_ALIAS = "com.tozny.e3db.crypto-";

    private static String getKeystoreAlias(String identifier, KeyProtection protection) {
//        switch (protection.protectionType()) {
//            // TODO: Lilli, do we want to do this? Make deleting keys harder? Simply just use identifier and not PT?
//            case NONE:        return KEYSTORE_ALIAS + identifier + "-NONE";
//            case FINGERPRINT: return KEYSTORE_ALIAS + identifier + "-FINGERPRINT";
//            case LOCK_SCREEN: return KEYSTORE_ALIAS + identifier + "-LOCK_SCREEN";
//            case PASSWORD:    return KEYSTORE_ALIAS + identifier + "-PASSWORD"; // TODO: Lilli, if you uncomment this, the delete method will need to be changed
//        }

        return KEYSTORE_ALIAS + identifier;
    }

    private static void checkMinSDK(KeyProtection protection) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (protection.protectionType() == FINGERPRINT || protection.protectionType() == LOCK_SCREEN)
                throw new IllegalArgumentException("info: SDK Version must be at least " + Build.VERSION_CODES.M);
        }
    }

    interface AuthenticatedCipherHandler {
        void onAuthenticated(Cipher cipher) throws Exception;
        void onCancel();
        void onError(Throwable e);
    }

    @SuppressLint("NewApi")
    static void getCipher(final Context context, final String identifier, final KeyProtection protection, final KeyAuthenticator banana, final CipherManager.GetCipher cipherGetter, final AuthenticatedCipherHandler authenticatedCipherHandler) throws Exception {
        checkMinSDK(protection);

        final Cipher cipher;
        final String alias = getKeystoreAlias(identifier, protection);

        switch(protection.protectionType()) {
            case NONE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, FSKSWrapper.getSecretKey(context, alias, protection, null)));
                else
                    authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(alias, protection)));

                break;

            case FINGERPRINT:
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M)
                    throw new IllegalStateException(protection.protectionType().toString() + " not supported below API 23.");

                    cipher = cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(alias, protection));

                    banana.authenticateWithFingerprint(new FingerprintManagerCompat.CryptoObject(cipher), new KeyAuthenticator.DeviceLockAuthenticatorCallbackHandler() {
                        @Override
                        public void handleAuthenticated() {
                            try {
                                authenticatedCipherHandler.onAuthenticated(cipher);
                            } catch (Exception e) {
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
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M)
                    throw new IllegalStateException(protection.protectionType().toString() + " not supported below API 23.");

                try {
                    cipher = cipherGetter.getCipher(context, identifier, AKSWrapper.getSecretKey(alias, protection));
                    authenticatedCipherHandler.onAuthenticated(cipher); // If the user unlocked the screen within the timeout limit, then this is already authenticated

                } catch (InvalidKeyException e) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && e instanceof KeyPermanentlyInvalidatedException) {
                        authenticatedCipherHandler.onError(e);

                    } else {

                        banana.authenticateWithLockScreen(new KeyAuthenticator.DeviceLockAuthenticatorCallbackHandler() {
                            @Override
                            public void handleAuthenticated() {
                                try {
                                    getCipher(context, identifier, protection, banana, cipherGetter, authenticatedCipherHandler);

                                } catch (Exception e) {
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
                banana.getPassword(/*user, */new KeyAuthenticator.PasswordAuthenticatorCallbackHandler() {

                    @Override
                    public void handlePassword(String password) throws UnrecoverableKeyException {
                        try {
                            authenticatedCipherHandler.onAuthenticated(cipherGetter.getCipher(context, identifier, FSKSWrapper.getSecretKey(context, alias, protection, password)));
                        } catch (Exception e) {

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

    static void removeSecretKey(Context context, String identifier) throws Exception {
        FSKSWrapper.removeSecretKey(context, getKeystoreAlias(identifier, null));
        AKSWrapper.removeSecretKey(getKeystoreAlias(identifier, null));
    }
}
