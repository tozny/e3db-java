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


public interface ICarrot {
    /**
     * Allows key provider to delegate key authentication &amp; program exeuction to the caller,
     * without exposing implementation details about how keys are stored and managed.
     * @param <A>
     */
    interface ICabin<A> {
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
         * Called when the key provider needs to ask external caller for a {@link IBanana}
         * instance.
         * @param callback
         */
        void handleAuthenticationRequired(ICanary callback);
    }

    /**
     * Used to retrieve a KeyAuthenticator from the caller.
     */
    interface ICanary {
        void callback(IBanana authenticator);
    }
}
