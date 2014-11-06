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

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
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
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.explain.ExplainRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.mlt.MoreLikeThisRequestBuilder;
import org.elasticsearch.action.percolate.MultiPercolateRequestBuilder;
import org.elasticsearch.action.percolate.PercolateRequestBuilder;
import org.elasticsearch.action.search.ClearScrollRequestBuilder;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.termvector.MultiTermVectorsRequestBuilder;
import org.elasticsearch.action.termvector.TermVectorRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.process.LoopbackAddress;
import org.sonar.process.ProcessConstants;
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
 * ElasticSearch Node used to connect to index.
 */
public class SearchClient extends TransportClient implements Startable {

  private final Profiling profiling;

  public SearchClient(Settings settings) {
    this(settings, new Profiling(settings));
  }

  @VisibleForTesting
  public SearchClient(Settings settings, Profiling profiling) {
    super(ImmutableSettings.settingsBuilder()
      .put("node.name", StringUtils.defaultIfEmpty(settings.getString(ProcessConstants.CLUSTER_NODE_NAME), "sq_local_client"))
      .put("network.bind_host", "localhost")
      .put("node.rack_id", StringUtils.defaultIfEmpty(settings.getString(ProcessConstants.CLUSTER_NODE_NAME), "unknown"))
      .put("cluster.name", StringUtils.defaultIfBlank(settings.getString(ProcessConstants.CLUSTER_NAME), "sonarqube"))
      .build());
    initLogging();
    this.addTransportAddress(new InetSocketTransportAddress(LoopbackAddress.get().getHostAddress(),
      settings.getInt(ProcessConstants.SEARCH_PORT)));
    this.profiling = profiling;
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

  private void initLogging() {
    ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
  }

  public RefreshRequestBuilder prepareRefresh(String... indices) {
    return new ProxyRefreshRequestBuilder(this, profiling).setIndices(indices);
  }

  public FlushRequestBuilder prepareFlush(String... indices) {
    return new ProxyFlushRequestBuilder(this, profiling).setIndices(indices);
  }

  public IndicesStatsRequestBuilder prepareStats(String... indices) {
    return new ProxyIndicesStatsRequestBuilder(this, profiling).setIndices(indices);
  }

  public NodesStatsRequestBuilder prepareNodesStats(String... nodesIds) {
    return new ProxyNodesStatsRequestBuilder(this, profiling).setNodesIds(nodesIds);
  }

  public ClusterStatsRequestBuilder prepareClusterStats() {
    return new ProxyClusterStatsRequestBuilder(this, profiling);
  }

  public ClusterStateRequestBuilder prepareState() {
    return new ProxyClusterStateRequestBuilder(this, profiling);
  }

  public ClusterHealthRequestBuilder prepareHealth(String... indices) {
    return new ProxyClusterHealthRequestBuilder(this, profiling).setIndices(indices);
  }

  public IndicesExistsRequestBuilder prepareExists(String... indices) {
    return new ProxyIndicesExistsRequestBuilder(this, profiling, indices);
  }

  public CreateIndexRequestBuilder prepareCreate(String index) {
    return new ProxyCreateIndexRequestBuilder(this, profiling, index);
  }

  public PutMappingRequestBuilder preparePutMapping(String... indices) {
    return new ProxyPutMappingRequestBuilder(this, profiling).setIndices(indices);
  }

  @Override
  public SearchRequestBuilder prepareSearch(String... indices) {
    return new ProxySearchRequestBuilder(this, profiling).setIndices(indices);
  }

  @Override
  public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
    return new ProxySearchScrollRequestBuilder(scrollId, this, profiling);
  }

  @Override
  public GetRequestBuilder prepareGet() {
    return new ProxyGetRequestBuilder(this, profiling);
  }

  @Override
  public GetRequestBuilder prepareGet(String index, String type, String id) {
    return new ProxyGetRequestBuilder(this, profiling).setIndex(index).setType(type).setId(id);
  }

  @Override
  public MultiGetRequestBuilder prepareMultiGet() {
    return new ProxyMultiGetRequestBuilder(this, profiling);
  }

  @Override
  public CountRequestBuilder prepareCount(String... indices) {
    return new ProxyCountRequestBuilder(this, profiling).setIndices(indices);
  }

  @Override
  public BulkRequestBuilder prepareBulk() {
    return new ProxyBulkRequestBuilder(this, profiling);
  }

  @Override
  public DeleteByQueryRequestBuilder prepareDeleteByQuery(String... indices) {
    return new ProxyDeleteByQueryRequestBuilder(this, profiling).setIndices(indices);
  }

  // ****************************************************************************************************************
  // Not yet implemented methods
  // ****************************************************************************************************************

  @Override
  public MultiSearchRequestBuilder prepareMultiSearch() {
    throw throwNotYetImplemented();
  }

  @Override
  public IndexRequestBuilder prepareIndex() {
    throw throwNotYetImplemented();
  }

  @Override
  public IndexRequestBuilder prepareIndex(String index, String type) {
    throw throwNotYetImplemented();
  }

  @Override
  public IndexRequestBuilder prepareIndex(String index, String type, @Nullable String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public UpdateRequestBuilder prepareUpdate() {
    throw throwNotYetImplemented();
  }

  @Override
  public UpdateRequestBuilder prepareUpdate(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public DeleteRequestBuilder prepareDelete() {
    throw throwNotYetImplemented();
  }

  @Override
  public DeleteRequestBuilder prepareDelete(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public PercolateRequestBuilder preparePercolate() {
    throw throwNotYetImplemented();
  }

  @Override
  public MultiPercolateRequestBuilder prepareMultiPercolate() {
    throw throwNotYetImplemented();
  }

  @Override
  public SuggestRequestBuilder prepareSuggest(String... indices) {
    throw throwNotYetImplemented();
  }

  @Override
  public MoreLikeThisRequestBuilder prepareMoreLikeThis(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public TermVectorRequestBuilder prepareTermVector(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public MultiTermVectorsRequestBuilder prepareMultiTermVectors() {
    throw throwNotYetImplemented();
  }

  @Override
  public ExplainRequestBuilder prepareExplain(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public ClearScrollRequestBuilder prepareClearScroll() {
    throw throwNotYetImplemented();
  }

  private static IllegalStateException throwNotYetImplemented() {
    return new IllegalStateException("Not yet implemented");
  }

  @Override
  public void start() {
    // nothing to do
  }

  @Override
  public void stop() {
    close();
  }
}
