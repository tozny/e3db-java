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

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

public class SecureStringManager {

    private final static String SECURE_STRING_STORAGE_DIRECTORY = "com.tozny.e3db.crypto";
    private final static String IV_DIRECTORY                    = "ivs";

    private static String filesDirectory(/*@NotNull*/ Context context) throws Exception {
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        File sssDirectory     = new File(filesDirectory + File.separator + SECURE_STRING_STORAGE_DIRECTORY);
        File ivDirectory      = new File(filesDirectory + File.separator + SECURE_STRING_STORAGE_DIRECTORY + File.separator + IV_DIRECTORY);

        boolean success = true;
        if (!sssDirectory.exists()) {
            success = sssDirectory.mkdir();
        }

        if (!success) {
            throw new Exception("Error creating secure string storage directory.");
        }

        if (!ivDirectory.exists()) {
            success = ivDirectory.mkdir();
        }

        if (!success) {
            throw new Exception("Error creating secure string storage directory.");
        }

        return filesDirectory;
    }

    private static String getInitializationVectorFilePath(/*@NotNull*/ Context context, /*@NotNull*/ String fileName) throws Exception {
        return filesDirectory(context) +
                File.separator + SECURE_STRING_STORAGE_DIRECTORY +
                File.separator + IV_DIRECTORY + File.separator + fileName;
    }

    private static String getEncryptedDataFilePath(/*@NotNull*/ Context context, /*@NotNull*/ String fileName) throws Exception {
        return filesDirectory(context) + File.separator + SECURE_STRING_STORAGE_DIRECTORY + File.separator + fileName;
    }

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

    private static void saveInitializationVector(Context context, String fileName, byte[] bytes) throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(getInitializationVectorFilePath(context, fileName)));
        fos.write(bytes);
        fos.flush();
        fos.close();
    }

    private static byte[] loadInitializationVector(Context context, String fileName) throws Exception {
        File file = new File(getInitializationVectorFilePath(context, fileName));
        int fileSize = (int)file.length();
        byte[] bytes = new byte[fileSize];
        FileInputStream fis = new FileInputStream(file);
        fis.read(bytes, 0, fileSize);
        fis.close();

        return bytes;
    }

    /**
     * Deletes the encrypted string from the file system.
     * @param context The application context.
     * @param fileName The fileName under which the the encrypted string is stored.
     * @throws Exception
     */
    public static void deleteStringFromSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName) throws Exception {
        checkArgs(context, fileName, "");

        if (new File(getEncryptedDataFilePath(context, fileName)).exists()) {
            File file = new File(getEncryptedDataFilePath(context, fileName));
            file.delete();
        }

        if (new File(getInitializationVectorFilePath(context, fileName)).exists()) {
            File file = new File(getInitializationVectorFilePath(context, fileName));
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
    public static void saveStringToSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName, /*@NotNull*/ String string, SecretKey key) throws Exception {
        checkArgs(context, fileName, string);

        Cipher input = Cipher.getInstance("AES/GCM/NoPadding");
        input.init(Cipher.ENCRYPT_MODE, key);

        IvParameterSpec ivParams = input.getParameters().getParameterSpec(IvParameterSpec.class);

        saveInitializationVector(context, fileName, ivParams.getIV());

        // TODO: Log b64 IV to make sure is new every time

        CipherOutputStream cipherOutputStream =
                new CipherOutputStream(new FileOutputStream(getEncryptedDataFilePath(context, fileName)), input);

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
    public static String loadStringFromSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName, SecretKey key) throws Exception {
        checkArgs(context, fileName, "");

        if (!new File(getEncryptedDataFilePath(context, fileName)).exists() || !new File(getInitializationVectorFilePath(context, fileName)).exists()) {
            return null;
        }

        GCMParameterSpec params = new GCMParameterSpec(128, loadInitializationVector(context, fileName)); // TODO: Lilli, do we know that it's always 128?

        Cipher output = Cipher.getInstance("AES/GCM/NoPadding");
        output.init(Cipher.DECRYPT_MODE, key, params);

        CipherInputStream cipherInputStream =
                new CipherInputStream(new FileInputStream(getEncryptedDataFilePath(context, fileName)), output);

        StringBuilder stringBuffer = new StringBuilder();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            stringBuffer.append((char)nextByte);
        }

        return stringBuffer.toString();
    }
}

