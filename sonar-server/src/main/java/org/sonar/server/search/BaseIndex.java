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
import org.elasticsearch.action.bulk.BulkRequestBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.persistence.Dto;
import org.sonar.server.es.ESNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class BaseIndex<D, E extends Dto<K>, K extends Serializable>
  implements Index<D, E, K> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseIndex.class);

  private final ESNode node;
  protected BaseNormalizer<E, K> normalizer;
  protected final IndexDefinition indexDefinition;

  public BaseIndex(IndexDefinition indexDefinition, BaseNormalizer<E, K> normalizer,
                   WorkQueue workQueue, ESNode node) {
    this.normalizer = normalizer;
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
    initializeIndex();
  }

  @Override
  public void stop() {

  }

  /* Cluster And ES Stats/Client methods */

  protected void initializeIndex() {
    String index = this.getIndexName();

    IndicesExistsResponse indexExistsResponse = getClient().admin().indices()
      .prepareExists(index).execute().actionGet();
    try {

      if (!indexExistsResponse.isExists()) {
        LOG.info("Setup of {} for type {}", this.getIndexName(), this.getIndexType());
        getClient().admin().indices().prepareCreate(index)
          .setSettings(getIndexSettings())
          .addMapping(getIndexType(), getMapping())
          .execute().actionGet();

      } else {
        LOG.info("Update of index {} for type {}", this.getIndexName(), this.getIndexType());
        getClient().admin().indices().preparePutMapping(index)
          .setType(getIndexType())
          .setIgnoreConflicts(true)
          .setSource(getMapping())
          .execute().actionGet();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Invalid configuration for index " + this.getIndexName(), e);
    }
  }

  /* Index management methods */

  protected abstract String getKeyValue(K key);

  protected abstract XContentBuilder getIndexSettings() throws IOException;

  protected abstract XContentBuilder getMapping() throws IOException;

  @Override
  public void refresh() {
    getClient()
      .admin()
      .indices()
      .prepareRefresh(this.getIndexName())
      .get();
  }

  /* Search methods */

  @Override
  public SearchResponse search(SearchRequestBuilder request,
                               FilterBuilder filter, QueryBuilder query) {

    request.setQuery(QueryBuilders.filteredQuery(query, filter));
    SearchResponse esResult = request.get();
    return esResult;
  }

  /* Base CRUD methods */

  protected abstract D toDoc(Map<String,Object> fields);

  public D getByKey(K key) {
    GetResponse response = getClient().prepareGet()
      .setType(this.getIndexType())
      .setIndex(this.getIndexName())
      .setId(this.getKeyValue(key))
      .setRouting(this.getKeyValue(key))
      .get();
    if (response.isExists()) {
      return toDoc(response.getSource());
    }
    return null;
  }

  protected void updateDocument(Collection<UpdateRequest> requests, K key) throws Exception {
    LOG.debug("UPDATE _id:{} in index {}", key, this.getIndexName());
    BulkRequestBuilder bulkRequest = getClient().prepareBulk();
    for(UpdateRequest request : requests){
      bulkRequest.add(request
      .index(this.getIndexName())
      .id(this.getKeyValue(key))
      .type(this.getIndexType()));
    }
    bulkRequest.get();
  }


  @Override
  public void upsert(Object obj, K key) throws Exception {
    if (this.normalizer.canNormalize(obj.getClass(), key.getClass())) {
      this.updateDocument(this.normalizer.normalizeOther(obj, key), key);
    } else {
      throw new IllegalStateException("Index " + this.getIndexName() +
        " cannot execute UPDATE for class: " + obj.getClass());
    }
  }

  @Override
  public void upsertByDto(E item) {
    try {
      this.updateDocument(normalizer.normalize(item), item.getKey());
    } catch (Exception e) {
      LOG.error("Could not update document for index {}: {}",
        this.getIndexName(), e.getMessage());
    }
  }

  @Override
  public void upsertByKey(K key) {
    try {
      this.updateDocument(normalizer.normalize(key), key);
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
      .setType(this.getIndexType())
      .setId(this.getKeyValue(key))
      .get();
  }

  @Override
  public void delete(Object obj, K key) throws Exception {
    if (this.normalizer.canNormalize(obj.getClass(), key.getClass())) {
      //TODO don't really know what to do here for the moment...
    } else {
      throw new IllegalStateException("Index " + this.getIndexName() +
        " cannot execute DELETE for class: " + obj.getClass());
    }
  }

  @Override
  public void deleteByKey(K key) {
    try {
      this.deleteDocument(key);
    } catch (Exception e) {
      LOG.error("Could not DELETE _id = '{}' for index '{}': {}",
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


  protected void addMatchField(XContentBuilder mapping, String field, String type) throws IOException {
    mapping.startObject(field)
      .field("type", type)
      .field("index", "not_analyzed")
      .endObject();
  }

  protected BoolFilterBuilder addMultiFieldTermFilter(Collection<String> values, BoolFilterBuilder filter, String... fields) {
    if (values != null && !values.isEmpty()) {
      BoolFilterBuilder valuesFilter = FilterBuilders.boolFilter();
      for (String value : values) {
        Collection<FilterBuilder> filterBuilders = new ArrayList<FilterBuilder>();
        for (String field : fields) {
          filterBuilders.add(FilterBuilders.termFilter(field, value));
        }
        valuesFilter.should(FilterBuilders.orFilter(filterBuilders.toArray(new FilterBuilder[filterBuilders.size()])));
      }
      filter.must(valuesFilter);
    }
    return filter;
  }


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
