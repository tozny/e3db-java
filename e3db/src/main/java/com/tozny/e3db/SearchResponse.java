package com.tozny.e3db;

/**
 * Holds results of a {@link Client#search(SearchRequest, ResultHandler)}  call.
 */
public interface SearchResponse extends QueryResponse {
  /**
   * @return total number of results that match the search.
   */
  long totalResults();
}
