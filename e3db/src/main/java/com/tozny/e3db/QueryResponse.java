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

import java.util.List;

/**
 * Holds results of a {@link Client#query(QueryParams, ResultHandler)} call.
 */
public interface QueryResponse {
  /**
   * Records that matched the query. Can be empty but never null.
   *
   * @return records.
   */
  List<Record> records();

  /**
   * The index of the last record returned; use this value when setting the {@link QueryParamsBuilder#after}
   * parameter to retrieve records following the last one returned here.
   *
   * @return last.
   */
  long last();
}
