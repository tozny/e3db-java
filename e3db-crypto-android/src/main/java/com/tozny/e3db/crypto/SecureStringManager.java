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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

public class SecureStringManager {

    private static void checkArgs(Context context, String fileName, String string) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("Method parameter 'context' cannot be null.");
        }

        if (fileName == null) { // TODO: Lilli, verify valid filename
            throw new IllegalArgumentException("Method parameter 'fileName' cannot be null.");
        }

        if (string == null) {
            throw new IllegalArgumentException("Method parameter 'string' cannot be null.");
        }
    }

    /**
     * Deletes the encrypted string from the file system.
     * @param context The application context.
     * @param fileName The fileName under which the the encrypted string is stored.
     * @throws Exception
     */
    public static void deleteStringFromSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName) throws Exception {
        checkArgs(context, fileName, "");

        if (new File(FileSystemManager.getEncryptedDataFilePath(context, fileName)).exists()) {
            File file = new File(FileSystemManager.getEncryptedDataFilePath(context, fileName));
            file.delete();
        }

        if (new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)).exists()) {
            File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
            file.delete();
        }
    }

    /**
     * Generates a keypair, and uses it to encrypt the string. Saves the encrypted string to the file system.
     * @param context The application context.
     * @param fileName The fileName under which the encrypted string is stored.
     * @param string The string to be encrypted.
     * @throws Exception
     */
    @SuppressLint("NewApi") // TODO: Lilli, replace w annotations later
    public static void saveStringToSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName, /*@NotNull*/ String string, /*SecretKey key*/ Cipher cipher) throws Exception {
        checkArgs(context, fileName, string);

//        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//        cipher.init(Cipher.ENCRYPT_MODE, key);
//
//        IvParameterSpec ivParams = cipher.getParameters().getParameterSpec(IvParameterSpec.class);
//
//        saveInitializationVector(context, fileName, ivParams.getIV());

        // TODO: Log b64 IV to make sure is new every time

        CipherOutputStream cipherOutputStream =
                new CipherOutputStream(new FileOutputStream(FileSystemManager.getEncryptedDataFilePath(context, fileName)), cipher);

        cipherOutputStream.write(string.getBytes("UTF-8"));
        cipherOutputStream.close();
    }

    /**
     * Loads and decrypts the encrypted string from the file system.
     * @param context The application context.
     * @param fileName The fileName under which the encrypted string is stored.
     * @return The decrypted string. If the key/file doesn't exist, returns `null`.
     * @throws Exception
     */
    @SuppressLint("NewApi") // TODO: Lilli, replace w annotations later
    public static String loadStringFromSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName, /*SecretKey key*/ Cipher cipher) throws Exception {
        checkArgs(context, fileName, "");

        if (!new File(FileSystemManager.getEncryptedDataFilePath(context, fileName)).exists()) {// || !new File(getInitializationVectorFilePath(context, fileName)).exists()) { // TODO: Lilli, if doesn't exist, where error?
            return null;
        }

//        GCMParameterSpec params = new GCMParameterSpec(128, loadInitializationVector(context, fileName)); // TODO: Lilli, do we know that it's always 128?
//
//        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//        cipher.init(Cipher.DECRYPT_MODE, key, params);

        CipherInputStream cipherInputStream =
                new CipherInputStream(new FileInputStream(FileSystemManager.getEncryptedDataFilePath(context, fileName)), cipher);

        StringBuilder stringBuffer = new StringBuilder();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            stringBuffer.append((char)nextByte);
        }

        return stringBuffer.toString();
    }
}

