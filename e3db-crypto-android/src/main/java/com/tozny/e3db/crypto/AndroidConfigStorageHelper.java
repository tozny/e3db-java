package com.tozny.e3db.crypto;


import android.content.Context;
import com.tozny.e3db.ConfigStorageHelper;


import javax.crypto.Cipher;


/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * 
 * Copyright (c) 2018 
 * 
 * All rights reserved.
 * 
 * e3db-java
 * 
 * Created by Lilli Szafranski on 4/4/18.
 * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


public class AndroidConfigStorageHelper implements ConfigStorageHelper {
    private Context context;
    private String identifier;
    private KeyProtection protection;
    private KeyAuthenticator keyAuthenticator;

    private static final String pattern = "^[a-zA-Z0-9-_]+$";

    private static void checkArgs(Context context, String identifier, Object handler) {
        if (context == null)
            throw new IllegalArgumentException("Context cannot be null.");

        if (identifier == null)
            throw new IllegalArgumentException("Identifier cannot be null.");

        if (handler == null)
            throw new IllegalArgumentException("Handler cannot be null.");

        if (!identifier.matches(pattern))
            throw new IllegalArgumentException("Identifier string can only contain alphanumeric characters, underscores, and hyphens.");

        if (identifier.length() > 100) /* In case device file system limits filenames 127 characters (and we're adding roughly 25 characters). */
            throw new IllegalArgumentException("Identifier string cannot be more than 127 characters in length.");

        if (identifier.trim().length() == 0)
            throw new IllegalArgumentException("Identifier string cannot be empty.");
    }

    private static void checkArgs(Context context, String identifier, String config, Object handler) {
        checkArgs(context, identifier, handler);

        if (config == null)
            throw new IllegalArgumentException("Config string cannot be null.");
    }

    private final static String COM_TOZNY_E3DB_CRYPTO = "com.tozny.e3db.crypto-";

    private static String full(String identifier, KeyProtection protection) {
        if (protection == null)
            return COM_TOZNY_E3DB_CRYPTO + identifier + "-NO";

        switch (protection.protectionType()) {
            case NONE:        return COM_TOZNY_E3DB_CRYPTO + identifier + "-NO";
            case FINGERPRINT: return COM_TOZNY_E3DB_CRYPTO + identifier + "-FP";
            case LOCK_SCREEN: return COM_TOZNY_E3DB_CRYPTO + identifier + "-LS";
            case PASSWORD:    return COM_TOZNY_E3DB_CRYPTO + identifier + "-PW";
        }

        return COM_TOZNY_E3DB_CRYPTO + identifier + "-NO";
    }

    public AndroidConfigStorageHelper(Context context, String identifier, KeyProtection protection, KeyAuthenticator keyAuthenticator) {
        this.context    = context;
        this.identifier = identifier;
        this.protection = protection == null ? KeyProtection.withNone() : protection;
        this.keyAuthenticator = keyAuthenticator;
    }

    @Override
    public void saveConfigSecurely(final String config, final SaveConfigHandler saveConfigHandler) {
        try {
            checkArgs(context, identifier, config, saveConfigHandler);

            final String fullIdentifier = full(identifier, protection);

            KeyStoreManager.getCipher(context, fullIdentifier, protection, keyAuthenticator, CipherManager.saveCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                @Override
                public void onAuthenticated(Cipher cipher) throws Throwable {
                    SecureStringManager.saveStringToSecureStorage(context, fullIdentifier, config, cipher);

                    saveConfigHandler.saveConfigDidSucceed();
                }

                @Override
                public void onCancel() {
                    saveConfigHandler.saveConfigDidCancel();
                }

                @Override
                public void onError(Throwable e) {
                    saveConfigHandler.saveConfigDidFail(new RuntimeException(e));
                }
            });

        } catch (Throwable e) {
            if (saveConfigHandler == null) throw new RuntimeException(e);

            saveConfigHandler.saveConfigDidFail(e);
        }
    }

    @Override
    public void loadConfigSecurely(final LoadConfigHandler loadConfigHandler) {
        try {
            checkArgs(context, identifier, loadConfigHandler);

            final String fullIdentifier = full(identifier, protection);

            if (!SecureStringManager.secureStringExists(context, fullIdentifier)) {
                loadConfigHandler.loadConfigNotFound();

            } else {
                KeyStoreManager.getCipher(context, fullIdentifier, protection, keyAuthenticator, CipherManager.loadCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                    @Override
                    public void onAuthenticated(Cipher cipher) throws Throwable {
                        String configString = SecureStringManager.loadStringFromSecureStorage(context, fullIdentifier, cipher);

                        loadConfigHandler.loadConfigDidSucceed(configString);
                    }

                    @Override
                    public void onCancel() {
                        loadConfigHandler.loadConfigDidCancel();
                    }

                    @Override
                    public void onError(Throwable e) {
                        loadConfigHandler.loadConfigDidFail(new RuntimeException(e));
                    }
                });

            }
        } catch (Throwable e) {
            if (loadConfigHandler == null) throw new RuntimeException(e);

            loadConfigHandler.loadConfigDidFail(e);
        }
    }

    @Override
    public void removeConfigSecurely(RemoveConfigHandler removeConfigHandler) {
        try {
            checkArgs(context, identifier, removeConfigHandler);

            String fullIdentifier = full(identifier, protection);

            Throwable throwable = null;
            
            try { KeyStoreManager.removeSecretKey(context, fullIdentifier); }
            catch (Throwable e) { throwable = e; }

            try { SecureStringManager.deleteStringFromSecureStorage(context, fullIdentifier); }
            catch (Throwable e) { throwable = e; }

            try { CipherManager.deleteInitializationVector(context, fullIdentifier); }
            catch (Throwable e) { throwable = e; }

            if (throwable != null) throw throwable;

            removeConfigHandler.removeConfigDidSucceed();

        } catch (Throwable e) {
            if (removeConfigHandler == null) throw new RuntimeException(e);

            removeConfigHandler.removeConfigDidFail(new RuntimeException(e));
        }
    }
}
