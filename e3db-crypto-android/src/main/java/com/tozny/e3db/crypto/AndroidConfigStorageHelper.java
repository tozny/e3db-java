package com.tozny.e3db.crypto;

import android.content.Context;
import com.tozny.e3db.ConfigStorageHelper;


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

    public AndroidConfigStorageHelper(Context context, String identifier, KeyProtection protection) {
        // TODO: Lilli, null checks
        this.context = context;
        this.identifier = identifier;
        this.protection = protection;
    }

    @Override
    public void saveConfigSecurely(String config) throws Exception {
        SecureStringManager.saveStringToSecureStorage(context, identifier, config, KeyStoreManager.getSecretKey(context, identifier, protection));
    }

    @Override
    public String loadConfigSecurely() throws Exception {
        return SecureStringManager.loadStringFromSecureStorage(context, identifier, KeyStoreManager.getSecretKey(context, identifier, protection));
    }

    @Override
    public void removeConfigSecurely() throws Exception {
        // TODO: Lilli, maybe delete the keys too?
        KeyStoreManager.removeSecretKey(context, identifier);

        SecureStringManager.deleteStringFromSecureStorage(context, identifier);
    }
}
