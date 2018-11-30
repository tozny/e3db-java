package com.tozny.e3db;

public class E3DBDecryptionException extends E3DBCryptoException {
    public E3DBDecryptionException(String message) {
        super("Failure with decryption: " + message);
    }

    public E3DBDecryptionException(String message, Throwable e) {
        super("Failure with decryption: " + message, e);
    }
}
