package com.tozny.e3db.crypto;

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * 
 * Copyright (c) 2018 
 * 
 * All rights reserved.
 * 
 * e3db-java
 * 
 * Created by Lilli Szafranski on 4/9/18.
 * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.AlgorithmParameters;
import java.security.spec.AlgorithmParameterSpec;


class CipherManager {

    interface GetCipher {
        Cipher getCipher(Context context, String identifier, SecretKey key) throws Exception;
    }

    private static void saveInitializationVector(Context context, String fileName, byte[] bytes) throws Exception {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)));
            fos.write(bytes);
            fos.flush();

        } finally {
            if (fos != null) fos.close();
        }
    }

    private static byte[] loadInitializationVector(Context context, String fileName) throws Exception {
        FileInputStream fis = null;
        byte[] bytes;

        try {
            File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
            int fileSize = (int) file.length();
            bytes = new byte[fileSize];
            fis = new FileInputStream(file);
            fis.read(bytes, 0, fileSize);

        } finally {
            if (fis != null) fis.close();
        }

        return bytes;
    }

    static void deleteInitializationVector(Context context, String fileName) throws Exception {
        if (new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)).exists()) {
            File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
            file.delete();
        }
    }

    static class SaveCipherGetter implements GetCipher {

        @Override
        public Cipher getCipher(Context context, String identifier, SecretKey key) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            saveInitializationVector(context, identifier, cipher.getIV());

            return cipher;
        }
    }

    static class LoadCipherGetter implements GetCipher {
        private static int RECC_AUTH_TAG_LEN = 128;

        @Override
        public Cipher getCipher(Context context, String identifier, SecretKey key) throws Exception {
            AlgorithmParameterSpec params;

            if (key.getClass().getSimpleName().equals("AndroidKeyStoreSecretKey") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                params = new GCMParameterSpec(RECC_AUTH_TAG_LEN, loadInitializationVector(context, identifier));
            else
                params = new IvParameterSpec(loadInitializationVector(context, identifier));

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, params);

            return cipher;
        }
    }

    static GetCipher saveCipherGetter() {
        return new SaveCipherGetter();
    }

    static GetCipher loadCipherGetter() {
        return new LoadCipherGetter();
    }
}
