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
    private IBanana handler;

    public AndroidConfigStorageHelper(Context context, String identifier, KeyProtection protection, IBanana handler) {
        // TODO: Lilli, null checks
        this.context = context;
        this.identifier = identifier;
        this.protection = protection;
        this.handler    = handler;
    }

    @Override
    public void saveConfigSecurely(final String config, final SaveConfigHandler saveConfigHandler) {
        try {
            KeyStoreManager.getCipher(context, identifier, protection, handler, CipherManager.saveCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                @Override
                public void onAuthenticated(Cipher cipher) throws Exception {
                    SecureStringManager.saveStringToSecureStorage(context, identifier, config, cipher);

                    if (saveConfigHandler != null) saveConfigHandler.onSaveConfigDidSucceed();
                }

                @Override
                public void onCancel() {
                    if (saveConfigHandler != null) saveConfigHandler.onSaveConfigDidCancel();
                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            if (saveConfigHandler != null) saveConfigHandler.onSaveConfigDidFail(e);

        }
    }

    @Override
    public void loadConfigSecurely(final LoadConfigHandler loadConfigHandler) {
        try {
            KeyStoreManager.getCipher(context, identifier, protection, handler, CipherManager.loadCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                @Override
                public void onAuthenticated(Cipher cipher) throws Exception {
                    String configString = SecureStringManager.loadStringFromSecureStorage(context, identifier, cipher);

                    if (loadConfigHandler != null) loadConfigHandler.onLoadConfigDidSucceed(configString);
                }

                @Override
                public void onCancel() {
                    if (loadConfigHandler != null) loadConfigHandler.onLoadConfigDidCancel();
                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            if (loadConfigHandler != null) loadConfigHandler.onLoadConfigDidFail(e);
        }
    }

    @Override
    public void removeConfigSecurely(RemoveConfigHandler removeConfigHandler) {
        try {
            KeyStoreManager.removeSecretKey(context, identifier);
            SecureStringManager.deleteStringFromSecureStorage(context, identifier);
            // TODO: Lilli, maybe delete the ciphers too?

            if (removeConfigHandler != null) removeConfigHandler.onRemoveConfigDidSucceed();

        } catch (Exception e) {
            if (removeConfigHandler != null) removeConfigHandler.onRemoveConfigDidFail(e);
        }
    }
}
