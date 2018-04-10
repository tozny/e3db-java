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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

class CipherManager {

    interface GetCipher {
        Cipher getCipher(Context context, String identifier, SecretKey key) throws Exception;
    }

    private static void saveInitializationVector(Context context, String fileName, byte[] bytes) throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)));
        fos.write(bytes);
        fos.flush();
        fos.close();
    }

    private static byte[] loadInitializationVector(Context context, String fileName) throws Exception {
        File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
        int fileSize = (int)file.length();
        byte[] bytes = new byte[fileSize];
        FileInputStream fis = new FileInputStream(file);
        fis.read(bytes, 0, fileSize);
        fis.close();

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

            IvParameterSpec ivParams = cipher.getParameters().getParameterSpec(IvParameterSpec.class);

            saveInitializationVector(context, identifier, ivParams.getIV());

            // TODO: Log b64 IV to make sure is new every time

            return cipher;
        }
    }

    static class LoadCipherGetter implements GetCipher {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public Cipher getCipher(Context context, String identifier, SecretKey key) throws Exception {
            GCMParameterSpec params = new GCMParameterSpec(128, loadInitializationVector(context, identifier)); // TODO: Lilli, do we know that it's always 128?

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
