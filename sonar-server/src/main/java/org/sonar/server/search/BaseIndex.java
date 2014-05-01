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
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.db.Dto;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.es.ESNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public abstract class BaseIndex<K extends Serializable, E extends Dto<K>> implements Index<E, K> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseIndex.class);

  private final Profiling profiling;
  private final ESNode node;
  protected BaseNormalizer<E, K> normalizer;

  public BaseIndex(BaseNormalizer<E, K> normalizer, WorkQueue workQueue,
                   Profiling profiling, ESNode node) {
    this.normalizer = normalizer;
    this.profiling = profiling;
    this.node = node;
  }

  protected Client getClient() {
    return node.client();
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

  private void initializeIndex() {

    String index = this.getIndexName();

    IndicesExistsResponse indexExistsResponse = getClient().admin().indices()
      .prepareExists(index).execute().actionGet();

    if (!indexExistsResponse.isExists()) {

      LOG.info("Setup of index {}", this.getIndexName());

      try {
        LOG.debug("Settings: {}", getIndexSettings().string());
        LOG.debug("Mapping: {}", getMapping().string());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      getClient().admin().indices().prepareCreate(index)
        .setSettings(getIndexSettings())
        .addMapping(getType(), getMapping())
        .execute().actionGet();
    }
  }

  /* Index management methods */

  protected abstract XContentBuilder getIndexSettings();

  protected abstract String getType();

  protected abstract XContentBuilder getMapping();

  /* Base CRUD methods */

  protected abstract String getKeyValue(K key);

  @Override
  public void refresh(){
    getClient().admin().indices().prepareRefresh(this.getIndexName()).get();
  }

  @Override
  public Hit getByKey(K key) {
    GetResponse result = getClient().prepareGet(this.getIndexName(), this.getType(), this.getKeyValue(key))
      .get();
    return Hit.fromMap(0, result.getSourceAsMap());
  }

  private void insertDocument(XContentBuilder doc, String key) throws ExecutionException, InterruptedException {
    LOG.debug("INSERT _id:{} in index {}", key, this.getIndexName());
    getClient().index(
      new IndexRequest(this.getIndexName(),
        getType(), key)
        .source(doc)
    ).get();
  }

  @Override
  public void insert(Object obj) throws InvalidIndexActionException {
    LOG.info("INSERT for class" + obj.getClass().getSimpleName());
    if(Dto.class.isAssignableFrom(obj.getClass())){
      this.insertByDto((E) obj);
    } else if(Serializable.class.isAssignableFrom(obj.getClass())) {
      this.insertByKey((K) obj);
    } else {
      throw new InvalidIndexActionException("Index " + this.getIndexName() +
        " cannot execute INSERT for class: " + obj.getClass());
    }
  }

  @Override
  public void insertByDto(E item) {
    try {
      XContentBuilder doc = normalizer.normalize(item);
      String keyValue = getKeyValue(item.getKey());
      if (doc != null && keyValue != null && !keyValue.isEmpty()) {
        insertDocument(doc, keyValue);
      } else {
        LOG.error("Could not normalize document {} for insert in {}",
          keyValue, getIndexName());
      }
    } catch (Exception e) {
      LOG.error("Could not update document for index {}: {}",
        getIndexName(), e.getMessage());
    }
  }

  @Override
  public void insertByKey(K key) {
    try {
      XContentBuilder doc = normalizer.normalize(key);
      String keyValue = getKeyValue(key);
      if (doc != null && keyValue != null && !keyValue.isEmpty()) {
        insertDocument(doc, keyValue);
      } else {
        LOG.error("Could not normalize document {} for insert in {}",
          key, getIndexName());
      }
    } catch (Exception e) {
      LOG.error("Could not update document for index {}: {}",
        getIndexName(), e.getMessage());
    }
  }

  private void updateDocument(XContentBuilder doc, String key) throws ExecutionException, InterruptedException {
    LOG.debug("UPDATE _id:{} in index {}", key, this.getIndexName());
    getClient().update(
      new UpdateRequest(this.getIndexName(),
        this.getType(), key)
        //.refresh(true)
        .doc(doc)
    ).get();
  }

  @Override
  public void update(Object obj) throws InvalidIndexActionException {
    if(Dto.class.isAssignableFrom(obj.getClass())){
      this.updateByDto((E) obj);
    } else if(Serializable.class.isAssignableFrom(obj.getClass())) {
      this.updateByKey((K) obj);
    } else {
      throw new InvalidIndexActionException("Index " + this.getIndexName() +
        " cannot execute UPDATE for class: " + obj.getClass());
    }
  }

  @Override
  public void updateByDto(E item) {
    try {
      XContentBuilder doc = normalizer.normalize(item);
      String keyValue = getKeyValue(item.getKey());
      this.updateDocument(doc, keyValue);
    } catch (Exception e) {
      LOG.error("Could not update document for index {}: {}",
        this.getIndexName(), e.getMessage());
    }
  }

  @Override
  public void updateByKey(K key) {
    try {
      XContentBuilder doc = normalizer.normalize(key);
      String keyValue = getKeyValue(key);
      this.updateDocument(doc, keyValue);
    } catch (Exception e) {
      LOG.error("Could not update document for index {}: {}",
        this.getIndexName(), e.getMessage());
    }
  }

  private void deleteDocument(String key) throws ExecutionException, InterruptedException {
    LOG.debug("DELETE _id:{} in index {}", key, this.getIndexName());
    getClient()
      .prepareDelete(this.getIndexName(), this.getType(), key)
      .get();
  }

  @Override
  public void delete(Object obj) throws InvalidIndexActionException {
    if(Dto.class.isAssignableFrom(obj.getClass())){
      this.deleteByDto((E) obj);
    } else if(Serializable.class.isAssignableFrom(obj.getClass())) {
      this.deleteByKey((K) obj);
    } else {
      throw new InvalidIndexActionException("Index " + this.getIndexName() +
        " cannot execute DELETE for class: " + obj.getClass());
    }
  }

  @Override
  public void deleteByKey(K key) {
    try {
      this.deleteDocument(this.getKeyValue(key));
    } catch (Exception e) {
      LOG.error("Could not DELETE _id:{} for index {}: {}",
        this.getKeyValue(key), this.getIndexName(), e.getMessage());    }
  }

  @Override
  public void deleteByDto(E item) {
    try {
      this.deleteDocument(this.getKeyValue(item.getKey()));
    } catch (Exception e) {
      LOG.error("Could not DELETE _id:{} for index {}: {}",
        this.getKeyValue(item.getKey()), this.getIndexName(), e.getMessage());    }
  }

  /* Synchronization methods */

  Long lastSynch = 0l;
  long cooldown = 30000;

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
    // TODO need to read that in the admin index;
    return 0l;
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
