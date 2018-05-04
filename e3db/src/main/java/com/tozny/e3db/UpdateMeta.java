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
 * Holds immutable metadata necessary to identify a record for
 * updating.
 *
 * <p>Consider using the {@link LocalUpdateMeta} implementation. To convert
 * a {@link RecordMeta}, use the {@link LocalUpdateMeta#fromRecordMeta(RecordMeta)}
 * method.
 */
public interface UpdateMeta {
  /**
   * The type of the record.
   * @return
   */
  String getType();

  /**
   * ID of the record.
   * @return
   */
  UUID getRecordId();

  /**
   * Version of the record.
   * @return
   */
  String getVersion();
}
