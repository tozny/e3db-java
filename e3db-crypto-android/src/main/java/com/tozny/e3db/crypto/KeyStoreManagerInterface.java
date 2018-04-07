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


import javax.crypto.Mac;
import java.security.KeyStoreException;
import java.security.Signature;

public interface KeyStoreManagerInterface {
    /**
     * Allows key provider to delegate key authentication &amp; program exeuction to the caller,
     * without exposing implementation details about how keys are stored and managed.
     * @param <A>
     */
    interface KeyAuthenticationHandler<A> {
        /**
         * Called when authentication succeeds (or was unnecessary).
         * @param a
         */
        void handle(A a);

        /**
         * Called when authentication was cancelled or timed out.
         */
        void handleCancel();

        /**
         * Called when some error occurrs during authentication.
         * @param t
         */
        void handleError(Throwable t);

        /**
         * Called when the key provider needs to ask external caller for a {@link KeyAuthenticator}
         * instance.
         * @param callback
         */
        void handleAuthenticationRequired(KeyAuthenticatorCallback callback);
    }

    /**
     * Used to retrieve a KeyAuthenticator from the caller.
     */
    interface KeyAuthenticatorCallback {
        void callback(KeyAuthenticator authenticator);
    }


}
