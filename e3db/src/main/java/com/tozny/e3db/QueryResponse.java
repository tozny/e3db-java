package com.tozny.e3db;

import java.util.List;

/**
 * Holds results of a {@link Client#query(QueryParams, ResultHandler)} call.
 */
public interface QueryResponse {
  /**
   * Records that matched the query. Can be empty but never null.
   * @return
   */
  List<Record> records();

  /**
   * The index of the last record returned; use this value when setting the {@link QueryParamsBuilder#after}
   * parameter to retrieve records following the last one returned here.
   * @return
   */
  long last();
}
