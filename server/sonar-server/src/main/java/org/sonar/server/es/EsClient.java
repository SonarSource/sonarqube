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
package org.sonar.server.es;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequestBuilder;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.flush.FlushRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.picocontainer.Startable;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.ClusterHealth;
import org.sonar.server.search.SearchClient;
import org.sonar.server.search.request.ProxyBulkRequestBuilder;
import org.sonar.server.search.request.ProxyClusterHealthRequestBuilder;
import org.sonar.server.search.request.ProxyClusterStateRequestBuilder;
import org.sonar.server.search.request.ProxyClusterStatsRequestBuilder;
import org.sonar.server.search.request.ProxyCountRequestBuilder;
import org.sonar.server.search.request.ProxyCreateIndexRequestBuilder;
import org.sonar.server.search.request.ProxyDeleteByQueryRequestBuilder;
import org.sonar.server.search.request.ProxyFlushRequestBuilder;
import org.sonar.server.search.request.ProxyGetRequestBuilder;
import org.sonar.server.search.request.ProxyIndicesExistsRequestBuilder;
import org.sonar.server.search.request.ProxyIndicesStatsRequestBuilder;
import org.sonar.server.search.request.ProxyMultiGetRequestBuilder;
import org.sonar.server.search.request.ProxyNodesStatsRequestBuilder;
import org.sonar.server.search.request.ProxyPutMappingRequestBuilder;
import org.sonar.server.search.request.ProxyRefreshRequestBuilder;
import org.sonar.server.search.request.ProxySearchRequestBuilder;
import org.sonar.server.search.request.ProxySearchScrollRequestBuilder;

/**
 * Facade to connect to Elasticsearch node. Handles correctly errors (logging + exceptions
 * with context) and profiling of requests.
 */
public class EsClient implements Startable {

  private final Profiling profiling;
  private final Client client;

  public EsClient(SearchClient deprecatedClient) {
    this.profiling = deprecatedClient.getProfiling();
    this.client = deprecatedClient;
  }

  EsClient(Profiling profiling, Client client) {
    this.profiling = profiling;
    this.client = client;
  }

  public ClusterHealth getClusterHealth() {
    ClusterHealth health = new ClusterHealth();
    ClusterStatsResponse clusterStatsResponse = this.prepareClusterStats().get();

    // Cluster health
    health.setClusterAvailable(clusterStatsResponse.getStatus() != ClusterHealthStatus.RED);

    // Number of nodes
    health.setNumberOfNodes(clusterStatsResponse.getNodesStats().getCounts().getTotal());

    return health;
  }


  public RefreshRequestBuilder prepareRefresh(String... indices) {
    return new ProxyRefreshRequestBuilder(client, profiling).setIndices(indices);
  }

  public FlushRequestBuilder prepareFlush(String... indices) {
    return new ProxyFlushRequestBuilder(client, profiling).setIndices(indices);
  }

  public IndicesStatsRequestBuilder prepareStats(String... indices) {
    return new ProxyIndicesStatsRequestBuilder(client, profiling).setIndices(indices);
  }

  public NodesStatsRequestBuilder prepareNodesStats(String... nodesIds) {
    return new ProxyNodesStatsRequestBuilder(client, profiling).setNodesIds(nodesIds);
  }

  public ClusterStatsRequestBuilder prepareClusterStats() {
    return new ProxyClusterStatsRequestBuilder(client, profiling);
  }

  public ClusterStateRequestBuilder prepareState() {
    return new ProxyClusterStateRequestBuilder(client, profiling);
  }

  public ClusterHealthRequestBuilder prepareHealth(String... indices) {
    return new ProxyClusterHealthRequestBuilder(client, profiling).setIndices(indices);
  }

  public IndicesExistsRequestBuilder prepareExists(String... indices) {
    return new ProxyIndicesExistsRequestBuilder(client, profiling, indices);
  }

  public CreateIndexRequestBuilder prepareCreate(String index) {
    return new ProxyCreateIndexRequestBuilder(client, profiling, index);
  }

  public PutMappingRequestBuilder preparePutMapping(String... indices) {
    return new ProxyPutMappingRequestBuilder(client, profiling).setIndices(indices);
  }

  public SearchRequestBuilder prepareSearch(String... indices) {
    return new ProxySearchRequestBuilder(client, profiling).setIndices(indices);
  }

  public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
    return new ProxySearchScrollRequestBuilder(scrollId, client, profiling);
  }

  public GetRequestBuilder prepareGet() {
    return new ProxyGetRequestBuilder(client, profiling);
  }

  public GetRequestBuilder prepareGet(String index, String type, String id) {
    return new ProxyGetRequestBuilder(client, profiling).setIndex(index).setType(type).setId(id);
  }

  public MultiGetRequestBuilder prepareMultiGet() {
    return new ProxyMultiGetRequestBuilder(client, profiling);
  }

  public CountRequestBuilder prepareCount(String... indices) {
    return new ProxyCountRequestBuilder(client, profiling).setIndices(indices);
  }

  public BulkRequestBuilder prepareBulk() {
    return new ProxyBulkRequestBuilder(client, profiling);
  }

  public DeleteByQueryRequestBuilder prepareDeleteByQuery(String... indices) {
    return new ProxyDeleteByQueryRequestBuilder(client, profiling).setIndices(indices);
  }

  public long getLastUpdatedAt(String indexName, String typeName) {
    SearchRequestBuilder request = prepareSearch(indexName)
      .setTypes(typeName)
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(0)
      .addAggregation(AggregationBuilders.max("latest").field("updatedAt"));

    Max max = request.get().getAggregations().get("latest");
    return (long) max.getValue();
  }

  @Override
  public void start() {
    // nothing to do
  }

  @Override
  public void stop() {
    // TODO re-enable when SearchClient is dropped
    //client.close();
  }

  protected Client nativeClient() {
    return client;
  }
}
