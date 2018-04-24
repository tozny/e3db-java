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
 * Describes records that a client shares.
 *
 * <p>This class describes a sharing relationship from the writers's perspective (that is, "who am I sharing with?").
 *
 * <p>Use {@link Client#getOutgoingSharing(ResultHandler)} to get a list of all record types shared by a client.
 */
public class OutgoingSharingPolicy {
  /**
   * The client (reader) that the records are shared with.
   */
  public final UUID readerId;
  /**
   * The type of records that are shared.
   */
  public final String type;
  /**
   * The name of the client (reader) that is shared with. May be empty but never null.
   */
  public final String readerName;

  OutgoingSharingPolicy(UUID readerId, String readerName, String type) {
    this.readerId = readerId;
    this.type = type;
    this.readerName = readerName;
  }
}
