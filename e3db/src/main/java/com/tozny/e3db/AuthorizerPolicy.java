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
 * Used for two purposes:
 *
 * <ul>
 *   <li>When returned by {@link Client#getAuthorizedBy(ResultHandler)}, represents a writer that authorized
 *   this client to share on its behalf.</li>
 *   <li>When returned by {@link Client#getAuthorizers(ResultHandler)}, represents another client that this
 *   client has authorized to share on its behalf.</li>
*  </ul>
 */
public interface AuthorizerPolicy {
  /**
   * ID of the authorizer that can share on the writer's behalf.
   * @return Ibid.
   */
  UUID authorizerId();

  /**
   * ID of the writer producing the records.
   * @return Ibid.
   */
  UUID writerId();

  /**
   * ID of the user associated with the records.
   * @return Ibid.
   */
  UUID userId();

  /**
   * Record type that can be shared.
   * @return Ibid.
   */
  String recordType();

  /**
   * ID of the client that added the authorization.
   * @return Ibid.
   */
  UUID authorizedBy();
}
