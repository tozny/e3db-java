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
import android.util.Log;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

class FSKSWrapper {
    private static final String MOBILE_AUTH_DB_KSTORE = "MobileAuthDb.kstore";
    private static final String TAG = "KeyProvider";

    private static volatile KeyStore fsKS;
    private static final Object keyStoreCreateLock = new Object();
    private final static Object keyStoreWriteLock = new Object();

    private static boolean fileExists(Context context, String privateFile) {
        File filesDir = context.getFilesDir();
        return new File(filesDir, privateFile).exists();
    }

    private static byte[] getRandomBytes(int numberOfBytes) throws Throwable {
        byte[] bytes = new byte[numberOfBytes];

        SecureRandom r = SecureRandom.getInstance("SHA1PRNG");
        r.nextBytes(bytes);
        return bytes;
    }

    private synchronized static String getPerf(Context context) throws Throwable {
       return "TODO";
    }

    private static KeyStore getFSKS(Context context) throws Throwable {
        if (fsKS == null) {
            synchronized (keyStoreCreateLock) {
                KeyStore result = fsKS;
                if (result == null) {
                    try {
                        result = KeyStore.getInstance("BKS");
                        Log.d(TAG, "Keystore: " + result.getClass().getCanonicalName());
                        if (fileExists(context, MOBILE_AUTH_DB_KSTORE)) {
                            // load key store
                            FileInputStream kstore = context.openFileInput(MOBILE_AUTH_DB_KSTORE);
                            try {
                                result.load(kstore, getPerf(context).toCharArray());
                            } finally {
                                kstore.close();
                            }
                        } else {
                            // create key store
                            result.load(null, null);
                            FileOutputStream out = context.openFileOutput(MOBILE_AUTH_DB_KSTORE, Context.MODE_PRIVATE);
                            try {
                                result.store(out, getPerf(context).toCharArray());
                            } finally {
                                out.close();
                            }
                        }

                        fsKS = result;
                    }
                    catch (KeyStoreException | IOException | NoSuchAlgorithmException| CertificateException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return fsKS;
    }

    private static void saveFSKS(Context context) throws Throwable {
        if (fsKS != null) {
            synchronized(keyStoreWriteLock) {
                try {
                    OutputStream output = context.openFileOutput(MOBILE_AUTH_DB_KSTORE, Context.MODE_PRIVATE);
                    try {
                        fsKS.store(output, getPerf(context).toCharArray());
                        Log.d(TAG, "Saved keystore.");
                    } finally {
                        output.close();
                    }
                } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                    Log.e(TAG, e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static KeyStore.ProtectionParameter getProtectionParameter(KeyProtection protection, String password) {
        if (protection.protectionType() == KeyProtection.KeyProtectionType.PASSWORD) {
            if (password == null || password.trim().length() == 0)
                throw new IllegalArgumentException("password cannot be blank.");

            return new KeyStore.PasswordProtection(password.toCharArray());

        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN && protection.protectionType() == KeyProtection.KeyProtectionType.NONE) {
            // On API 16, the KeyStore will throw when `setEntry` is called with a null ProtectionParam.
            // Passing a null password wrapped a PasswordProtection instance has the same effect as no password, however.
            return new KeyStore.PasswordProtection(null);

        } else {
            return null;

        }
    }

    private static void createSecretKeyIfNeeded(Context context, String alias, KeyProtection protection, String password) throws Throwable {

        KeyStore keyStore = getFSKS(context);

        if (!keyStore.containsAlias(alias)) {

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            SecureRandom random = new SecureRandom();
            keyGen.init(random);
            SecretKey secretKey = keyGen.generateKey();

            keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), getProtectionParameter(protection, password));

            saveFSKS(context);
        }
    }

    static SecretKey getSecretKey(Context context, String alias, KeyProtection protection, String password) throws Throwable {
        createSecretKeyIfNeeded(context, alias, protection, password);

        KeyStore keyStore = getFSKS(context);

        return (SecretKey) keyStore.getKey(alias, (password == null) ? null : password.toCharArray());
    }

    static void removeSecretKey(Context context, String alias) throws Throwable {
        KeyStore keyStore = getFSKS(context);

        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias);
            saveFSKS(context);
        }
    }
}
