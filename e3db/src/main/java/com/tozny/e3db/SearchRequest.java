package com.tozny.e3db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tozny.e3db.Client.iso8601;


/**
 * Holds all parameters for an E3DB query operation.
 *
 * <p>This class allows you to specify all parameters for performing a Search against E3DB (using the {@link Client#search(SearchRequest, ResultHandler)}  method).
 * Instances of this class must be created using the {@link SearchRequestBuilder} class.
 *
 * <p>By default, queries are limited to 50 records, only return records written by the calling client, and results
 * do not include the data associated with each record (only the {@link Record#meta()} portion). See
 * the {@code SearchRequestBuilder} class for documentation about configuring these parameters.
 *
 * <p>Given an instance of this class, you can use the {@link #buildOn()} method to
 * create a {@code SearchRequestBuilder} that is pre-configured to the values held by the {@code SearchRequest}
 * instance. This makes pagination easy to implement, where the {@link #nextToken} parameter is the only
 * thing that needs to vary.
 *
 * <h1>Behavior with Multiple Filters</h1>
 * <p>
 * {@code SearchRequest} supports both {@link #match} and {@link #exclude} filters. For additional configuration concerns
 * see {@code SearchParams}.
 *
 *
 * <h1>Getting all Shared Records</h1>
 * <p>
 * When true, the {@code includeAllWriters} flag indicates that results should include all records
 * shared with the client making the query request.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchRequest {
  /**
   * By default nextToken should be 0 for the first request of a search. For subsequent requests of the same search,
   * it should be set to the {@code SearchResponse#last()} value.
   */
  public final long nextToken;
  /**
   * The maximum number of records to return. If 0, uses the server default of 50. Maximum page size is limited to 1000
   */
  public final int limit;
  /**
   * If {@code true}, include all records shared with this client. Otherwise only includes records written by this client.
   */
  public final boolean includeAllWriters;
  /**
   * If true, include record data with results. Otherwise, only include metadata.
   *
   * <p>When this value is {@code true}, then {@link Record#data()} will contain the record's unencrypted data. If {@code false}
   * {@code data()} will return an empty {@code Map}.
   *
   * <p>In either case, {@link Record#meta()} will always return  metadata about the record.
   */
  public final boolean includeData;

  /**
   * A list of {@code SearchParam} that are logically ORed together and any matching record is returned.
   * If no items are provided all records will match.
   * <p>
   * {@link #exclude} supersedes match, such that if match and exclude associate the same record it will not be returned
   */
  public List<SearchParams> match;

  /**
   * A list of {@code SearchParam} that are logically ORed together and any non-matching record returned.
   * <p>
   * exclude supersedes {@link #match}, such that if match and exclude associate the same record it will not be returned
   */
  public List<SearchParams> exclude;

  /**
   * If not {@code null} only records within the provided {@link SearchRange} will be returned.
   */
  public final SearchRange range;

  /**
   * If not {@code null} returns the records ordered as specified by the provided {@link SearchOrder}.
   * <p>
   * If {@code null} results are returned in ascending order by rec
   */
  public final SearchOrder order;

  SearchRequest(long nextToken, int limit, boolean includeAllWriters, boolean includeData, List<SearchParams> match, List<SearchParams> exclude, SearchRange range, SearchOrder order) {
    this.nextToken = nextToken;
    this.limit = limit;
    this.includeAllWriters = includeAllWriters;
    this.includeData = includeData;
    this.match = match;
    this.exclude = exclude;
    this.range = range;
    this.order = order;
  }

  /**
   * Create a builder matching the parameters in this instance.
   *
   * @return The builder.
   */
  public SearchRequestBuilder buildOn() {
    return new SearchRequestBuilder().
            setNextToken(this.nextToken).
            setLimit(this.limit).
            setIncludeAllWriters(this.includeAllWriters).
            setIncludeData(this.includeData).
            setMatch(this.match).
            setExclude(this.exclude).
            setRange(this.range).
            setOrder(this.order);
  }

  /**
   * Defines {@code SearchTerms}, {@code SearchParamCondition}, and {@code SearchParamStrategy} that
   * records much match. Used to include and exclude from search results.
   * <p>
   * If {@code SearchParamCondition} is {@link SearchParamCondition#OR} is used than if any of the
   * {@code SearchTerms} match the record matches. If {@code SearchParamCondition} is {@link SearchParamCondition#AND}
   * then all {@code SearchTerms} must match.
   * <p>
   * {@code SearchParamStrategy} is the method by which the terms must match. Additional information can be found
   * in the enum definition
   */
  public static class SearchParams {
    public final SearchParamCondition condition;
    public final SearchParamStrategy strategy;
    public final SearchTerms terms;

    public SearchParams(SearchParamCondition condition, SearchParamStrategy strategy, SearchTerms terms) {
      this.condition = condition;
      this.strategy = strategy;
      this.terms = terms;
    }
  }

  /**
   * Logic type to be used in a {@code SearchParam}
   */
  public enum SearchParamCondition {
    OR,
    AND
  }

  /**
   * Term matching strategies
   */
  public enum SearchParamStrategy {
    /**
     * Only exact matches will match
     */
    EXACT,
    /**
     * Matches terms using a fuzzy search
     * It will perform similarly to a simple version of
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-fuzzy-query.html
     */
    FUZZY,
    /**
     * Matches terms using a wildcard search
     * It will perform wildcard searches using the patterns defined here
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-wildcard-query.html
     */
    WILDCARD,
    /**
     * Matches terms using a Regular Expression search
     * It will perform regex operations using the syntax provided here
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/regexp-syntax.html
     */
    REGEXP
  }


  /**
   * Holds all terms that can be searched on. Only values in {@link Record#meta()} is available to query
   */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class SearchTerms {
    /**
     * Keys of {@link RecordMeta#plain()}
     */
    public final List<String> keys;
    /**
     * Values of {@link RecordMeta#plain()}
     */
    public final List<String> values;
    /**
     * Key-Value pairs of {@link RecordMeta#plain()}
     */
    public final Map<String, String> tags;
    /**
     * One or more recordIds
     */
    public final List<UUID> recordIds;
    /**
     * ClientIds of the record writer. If the enclosing {@link SearchRequest#includeAllWriters} is {@code false}
     * writerIds included in this list that are not the calling client will not return results
     */
    public final List<UUID> writerIds;
    /**
     * ClientIds of the record user. If the enclosing {@link SearchRequest#includeAllWriters} is {@code false}
     * writerIds included in this list that are not the calling client will not return results
     */
    public final List<UUID> userIds;
    /**
     * One or more record types. Record type can be seen in {@link Client#write(String, RecordData, Map, ResultHandler)} as the first String parameter
     */
    @JsonProperty("content_types")
    public final List<String> recordTypes;

    SearchTerms(List<String> keys, List<String> values, Map<String, String> tags, List<UUID> recordIds, List<UUID> writerIds, List<UUID> userIds, List<String> recordTypes) {
      this.keys = keys;
      this.values = values;
      this.tags = tags;
      this.recordIds = recordIds;
      this.writerIds = writerIds;
      this.userIds = userIds;
      this.recordTypes = recordTypes;
    }

    /**
     * Create a builder matching the parameters in this instance.
     *
     * @return The builder.
     */
    public SearchTermsBuilder buildOn() {
      return new SearchTermsBuilder().
              setKeys(this.keys).
              setValues(this.values).
              setTags(this.tags).
              setRecordIds(this.recordIds).
              setWriterIds(this.writerIds).
              setUserIds(this.userIds).
              setRecordTypes(this.recordTypes);
    }
  }


  /**
   * Used to specify Search terms for a search operation
   *
   * <p>This class must be used to construct a {@code SearchTerms} instance that will subsequently
   * be used in a {@code SearchRequest}
   *
   * <p>See the {@code SearchTerms} class for information about default values for each parameter.
   *
   *
   */
  public static class SearchTermsBuilder {
    private List<String> keys = new ArrayList<>();
    private List<String> values = new ArrayList<>();
    private Map<String, String> tags = new HashMap<>();
    private List<UUID> recordIds = new ArrayList<>();
    private List<UUID> writerIds = new ArrayList<>();
    private List<UUID> userIds = new ArrayList<>();
    private List<String> recordTypes = new ArrayList<>();

    public SearchTermsBuilder setKeys(List<String> keys) {
      this.keys = keys;
      return this;
    }

    public SearchTermsBuilder addKeys(String... keys) {
      if (this.keys == null) {
        this.keys = new ArrayList<>(Arrays.asList(keys));
      } else {
        this.keys.addAll(Arrays.asList(keys));
      }
      return this;
    }

    public SearchTermsBuilder setValues(List<String> values) {
      this.values = values;
      return this;
    }

    public SearchTermsBuilder addValues(String... values) {
      if (this.values == null) {
        this.values = new ArrayList<>(Arrays.asList(values));
      } else {
        this.values.addAll(Arrays.asList(values));
      }
      return this;
    }

    public SearchTermsBuilder setTags(Map<String, String> tags) {
      this.tags = tags;
      return this;
    }

    public SearchTermsBuilder setTag(String key, String value) {
      if (this.tags == null) {
        this.tags = new HashMap<>();
      }
      this.tags.put(key, value);
      return this;
    }

    public SearchTermsBuilder setRecordIds(List<UUID> recordIds) {
      this.recordIds = recordIds;
      return this;
    }

    public SearchTermsBuilder addRecordIds(UUID... recordIds) {
      if (this.recordIds == null) {
        this.recordIds = new ArrayList<>(Arrays.asList(recordIds));
      } else {
        this.recordIds.addAll(Arrays.asList(recordIds));
      }
      return this;
    }

    public SearchTermsBuilder setWriterIds(List<UUID> writerIds) {
      this.writerIds = writerIds;
      return this;
    }

    public SearchTermsBuilder addWriterIds(UUID... writerIds) {
      if (this.writerIds == null) {
        this.writerIds = new ArrayList<>(Arrays.asList(writerIds));
      } else {
        this.writerIds.addAll(Arrays.asList(writerIds));
      }
      return this;
    }

    public SearchTermsBuilder setUserIds(List<UUID> userIds) {
      this.userIds = userIds;
      return this;
    }


    public SearchTermsBuilder addUserIds(UUID... userIds) {
      if (this.userIds == null) {
        this.userIds = new ArrayList<>(Arrays.asList(userIds));
      } else {
        this.userIds.addAll(Arrays.asList(userIds));
      }
      return this;
    }

    public SearchTermsBuilder setRecordTypes(List<String> recordTypes) {
      this.recordTypes = recordTypes;
      return this;
    }

    public SearchTermsBuilder addRecordTypes(String... recordTypes) {
      if (this.recordTypes == null) {
        this.recordTypes = new ArrayList<>(Arrays.asList(recordTypes));
      } else {
        this.recordTypes.addAll(Arrays.asList(recordTypes));
      }
      return this;
    }

    public SearchTerms build() {
      return new SearchTerms(keys, values, tags, recordIds, writerIds, userIds, recordTypes);
    }
  }

  /**
   * Enum to define the order in which search results are returned.
   */
  public enum SearchSortOrder {
    ASCENDING,
    DESCENDING
  }


  /**
   * The ordering strategy in which to return search results
   *
   * The default value is {@link RecordMeta#created()} in ascending order.
   *
   * There are currently no additional fields that can be sorted on.
   */
  public static class SearchOrder {
    public final SearchSortOrder sortOrder;

    public SearchOrder(SearchSortOrder sortOrder) {
      this.sortOrder = sortOrder;
    }

    public static SearchOrder SearchOrderAscending() {
      return new SearchOrder(SearchSortOrder.ASCENDING);
    }

    public static SearchOrder SearchOrderDescending() {
      return new SearchOrder(SearchSortOrder.DESCENDING);
    }
  }

  /**
   * The window in which a search should occur
   *
   * {@code SearchRangeType} specifies if the range should use {@link RecordMeta#created()}
   * {@link RecordMeta#lastModified()} by using {@link SearchRangeType#CREATED} and {@link SearchRangeType#MODIFIED} respectively
   *
   * If {@link #start} is {@code null} the search range will be unbounded from the beginning of time.
   *
   * If {@link #end} is {@code null} the search range will be unbounded from {@link #start} to all time in the future
   */
  public static class SearchRange {
    @JsonProperty("range_key")
    public final SearchRangeType rangeType;
    @JsonIgnore
    public final Date start;
    @JsonIgnore
    public final Date end;

    @JsonProperty("after")
    @JsonInclude(Include.NON_NULL)
    public String getStartISO8601() {
      if (this.start == null) {
        return null;
      }
      return iso8601.format(this.start);
    }

    @JsonProperty("before")
    @JsonInclude(Include.NON_NULL)
    public String getEndISO8601() {
      if (this.end == null) {
        return null;
      }
      return iso8601.format(this.end);
    }

    public SearchRange(SearchRangeType rangeType, Date start, Date end) {
      this.rangeType = rangeType;
      this.start = start;
      this.end = end;
    }
  }

  public enum SearchRangeType {
    CREATED,
    MODIFIED
  }


}
