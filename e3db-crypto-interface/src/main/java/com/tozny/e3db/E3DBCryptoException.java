package com.tozny.e3db;

/**
 * An exception related to underlying cryptography libraries for tasks such as keygen, key-management, encryption or decryption
 */
public class E3DBCryptoException extends Exception {
    public E3DBCryptoException(String message) {
        super(message);
    }

    public E3DBCryptoException(String message, Throwable e) {
        super(message, e);
    }

    public E3DBCryptoException(Throwable e) {
        super(e);
    }
}
