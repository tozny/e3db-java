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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;

class AKSWrapper {
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void createSecretKeyIfNeeded(String alias, KeyProtection protection) throws Throwable {

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(alias)) {
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(false);
            }

            switch(protection.protectionType()) {
                case FINGERPRINT:
                    builder.setUserAuthenticationRequired(true);

                    break;

                case LOCK_SCREEN:
                    builder.setUserAuthenticationRequired(true)
                            .setUserAuthenticationValidityDurationSeconds(protection.validUntilSecondsSinceUnlock());
                    break;

                case PASSWORD:
                    throw new IllegalArgumentException("info: Password protection not supported.");

                case NONE:
                    break;

                default:
                    throw new IllegalStateException("Unhandled protection type: " + protection.protectionType());
            }

            KeyGenParameterSpec spec = builder.build();

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(spec);

            SecretKey key = keyGenerator.generateKey();
        }
    }

    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.M)
    static SecretKey getSecretKey(String alias, KeyProtection protection) throws Throwable {

        createSecretKeyIfNeeded(alias, protection);

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        return (SecretKey) keyStore.getKey(alias, null);
    }

    static void removeSecretKey(String alias) throws Throwable {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (keyStore.containsAlias(alias))
            keyStore.deleteEntry(alias);
    }
}
