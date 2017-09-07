package com.tozny.e3db;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class QueryParams {
  public final Integer count;
  public final Boolean includeData;
  public final List<UUID> writerIds;
  public final List<UUID> userIds;
  public final List<UUID> recordIds;
  public final List<String> types;
  public final Long after;
  public final Boolean includeAllWriters;
  
  public static final QueryParams ALL = new QueryParamsBuilder().build();

  QueryParams(List<String> types, Integer count, Boolean includeData, List<UUID> writerIds, List<UUID> userIds, List<UUID> recordIds, Long after, Boolean includeAllWriters) {
    this.types = types;
    this.count = count;
    this.includeData = includeData;
    this.writerIds = writerIds;
    this.userIds = userIds;
    this.recordIds = recordIds;
    this.after = after;
    this.includeAllWriters = includeAllWriters;
  }

  public QueryParamsBuilder buildOn() {
    QueryParamsBuilder builder = new QueryParamsBuilder();
    if(this.after != null)
      builder.setAfter(this.after);

    if(this.count != null)
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
