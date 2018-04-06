package com.tozny.e3db.crypto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import com.tozny.e3db.ConfigStorageHelper;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;


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

    private String fileName;

    public AndroidConfigStorageHelper(Context context, String fileName) {
        // TODO: Null checks
        this.context = context;
        this.fileName = fileName;
    }

    @Override
    public void saveConfigSecurely(String config) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            saveStringToSecureStorage(context, fileName, config);
        } else {
            throw new IllegalStateException("Not yet implemented.");
        }
    }

    @Override
    public String loadConfigSecurely() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return loadStringFromSecureStorage(context, fileName);
        } else {
            throw new IllegalStateException("Not yet implemented.");
        }
    }

    @Override
    public void removeConfigSecurely() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deleteStringFromSecureStorage(context, fileName);
        } else {
            throw new IllegalStateException("Not yet implemented.");
        }
    }

    private final static String SECURE_STRING_STORAGE_DIRECTORY = "com.tozny.e3db.crypto";
    private final static String KEYSTORE_ALIAS                  = "com.tozny.e3db.crypto";
    private final static String IV_DIRECTORY = "ivs";

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

    //@android.support.annotation.RequiresApi(api = Build.VERSION_CODES.M)
    private static void createKeyPairIfNeeded() throws Exception {

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // TODO: Lilli, use annotation instead
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEYSTORE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        //.setRandomizedEncryptionRequired(false)
                        .build();


                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                keyGenerator.init(spec);

                SecretKey key = keyGenerator.generateKey();
            }
        }
    }

    private static void saveInitializationVector(Context context, String fileName, byte[] bytes) throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(getInitializationVectorFilePath(context, fileName)));
        fos.write(bytes);
        fos.flush();
        fos.close();
    }

    private static byte[] loadInitializationVector(Context context, String fileName) throws Exception {
//        byte[] bytes = new byte[12];

//        if (!new File(getInitializationVectorFilePath(context, fileName)).exists()) {
//
//            SecureRandom random = new SecureRandom();
//            random.nextBytes(bytes);
//
//            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(getInitializationVectorFilePath(context, fileName)));
//            bos.write(bytes);
//            bos.flush();
//            bos.close();
//
//        } else {
//            FileInputStream inputStream = new FileInputStream(getInitializationVectorFilePath(context, fileName));
//
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            int read = 0;
//            while ((read = inputStream.read(bytes, 0, bytes.length)) != -1) {
//                baos.write(bytes, 0, read);
//            }
//            baos.flush();
//        }

        File file = new File(getInitializationVectorFilePath(context, fileName));
        int fileSize = (int)file.length();
        byte[] bytes = new byte[fileSize];
        FileInputStream fis = new FileInputStream(file);//context.openFileInput(getInitializationVectorFilePath(context, fileName));
        fis.read(bytes, 0, fileSize);
        fis.close();

        return bytes;
    }

    /**
     * Deletes the encrypted string from the file system.
     * @param context The application context.
     * @param fileName The fileName under which the the encrypted string is stored.
     * @return `true` if deletion is successful, `false` if deletion failed or if the key/file does not exist.
     * @throws Exception
     */
    public static boolean deleteStringFromSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName) throws Exception {
        checkArgs(context, fileName, "");

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEYSTORE_ALIAS) || !new File(getEncryptedDataFilePath(context, fileName)).exists()) {
            return false;
        }

        // TODO: Also check and delete IV file

        //keyStore.deleteEntry(KEYSTORE_ALIAS);

        File file = new File(getEncryptedDataFilePath(context, fileName));
        return file.delete();
    }

    /**
     * Generates a keypair, and uses it to encrypt the string. Saves the encrypted string to the file system.
     * @param context The application context.
     * @param fileName The fileName under which the encrypted string is stored.
     * @param string The string to be encrypted.
     * @throws Exception
     */
    @SuppressLint("NewApi") // TODO: Lilli, replace w annotations later
    public static void saveStringToSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName, /*@NotNull*/ String string) throws Exception {
        checkArgs(context, fileName, string);

        createKeyPairIfNeeded();

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        SecretKey key = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        //GCMParameterSpec params = new GCMParameterSpec(128, getInitializationVector(context, fileName)); // Use SecureRandom to get 12 random bytes

        Cipher input = Cipher.getInstance("AES/GCM/NoPadding");
        input.init(Cipher.ENCRYPT_MODE, key);//, params);

        IvParameterSpec ivParams = input.getParameters().getParameterSpec(IvParameterSpec.class);

        saveInitializationVector(context, fileName, ivParams.getIV());

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
    public static String loadStringFromSecureStorage(/*@NotNull*/ Context context, /*@NotNull*/ String fileName) throws Exception {
        checkArgs(context, fileName, "");

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEYSTORE_ALIAS) || !new File(getEncryptedDataFilePath(context, fileName)).exists()) { // TODO: Lilli, also check for IV file
            return null;
        }

        SecretKey key = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        //GCMParameterSpec params = new GCMParameterSpec(128, getInitializationVector(context, fileName));

        IvParameterSpec ivParams = new IvParameterSpec(loadInitializationVector(context, fileName));

        Cipher output = Cipher.getInstance("AES/GCM/NoPadding");
        output.init(Cipher.DECRYPT_MODE, key, ivParams);//params);

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
