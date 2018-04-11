package com.tozny.e3db;

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


public interface ConfigStorageHelper { // TODO: Look at resultHandler interface for renaming these
    interface SaveConfigHandler {
        void saveConfigDidSucceed();
        void saveConfigDidCancel();
        void saveConfigDidFail(Throwable e);
    }

    interface LoadConfigHandler {
        void loadConfigDidSucceed(String config);
        void loadConfigDidCancel();
        void loadConfigNotFound();
        void loadConfigDidFail(Throwable e);
    }

    interface RemoveConfigHandler {
        void removeConfigDidSucceed();
        void removeConfigDidFail(Throwable e);
    }

    void saveConfigSecurely(String config, SaveConfigHandler handler);

    void loadConfigSecurely(LoadConfigHandler handler);

    void removeConfigSecurely(RemoveConfigHandler handler);
}
