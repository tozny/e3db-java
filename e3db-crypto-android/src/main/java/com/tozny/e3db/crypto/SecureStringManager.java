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
import org.jetbrains.annotations.NotNull;

import javax.crypto.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

class SecureStringManager {

    static boolean secureStringExists(@NotNull Context context, @NotNull String fileName) throws Throwable {
        return new File(FileSystemManager.getEncryptedDataFilePath(context, fileName)).exists();
    }

    static void deleteStringFromSecureStorage(@NotNull Context context, @NotNull String fileName) throws Throwable {
        if (new File(FileSystemManager.getEncryptedDataFilePath(context, fileName)).exists()) {
            File file = new File(FileSystemManager.getEncryptedDataFilePath(context, fileName));
            file.delete();
        }
    }

    static void saveStringToSecureStorage(@NotNull Context context, @NotNull String fileName, @NotNull String string, @NotNull Cipher cipher) throws Throwable {
        CipherOutputStream cipherOutputStream = null;

        try {
            cipherOutputStream =
                    new CipherOutputStream(new FileOutputStream(FileSystemManager.getEncryptedDataFilePath(context, fileName)), cipher);

            cipherOutputStream.write(string.getBytes("UTF-8"));

        } finally {
             if (cipherOutputStream != null) cipherOutputStream.close();
        }
    }

    static String loadStringFromSecureStorage(@NotNull Context context, @NotNull String fileName, @NotNull Cipher cipher) throws Throwable {
        CipherInputStream cipherInputStream = null;
        StringBuilder stringBuffer = new StringBuilder();

        try {
            cipherInputStream = new CipherInputStream(new FileInputStream(FileSystemManager.getEncryptedDataFilePath(context, fileName)), cipher);

            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                stringBuffer.append((char) nextByte);
            }

        } finally {
            if (cipherInputStream != null) cipherInputStream.close();
        }

        return stringBuffer.toString();
    }
}

