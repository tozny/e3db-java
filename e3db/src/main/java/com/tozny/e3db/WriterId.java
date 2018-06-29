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

import static com.tozny.e3db.Checks.checkNotNull;

/**
 * Represents a writer when calling {@link Client#shareOnBehalfOf(WriterId, String, UUID, ResultHandler)}.
 */
public final class WriterId {
  private final UUID writerId;

  private WriterId(UUID writerId) {
    checkNotNull(writerId, "writerId");
    this.writerId = writerId;
  }

  public static WriterId writerId(UUID writerId) {
    return new WriterId(writerId);
  }

  public UUID getWriterId() {
    return writerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WriterId writerId1 = (WriterId) o;

    return writerId != null ? writerId.equals(writerId1.writerId) : writerId1.writerId == null;
  }

  @Override
  public int hashCode() {
    return writerId != null ? writerId.hashCode() : 0;
  }

}
