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
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.db.Dao;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;

import java.io.Serializable;
import java.util.Collection;

public abstract class BaseIndex<K extends Serializable> implements Index<K> {

  private static final String PROFILE_DOMAIN = "es";
  private static final Logger LOG = LoggerFactory.getLogger(BaseIndex.class);

  public static final String ES_CLUSTER_NAME = "sonarcluster";

  private static final String LOCAL_ES_NODE_HOST = "localhost";
  private static final int LOCAL_ES_NODE_PORT = 9300;

  private final Profiling profiling;
  private Client client;
  private WorkQueue workQueue;
  private IndexSynchronizer<K> synchronizer;
  protected Dao<?,K> dao;

  public BaseIndex(WorkQueue workQueue, Dao<?,K> dao, Profiling profiling) {
    this.profiling = profiling;
    this.workQueue = workQueue;
    this.synchronizer = IndexSynchronizer.getOnetimeSynchronizer(this, this.workQueue);
    this.dao = dao;
  }

  protected Dao<?,K> getDao(){
    return this.dao;
  }

  protected Client getClient(){
    return this.client;
  }

  /* Component Methods */

  @Override
  public void start() {

    /* Connect to the local ES Cluster */
    this.connect();

    /* Setup the index if necessary */
    this.intializeIndex();

    /* Launch synchronization */
    synchronizer.start();
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

  public void connect(){
    /* Settings to access our local ES node */
    Settings settings = ImmutableSettings.settingsBuilder()
      .put("client.transport.sniff", true)
      .put("cluster.name", ES_CLUSTER_NAME)
      .put("node.name", "localclient_")
      .build();

    this.client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress(LOCAL_ES_NODE_HOST, LOCAL_ES_NODE_PORT));

    /*
     * Cannot do that yet, need version >= 1.0
     * ImmutableList<DiscoveryNode> nodes = client.connectedNodes();
     * if (nodes.isEmpty()) {
     * throw new ElasticSearchUnavailableException("No nodes available. Verify ES is running!");
     * } else {
     * log.info("connected to nodes: " + nodes.toString());
     * }
     */
  }

  /* Cluster And ES Stats/Client methods */

  private void intializeIndex() {

    String index = this.getIndexName();

    IndicesExistsResponse indexExistsResponse = client.admin().indices()
      .prepareExists(index).execute().actionGet();

    if (!indexExistsResponse.isExists()) {

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


  /* Index management and Tx methods */

  protected abstract Settings getIndexSettings();

  protected abstract String getType();

  protected abstract XContentBuilder getMapping();

  public abstract Collection<K> synchronizeSince(Long date);


  /* Base CRUD methods */

  protected abstract QueryBuilder getKeyQuery(K key);

  @Override
  public Hit getByKey(K key) {
    getClient().prepareSearch(this.getIndexName())
      .setQuery(getKeyQuery(key))
      .get();
    return null;
  }

  @Override
  public void insert(K key) {
    this.update(key);
  }

  @Override
  public void update(K key) {
    IndexResponse result = getClient().index(new IndexRequest()
      .type(this.getType())
      .index(this.getIndexName())
      .source(this.normalize(key))).actionGet();

  }

  @Override
  public void delete(K key) {
    DeleteByQueryResponse result = getClient().prepareDeleteByQuery(this.getIndexName())
      .setQuery(getKeyQuery(key)).get();
  }

  /* Synchronization methods */

  Long lastSynch = 0l;
  long cooldown = 30000;

  @Override
  public void setLastSynchronization(Long time) {
    if(time > (getLastSynchronization() + cooldown)){
      LOG.trace("Updating synchTime updating");
      lastSynch = time;
    } else {
      LOG.trace("Not updating synchTime, still cooling down");
    }

  }

  @Override
  public Long getLastSynchronization() {
    // need to read that in the admin index;
    return 0l;
  }
}
