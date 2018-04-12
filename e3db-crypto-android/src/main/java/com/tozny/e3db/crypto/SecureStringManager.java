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


import javax.crypto.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

class SecureStringManager {

    static boolean secureStringExists(Context context, String fileName) throws Throwable {
        return new File(FileSystemManager.getEncryptedDataFilePath(context, fileName)).exists();
    }

    static void deleteStringFromSecureStorage(Context context, String fileName) throws Throwable {
        if (new File(FileSystemManager.getEncryptedDataFilePath(context, fileName)).exists()) {
            File file = new File(FileSystemManager.getEncryptedDataFilePath(context, fileName));
            file.delete();
        }
    }

    static void saveStringToSecureStorage(Context context, String fileName, String string, Cipher cipher) throws Throwable {
        CipherOutputStream cipherOutputStream = null;

        try {
            cipherOutputStream =
                    new CipherOutputStream(new FileOutputStream(FileSystemManager.getEncryptedDataFilePath(context, fileName)), cipher);

            cipherOutputStream.write(string.getBytes("UTF-8"));

        } finally {
             if (cipherOutputStream != null) cipherOutputStream.close();
        }
    }

    static String loadStringFromSecureStorage(Context context, String fileName, Cipher cipher) throws Throwable {
        ArrayList<Byte> values = new ArrayList<>();

        CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(FileSystemManager.getEncryptedDataFilePath(context, fileName)), cipher);

        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for (int i = 0; i < values.size(); i++) {
            bytes[i] = values.get(i);
        }

        return new String(bytes, "UTF-8");
    }
}

