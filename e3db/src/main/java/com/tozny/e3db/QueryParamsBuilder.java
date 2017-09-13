package com.tozny.e3db;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tozny.e3db.Checks.*;

/**
 * Used to specify parameters for a query operation.
 *
 * <p>This class must be used to construct a {@link QueryParams} instance that will subsequently
 * be used in a call to {@link Client#query(QueryParams, ResultHandler)}.
 *
 * <p>See the {@code QueryParams} class for information about default values for each parameter.
 *
 * <h1>Variadic Arguments</h1>
 *
 * After calling a method such as {@link #setTypes(String...)}, you might wonder how to clear the
 * list of types matched, or how to call the method such that no types match.
 *
 * <p>To clear the set of types matched (and thus match all types), call as {@code setTypes(null)}. To match an empty list of
 * types, use {@code setTypes()}.
 *
 * In general, calling {@code setX()} will match no items, while {@code setX(null)} will match all items. {@code setX(item1, item2)}
 * will match the list of items given.
 *
 * <p><b>Note</b>: This only applies when {@code setX} is called at least once; otherwise, no filters
 * on types, writers, etc. will be applied.
 *
 */
public class QueryParamsBuilder {
  private List<String> types = null;
  private int count = -1;
  private long after = 0;
  private List<UUID> writerIds = null;
  private List<UUID> userIds = null;
  private List<UUID> recordIds = null;
  private Boolean includeAllWriters = null;
  private Boolean includeData = null;

  private void checkIds(List<UUID> ids, String name) {
    for(UUID id : ids)
      if(id == null)
        checkNotNull(id, name);
  }

  private void checkState() {
    if(types != null)
      for(String s : types)
        checkNotEmpty(s, "type");

    if(writerIds != null)
       checkIds(writerIds, "writerId");

    if(userIds != null)
      checkIds(userIds, "userId");

    if(recordIds != null)
      checkIds(recordIds, "recordId");

    if(count < -1)
      throw new IllegalArgumentException("count");

    if(after < 0)
      throw new IllegalArgumentException("after");
  }

  /**
   * Specify the type of records to match. Defaults to {@code null}.
   */
  public QueryParamsBuilder setTypes(String... types) {
    this.types = types == null ? null : Arrays.asList(types);
    return this;
  }

  /**
   * The list of types to match. Defaults to {@code null}.
   */
  public List<String> getTypes() {
    return this.types;
  }

  /**
   * Filter records to those written by the given IDs. Defaults to {@code null}.
   */
  public QueryParamsBuilder setWriters(UUID... writerIds) {
    this.writerIds = writerIds == null ? null : Arrays.asList(writerIds);
    return this;
  }

  /**
   * The list of writers to match. Defaults to {@code null}.
   */
  public List<UUID> getWriters() {
    return this.writerIds;
  }

  /**
   * Filter records to those about the given IDs. Defaults to {@code null}.
   */
  public QueryParamsBuilder setUsers(UUID... userIds) {
    this.userIds = userIds == null ? null : Arrays.asList(userIds);
    return this;
  }

  /**
   * The list of user IDs to match. Defaults to {@code null}.
   */
  public List<UUID> getUsers() {
    return this.userIds;
  }

  /**
   * Filter records to this in the given set of IDs. Defaults to {@code null}.
   */
  public QueryParamsBuilder setRecords(UUID... recordIds) {
    this.recordIds = recordIds == null ? null : Arrays.asList(recordIds);
    return this;
  }

  /**
   * The list of record IDs to match. Defaults to {@code null}.
   */
  public List<UUID> getRecords() {
    return this.recordIds;
  }

  /**
   * Sets the number of records to return. Pass {@code -1} to use the server default of 50.
   * @param count
   */
  public QueryParamsBuilder setCount(int count) {
    this.count = count;
    return this;
  }

  /**
   * The number of records to return. Defaults to {@code -1}, which uses the server default
   * of 50.
   */
  public int getCount() {
    return this.count;
  }

  /**
   * Specifies an index <b>after</b> which the first record returned should occur. Defaults to {@code 0},
   * which means results should start at the first record that matches.
   *
   * <p>The {@code after} value should be obtained from value returned by {@link QueryResponse#last()}.
   */
  public QueryParamsBuilder setAfter(long after) {
    this.after = after;
    return this;
  }

  /**
   * The index after which to start returning results. Defaults to {@code 0}.
   */
  public long getAfter() {
    return this.after;
  }

  /**
   * When {@code true}, results should include records shared with this client (that also match
   * any other criteria). Defaults to {@code null}, which uses the server default {@code false}.
   * @param includeAllWriters
   */
  public QueryParamsBuilder setIncludeAllWriters(Boolean includeAllWriters) {
    this.includeAllWriters = includeAllWriters;
    return this;
  }

  /**
   * Whether to include shared records in results. Defaults to {@code null}.
   */
  public Boolean getIncludeAllWriters() {
    return this.includeAllWriters;
  }

  /**
   * Specifies whether to include record data with results. Defaults to {@code null}, which uses
   * the server default of {@code false}.
   */
  public QueryParamsBuilder setIncludeData(Boolean includeData) {
    this.includeData = includeData;
    return this;
  }

  /**
   * Whether record data should be included in results or not. Defaults to {@code null}, which uses
   * the server default of {@code false}.
   */
  public Boolean getIncludeData() {
    return this.includeData;
  }

  /**
   * Create a {@code QueryParams} instance based on the criteria specified.
   */
  public QueryParams build() {
    checkState();
    return new QueryParams(types, count, includeData, writerIds, userIds, recordIds, after, includeAllWriters);
  }
}
