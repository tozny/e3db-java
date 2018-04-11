package com.tozny.e3db.crypto;

import android.content.Context;
import com.tozny.e3db.ConfigStorageHelper;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import java.util.regex.Pattern;


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

    private static final String pattern = "^[a-zA-Z0-9-_\\s]+$";

    private static void checkArgs(Context context, String identifier) {
        if (context == null)
            throw new IllegalArgumentException("Context cannot be null.");

        if (identifier == null)
            throw new IllegalArgumentException("Identifier cannot be null.");

        if (!identifier.matches(pattern))
            throw new IllegalArgumentException("Identifier string can only contain alphanumeric characters, underscores, hyphens, and spaces.");

        if (identifier.length() > 127)
            throw new IllegalArgumentException("Identifier string cannot be more than 127 characters in length.");

        if (identifier.trim().length() == 0)
            throw new IllegalArgumentException("Identifier string cannot be empty.");
    }

    private static void checkArgs(Context context, String identifier, String config) {
        checkArgs(context, identifier);

        if (config == null)
            throw new IllegalArgumentException("Config string cannot be null.");
    }

    public AndroidConfigStorageHelper(@NotNull Context context, @NotNull String identifier, KeyProtection protection, KeyAuthenticator keyAuthenticator) {
        this.context    = context;
        this.identifier = identifier;
        this.protection = protection == null ? KeyProtection.withNone() : protection;
        this.keyAuthenticator = keyAuthenticator;
    }

    @Override
    public void saveConfigSecurely(@NotNull final String config, final SaveConfigHandler saveConfigHandler) {
        try {
            checkArgs(context, identifier, config);

            KeyStoreManager.getCipher(context, identifier, protection, keyAuthenticator, CipherManager.saveCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                @Override
                public void onAuthenticated(Cipher cipher) throws Throwable {
                    SecureStringManager.saveStringToSecureStorage(context, identifier, config, cipher);

                    if (saveConfigHandler != null) saveConfigHandler.saveConfigDidSucceed();
                }

                @Override
                public void onCancel() {
                    if (saveConfigHandler != null) saveConfigHandler.saveConfigDidCancel();
                }

                @Override
                public void onError(Throwable e) {
                    if (saveConfigHandler != null) saveConfigHandler.saveConfigDidFail(new RuntimeException(e));
                }
            });

        } catch (Throwable e) {
            if (saveConfigHandler != null) saveConfigHandler.saveConfigDidFail(e);
        }
    }

    @Override
    public void loadConfigSecurely(final LoadConfigHandler loadConfigHandler) {
        try {
            checkArgs(context, identifier);

            if (!SecureStringManager.secureStringExists(context, identifier)) {
                if (loadConfigHandler != null) loadConfigHandler.loadConfigNotFound();

            } else {
                KeyStoreManager.getCipher(context, identifier, protection, keyAuthenticator, CipherManager.loadCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                    @Override
                    public void onAuthenticated(Cipher cipher) throws Throwable {
                        String configString = SecureStringManager.loadStringFromSecureStorage(context, identifier, cipher);

                        if (loadConfigHandler != null) loadConfigHandler.loadConfigDidSucceed(configString);
                    }

                    @Override
                    public void onCancel() {
                        if (loadConfigHandler != null) loadConfigHandler.loadConfigDidCancel();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (loadConfigHandler != null) loadConfigHandler.loadConfigDidFail(new RuntimeException(e));
                    }
                });

            }
        } catch (Throwable e) {
            if (loadConfigHandler != null) loadConfigHandler.loadConfigDidFail(e);
        }
    }

    @Override
    public void removeConfigSecurely(RemoveConfigHandler removeConfigHandler) {
        try {
            checkArgs(context, identifier);

            Throwable throwable = null;
            
            try { KeyStoreManager.removeSecretKey(context, identifier); }
            catch (Throwable e) { throwable = e; }

            try { SecureStringManager.deleteStringFromSecureStorage(context, identifier); }
            catch (Throwable e) { throwable = e; }

            try { CipherManager.deleteInitializationVector(context, identifier); }
            catch (Throwable e) { throwable = e; }

            if (throwable != null) throw throwable;

            if (removeConfigHandler != null) removeConfigHandler.removeConfigDidSucceed();

        } catch (Throwable e) {
            if (removeConfigHandler != null) removeConfigHandler.removeConfigDidFail(e);
        }
    }
}
