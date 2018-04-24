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
 * Holds information necessary to authenticate with E3DB. See {@code Client.register} for
 * registering and capturing credentials; Use {@link Config#fromJson(String)} and {@link Config#json()} to convert credentials to a
 * standard JSON format.
 */
public interface ClientCredentials {
  /**
   * Username (API Key) for the client.
   * @return apiKey.
   */
  String apiKey();

  /**
   * Password (API secret) for the client.
   * @return apiSecret.
   */
  String apiSecret();

  /**
   * ID of the client.
   * @return clientId.
   */
  UUID clientId();

  /**
   * Name of the client. Can be empty but never null.
   * @return name.
   */
  String name();

  /**
   * Public key for the client, as a Base64URL encoded string.
   * @return publicKey.
   */
  String publicKey();

  /**
   * Public signing key for the client, as a Base64URL encoded string.
   * @return publicSignKey.
   */
  String publicSignKey();

  /**
   * Indicates if the client is enabled or not.
   * @return enabled.
   */
  boolean enabled();

  /**
   * API  host with which the client was registered (always 'https://api.e3db.com').
   * @return host.
   */
  String host();
}
