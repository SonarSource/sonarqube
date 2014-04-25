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
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;
import org.sonar.server.cluster.WorkQueue;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class BaseIndex<K extends Serializable> implements Index<K>{

  private static final String ES_EXECUTE_FAILED = "Failed execution of {}. Root is {}";

  private static final String BULK_EXECUTE_FAILED = "Execution of bulk operation failed";
  private static final String BULK_INTERRUPTED = "Interrupted during bulk operation";

  private static final String PROFILE_DOMAIN = "es";
  private static final Logger LOG = LoggerFactory.getLogger(BaseIndex.class);

  public static final String ES_CLUSTER_NAME = "sonarcluster";

  private static final String LOCAL_ES_NODE_HOST = "localhost";
  private static final int LOCAL_ES_NODE_PORT = 9300;

  private final Profiling profiling;
  private Client client;
  private WorkQueue workQueue;
  private IndexSynchronizer synchronizer;

  public BaseIndex(WorkQueue workQueue, Profiling profiling) {
    this.profiling = profiling;
    this.workQueue = workQueue;
    this.synchronizer = IndexSynchronizer.getOnetimeSynchronizer(this, this.workQueue);
  }

  @Override
  public void start() {

    /* Settings to access our local ES node */
    Settings settings = ImmutableSettings.settingsBuilder()
      .put("client.transport.sniff", true)
      .put("cluster.name", ES_CLUSTER_NAME)
      .put("node.name", "localclient_")
      .build();

    this.client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress(LOCAL_ES_NODE_HOST, LOCAL_ES_NODE_PORT));

    /* Cannot do that yet, need version >= 1.0
    ImmutableList<DiscoveryNode> nodes = client.connectedNodes();
    if (nodes.isEmpty()) {
        throw new ElasticSearchUnavailableException("No nodes available. Verify ES is running!");
    } else {
        log.info("connected to nodes: " + nodes.toString());
    }
    */

    /* Launch synchronization */
    synchronizer.start();
  }

  @Override
  public void stop() {
    if (client != null) {
      client.close();
    }
  }

  public Collection<K> synchronizeSince(Long date) {
    // TODO Auto-generated method stub
    return Collections.EMPTY_LIST;
  }

  public ClusterStatsNodes getNodesStats() {
    StopWatch watch = createWatch();
    try {
      return client.admin().cluster().prepareClusterStats().get().getNodesStats();
    } finally {
      watch.stop("ping from transport client");
    }
  }

  private StopWatch createWatch() {
    return profiling.start(PROFILE_DOMAIN, Level.FULL);
  }

  @Override
  public Hit getByKey(K key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void insert(K key) {
    // TODO Auto-generated method stub

  }

  @Override
  public void udpate(K key) {
    // TODO Auto-generated method stub

  }

  @Override
  public void delete(K key) {
    // TODO Auto-generated method stub

  }

  @Override
  public K dequeueInsert() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public K dequeueUpdate() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public K dequeueDelete() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public abstract Map<String, Object> normalize(K key);


  @Override
  public String getIndexName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setLastSynchronization(Long time) {
    // TODO Auto-generated method stub

  }

  @Override
  public Long getLastSynchronization() {
    // TODO Auto-generated method stub
    return null;
  }
}
