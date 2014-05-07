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

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.db.Dto;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.es.ESNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class BaseIndex<R, Q, E extends Dto<K>, K extends Serializable>
  implements Index<R, Q, E, K> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseIndex.class);

  private final Profiling profiling;
  private final ESNode node;
  protected BaseNormalizer<E, K> normalizer;
  protected final IndexDefinition indexDefinition;

  public BaseIndex(IndexDefinition indexDefinition, BaseNormalizer<E, K> normalizer, WorkQueue workQueue,
                   Profiling profiling, ESNode node) {
    this.normalizer = normalizer;
    this.profiling = profiling;
    this.node = node;
    this.indexDefinition = indexDefinition;
  }

  @Override
  public String getIndexName() {
    return this.indexDefinition.getIndexName();
  }

  @Override
  public String getIndexType() {
    return this.indexDefinition.getIndexType();
  }

  protected Client getClient() {
    return node.client();
  }

  protected ESNode getNode() {
    return this.node;
  }

  /* Component Methods */

  @Override
  public void start() {

    /* Setup the index if necessary */
    this.initializeIndex();
  }

  @Override
  public void stop() {

  }

  /* Cluster And ES Stats/Client methods */

  protected void initializeIndex() {

    String index = this.getIndexName();

    IndicesExistsResponse indexExistsResponse = getClient().admin().indices()
      .prepareExists(index).execute().actionGet();

    if (!indexExistsResponse.isExists()) {

      try {
        LOG.info("Setup of index {}", this.getIndexName());
        getClient().admin().indices().prepareCreate(index)
          .setSettings(getIndexSettings())
          .addMapping(this.indexDefinition.getIndexType(), getMapping())
          .execute().actionGet();
      } catch (Exception e) {
        throw new RuntimeException("Invalid configuration for index " + this.getIndexName(), e);
      }
    }
  }

  /* Index management methods */

  protected abstract String getKeyValue(K key);

  protected abstract XContentBuilder getIndexSettings() throws IOException;

  protected abstract XContentBuilder getMapping() throws IOException;

  @Override
  public void refresh() {
    getClient().admin().indices().prepareRefresh(this.getIndexName()).get();
  }

  /* Search methods */

  protected abstract QueryBuilder getQuery(Q query, QueryOptions options);

  protected abstract FilterBuilder getFilter(Q query, QueryOptions options);

  protected abstract SearchRequestBuilder buildRequest(Q query, QueryOptions options);

  @Override
  public Result<R> search(Q query) {
    return this.search(query, new QueryOptions());
  }

  public Result<R> search(Q query, QueryOptions options) {

    SearchRequestBuilder esSearch = this.buildRequest(query, options);
    FilterBuilder fb = this.getFilter(query, options);
    QueryBuilder qb = this.getQuery(query, options);

    esSearch.setQuery(QueryBuilders.filteredQuery(qb, fb));

    SearchResponse esResult = esSearch.get();

    Result<R> result = new Result<R>(esResult);

    if(esResult != null){
      result
        .setTotal((int) esResult.getHits().totalHits())
        .setTime(esResult.getTookInMillis());

      for (SearchHit hit : esResult.getHits()) {
        result.getHits().add(this.getSearchResult(hit));
      }
    }

    return result;
  }

  /* Transform Methods */

  protected abstract R getSearchResult(Map<String, Object> fields);

  protected R getSearchResult(SearchHit hit){
    Map<String, Object> fields = new HashMap<String, Object>();
    for (Map.Entry<String, SearchHitField> field:hit.getFields().entrySet()){
      fields.put(field.getKey(),field.getValue().getValue());
    }
    return this.getSearchResult(fields);
  }

  /* Base CRUD methods */

  @Override
  public R getByKey(K key) {
    GetResponse result = getClient().prepareGet(this.getIndexName(),
      this.indexDefinition.getIndexType(), this.getKeyValue(key))
      .get();
    return this.getSearchResult(result.getSource());
  }

  private void insertDocument(UpdateRequest request, K key) throws Exception {
    LOG.debug("INSERT _id:{} in index {}", this.getKeyValue(key), this.getIndexName());
    updateDocument(request, key);
  }

  @Override
  public void insert(Object obj, K key) throws Exception {
    if (this.normalizer.canNormalize(obj.getClass(), key.getClass())) {
      this.updateDocument(this.normalizer.normalizeOther(obj, key), key);
    } else {
      throw new IllegalStateException("No normalizer method available for " +
        obj.getClass().getSimpleName() + " in " + normalizer.getClass().getSimpleName());
    }
  }

  @Override
  public void insertByDto(E item) {
    try {
      UpdateRequest doc = normalizer.normalize(item);
      insertDocument(doc, item.getKey());
    } catch (Exception e) {
      throw new IllegalStateException(this.getClass().getSimpleName() +
        "cannot execute INSERT_BY_DTO for " + item.getClass().getSimpleName() +
        " as " + this.getIndexType() +
        " on key: " + item.getKey(), e);
    }
  }

  @Override
  public void insertByKey(K key) {
    try {
      UpdateRequest doc = normalizer.normalize(key);
      insertDocument(doc, key);
    } catch (Exception e) {
      throw new IllegalStateException(this.getClass().getSimpleName() +
        "cannot execute INSERT_BY_KEY for " + key.getClass().getSimpleName() +
        " as " + this.getIndexType() +
        " on key: " + key, e);
    }
  }


  protected void updateDocument(UpdateRequest request, K key) throws Exception {
    LOG.debug("UPDATE _id:{} in index {}", key, this.getIndexName());
    getClient().update(request
      .index(this.getIndexName())
      .id(this.getKeyValue(key))
      .type(this.getIndexType())).get();
  }


  @Override
  public void update(Object obj, K key) throws Exception {
    if (this.normalizer.canNormalize(obj.getClass(), key.getClass())) {
      this.updateDocument(this.normalizer.normalizeOther(obj, key), key);
    } else {
      throw new IllegalStateException("Index " + this.getIndexName() +
        " cannot execute INSERT for class: " + obj.getClass());
    }
  }

  @Override
  public void updateByDto(E item) {
    try {
      UpdateRequest doc = normalizer.normalize(item);
      this.updateDocument(doc, item.getKey());
    } catch (Exception e) {
      LOG.error("Could not update document for index {}: {}",
        this.getIndexName(), e.getMessage());
    }
  }

  @Override
  public void updateByKey(K key) {
    try {
      UpdateRequest doc = normalizer.normalize(key);
      this.updateDocument(doc, key);
    } catch (Exception e) {
      LOG.error("Could not update document for index {}: {}",
        this.getIndexName(), e.getMessage());
    }
  }

  private void deleteDocument(K key) throws ExecutionException, InterruptedException {
    LOG.debug("DELETE _id:{} in index {}", key, this.getIndexName());
    getClient()
      .prepareDelete()
      .setIndex(this.getIndexName())
      .setType(this.indexDefinition.getIndexType())
      .setIndex(this.getKeyValue(key))
      .get();
  }

  @Override
  public void delete(Object obj, K key) throws Exception {
    if (this.normalizer.canNormalize(obj.getClass(), key.getClass())) {
      //TODO don't really know what to do here for the moment...
    } else {
      throw new IllegalStateException("Index " + this.getIndexName() +
        " cannot execute INSERT for class: " + obj.getClass());
    }
  }

  @Override
  public void deleteByKey(K key) {
    try {
      this.deleteDocument(key);
    } catch (Exception e) {
      LOG.error("Could not DELETE _id:{} for index {}: {}",
        this.getKeyValue(key), this.getIndexName(), e.getMessage());
    }
  }

  @Override
  public void deleteByDto(E item) {
    try {
      this.deleteDocument(item.getKey());
    } catch (Exception e) {
      LOG.error("Could not DELETE _id:{} for index {}: {}",
        this.getKeyValue(item.getKey()), this.getIndexName(), e.getMessage());
    }
  }

  /* Synchronization methods */

  Long lastSynch = 0L;
  Long cooldown = 30000L;

  @Override
  public void setLastSynchronization(Long time) {
    if (time > (getLastSynchronization() + cooldown)) {
      LOG.trace("Updating synchTime updating");
      lastSynch = time;
    } else {
      LOG.trace("Not updating synchTime, still cooling down");
    }

  }

  @Override
  public Long getLastSynchronization() {
    //TODO need to read that in the admin index;
    return 0L;
  }

  /* ES QueryHelper Methods */

  protected BoolFilterBuilder addTermFilter(String field, Collection<String> values, BoolFilterBuilder filter) {
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

  protected BoolFilterBuilder addTermFilter(String field, String value, BoolFilterBuilder filter) {
    if (value != null && !value.isEmpty()) {
      filter.must(FilterBuilders.termFilter(field, value));
    }
    return filter;
  }
}
