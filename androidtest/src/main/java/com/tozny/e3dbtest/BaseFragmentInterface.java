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


import com.tozny.e3db.crypto.KeyProtection;
import com.tozny.e3db.crypto.KeyStoreManagerInterface;

public interface BaseFragmentInterface {
    String configName();
    KeyProtection keyProtection();
    KeyStoreManagerInterface.KeyAuthenticationHandler keyAuthenticationHandler();
}
