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
import com.google.common.collect.Multimap;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.update.UpdateRequest;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.persistence.Dto;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;

public abstract class BaseIndex<DOMAIN, DTO extends Dto<KEY>, KEY extends Serializable>
  implements Index<DOMAIN, DTO, KEY> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseIndex.class);

  private final SearchClient client;
  private final BaseNormalizer<DTO, KEY> normalizer;
  private final IndexDefinition indexDefinition;

  protected BaseIndex(IndexDefinition indexDefinition, BaseNormalizer<DTO, KEY> normalizer,
                      WorkQueue workQueue, SearchClient client) {
    this.normalizer = normalizer;
    this.client = client;
    this.indexDefinition = indexDefinition;
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

  public SearchClient getClient(){
    return client;
  }

  // Scrolling within the index
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

    Max max = (Max) response.getAggregations().get("latest");

    if (max.getValue() > 0) {
      date = new DateTime(Double.valueOf(max.getValue()).longValue()).toDate();
    } else {
      date = new Date(0L);
    }

    LOG.info("Index {}:{} has last update of {}", this.getIndexName(), this.getIndexType(), date);
    return date;
  }

  /* Index management methods */

  protected abstract String getKeyValue(KEY key);

  protected abstract Settings getIndexSettings() throws IOException;

  protected abstract Map mapProperties();

  protected abstract Map mapKey();

  protected Map mapDomain() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("dynamic", false);
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

  @Override
  public void refresh() {
    client.execute(client
      .admin()
      .indices()
      .prepareRefresh(this.getIndexName())
      .setForce(false)
      .setIndices(this.getIndexName()));
  }

  /* Base CRUD methods */

  protected abstract DOMAIN toDoc(Map<String, Object> fields);

  public DOMAIN getByKey(KEY key) {
    GetRequestBuilder request = client.prepareGet()
      .setType(this.getIndexType())
      .setIndex(this.getIndexName())
      .setId(this.getKeyValue(key))
      .setRouting(this.getKeyValue(key));

    GetResponse response = client.execute(request);

    if (response.isExists()) {
      return toDoc(response.getSource());
    }
    return null;
  }

  protected void updateDocument(Collection<UpdateRequest> requests, KEY key) {
    LOG.debug("UPDATE _id:{} in index {}", key, this.getIndexName());
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    for (UpdateRequest request : requests) {
      // if request has no ID then no upsert possible!
      if (request.id() == null || request.id().isEmpty()) {
        bulkRequest.add(new IndexRequest()
          .source(request.doc().sourceAsMap())
          .type(this.getIndexType())
          .index(this.getIndexName()));
      } else {
        bulkRequest.add(request
          .id(this.getKeyValue(key))
          .index(this.getIndexName())
          .type(this.getIndexType()));
      }
    }
    BulkResponse response = client.execute(bulkRequest);
  }

  @Override
  public void upsert(KEY key, Object object, Object... objects) throws Exception {
    long t0 = System.currentTimeMillis();
    List<UpdateRequest> requests = this.normalizer.normalizeNested(object, key);
    for (Object additionalObject : objects) {
      requests.addAll(this.normalizer.normalizeNested(additionalObject, key));
    }
    long t1 = System.currentTimeMillis();
    this.updateDocument(requests, key);
    long t2 = System.currentTimeMillis();
    LOG.debug("UPSERT [object] time:{}ms ({}ms normalize, {}ms elastic)",
      t2 - t0, t1 - t0, t2 - t1);
  }

  @Override
  public void upsert(DTO item, DTO... items) {
    try {
      long t0 = System.currentTimeMillis();
      List<UpdateRequest> requests = normalizer.normalize(item);
      for (DTO additionalItem : items) {
        requests.addAll(normalizer.normalize(additionalItem));
      }
      long t1 = System.currentTimeMillis();
      this.updateDocument(requests, item.getKey());
      long t2 = System.currentTimeMillis();
      LOG.debug("UPSERT [dto] time:{}ms ({}ms normalize, {}ms elastic)",
        t2 - t0, t1 - t0, t2 - t1);
    } catch (Exception e) {
      LOG.error("Could not update document for index {}: {}",
        this.getIndexName(), e.getMessage(), e);
    }
  }

  private void deleteDocument(KEY key) throws ExecutionException, InterruptedException {
    LOG.debug("DELETE _id:{} in index {}", key, this.getIndexName());
    DeleteRequestBuilder request = client
      .prepareDelete()
      .setIndex(this.getIndexName())
      .setType(this.getIndexType())
      .setId(this.getKeyValue(key));
    DeleteResponse response = client.execute(request);
  }

  @Override
  public void delete(KEY key, Object object, Object... objects) throws Exception {
    LOG.debug("DELETE NESTED _id:{} in index {}", key, this.getIndexName());
    List<UpdateRequest> requests = this.normalizer.deleteNested(object, key);
    for (Object additionalObject : objects) {
      requests.addAll(this.normalizer.deleteNested(additionalObject, key));
    }
    this.updateDocument(requests, key);
  }

  @Override
  public void deleteByKey(KEY key, KEY... keys) {
    try {
      this.deleteDocument(key);
      for (KEY additionalKey : keys) {
        this.deleteDocument(additionalKey);
      }
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Could not DELETE _id = '%s' for index '%s",
        getKeyValue(key), getIndexName()), e);
    }
  }

  @Override
  public void deleteByDto(DTO item, DTO... items) {
    try {
      this.deleteDocument(item.getKey());
      for (DTO additionalItem : items) {
        this.deleteDocument(additionalItem.getKey());
      }
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Could not DELETE _id = '%s' for index '%s",
        getKeyValue(item.getKey()), getIndexName()), e);
    }
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
}
