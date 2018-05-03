package com.tozny.e3db;

import java.util.UUID;

/**
 * An encrypted key, intended for a given client, that can be used to encrypt or
 * decrypt documents.
 *
 * <p>Consider using the {@link LocalMeta} implementation.
 */
public interface EAKInfo {
  /**
   * The encrypted key, as a Base64URL-encoded string.
   * @return key.
   */
  String getKey();

  /**
   * Public key of the authorizer, as a Base64URL-encoded string.
   * @return publicKey.
   */
  String getPublicKey();

  /**
   * ID of the authorizer.
   * @return authorizerId.
   */
  UUID getAuthorizerId();

  /**
   * ID of the signer
   * @return signerId.
   */
  UUID getSignerId();

  /**
   * Public key of the signer, as a Base64URL-encoded string.
   * @return signerPublicKey.
   */
  String getSignerSigningKey();
}
