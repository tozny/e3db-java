package com.tozny.e3db;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Holds all parameters for an E3DB query operation.
 *
 * <p>This class allows you to specify all parameters for performing a query against E3DB (using the {@link Client#query(QueryParams, ResultHandler)}  method).
 * Instances of this class must be created using the {@link QueryParamsBuilder} class.
 *
 * <p>By default, queries are limited to 50 records, only return records written by the client, and results
 * do not include the data associated with each record (only the {@link Record#meta()} portion). See
 * the {@code QueryParamsBuilder} class for documentation about configuring these parameters.
 *
 * <p>Given an instance of this class, you can use the {@link #buildOn()} method to
 * create a {@code QueryParamsBuilder} that is pre-configured to the values held by the {@code QueryParams}
 * instance. This makes pagination easy to implement, where the {@link #after} parameter is the only
 * thing that needs to vary.
 *
 * <h1>Behavior with Multiple Filters</h1>
 *
 * All filters specified act as conjunctions; that is, a record must match <b>every</b> filter criteria
 * to be returned. For example, if record types and record IDs are given (via {@code types} and
 * {@code recordIds}), then records will only be returned if they have one of the give IDs <b>and</b> if
 * they have the given content type. The same applies for {@code writerIds} and {@code userIds}.
 *
 * <h1>Getting all Shared Records</h1>
 *
 * When true, the {@code includeAllWriters} flag indicates that results should include all records
 * shared with the client making the query request.
 *
 * <h1>Filtering Writers</h1>
 *
 * If {@code writerIds} is also set, then results will be limited to records written by the
 * set of IDs. <b>If the calling client does not include their own ID in the set, their records will
 * not be included in the results!</b>
 *
 */
public class QueryParams {
  /**
   * The maximum number of records to return. If -1, uses the server default of 50.
   */
  public final int count;
  /**
   * If true, include record data with results. Otherwise, only include metadata. Defaults to {@code null}, which uses the
   * server default of {@code false}.
   *
   * <p>When this value is {@code true}, then {@link Record#data()} will contain the record's unencrypted data. If {@code false} or {@code null},
   * {@code data()} will return an empty {@code Map}.
   *
   * <p>In either case, {@link Record#meta()} will always return  metadata about the record.
   */
  public final Boolean includeData;
  /**
   * If not null, limit results to records written by the given set of IDs. If empty, no records will be returned. Defaults to {@code null}.
   */
  public final List<UUID> writerIds;
  /**
   * If not null, limit results to records about given set of IDs. If empty, no records will be returned. Defaults to {@code null}.
   */
  public final List<UUID> userIds;
  /**
   * If not null, limit results to the records specified. If empty, no records will be returned. Defaults to {@code null}.
   */
  public final List<UUID> recordIds;
  /**
   * If not null, limit results to records of the given types. If empty, no records will be returned. Defaults to {@code null}.
   */
  public final List<String> types;
  /**
   * If greater than 0, limit results to the records appearing "after" the given
   * index. Defaults to 0, which specifies that results start with the
   * first record that matched.
   *
   * <p>For use with {@link QueryResponse#last()} when
   * implementing pagination.
   */
  public final long after;
  /**
   * If {@code true}, include all records shared with this client. Defaults to {@code null}, which uses the server default of {@code false}.
   *
   * <p>Note
   * that {@link #writerIds} must be {@code null} when this is {@code true}, or {@code writerIds} will
   * take precedence.
   */
  public final Boolean includeAllWriters;
  
  QueryParams(List<String> types, int count, Boolean includeData, List<UUID> writerIds, List<UUID> userIds, List<UUID> recordIds, long after, Boolean includeAllWriters) {
    this.types = types;
    this.count = count;
    this.includeData = includeData;
    this.writerIds = writerIds;
    this.userIds = userIds;
    this.recordIds = recordIds;
    this.after = after;
    this.includeAllWriters = includeAllWriters;
  }

  /**
   * Create a builder matching the parameters in this instance.
   *
   * @return
   */
  public QueryParamsBuilder buildOn() {
    QueryParamsBuilder builder = new QueryParamsBuilder();
    if(this.after > 0)
      builder.setAfter(this.after);

    if(this.count != -1)
      builder.setCount(this.count);

    if(this.includeAllWriters != null)
      builder.setIncludeAllWriters(this.includeAllWriters);

    if(this.includeData != null)
      builder.setIncludeData(this.includeData);

    builder.setRecords(this.recordIds == null ? null : this.recordIds.toArray(new UUID[0]));
    builder.setTypes(this.types == null ? null : this.types.toArray(new String[0]));
    builder.setUsers(this.userIds == null ? null : this.userIds.toArray(new UUID[0]));
    builder.setWriters(this.writerIds == null ? null : this.writerIds.toArray(new UUID[0]));

    return builder;
  }

}
