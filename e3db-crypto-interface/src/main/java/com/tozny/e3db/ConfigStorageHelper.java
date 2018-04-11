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
        void onSaveConfigDidSucceed();
        void onSaveConfigDidCancel();
        void onSaveConfigDidFail(Throwable e);
    }

    interface LoadConfigHandler {
        void onLoadConfigDidSucceed(String config);
        void onLoadConfigDidCancel();
        void onLoadConfigNotFound();
        void onLoadConfigDidFail(Throwable e);
    }

    interface RemoveConfigHandler {
        void onRemoveConfigDidSucceed();
        void onRemoveConfigDidFail(Throwable e);
    }

    void saveConfigSecurely(String config, SaveConfigHandler handler);

    void loadConfigSecurely(LoadConfigHandler handler);

    void removeConfigSecurely(RemoveConfigHandler handler);
}
