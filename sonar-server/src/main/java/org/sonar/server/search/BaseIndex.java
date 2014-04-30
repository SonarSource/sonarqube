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

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsNodes;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
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
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;
import org.sonar.server.es.ESNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

public abstract class BaseIndex<K extends Serializable, E extends Dto<K>> implements Index<K> {

  private static final String PROFILE_DOMAIN = "es";

  private static final Logger LOG = LoggerFactory.getLogger(BaseIndex.class);

  public static final String ES_CLUSTER_NAME = "sonarqube";

  private static final String LOCAL_ES_NODE_HOST = "localhost";
  private static final int LOCAL_ES_NODE_PORT = 9300;

  private final Profiling profiling;
  private Client client;
  private final ESNode node;
  protected BaseNormalizer<E, K> normalizer;

  public BaseIndex(BaseNormalizer<E, K> normalizer, WorkQueue workQueue,
    Profiling profiling, ESNode node) {
    this.normalizer = normalizer;
    this.profiling = profiling;
    this.node = node;
  }

  protected Client getClient() {
    return this.client;
  }

  /* Component Methods */

  @Override
  public void start() {

    /* Connect to the local ES Cluster */
    this.connect();

    /* Setup the index if necessary */
    this.intializeIndex();
  }

  @Override
  public void stop() {
    if (client != null) {
      client.close();
    }
  }

  private StopWatch createWatch() {
    return profiling.start(PROFILE_DOMAIN, Level.FULL);
  }

  public void connect() {
    this.client = this.node.client();
  }

  /* Cluster And ES Stats/Client methods */

  private void intializeIndex() {

    String index = this.getIndexName();

    IndicesExistsResponse indexExistsResponse = client.admin().indices()
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

      client.admin().indices().prepareCreate(index)
        .setSettings(getIndexSettings())
        .addMapping(getType(), getMapping())
        .execute().actionGet();
    }
  }

  public ClusterStatsNodes getNodesStats() {
    StopWatch watch = createWatch();
    try {
      return client.admin().cluster().prepareClusterStats().get().getNodesStats();
    } finally {
      watch.stop("ping from transport client");
    }
  }

  /* Index management methods */

  protected abstract XContentBuilder getIndexSettings();

  protected abstract String getType();

  protected abstract XContentBuilder getMapping();

  /* Base CRUD methods */

  protected abstract String getKeyValue(K key);

  @Override
  public Hit getByKey(K key) {
    GetResponse result = getClient().prepareGet(this.getIndexName(), this.getType(), this.getKeyValue(key))
      .get();
    return Hit.fromMap(0, result.getSourceAsMap());
  }

  @Override
  public void insert(K key) {
    try {
      XContentBuilder doc = normalizer.normalize(key);
      String keyValue = this.getKeyValue(key);
      if (doc != null && keyValue != null && !keyValue.isEmpty()) {
        LOG.debug("Update document with key {}", key);
        IndexResponse result = getClient().index(
          new IndexRequest(this.getIndexName(),
            this.getType(), keyValue)
            .refresh(true)
            .source(doc)).get();
      } else {
        LOG.error("Could not normalize document {} for insert in ",
          key, this.getIndexName());
      }
    } catch (Exception e) {
      LOG.error("Could not update documet for index {}: {}",
        this.getIndexName(), e.getMessage());
    }
  }

  @Override
  public void update(K key) {
    try {
      LOG.info("Update document with key {}", key);
      XContentBuilder doc = normalizer.normalize(key);
      UpdateResponse result = getClient().update(
        new UpdateRequest(this.getIndexName(),
          this.getType(), this.getKeyValue(key))
                .refresh(true)
          .doc(doc)).get();
    } catch (Exception e) {
      LOG.error("Could not update documet for index {}: {}",
        this.getIndexName(), e.getMessage());
    }
  }

  @Override
  public void delete(K key) {
    LOG.info("Deleting document with key {}", key);
    DeleteResponse result = getClient()
      .prepareDelete(this.getIndexName(), this.getType(), this.getKeyValue(key))
      .get();
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
