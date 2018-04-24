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
 * Describes records shared with a client.
 *
 * <p>This class describes a sharing relationship from the reader's perspective (that is, "who shares with me?").
 *
 * <p>Use {@link Client#getIncomingSharing(ResultHandler)} to get a list of record types shared with a client.
 */
public class IncomingSharingPolicy {
  /**
   * The client (writer) who is sharing records.
   */
  public final UUID writerId;
  /**
   * The type of records shared.
   */
  public final String type;
  /**
   * The name of the client (writer) who is sharing. May be empty but never null.
   */
  public final String writerName;

  IncomingSharingPolicy(UUID writerId, String writerName, String type) {
    this.writerId = writerId;
    this.type = type;
    this.writerName = writerName;
  }
}
