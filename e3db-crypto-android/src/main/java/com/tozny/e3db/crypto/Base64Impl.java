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



import android.util.Base64;

public class Base64Impl  {

    static String encode(byte[] s) {
        String result = Base64.encodeToString(s, Base64.NO_WRAP);
        return result;
    }

    static String encodeUrl(byte[] s) {
        String result = Base64.encodeToString(s, Base64.NO_WRAP | Base64.URL_SAFE);
        return result;
    }

    static String encodeWithWrapping(byte[] s) {
        String result = Base64.encodeToString(s, 0).trim();
        return result;
    }

    static byte[] decode(String s) {
        byte[] result = Base64.decode(s, 0);
        return result;
    }
}
