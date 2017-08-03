package com.tozny.e3db;

import java.util.Arrays;
import java.util.List;

public class QueryParams {
  public final List<String> types;
  public final long count;
  public final long after;

  public static final QueryParams ALL = new QueryParamsBuilder().build();

  private QueryParams(List<String> types, long count, long after) {
    this.types = types;
    this.count = count;
    this.after = after;
  }

  public QueryParamsBuilder buildOn() {
    QueryParamsBuilder builder = new QueryParamsBuilder();
    builder.setAfter(this.after);
    builder.setCount(this.count);
    if(this.types != null) {
      String[] types = new String[this.types.size()];
      this.types.toArray(types);
      builder.setTypes(types);
    }
    return builder;
  }

  public static class QueryParamsBuilder {
    private List<String> types = null;
    private long count = 100;
    private long after = 0;

    private void checkState() {
      if(types != null)
        for(String s : types)
          if(s == null)
            throw new IllegalArgumentException("null type");

      if(count < 0)
        throw new IllegalArgumentException("count");
    }

    public QueryParamsBuilder allTypes() {
      this.types = null;
      return this;
    }

    public QueryParamsBuilder setTypes(String... types) {
      this.types = Arrays.asList(types);
      return this;
    }

    public QueryParamsBuilder setCount(long count) {
      this.count = count;
      return this;
    }

    public QueryParamsBuilder setAfter(long after) {
      this.after = after;
      return this;
    }

    public QueryParams build() {
      checkState();
      return new QueryParams(types, count, after);
    }
  }
}
