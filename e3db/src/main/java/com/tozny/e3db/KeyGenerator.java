package com.tozny.e3db;

import static com.tozny.e3db.Base64.decodeURL;
import static com.tozny.e3db.Base64.encodeURL;
import static com.tozny.e3db.Checks.checkNotEmpty;

public final class KeyGenerator {

    /**
     * Generates a private key that can be used for Curve25519
     * public-key encryption.
     *
     * <p>The associated public key can be retrieved using {@link #getPublicKey(String)}.
     *
     * @return A Base64URL-encoded string representing the new private key.
     *
     */
    public static String generateKey() {
        return encodeURL(Platform.crypto.newPrivateKey());
    }

    /**
     * Gets the public key for the given private key.
     *
     * <p>The private key must be a Base64URL-encoded string.
     *
     * The returned value represents the key as a Base64URL-encoded string.
     *
     * @param privateKey Curve25519 private key as a Base64URL-encoded string.
     * @return The public key portion of the private key, as a Base64URL-encoded string.
     */
    public static String getPublicKey(String privateKey) {
        checkNotEmpty(privateKey, "privateKey");
        byte[] arr = decodeURL(privateKey);
        checkNotEmpty(arr, "privateKey");
        return encodeURL(Platform.crypto.getPublicKey(arr));
    }

    /**
     * Generates a private key that can be used for Ed25519
     * public-key signatures.
     *
     * <p>The associated public key can be retrieved using {@link #getPublicKey(String)}.
     *
     * @return A Base64URL-encoded string representing the new private key.
     *
     */
    public static String generateSigningKey() {
        return encodeURL(Platform.crypto.newPrivateSigningKey());
    }

    /**
     * Gets the public signing key for the given private key.
     *
     * <p>The private key must be a Base64URL-encoded string.
     *
     * The returned value represents the key as a Base64URL-encoded string.
     *
     * @param privateSigningKey Ed25519 private key as a Base64URL-encoded string.
     * @return The public key portion of the private key, as a Base64URL-encoded string.
     */
    public static String getPublicSigningKey(String privateSigningKey) {
        checkNotEmpty(privateSigningKey, "privateSigningKey");
        byte[] arr = decodeURL(privateSigningKey);
        checkNotEmpty(arr, "privateSigningKey");
        return encodeURL(Platform.crypto.getPublicSigningKey(arr));
    }
}
