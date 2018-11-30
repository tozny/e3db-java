package com.tozny.e3db;

public class E3DBEncryptionException extends E3DBCryptoException {
    public E3DBEncryptionException(String message) {
        super("Failure with encryption: " + message);
    }

    public E3DBEncryptionException(String message, Throwable e) {
        super("Failure with encryption: " + message, e);
    }
}
