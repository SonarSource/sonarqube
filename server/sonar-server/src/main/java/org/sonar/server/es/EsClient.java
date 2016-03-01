/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequestBuilder;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.flush.FlushRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.LoopbackAddress;
import org.sonar.process.ProcessProperties;
import org.sonar.server.es.request.ProxyBulkRequestBuilder;
import org.sonar.server.es.request.ProxyClearCacheRequestBuilder;
import org.sonar.server.es.request.ProxyClusterHealthRequestBuilder;
import org.sonar.server.es.request.ProxyClusterStateRequestBuilder;
import org.sonar.server.es.request.ProxyClusterStatsRequestBuilder;
import org.sonar.server.es.request.ProxyCountRequestBuilder;
import org.sonar.server.es.request.ProxyCreateIndexRequestBuilder;
import org.sonar.server.es.request.ProxyDeleteByQueryRequestBuilder;
import org.sonar.server.es.request.ProxyDeleteRequestBuilder;
import org.sonar.server.es.request.ProxyFlushRequestBuilder;
import org.sonar.server.es.request.ProxyGetRequestBuilder;
import org.sonar.server.es.request.ProxyIndexRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesExistsRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesStatsRequestBuilder;
import org.sonar.server.es.request.ProxyMultiGetRequestBuilder;
import org.sonar.server.es.request.ProxyNodesStatsRequestBuilder;
import org.sonar.server.es.request.ProxyPutMappingRequestBuilder;
import org.sonar.server.es.request.ProxyRefreshRequestBuilder;
import org.sonar.server.es.request.ProxySearchRequestBuilder;
import org.sonar.server.es.request.ProxySearchScrollRequestBuilder;

/**
 * Facade to connect to Elasticsearch node. Handles correctly errors (logging + exceptions
 * with context) and profiling of requests.
 */
public class EsClient implements Startable {

  public static final Logger LOGGER = Loggers.get("es");
  private final Settings settings;
  private Client nativeClient = null;

  public EsClient(Settings settings) {
    this.settings = settings;
  }

  EsClient(Settings settings, Client nativeClient) {
    this.settings = settings;
    this.nativeClient = nativeClient;
  }

  public RefreshRequestBuilder prepareRefresh(String... indices) {
    return new ProxyRefreshRequestBuilder(nativeClient()).setIndices(indices);
  }

  public FlushRequestBuilder prepareFlush(String... indices) {
    return new ProxyFlushRequestBuilder(nativeClient()).setIndices(indices);
  }

  public IndicesStatsRequestBuilder prepareStats(String... indices) {
    return new ProxyIndicesStatsRequestBuilder(nativeClient()).setIndices(indices);
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

  public ClusterHealthRequestBuilder prepareHealth(String... indices) {
    return new ProxyClusterHealthRequestBuilder(nativeClient()).setIndices(indices);
  }

  public void waitForStatus(ClusterHealthStatus status) {
    prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForStatus(status).get();
  }

  public IndicesExistsRequestBuilder prepareIndicesExist(String... indices) {
    return new ProxyIndicesExistsRequestBuilder(nativeClient(), indices);
  }

  public CreateIndexRequestBuilder prepareCreate(String index) {
    return new ProxyCreateIndexRequestBuilder(nativeClient(), index);
  }

  public PutMappingRequestBuilder preparePutMapping(String... indices) {
    return new ProxyPutMappingRequestBuilder(nativeClient()).setIndices(indices);
  }

  public SearchRequestBuilder prepareSearch(String... indices) {
    return new ProxySearchRequestBuilder(nativeClient()).setIndices(indices);
  }

  public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
    return new ProxySearchScrollRequestBuilder(scrollId, nativeClient());
  }

  public GetRequestBuilder prepareGet() {
    return new ProxyGetRequestBuilder(nativeClient());
  }

  public GetRequestBuilder prepareGet(String index, String type, String id) {
    return new ProxyGetRequestBuilder(nativeClient()).setIndex(index).setType(type).setId(id);
  }

  public MultiGetRequestBuilder prepareMultiGet() {
    return new ProxyMultiGetRequestBuilder(nativeClient());
  }

  public CountRequestBuilder prepareCount(String... indices) {
    return new ProxyCountRequestBuilder(nativeClient()).setIndices(indices);
  }

  public BulkRequestBuilder prepareBulk() {
    return new ProxyBulkRequestBuilder(nativeClient());
  }

  public DeleteRequestBuilder prepareDelete(String index, String type, String id) {
    return new ProxyDeleteRequestBuilder(nativeClient(), index).setType(type).setId(id);
  }

  /**
   * @deprecated delete-by-query is dropped from ES 2.0 and should not be used. See
   * https://www.elastic.co/guide/en/elasticsearch/reference/1.7/docs-delete-by-query.html
   */
  @Deprecated
  public DeleteByQueryRequestBuilder prepareDeleteByQuery(String... indices) {
    return new ProxyDeleteByQueryRequestBuilder(nativeClient()).setIndices(indices);
  }

  public IndexRequestBuilder prepareIndex(String index, String type) {
    return new ProxyIndexRequestBuilder(nativeClient()).setIndex(index).setType(type);
  }

  public OptimizeRequestBuilder prepareOptimize(String indexName) {
    // TODO add proxy for profiling
    return nativeClient().admin().indices().prepareOptimize(indexName)
      .setMaxNumSegments(1);
  }

  public ClearIndicesCacheRequestBuilder prepareClearCache(String... indices) {
    return new ProxyClearCacheRequestBuilder(nativeClient()).setIndices(indices);
  }

  public long getMaxFieldValue(String indexName, String typeName, String fieldName) {
    SearchRequestBuilder request = prepareSearch(indexName)
      .setTypes(typeName)
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(0)
      .addAggregation(AggregationBuilders.max("latest").field(fieldName));

    Max max = request.get().getAggregations().get("latest");
    return (long) max.getValue();
  }

  @Override
  public void start() {
    if (nativeClient == null) {
      ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
      org.elasticsearch.common.settings.Settings esSettings = ImmutableSettings.settingsBuilder()
        .put("node.name", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.CLUSTER_NODE_NAME), "sq_local_client"))
        .put("network.bind_host", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.SEARCH_HOST), "localhost"))
        .put("node.rack_id", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.CLUSTER_NODE_NAME), "unknown"))
        .put("cluster.name", StringUtils.defaultIfBlank(settings.getString(ProcessProperties.CLUSTER_NAME), "sonarqube"))
        .build();
      nativeClient = new TransportClient(esSettings);
      ((TransportClient) nativeClient).addTransportAddress(new InetSocketTransportAddress(StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.SEARCH_HOST),
        LoopbackAddress.get()
          .getHostAddress()),
        settings.getInt(ProcessProperties.SEARCH_PORT)));
    }
  }

  @Override
  public void stop() {
    if (nativeClient != null) {
      nativeClient.close();
    }
  }

  protected Client nativeClient() {
    return nativeClient;
  }
}
