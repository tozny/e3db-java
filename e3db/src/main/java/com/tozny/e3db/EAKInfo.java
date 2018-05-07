/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */
package com.tozny.e3db;

import java.util.UUID;

/**
 * An encrypted key, intended for a given client, that can be used to encrypt or
 * decrypt documents.
 *
 * <p>Consider using the {@link LocalEAKInfo} implementation.
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
