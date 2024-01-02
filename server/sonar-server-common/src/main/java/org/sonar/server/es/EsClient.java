/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Cancellable;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Priority;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.es.response.ClusterStatsResponse;
import org.sonar.server.es.response.IndicesStatsResponse;
import org.sonar.server.es.response.NodeStatsResponse;

import static org.sonar.server.es.EsRequestDetails.computeDetailsAsString;

/**
 * Wrapper to connect to Elasticsearch node. Handles correctly errors (logging + exceptions
 * with context) and profiling of requests.
 */
public class EsClient implements Closeable {
  public static final Logger LOGGER = Loggers.get("es");
  private static final String ES_USERNAME = "elastic";
  private final RestHighLevelClient restHighLevelClient;
  private final Gson gson;

  public EsClient(HttpHost... hosts) {
    this(new MinimalRestHighLevelClient(null, hosts));
  }

  public EsClient(@Nullable String searchPassword, HttpHost... hosts) {
    this(new MinimalRestHighLevelClient(searchPassword, hosts));
  }

  EsClient(RestHighLevelClient restHighLevelClient) {
    this.restHighLevelClient = restHighLevelClient;
    this.gson = new GsonBuilder().create();
  }

  public BulkResponse bulk(BulkRequest bulkRequest) {
    return execute(() -> restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT));
  }

  public Cancellable bulkAsync(BulkRequest bulkRequest, ActionListener<BulkResponse> listener) {
    return restHighLevelClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, listener);
  }

  public static SearchRequest prepareSearch(String indexName) {
    return Requests.searchRequest(indexName);
  }

  public static SearchRequest prepareSearch(IndexType.IndexMainType mainType) {
    return Requests.searchRequest(mainType.getIndex().getName()).types(mainType.getType());
  }

  public static SearchRequest prepareSearch(String index, String type) {
    return Requests.searchRequest(index).types(type);
  }

  public SearchResponse search(SearchRequest searchRequest) {
    return execute(() -> restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(searchRequest));
  }

  public SearchResponse scroll(SearchScrollRequest searchScrollRequest) {
    return execute(() -> restHighLevelClient.scroll(searchScrollRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(searchScrollRequest));
  }

  public ClearScrollResponse clearScroll(ClearScrollRequest clearScrollRequest) {
    return execute(() -> restHighLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT));
  }

  public DeleteResponse delete(DeleteRequest deleteRequest) {
    return execute(() -> restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(deleteRequest));
  }

  public RefreshResponse refresh(Index... indices) {
    RefreshRequest refreshRequest = new RefreshRequest()
      .indices(Arrays.stream(indices).map(Index::getName).toArray(String[]::new));
    return execute(() -> restHighLevelClient.indices().refresh(refreshRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(refreshRequest));
  }

  public ForceMergeResponse forcemerge(ForceMergeRequest forceMergeRequest) {
    return execute(() -> restHighLevelClient.indices().forcemerge(forceMergeRequest, RequestOptions.DEFAULT));
  }

  public AcknowledgedResponse putSettings(UpdateSettingsRequest req) {
    return execute(() -> restHighLevelClient.indices().putSettings(req, RequestOptions.DEFAULT));
  }

  public ClearIndicesCacheResponse clearCache(ClearIndicesCacheRequest request) {
    return execute(() -> restHighLevelClient.indices().clearCache(request, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(request));
  }

  public IndexResponse index(IndexRequest indexRequest) {
    return execute(() -> restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(indexRequest));
  }

  public GetResponse get(GetRequest request) {
    return execute(() -> restHighLevelClient.get(request, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(request));
  }

  public GetIndexResponse getIndex(GetIndexRequest getRequest) {
    return execute(() -> restHighLevelClient.indices().get(getRequest, RequestOptions.DEFAULT));
  }

  public boolean indexExists(GetIndexRequest getIndexRequest) {
    return execute(() -> restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(getIndexRequest));
  }

  public CreateIndexResponse create(CreateIndexRequest createIndexRequest) {
    return execute(() -> restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(createIndexRequest));
  }

  public AcknowledgedResponse deleteIndex(DeleteIndexRequest deleteIndexRequest) {
    return execute(() -> restHighLevelClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT));
  }

  public AcknowledgedResponse putMapping(PutMappingRequest request) {
    return execute(() -> restHighLevelClient.indices().putMapping(request, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(request));
  }

  public ClusterHealthResponse clusterHealth(ClusterHealthRequest clusterHealthRequest) {
    return execute(() -> restHighLevelClient.cluster().health(clusterHealthRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(clusterHealthRequest));
  }

  public void waitForStatus(ClusterHealthStatus clusterHealthStatus) {
    clusterHealth(new ClusterHealthRequest().waitForEvents(Priority.LANGUID).waitForStatus(clusterHealthStatus));
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-nodes-stats.html
  public NodeStatsResponse nodesStats() {
    return execute(() -> {
      Request request = new Request("GET", "/_nodes/stats/fs,process,jvm,indices,breaker");
      Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
      return NodeStatsResponse.toNodeStatsResponse(gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class));
    });
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-stats.html
  public IndicesStatsResponse indicesStats(String... indices) {
    return execute(() -> {
      Request request = new Request("GET", "/" + (indices.length > 0 ? (String.join(",", indices) + "/") : "") + "_stats");
      request.addParameter("level", "shards");
      Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
      return IndicesStatsResponse.toIndicesStatsResponse(gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class));
    }, () -> computeDetailsAsString(indices));
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-stats.html
  public ClusterStatsResponse clusterStats() {
    return execute(() -> {
      Request request = new Request("GET", "/_cluster/stats");
      Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
      return ClusterStatsResponse.toClusterStatsResponse(gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class));
    });
  }

  public GetSettingsResponse getSettings(GetSettingsRequest getSettingsRequest) {
    return execute(() -> restHighLevelClient.indices().getSettings(getSettingsRequest, RequestOptions.DEFAULT));
  }

  public GetMappingsResponse getMapping(GetMappingsRequest getMappingsRequest) {
    return execute(() -> restHighLevelClient.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT));
  }

  @Override
  public void close() {
    try {
      restHighLevelClient.close();
    } catch (IOException e) {
      throw new ElasticsearchException("Could not close ES Rest high level client", e);
    }
  }

  /**
   * Internal usage only
   *
   * @return native ES client object
   */
  RestHighLevelClient nativeClient() {
    return restHighLevelClient;
  }

  static class MinimalRestHighLevelClient extends RestHighLevelClient {
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 60000;

    public MinimalRestHighLevelClient(@Nullable String searchPassword, HttpHost... hosts) {
      super(buildHttpClient(searchPassword, hosts));
    }

    MinimalRestHighLevelClient(RestClient restClient) {
      super(restClient, RestClient::close, Lists.newArrayList());
    }

    @NotNull
    private static RestClientBuilder buildHttpClient(@Nullable String searchPassword,
      HttpHost[] hosts) {
      return RestClient.builder(hosts)
        .setRequestConfigCallback(r -> r
          .setConnectTimeout(CONNECT_TIMEOUT)
          .setSocketTimeout(SOCKET_TIMEOUT))
        .setHttpClientConfigCallback(httpClientBuilder -> {
          if (searchPassword != null) {
            BasicCredentialsProvider provider = getBasicCredentialsProvider(searchPassword);
            httpClientBuilder.setDefaultCredentialsProvider(provider);
          }
          return httpClientBuilder;
        });
    }

    private static BasicCredentialsProvider getBasicCredentialsProvider(String searchPassword) {
      BasicCredentialsProvider provider = new BasicCredentialsProvider();
      provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(ES_USERNAME, searchPassword));
      return provider;
    }

  }

  <R> R execute(EsRequestExecutor<R> executor) {
    return execute(executor, () -> "");
  }

  <R> R execute(EsRequestExecutor<R> executor, Supplier<String> requestDetails) {
    Profiler profiler = Profiler.createIfTrace(EsClient.LOGGER).start();
    try {
      return executor.execute();
    } catch (Exception e) {
      throw new ElasticsearchException("Fail to execute es request" + requestDetails.get(), e);
    } finally {
      if (profiler.isTraceEnabled()) {
        profiler.stopTrace(requestDetails.get());
      }
    }
  }

  @FunctionalInterface
  interface EsRequestExecutor<R> {
    R execute() throws IOException;
  }

}
