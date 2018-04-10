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
import android.support.annotation.RequiresApi;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

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

//    private static String privateKeyAlias(String identifier) {
//        return identifier + "-com.tozny.key"; // TODO: Lilli, go through these and clean up and stuff
//    }

    private static void checkMinSDK(KeyProtection protection) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if(protection.protectionType() == FINGERPRINT || protection.protectionType() == LOCK_SCREEN)
                throw new IllegalArgumentException("info: SDK Version must be at least " + Build.VERSION_CODES.M);
        }
    }

    interface AuthenticatedCipherHandler {
        void onAuthenticated(Cipher cipher) throws Exception;
        void onCancel();
        void onError(Throwable e);
    }

    @SuppressLint("NewApi")
    static void getCipher(final Context context, final String identifier, final KeyProtection protection, final IBanana banana, final CipherManager.GetCipher cipherGetter, final AuthenticatedCipherHandler authenticatedCipherHandler) throws Exception {
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

                    banana.authenticateWithFingerprint(new FingerprintManagerCompat.CryptoObject(cipher), new IApple.IAardvark() {
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

                        banana.authenticateWithLockScreen(new IApple.IAardvark() {
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
                banana.getPassword(/*user, */new IBanana.IBoston() {

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

//    static SecretKey getSecretKey(Context context, String identifier, KeyProtection protection) throws Exception {
//        checkMinSDK(protection);
//
//        switch(protection.protectionType()) {
//            case NONE:
//                //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
//                    return FSKSWrapper.getSecretKey(context, getKeystoreAlias(identifier, protection), protection);
//                //else
//                //    return AKSWrapper.getSecretKey(getKeystoreAlias(identifier, protection), protection);
//
//            case FINGERPRINT:
//            case LOCK_SCREEN:
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
//                    return AKSWrapper.getSecretKey(getKeystoreAlias(identifier, protection), protection);
//                else
//                    throw new IllegalStateException(protection.protectionType().toString() + " not supported below API 23.");
//
//            case PASSWORD:
//                return FSKSWrapper.getSecretKey(context, getKeystoreAlias(identifier, protection), protection);
//
//            default:
//                throw new IllegalStateException("Unhandled key protection: " + protection.protectionType().toString());
//        }
//    }
//
//    // TODO: Lilli, combine this method w above...
//    public void getSigner(final Context context, final String identifier, final KeyProtection protection, final ICarrot.ICabin<Signature> handler) throws Exception {
//
//        checkMinSDK(protection);
//
//        switch(protection.protectionType()) {
//            case NONE:
//                try {
//                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//                        handler.handle(getSignatureFromKeyStore(FSKSWrapper.getKeyStore(context), identifier, null));
//
//                    } else {
//                        handler.handle(getSignatureFromKeyStore(AKSWrapper.getKeyStore(context), identifier, null));
//
//                    }
//
//                } catch (Exception e) {
//                    handler.handleError(e); // TODO: Lilli, handler handles exception?
//
//                }
//
//                break;
//
//            case FINGERPRINT:
//                try {
//                    final Signature signature = getSignatureFromKeyStore(AKSWrapper.getKeyStore(context), identifier, null);
//
//                    handler.handleAuthenticationRequired(new ICarrot.ICanary() {
//                        @SuppressLint("NewApi")
//                        @Override
//                        public void callback(IBanana authenticator) {
//                            authenticator.authenticateWithFingerprint(/*user, */new FingerprintManagerCompat.CryptoObject(signature), new IApple.IAardvark() {
//                                @Override
//                                public void handleAuthenticated() {
//                                    handler.handle(signature);
//
//                                }
//
//                                @Override
//                                public void handleCancel() {
//                                    handler.handleCancel();
//
//                                }
//
//                                @Override
//                                public void handleError(Throwable e) {
//                                    handler.handleError(e);
//
//                                }
//                            });
//                        }
//                    });
//
//                } catch (Exception e) {
//                    handler.handleError(e);
//
//                }
//
//                break;
//
//            case LOCK_SCREEN:
//                try {
//                    handler.handle(getSignatureFromKeyStore(AKSWrapper.getKeyStore(context), identifier, null));
//
//                } catch (InvalidKeyException e) {
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && e instanceof KeyPermanentlyInvalidatedException) {
//                        handler.handleError(e);
//
//                    } else {
//                        handler.handleAuthenticationRequired(new ICarrot.ICanary() {
//                            @SuppressLint("NewApi")
//                            @Override
//                            public void callback(IBanana authenticator) {
//                                authenticator.authenticateWithLockScreen(/*user, */new IApple.IAardvark() {
//                                    @Override
//                                    public void handleAuthenticated() {
//                                        try {
//                                            getSigner(/*user,*/context, identifier, protection, handler);
//
//                                        } catch (Exception e1) {
//                                            handler.handleError(e1);
//
//                                        }
//                                    }
//
//                                    @Override
//                                    public void handleCancel() {
//                                        handler.handleCancel();
//
//                                    }
//
//                                    @Override
//                                    public void handleError(Throwable e) {
//                                        handler.handleError(e);
//
//                                    }
//                                });
//                            }
//                        });
//                    }
//                } catch (Exception e) { // TODO: Lilli, make sure other exception is caught first
//                    handler.handleError(e);
//
//                }
//
//                break;
//
//            case PASSWORD:
//                handler.handleAuthenticationRequired(new ICarrot.ICanary() {
//
//                    @Override
//                    public void callback(IBanana authenticator) {
//                        authenticator.getPassword(/*user, */new IBanana.IBoston() {
//
//                            @Override
//                            public void handlePassword(String password) throws UnrecoverableKeyException {
//                                try {
//                                    handler.handle(getSignatureFromKeyStore(FSKSWrapper.getKeyStore(context), identifier, password));
//
//                                } catch (Exception e) {
//                                    handler.handleError(e);
//
//                                }
//                            }
//                        });
//                    }
//                });
//
//                break;
//
//            default:
//                throw new IllegalStateException("Unhandled protection type: " + protection.protectionType());
//        }
//    }
//
//    public static Signature getSignatureFromKeyStore(KeyStore keyStore, String identifier, String password) throws Exception {
//        Signature signature = Signature.getInstance("SHA256withRSA"); // TODO: Lilli, remove magic strings?
//        String alias = privateKeyAlias(identifier);
//
//        Key signingKey = keyStore.getKey(alias, password == null ? null : password.toCharArray());
//        if (signingKey == null)
//            throw new KeyNotFoundException(alias);
//
//        if (signingKey instanceof PrivateKey) {
//            signature.initSign((PrivateKey) signingKey);
//        } else {
//            // filesystem-based keystore will store private keys as
//            // encoded bytes. Decode from PKCS#8 format.
//            KeyFactory converter = KeyFactory.getInstance("RSA");
//            signature.initSign(converter.generatePrivate(new PKCS8EncodedKeySpec(signingKey.getEncoded())));
//        }
//
//        return signature;
//    }

    static void removeSecretKey(Context context, String identifier) throws Exception {
        FSKSWrapper.removeSecretKey(context, getKeystoreAlias(identifier, null));
        AKSWrapper.removeSecretKey(getKeystoreAlias(identifier, null));
    }

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
