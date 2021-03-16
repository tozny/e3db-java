package com.tozny.e3db;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to specify parameters for a search operation.
 *
 * <p>This class must be used to construct a {@link SearchRequest} instance that will subsequently
 * be used in a call to {@link Client#search(SearchRequest, ResultHandler)}
 *
 * <p>See the {@code SearchRequest} class for information about default values for each parameter.
 *
 *
 */
public class SearchRequestBuilder {
  private long nextToken;
  private int limit;
  private boolean includeAllWriters;
  private boolean includeData;
  private List<SearchRequest.SearchParams> match;
  private List<SearchRequest.SearchParams> exclude;
  private SearchRequest.SearchRange range;
  private SearchRequest.SearchOrder order;

  public SearchRequestBuilder setNextToken(long nextToken) {
    this.nextToken = nextToken;
    return this;
  }

  public SearchRequestBuilder setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public SearchRequestBuilder setIncludeAllWriters(boolean includeAllWriters) {
    this.includeAllWriters = includeAllWriters;
    return this;
  }

  public SearchRequestBuilder setIncludeData(boolean includeData) {
    this.includeData = includeData;
    return this;
  }

  public SearchRequestBuilder setMatch(List<SearchRequest.SearchParams> match) {
    this.match = match;
    return this;
  }

  public SearchRequestBuilder setExclude(List<SearchRequest.SearchParams> exclude) {
    this.exclude = exclude;
    return this;
  }

  public SearchRequestBuilder setRange(SearchRequest.SearchRange range) {
    this.range = range;
    return this;
  }

  public SearchRequestBuilder setOrder(SearchRequest.SearchOrder order) {
    this.order = order;
    return this;
  }

  public SearchRequest build() {
    if (this.match == null) {
      this.match = new ArrayList<>();
    }
    if (this.exclude == null) {
      this.exclude = new ArrayList<>();
    }
    return new SearchRequest(this.nextToken, this.limit, this.includeAllWriters, this.includeData, this.match, this.exclude, this.range, this.order);
  }
}