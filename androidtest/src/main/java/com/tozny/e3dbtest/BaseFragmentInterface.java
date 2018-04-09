package com.tozny.e3dbtest;

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * 
 * Copyright (c) 2018 
 * 
 * All rights reserved.
 * 
 * e3db-java
 * 
 * Created by Lilli Szafranski on 4/7/18.
 * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


import com.tozny.e3db.crypto.IBanana;
import com.tozny.e3db.crypto.KeyProtection;

public interface BaseFragmentInterface {
    String configName();
    KeyProtection keyProtection();
    IBanana keyAuthenticationHandler();
}
