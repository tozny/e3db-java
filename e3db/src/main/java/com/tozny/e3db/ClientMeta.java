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

import java.util.Map;
import java.util.UUID;

/**
 * Immutable information about a locally stored record (a subset
 * of {@link RecordMeta}.
 *
 * <p>Consider using the {@link LocalMeta} implementation.
 */
public interface ClientMeta extends Signable {
  /**
   * ID of client that wrote the record.
   *
   * @return writerId.
   */
  UUID writerId();

  /**
   * ID of user associated with the record.
   *
   * This field is always equal to writer ID when records are written by this
   * client.
   *
   * @return userId.
   */
  UUID userId();

  /**
   * The type given to the record.
   *
   * @return type.
   */
  String type();

  /**
   * Unencrypted, metadata values stored with the record. May be {@code null}.
   *
   * @return plain.
   */
  Map<String, String> plain();
}
