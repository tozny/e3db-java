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


import android.content.Context;
import android.os.Build;
import javax.crypto.SecretKey;

public class KeyStoreManager {

    private final static String KEYSTORE_ALIAS = "com.tozny.e3db.crypto-";

    private static String getKeystoreAlias(String identifier, KeyProtection protection) {
        switch (protection.protectionType()) {
            // TODO: Lilli, do we want to do this? Make deleting keys harder? Simply just use identifier and not PT?
            case NONE:        return KEYSTORE_ALIAS + identifier + "-NONE";
            case FINGERPRINT: return KEYSTORE_ALIAS + identifier + "-FINGERPRINT";
            case LOCK_SCREEN: return KEYSTORE_ALIAS + identifier + "-LOCK_SCREEN";
            case PASSWORD:    return KEYSTORE_ALIAS + identifier + "-PASSWORD";
        }

        return KEYSTORE_ALIAS + identifier;
    }

    static SecretKey getSecretKey(Context context, String identifier, KeyProtection protection) throws Exception {

        switch(protection.protectionType()) {
            case NONE:
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    return FSKSWrapper.getSecretKey(context, getKeystoreAlias(identifier, protection), protection);
                else
                    return AKSWrapper.getSecretKey(getKeystoreAlias(identifier, protection), protection);

            case FINGERPRINT:
            case LOCK_SCREEN:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    return AKSWrapper.getSecretKey(getKeystoreAlias(identifier, protection), protection);
                else
                    throw new IllegalStateException(protection.protectionType().toString() + " not supported below API 23.");

            case PASSWORD:
                return FSKSWrapper.getSecretKey(context, getKeystoreAlias(identifier, protection), protection);

            default:
                throw new IllegalStateException("Unhandled key protection: " + protection.protectionType().toString());
        }
    }

    // TODO: Lilli, combine this method w above...
//    public void getSigner(final ToznyUser user, final KeyAuthenticationHandler<Signature> handler) throws KeyNotFoundException {
//        KeyStorageInfo info = KeyStorageInfo.forUser(user);
//        checkMinSDK(info);
//
//        switch(info.getKeyProtection().protectionType()) {
//            case NONE:
//                try {
//                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
//                        handler.handle(KeyStoreUtils.getSignatureFromKeyStore(info, getFSKS(), null));
//                    else
//                        handler.handle(AndroidKeyStore.INSTANCE.getSigner(info));
//                } catch (UnrecoverableKeyException | InvalidKeyException e) {
//                    handler.handleError(e);
//                }
//                break;
//            case FINGERPRINT:
//                try {
//                    final Signature signature = AndroidKeyStore.INSTANCE.getSigner(info);
//                    handler.handleAuthenticationRequired(new KeyAuthenticatorCallback() {
//                        @SuppressLint("NewApi")
//                        @Override
//                        public void callback(KeyAuthenticator authenticator) {
//                            authenticator.authenticatWithFingerprint(user, new FingerprintManagerCompat.CryptoObject(signature), new Tozny.KeyAuthenticatedHandler() {
//                                @Override
//                                public void handleAuthenticated() {
//                                    handler.handle(signature);
//                                }
//
//                                @Override
//                                public void handleCancel() {
//                                    handler.handleCancel();
//                                }
//
//                                @Override
//                                public void handleError(Throwable e) {
//                                    handler.handleError(e);
//                                }
//                            });
//                        }
//                    });
//                } catch (UnrecoverableKeyException | InvalidKeyException e) {
//                    handler.handleError(e);
//                }
//                break;
//            case LOCK_SCREEN:
//                try {
//                    handler.handle(AndroidKeyStore.INSTANCE.getSigner(info));
//                } catch (UnrecoverableKeyException e) {
//                    handler.handleError(e);
//                } catch (InvalidKeyException e) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && e instanceof KeyPermanentlyInvalidatedException)
//                        handler.handleError(e);
//                    else
//                        handler.handleAuthenticationRequired(new KeyAuthenticatorCallback() {
//                            @SuppressLint("NewApi")
//                            @Override
//                            public void callback(KeyAuthenticator authenticator) {
//                                authenticator.authenticateWithLockScreen(user, new Tozny.KeyAuthenticatedHandler() {
//                                    @Override
//                                    public void handleAuthenticated() {
//                                        try {
//                                            getSigner(user, handler);
//                                        } catch (KeyNotFoundException e1) {
//                                            handler.handleError(e1);
//                                        }
//                                    }
//
//                                    @Override
//                                    public void handleCancel() {
//                                        handler.handleCancel();
//                                    }
//
//                                    @Override
//                                    public void handleError(Throwable e) {
//                                        handler.handleError(e);
//                                    }
//                                });
//                            }
//                        });
//                }
//                break;
//            case PASSWORD:
//                handler.handleAuthenticationRequired(new KeyAuthenticatorCallback() {
//                    @Override
//                    public void callback(KeyAuthenticator authenticator) {
//                        authenticator.getPassword(user, new KeyAuthenticator.PasswordHandler() {
//                            @Override
//                            public void handlePassword(String password) throws UnrecoverableKeyException {
//                                try {
//                                    handler.handle(KeyStoreUtils.getSignatureFromKeyStore(KeyStorageInfo.forUser(user), getFSKS(), password));
//                                } catch (InvalidKeyException | KeyNotFoundException e) {
//                                    handler.handleError(e);
//                                }
//                            }
//                        });
//                    }
//                });
//                break;
//            default:
//                throw new IllegalStateException("Unhandled protection type: " + info.getKeyProtection().protectionType());
//        }
//    }
//
//    public void removeKey(KeyStorageInfo info, TOTPConfig config) throws KeyStoreException {
//        checkMinSDK(info);
//
//        switch(info.getKeyProtection().protectionType()) {
//            case NONE:
//                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//                    KeyStoreUtils.removeKeyFromStore(info, config, getFSKS());
//                    saveFSKS();
//                }
//                else
//                    AndroidKeyStore.INSTANCE.removeKey(info, config);
//                break;
//            case FINGERPRINT:
//            case LOCK_SCREEN:
//                AndroidKeyStore.INSTANCE.removeKey(info, config);
//                break;
//            case PASSWORD:
//                KeyStoreUtils.removeKeyFromStore(info, config, getFSKS());
//                saveFSKS();
//                break;
//            default:
//                throw new IllegalStateException("Unhandled key protection: " + info.getKeyProtection());
//        }
//    }
}
