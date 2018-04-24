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
import static com.tozny.e3db.Checks.*;

/**
 * Specifies the unencrypted data for a record.
 *
 * <p>This class is only used when creating a record via {@link Client#write(String, RecordData, Map, ResultHandler)}
 * or updating an existing record using {@link Client#update(RecordMeta, RecordData, Map, ResultHandler)}).
 */
public class RecordData {
  private final Map<String, String> cleartext;

  /**
   * Create a new instance using the provided {@code Map}.
   * The {@code cleartext} parameter must contain at least one non-blank entry (with a non-blank key).
   *
   * @param cleartext cleartext.
   */
  public RecordData(Map<String, String> cleartext) {
    if(cleartext == null)
      throw new IllegalArgumentException("cleartext null");

    checkMap(cleartext, "cleartext");
    this.cleartext = cleartext;
  }


  /**
   * The unencrypted data that will be encrypted and written to E3DB.
   *
   * @return cleartext.
   */
  public Map<String, String> getCleartext() {
    return cleartext;
  }

}
