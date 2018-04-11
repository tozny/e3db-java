package com.tozny.e3db.crypto;

import android.content.Context;
import com.tozny.e3db.ConfigStorageHelper;
import org.jetbrains.annotations.NotNull;

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

    private static void checkArgs(Context context, String identifier) {
        if (context == null)
            throw new IllegalArgumentException("Method parameter 'context' cannot be null.");

        if (identifier == null)
            throw new IllegalArgumentException("Method parameter 'identifier' cannot be null.");

        // TODO: Lilli, verify valid filename
    }

    private static void checkArgs(String config) {
        if (config == null)
            throw new IllegalArgumentException("Method parameter 'config' cannot be null.");
    }

    public AndroidConfigStorageHelper(@NotNull Context context, @NotNull String identifier, KeyProtection protection, KeyAuthenticator keyAuthenticator) {
        checkArgs(context, identifier);

        this.context    = context;
        this.identifier = identifier;
        this.protection = protection == null ? KeyProtection.withNone() : protection;
        this.keyAuthenticator = keyAuthenticator;
    }

    @Override
    public void saveConfigSecurely(@NotNull final String config, final SaveConfigHandler saveConfigHandler) {
        checkArgs(config);

        try {
            KeyStoreManager.getCipher(context, identifier, protection, keyAuthenticator, CipherManager.saveCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                @Override
                public void onAuthenticated(Cipher cipher) throws Throwable {
                    SecureStringManager.saveStringToSecureStorage(context, identifier, config, cipher);

                    if (saveConfigHandler != null) saveConfigHandler.onSaveConfigDidSucceed();
                }

                @Override
                public void onCancel() {
                    if (saveConfigHandler != null) saveConfigHandler.onSaveConfigDidCancel();
                }

                @Override
                public void onError(Throwable e) {
                    if (saveConfigHandler != null) saveConfigHandler.onSaveConfigDidFail(new RuntimeException(e));
                }
            });

        } catch (Throwable e) {
            if (saveConfigHandler != null) saveConfigHandler.onSaveConfigDidFail(e);
        }
    }

    @Override
    public void loadConfigSecurely(final LoadConfigHandler loadConfigHandler) {
        try {
            if (!SecureStringManager.secureStringExists(context, identifier)) {
                if (loadConfigHandler != null) loadConfigHandler.onLoadConfigNotFound();

            } else {
                KeyStoreManager.getCipher(context, identifier, protection, keyAuthenticator, CipherManager.loadCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                    @Override
                    public void onAuthenticated(Cipher cipher) throws Throwable {
                        String configString = SecureStringManager.loadStringFromSecureStorage(context, identifier, cipher);

                        if (loadConfigHandler != null) loadConfigHandler.onLoadConfigDidSucceed(configString);
                    }

                    @Override
                    public void onCancel() {
                        if (loadConfigHandler != null) loadConfigHandler.onLoadConfigDidCancel();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (loadConfigHandler != null) loadConfigHandler.onLoadConfigDidFail(new RuntimeException(e));
                    }
                });

            }
        } catch (Throwable e) {
            if (loadConfigHandler != null) loadConfigHandler.onLoadConfigDidFail(e);
        }
    }

    @Override
    public void removeConfigSecurely(RemoveConfigHandler removeConfigHandler) {
        try { // TODO: Lilli, separate try/catches for better clean-up
            KeyStoreManager.removeSecretKey(context, identifier);
            SecureStringManager.deleteStringFromSecureStorage(context, identifier);
            CipherManager.deleteInitializationVector(context, identifier);

            if (removeConfigHandler != null) removeConfigHandler.onRemoveConfigDidSucceed();

        } catch (Throwable e) {
            if (removeConfigHandler != null) removeConfigHandler.onRemoveConfigDidFail(e);
        }
    }
}
