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


import java.io.File;

class FileSystemManager {

    private final static String SECURE_STRING_STORAGE_DIRECTORY = "com.tozny.e3db.crypto";
    private final static String IV_DIRECTORY                    = "ivs";
    private static final String FSKS                            = "fsks";
    private static final String LOC                             = "sys";

    private static String filesDirectory(Context context) throws Throwable {
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

    static String getInitializationVectorFilePath(Context context, String fileName) throws Throwable {
        return filesDirectory(context) +
                File.separator + SECURE_STRING_STORAGE_DIRECTORY +
                File.separator + IV_DIRECTORY + File.separator + fileName;
    }

    static String getEncryptedDataFilePath(Context context, String fileName) throws Throwable {
        return filesDirectory(context) + File.separator + SECURE_STRING_STORAGE_DIRECTORY + File.separator + fileName;
    }

    static String getFsksNameFilePath(Context context) throws Throwable {
        return SECURE_STRING_STORAGE_DIRECTORY + "." + FSKS;
    }

    static String getFsksLocFilePath(Context context) throws Throwable {
        return SECURE_STRING_STORAGE_DIRECTORY + "." + LOC;
    }
}
