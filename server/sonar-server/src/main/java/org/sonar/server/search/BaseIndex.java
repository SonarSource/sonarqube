/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.search;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;
import org.elasticsearch.search.fetch.source.FetchSourceContext;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.persistence.Dto;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class BaseIndex<DOMAIN, DTO extends Dto<KEY>, KEY extends Serializable>
  implements Index<DOMAIN, DTO, KEY> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseIndex.class);

  private final SearchClient client;
  private final BaseNormalizer<DTO, KEY> normalizer;
  private final IndexDefinition indexDefinition;

  protected BaseIndex(IndexDefinition indexDefinition, BaseNormalizer<DTO, KEY> normalizer, SearchClient client) {
    this.normalizer = normalizer;
    this.client = client;
    this.indexDefinition = indexDefinition;
  }

  @Override
  public BaseNormalizer<DTO, KEY> getNormalizer() {
    return normalizer;
  }

  @Override
  public final String getIndexName() {
    return this.indexDefinition.getIndexName();
  }

  @Override
  public final String getIndexType() {
    return this.indexDefinition.getIndexType();
  }

  /* Component Methods */

  @Override
  public void start() {
    /* Setup the index if necessary */
    initializeIndex();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public SearchClient getClient() {
    return client;
  }

  // Scrolling within the index
  @Override
  public Iterator<DOMAIN> scroll(final String scrollId) {
    return new Iterator<DOMAIN>() {

      private final Queue<SearchHit> hits = new ArrayDeque<SearchHit>();

      private void fillQueue() {
        try {
          SearchScrollRequestBuilder esRequest = client.prepareSearchScroll(scrollId)
            .setScroll(TimeValue.timeValueMinutes(3));
          Collections.addAll(hits, ((SearchResponse) client.execute(esRequest)).getHits().getHits());
        } catch (Exception e) {
          throw new IllegalStateException("Error while filling in the scroll buffer", e);
        }
      }

      @Override
      public boolean hasNext() {
        if (hits.isEmpty()) {
          fillQueue();
        }
        return !hits.isEmpty();
      }

      @Override
      public DOMAIN next() {
        if (hits.isEmpty()) {
          fillQueue();
        }
        return toDoc(hits.poll().getSource());
      }

      @Override
      public void remove() {
        throw new IllegalStateException("Cannot remove item from scroll Iterable!!!" +
          " Use Service or DAO classes instead");
      }
    };
  }

  /* Cluster And ES Stats/Client methods */

  protected void initializeIndex() {
    String index = this.getIndexName();

    IndicesExistsResponse indexExistsResponse = client.admin().indices()
      .prepareExists(index).execute().actionGet();
    try {

      if (!indexExistsResponse.isExists()) {
        LOG.debug("Setup of {} for type {}", this.getIndexName(), this.getIndexType());
        client.admin().indices().prepareCreate(index)
          .setSettings(getIndexSettings())
          .execute().actionGet();
      }

      LOG.debug("Update of index {} for type {}", this.getIndexName(), this.getIndexType());
      client.admin().indices().preparePutMapping(index)
        .setType(getIndexType())
        .setIgnoreConflicts(true)
        .setSource(mapDomain())
        .get();

    } catch (Exception e) {
      throw new IllegalStateException("Invalid configuration for index " + this.getIndexName(), e);
    }
  }

  @Override
  public IndexStat getIndexStat() {
    IndexStat stat = new IndexStat();

    /** get total document count */
    CountRequestBuilder countRequest = client.prepareCount(this.getIndexName())
      .setTypes(this.getIndexType())
      .setQuery(QueryBuilders.matchAllQuery());
    CountResponse response = client.execute(countRequest);
    stat.setDocumentCount(response.getCount());

    /** get Management information */
    stat.setLastUpdate(getLastSynchronization());
    return stat;
  }

  /* Synchronization methods */

  @Override
  public Date getLastSynchronization() {

    Date date;
    SearchRequestBuilder request = client.prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(0)
      .addAggregation(AggregationBuilders.max("latest")
        .field(BaseNormalizer.UPDATED_AT_FIELD));

    SearchResponse response = client.execute(request);

    Max max = response.getAggregations().get("latest");

    if (max.getValue() > 0) {
      date = new DateTime(Double.valueOf(max.getValue()).longValue()).toDate();
    } else {
      date = new Date(0L);
    }

    LOG.debug("Index {}:{} has last update of {}", this.getIndexName(), this.getIndexType(), date);
    return date;
  }

  /* Index management methods */

  protected abstract String getKeyValue(KEY key);

  public final Settings getIndexSettings() {
    ImmutableSettings.Builder settings = this.addCustomIndexSettings(this.getBaseIndexSettings());

    // In case there is a replication factor set by the index,
    // it is removed since we're using global cluster state
    // see https://jira.codehaus.org/browse/SONAR-5687
    settings.remove("index.number_of_replicas");

    return settings.build();
  }

  protected ImmutableSettings.Builder addCustomIndexSettings(ImmutableSettings.Builder baseIndexSettings) {
    return baseIndexSettings;
  }

  protected abstract Map mapProperties();

  protected abstract Map mapKey();

  protected Map mapDomain() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("dynamic", false);
    mapping.put("_all", ImmutableMap.of("enabled", false));
    if (mapKey() != null) {
      mapping.put("_id", mapKey());
    }
    mapping.put("properties", mapProperties());
    LOG.debug("Index Mapping {}", mapping.get("properties"));
    return mapping;
  }

  protected Map mapField(IndexField field) {
    return mapField(field, true);
  }

  protected Map mapField(IndexField field, boolean allowRecursive) {
    if (field.type() == IndexField.Type.TEXT) {
      return mapTextField(field, allowRecursive);
    } else if (field.type() == IndexField.Type.STRING) {
      return mapStringField(field, allowRecursive);
    } else if (field.type() == IndexField.Type.BOOLEAN) {
      return mapBooleanField(field);
    } else if (field.type() == IndexField.Type.OBJECT) {
      return mapNestedField(field);
    } else if (field.type() == IndexField.Type.DATE) {
      return mapDateField(field);
    } else if (field.type() == IndexField.Type.NUMERIC) {
      return mapNumericField(field);
    } else {
      throw new IllegalStateException("Mapping does not exist for type: " + field.type());
    }
  }

  protected Map mapNumericField(IndexField field) {
    return ImmutableMap.of("type", "double");
  }

  protected Map mapBooleanField(IndexField field) {
    return ImmutableMap.of("type", "boolean");
  }

  protected Map mapNestedField(IndexField field) {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("type", "nested");
    mapping.put("dynamic", "true");
    Map<String, Object> mappings = new HashMap<String, Object>();
    for (IndexField nestedField : field.nestedFields()) {
      if (nestedField != null) {
        mappings.put(nestedField.field(), mapField(nestedField));
      }
    }
    mapping.put("properties", mappings);
    return mapping;
  }

  protected Map mapDateField(IndexField field) {
    return ImmutableMap.of(
      "type", "date",
      "format", "date_time");
  }

  protected boolean needMultiField(IndexField field) {
    return (field.type() == IndexField.Type.TEXT
      || field.type() == IndexField.Type.STRING)
      && (field.sortable() || field.searchable());
  }

  protected Map mapSortField(IndexField field) {
    return ImmutableMap.of(
      "type", "string",
      "index", "analyzed",
      "analyzer", "sortable");
  }

  protected Map mapGramsField(IndexField field) {
    return ImmutableMap.of(
      "type", "string",
      "index", "analyzed",
      "index_analyzer", "index_grams",
      "search_analyzer", "search_grams");
  }

  protected Map mapWordsField(IndexField field) {
    return ImmutableMap.of(
      "type", "string",
      "index", "analyzed",
      "index_analyzer", "index_words",
      "search_analyzer", "search_words");
  }

  protected Map mapMultiField(IndexField field) {
    Map<String, Object> mapping = new HashMap<String, Object>();
    if (field.sortable()) {
      mapping.put(IndexField.SORT_SUFFIX, mapSortField(field));
    }
    if (field.searchable()) {
      if (field.type() != IndexField.Type.TEXT) {
        mapping.put(IndexField.SEARCH_PARTIAL_SUFFIX, mapGramsField(field));
      }
      mapping.put(IndexField.SEARCH_WORDS_SUFFIX, mapWordsField(field));
    }
    mapping.put(field.field(), mapField(field, false));
    return mapping;
  }

  protected Map mapStringField(IndexField field, boolean allowRecursive) {
    Map<String, Object> mapping = new HashMap<String, Object>();
    // check if the field needs to be MultiField
    if (allowRecursive && needMultiField(field)) {
      mapping.put("type", "multi_field");
      mapping.put("fields", mapMultiField(field));
    } else {
      mapping.put("type", "string");
      mapping.put("index", "analyzed");
      mapping.put("index_analyzer", "keyword");
      mapping.put("search_analyzer", "whitespace");
    }
    return mapping;
  }

  protected Map mapTextField(IndexField field, boolean allowRecursive) {
    Map<String, Object> mapping = new HashMap<String, Object>();
    // check if the field needs to be MultiField
    if (allowRecursive && needMultiField(field)) {
      mapping.put("type", "multi_field");
      mapping.put("fields", mapMultiField(field));
    } else {
      mapping.put("type", "string");
      mapping.put("index", "analyzed");
      mapping.put("index_analyzer", "keyword");
      mapping.put("search_analyzer", "whitespace");
    }
    return mapping;
  }

  /* Base CRUD methods */

  protected abstract DOMAIN toDoc(Map<String, Object> fields);

  public DOMAIN getByKey(KEY key) {
    DOMAIN value = getNullableByKey(key);
    if (value == null) {
      throw new NotFoundException(String.format("Key '%s' not found", key));
    }
    return value;
  }

  @CheckForNull
  @Override
  public DOMAIN getNullableByKey(KEY key) {
    GetRequestBuilder request = client.prepareGet()
      .setType(this.getIndexType())
      .setIndex(this.getIndexName())
      .setId(this.getKeyValue(key))
      .setFetchSource(true)
      .setRouting(this.getKeyValue(key));

    GetResponse response = client.execute(request);

    if (response.isExists()) {
      return toDoc(response.getSource());
    }
    return null;
  }

  public List<DOMAIN> getByKeys(Collection<KEY> keys) {
    List<DOMAIN> results = new ArrayList<DOMAIN>();
    MultiGetRequestBuilder request = client.prepareMultiGet()
      .setPreference("_local");
    for (KEY key : keys) {
      request.add(new MultiGetRequest
        .Item(getIndexName(), getIndexType(), getKeyValue(key))
          .routing(getKeyValue(key))
          .fetchSourceContext(FetchSourceContext.FETCH_SOURCE));
    }

    try {
      MultiGetResponse response = client.execute(request);
      if (response.getResponses() != null) {
        for (MultiGetItemResponse item : response.getResponses()) {
          results.add(toDoc(item.getResponse().getSource()));
        }
      }
    } catch (Exception e) {
      LOG.debug("could not multi-get.", e);
    }
    return results;
  }

  public Collection<DOMAIN> getByKeys(KEY... keys) {
    return getByKeys(ImmutableSet.copyOf(keys));
  }

  /* ES QueryHelper Methods */

  protected BoolFilterBuilder addTermFilter(BoolFilterBuilder filter, String field, @Nullable Collection<String> values) {
    if (values != null && !values.isEmpty()) {
      BoolFilterBuilder valuesFilter = FilterBuilders.boolFilter();
      for (String value : values) {
        FilterBuilder valueFilter = FilterBuilders.termFilter(field, value);
        valuesFilter.should(valueFilter);
      }
      filter.must(valuesFilter);
    }
    return filter;
  }

  protected BoolFilterBuilder addTermFilter(BoolFilterBuilder filter, String field, @Nullable String value) {
    if (value != null && !value.isEmpty()) {
      filter.must(FilterBuilders.termFilter(field, value));
    }
    return filter;
  }

  public Long countAll() {
    return client.prepareCount(this.getIndexName())
      .setTypes(this.getIndexType())
      .get().getCount();
  }

  public Map<String, Long> countByField(IndexField indexField, FilterBuilder filter) {
    Map<String, Long> counts = new HashMap<String, Long>();

    SearchRequestBuilder request = client.prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setQuery(QueryBuilders.filteredQuery(
        QueryBuilders.matchAllQuery(),
        filter))
      .setSize(0)
      .addAggregation(AggregationBuilders
        .terms(indexField.field())
        .field(indexField.field())
        .order(Terms.Order.count(false))
        .size(Integer.MAX_VALUE)
        .minDocCount(0));

    SearchResponse response = client.execute(request);

    Terms values =
      response.getAggregations().get(indexField.field());

    for (Terms.Bucket value : values.getBuckets()) {
      counts.put(value.getKey(), value.getDocCount());
    }
    return counts;
  }

  public Map<String, Long> countByField(IndexField indexField) {
    return countByField(indexField, FilterBuilders.matchAllFilter());
  }

  // Response helpers
  protected Multimap<String, FacetValue> processAggregations(Aggregations aggregations) {
    Multimap<String, FacetValue> stats = ArrayListMultimap.create();
    if (aggregations != null) {
      for (Aggregation aggregation : aggregations.asList()) {
        if (aggregation.getClass().isAssignableFrom(StringTerms.class)) {
          for (Terms.Bucket value : ((Terms) aggregation).getBuckets()) {

            FacetValue facetValue = new FacetValue(value.getKey(), (int) value.getDocCount());
            facetValue.setSubFacets(processAggregations(value.getAggregations()));
            stats.put(aggregation.getName(), facetValue);
          }
        } else if (aggregation.getClass().isAssignableFrom(InternalValueCount.class)) {
          InternalValueCount count = (InternalValueCount) aggregation;
          FacetValue facetValue = new FacetValue(count.getName(), (int) count.getValue());
          stats.put(count.getName(), facetValue);
        }
      }
    }
    return stats;
  }

  private ImmutableSettings.Builder getBaseIndexSettings() {
    return ImmutableSettings.builder()

      .put("index.number_of_replicas", 0)
      .put("index.number_of_shards", 1)

      // Disallow dynamic mapping (too expensive)
      .put("index.mapper.dynamic", false)

      // Sortable text analyzer
      .put("index.analysis.analyzer.sortable.type", "custom")
      .put("index.analysis.analyzer.sortable.tokenizer", "keyword")
      .putArray("index.analysis.analyzer.sortable.filter", "trim", "lowercase", "truncate")

      // Edge NGram index-analyzer
      .put("index.analysis.analyzer.index_grams.type", "custom")
      .put("index.analysis.analyzer.index_grams.tokenizer", "whitespace")
      .putArray("index.analysis.analyzer.index_grams.filter", "trim", "lowercase", "gram_filter")

      // Edge NGram search-analyzer
      .put("index.analysis.analyzer.search_grams.type", "custom")
      .put("index.analysis.analyzer.search_grams.tokenizer", "whitespace")
      .putArray("index.analysis.analyzer.search_grams.filter", "trim", "lowercase")

      // Word index-analyzer
      .put("index.analysis.analyzer.index_words.type", "custom")
      .put("index.analysis.analyzer.index_words.tokenizer", "standard")
      .putArray("index.analysis.analyzer.index_words.filter",
        "standard", "word_filter", "lowercase", "stop", "asciifolding", "porter_stem")

      // Word search-analyzer
      .put("index.analysis.analyzer.search_words.type", "custom")
      .put("index.analysis.analyzer.search_words.tokenizer", "standard")
      .putArray("index.analysis.analyzer.search_words.filter",
        "standard", "lowercase", "stop", "asciifolding", "porter_stem")

      // Edge NGram filter
      .put("index.analysis.filter.gram_filter.type", "edgeNGram")
      .put("index.analysis.filter.gram_filter.min_gram", 2)
      .put("index.analysis.filter.gram_filter.max_gram", 15)
      .putArray("index.analysis.filter.gram_filter.token_chars", "letter", "digit", "punctuation", "symbol")

      // Word filter
      .put("index.analysis.filter.word_filter.type", "word_delimiter")
      .put("index.analysis.filter.word_filter.generate_word_parts", true)
      .put("index.analysis.filter.word_filter.catenate_words", true)
      .put("index.analysis.filter.word_filter.catenate_numbers", true)
      .put("index.analysis.filter.word_filter.catenate_all", true)
      .put("index.analysis.filter.word_filter.split_on_case_change", true)
      .put("index.analysis.filter.word_filter.preserve_original", true)
      .put("index.analysis.filter.word_filter.split_on_numerics", true)
      .put("index.analysis.filter.word_filter.stem_english_possessive", true)

      // Path Analyzer
      .put("index.analysis.analyzer.path_analyzer.type", "custom")
      .put("index.analysis.analyzer.path_analyzer.tokenizer", "path_hierarchy");

  }
}
