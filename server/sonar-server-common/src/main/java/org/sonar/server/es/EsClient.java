/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.es;

import java.io.Closeable;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequestBuilder;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Priority;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.request.ProxyClearCacheRequestBuilder;
import org.sonar.server.es.request.ProxyClusterHealthRequestBuilder;
import org.sonar.server.es.request.ProxyClusterStateRequestBuilder;
import org.sonar.server.es.request.ProxyClusterStatsRequestBuilder;
import org.sonar.server.es.request.ProxyCreateIndexRequestBuilder;
import org.sonar.server.es.request.ProxyDeleteRequestBuilder;
import org.sonar.server.es.request.ProxyGetRequestBuilder;
import org.sonar.server.es.request.ProxyIndexRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesExistsRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesStatsRequestBuilder;
import org.sonar.server.es.request.ProxyNodesStatsRequestBuilder;
import org.sonar.server.es.request.ProxyPutMappingRequestBuilder;
import org.sonar.server.es.request.ProxyRefreshRequestBuilder;
import org.sonar.server.es.request.ProxySearchRequestBuilder;
import org.sonar.server.es.request.ProxySearchScrollRequestBuilder;

import static java.util.Objects.requireNonNull;

/**
 * Facade to connect to Elasticsearch node. Handles correctly errors (logging + exceptions
 * with context) and profiling of requests.
 */
public class EsClient implements Closeable {

  public static final Logger LOGGER = Loggers.get("es");

  private final Client nativeClient;

  public EsClient(Client nativeClient) {
    this.nativeClient = requireNonNull(nativeClient);
  }

  public EsClient() {
    this.nativeClient = null;
  }

  public RefreshRequestBuilder prepareRefresh(Index index) {
    return new ProxyRefreshRequestBuilder(nativeClient()).setIndices(index.getName());
  }

  public IndicesStatsRequestBuilder prepareStats() {
    return new ProxyIndicesStatsRequestBuilder(nativeClient());
  }

  public IndicesStatsRequestBuilder prepareStats(Index index) {
    return new ProxyIndicesStatsRequestBuilder(nativeClient()).setIndices(index.getName());
  }

  public NodesStatsRequestBuilder prepareNodesStats(String... nodesIds) {
    return new ProxyNodesStatsRequestBuilder(nativeClient()).setNodesIds(nodesIds);
  }

  public ClusterStatsRequestBuilder prepareClusterStats() {
    return new ProxyClusterStatsRequestBuilder(nativeClient());
  }

  public ClusterStateRequestBuilder prepareState() {
    return new ProxyClusterStateRequestBuilder(nativeClient());
  }

  public ClusterHealthRequestBuilder prepareHealth() {
    return new ProxyClusterHealthRequestBuilder(nativeClient());
  }

  public void waitForStatus(ClusterHealthStatus status) {
    prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForStatus(status).get();
  }

  public IndicesExistsRequestBuilder prepareIndicesExist(Index index) {
    return new ProxyIndicesExistsRequestBuilder(nativeClient(), index.getName());
  }

  public CreateIndexRequestBuilder prepareCreate(Index index) {
    return new ProxyCreateIndexRequestBuilder(nativeClient(), index.getName());
  }

  public PutMappingRequestBuilder preparePutMapping(Index index) {
    return new ProxyPutMappingRequestBuilder(nativeClient()).setIndices(index.getName());
  }

  public SearchRequestBuilder prepareSearch(Index index) {
    return new ProxySearchRequestBuilder(nativeClient()).setIndices(index.getName());
  }

  public SearchRequestBuilder prepareSearch(IndexMainType indexType) {
    return new ProxySearchRequestBuilder(nativeClient())
      .setIndices(indexType.getIndex().getName())
      .setTypes(indexType.getType());
  }

  public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
    return new ProxySearchScrollRequestBuilder(scrollId, nativeClient());
  }

  public GetRequestBuilder prepareGet(IndexType indexType, String id) {
    IndexMainType mainType = indexType.getMainType();
    return new ProxyGetRequestBuilder(nativeClient()).setIndex(mainType.getIndex().getName()).setType(mainType.getType()).setId(id);
  }

  public DeleteRequestBuilder prepareDelete(IndexType indexType, String id) {
    IndexMainType mainType = indexType.getMainType();
    return new ProxyDeleteRequestBuilder(nativeClient(), mainType.getIndex().getName()).setType(mainType.getType()).setId(id);
  }

  DeleteRequestBuilder prepareDelete(String index, String type, String id) {
    return new ProxyDeleteRequestBuilder(nativeClient(), index).setType(type).setId(id);
  }

  public IndexRequestBuilder prepareIndex(IndexType indexType) {
    IndexMainType mainType = indexType.getMainType();
    return new ProxyIndexRequestBuilder(nativeClient()).setIndex(mainType.getIndex().getName()).setType(mainType.getType());
  }

  public ForceMergeRequestBuilder prepareForceMerge(String indexName) {
    // TODO add proxy for profiling
    return nativeClient().admin().indices().prepareForceMerge(indexName)
      .setMaxNumSegments(1);
  }

  public ClearIndicesCacheRequestBuilder prepareClearCache(String... indices) {
    return new ProxyClearCacheRequestBuilder(nativeClient()).setIndices(indices);
  }

  public Client nativeClient() {
    return nativeClient;
  }

  @Override
  public void close() {
    nativeClient.close();
  }
}
