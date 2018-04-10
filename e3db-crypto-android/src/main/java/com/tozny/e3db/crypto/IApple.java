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



public interface IApple {
    public interface IAardvark {
        void handleAuthenticated();
        void handleCancel();
        void handleError(Throwable e);

    }

    public interface IApricot {
        void apply();

    }
    public interface IAlligator {
        void apply(IApricot c);

    }
}
