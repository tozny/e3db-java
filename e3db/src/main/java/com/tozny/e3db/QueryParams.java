package com.tozny.e3db;

import com.sun.org.apache.xpath.internal.operations.Bool;

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

  private QueryParams(List<String> types, Integer count, Boolean includeData, List<UUID> writerIds, List<UUID> userIds, List<UUID> recordIds, Long after, Boolean includeAllWriters) {
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

  public static class QueryParamsBuilder {
    private List<String> types = null;
    private Integer count = null;
    private Long after = null;
    private List<UUID> writerIds = null;
    private List<UUID> userIds = null;
    private List<UUID> recordIds = null;
    private Boolean includeAllWriters = null;
    private Boolean includeData = null;

    private void checkIds(List<UUID> ids, String name) {
      for(UUID id : ids)
        if(id == null)
          throw new IllegalStateException("null " + name);
    }

    private void checkState() {
      if(types != null)
        for(String s : types)
          if(s == null || s.length() == 0)
            throw new IllegalArgumentException("null or empty type");

      if(writerIds != null)
         checkIds(writerIds, "writerId");

      if(userIds != null)
        checkIds(userIds, "userId");

      if(recordIds != null)
        checkIds(recordIds, "recordId");

      if(count != null && count < 0)
        throw new IllegalArgumentException("count");
    }

    public QueryParamsBuilder setTypes(String... types) {
      this.types = Arrays.asList(types);
      return this;
    }

    public List<String> getTypes() {
      return this.types;
    }

    public QueryParamsBuilder setWriters(UUID... writerIds) {
      this.writerIds = Arrays.asList(writerIds);
      return this;
    }

    public List<UUID> getWriters() {
      return this.writerIds;
    }

    public QueryParamsBuilder setUsers(UUID... userIds) {
      this.userIds = Arrays.asList(userIds);
      return this;
    }

    public List<UUID> getUsers() {
      return this.userIds;
    }

    public QueryParamsBuilder setRecords(UUID... recordIds) {
      this.recordIds = Arrays.asList(recordIds);
      return this;
    }

    public List<UUID> getRecords() {
      return this.recordIds;
    }

    public QueryParamsBuilder setCount(int count) {
      this.count = count;
      return this;
    }

    public Integer getCount() {
      return this.count;
    }

    public QueryParamsBuilder setAfter(long after) {
      this.after = after;
      return this;
    }

    public Long getAfter() {
      return this.after;
    }

    public QueryParamsBuilder setIncludeAllWriters(boolean includeAllWriters) {
      this.includeAllWriters = includeAllWriters;
      return this;
    }

    public Boolean getIncludeAllWriters() {
      return this.includeAllWriters;
    }

    public QueryParamsBuilder setIncludeData(boolean includeData) {
      this.includeData = includeData;
      return this;
    }

    public Boolean getIncludeData() {
      return this.includeData;
    }

    public QueryParams build() {
      checkState();
      return new QueryParams(types, count, includeData, writerIds, userIds, recordIds, after, includeAllWriters);
    }
  }
}
