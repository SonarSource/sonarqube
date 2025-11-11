/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.ClearScrollRequest;
import co.elastic.clients.elasticsearch.core.ClearScrollResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.indices.ClearCacheRequest;
import co.elastic.clients.elasticsearch.indices.ClearCacheResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.ForcemergeRequest;
import co.elastic.clients.elasticsearch.indices.ForcemergeResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.util.ObjectBuilder;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.Cancellable;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Priority;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.es.response.ClusterStatsResponse;
import org.sonar.server.es.response.IndicesStatsResponse;
import org.sonar.server.es.response.NodeStatsResponse;

import static org.sonar.server.es.EsClient.MinimalRestHighLevelClient.buildHttpClient;
import static org.sonar.server.es.EsRequestDetails.computeDetailsAsString;

/**
 * Wrapper to connect to Elasticsearch node. Handles correctly errors (logging + exceptions
 * with context) and profiling of requests.
 */
public class EsClient implements Closeable {
  public static final Logger LOGGER = Loggers.get("es");
  private static final String ES_USERNAME = "elastic";

  // Old client - to be deprecated
  private final RestHighLevelClient restHighLevelClient;

  // New Java API Client
  private final ElasticsearchClient elasticsearchClient;
  private final RestClient restClient;

  private final Gson gson;

  public EsClient(HttpHost... hosts) {
    this(buildHttpClient(null, null, null, hosts).build());
  }

  public EsClient(@Nullable String searchPassword, @Nullable String keyStorePath, @Nullable String keyStorePassword, HttpHost... hosts) {
    this(buildHttpClient(searchPassword, keyStorePath, keyStorePassword, hosts).build());
  }

  EsClient(RestClient restClient) {
    //The restClient is shared by both the old and the new client, by extracting it we have one less dependency on the RestHighLevelClient
    this.restClient = restClient;
    this.restHighLevelClient = new MinimalRestHighLevelClient(restClient);

    // Create new Java API Client using the same RestClient
    RestClientTransport transport = new RestClientTransport(this.restClient, new JacksonJsonpMapper());
    this.elasticsearchClient = new ElasticsearchClient(transport);

    this.gson = new GsonBuilder().create();
  }

  /**
   * Bulk operations using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the bulk request using the new API builder
   * @return The bulk response from Elasticsearch
   */
  public co.elastic.clients.elasticsearch.core.BulkResponse bulkV2(
    Function<co.elastic.clients.elasticsearch.core.BulkRequest.Builder,
      ObjectBuilder<co.elastic.clients.elasticsearch.core.BulkRequest>> fn) {
    return execute(() -> elasticsearchClient.bulk(fn));
  }

  /**
   * @deprecated Use {@link #bulkV2(java.util.function.Function)} instead. This method uses the old Elasticsearch API.
   */
  @Deprecated(since = "2025.6", forRemoval = true)
  public Cancellable bulkAsync(BulkRequest bulkRequest, ActionListener<BulkResponse> listener) {
    return restHighLevelClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, listener);
  }

  /**
   * @deprecated Use the new API search methods directly. This method uses the old Elasticsearch API.
   */
  @Deprecated(since = "2025.6", forRemoval = true)
  public static SearchRequest prepareSearch(String indexName) {
    return Requests.searchRequest(indexName);
  }

  /**
   * @deprecated Use the new API search methods directly. This method uses the old Elasticsearch API.
   */
  @Deprecated(since = "2025.6", forRemoval = true)
  public static SearchRequest prepareSearch(IndexType.IndexMainType mainType) {
    return Requests.searchRequest(mainType.getIndex().getName());
  }

  /**
   * @deprecated Use {@link #searchV2(java.util.function.Function, Class)} instead. This method uses the old Elasticsearch API.
   */
  @Deprecated(since = "2025.6", forRemoval = true)
  public SearchResponse search(SearchRequest searchRequest) {
    return execute(() -> restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(searchRequest));
  }

  /**
   * Search operation using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn             A function that configures the search request using the new API builder
   * @param tDocumentClass The class of the document type to return
   * @return The search response from Elasticsearch
   */
  public <T> co.elastic.clients.elasticsearch.core.SearchResponse<T> searchV2(
    Function<co.elastic.clients.elasticsearch.core.SearchRequest.Builder,
      ObjectBuilder<co.elastic.clients.elasticsearch.core.SearchRequest>> fn,
    Class<T> tDocumentClass) {
    return execute(() -> elasticsearchClient.search(fn, tDocumentClass));
  }

  /**
   * @deprecated Use {@link #scrollV2(Function)} instead. This method uses the old Elasticsearch API.
   */
  @Deprecated(since = "2025.6", forRemoval = true)
  public SearchResponse scroll(SearchScrollRequest searchScrollRequest) {
    return execute(() -> restHighLevelClient.scroll(searchScrollRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(searchScrollRequest));
  }

  public ScrollResponse<Void> scrollV2(Function<ScrollRequest.Builder, ObjectBuilder<ScrollRequest>> fn) {
    return execute(() -> elasticsearchClient.scroll(fn, Void.class));
  }

  /**
   * Clear scroll operation using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the clear scroll request using the new API builder
   * @return The clear scroll response from Elasticsearch
   */
  public ClearScrollResponse clearScrollV2(Function<ClearScrollRequest.Builder, ObjectBuilder<ClearScrollRequest>> fn) {
    return execute(() -> elasticsearchClient.clearScroll(fn));
  }

  /**
   * Delete a document using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the delete request using the new API builder
   * @return The delete response from Elasticsearch
   */
  public DeleteResponse deleteV2(Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>> fn) {
    return execute(() -> elasticsearchClient.delete(fn));
  }

  public co.elastic.clients.elasticsearch.indices.RefreshResponse refreshV2(Index... indices) {
    List<String> indexNames = Arrays.stream(indices).map(Index::getName).toList();
    return execute(() -> elasticsearchClient.indices().refresh(rr -> rr.index(indexNames)));
  }

  /**
   * Force merge indices using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the force merge request using the new API builder
   * @return The force merge response from Elasticsearch
   */
  public ForcemergeResponse forcemergeV2(Function<ForcemergeRequest.Builder, ObjectBuilder<ForcemergeRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().forcemerge(fn));
  }

  /**
   * Update index settings using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the put index settings request using the new API builder
   * @return The put index settings response from Elasticsearch
   */
  public PutIndicesSettingsResponse putSettingsV2(
    Function<PutIndicesSettingsRequest.Builder, ObjectBuilder<PutIndicesSettingsRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().putSettings(fn));
  }

  /**
   * Clear indices cache using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the clear cache request using the new API builder
   * @return The clear cache response from Elasticsearch
   */
  public ClearCacheResponse clearCacheV2(Function<ClearCacheRequest.Builder, ObjectBuilder<ClearCacheRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().clearCache(fn));
  }

  /**
   * Index a document using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the index request using the new API builder
   * @return The index response from Elasticsearch
   */
  public <T> IndexResponse indexV2(Function<IndexRequest.Builder<T>, ObjectBuilder<IndexRequest<T>>> fn) {
    return execute(() -> elasticsearchClient.index(fn));
  }

  /**
   * Get a document using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn             A function that configures the get request using the new API builder
   * @param tDocumentClass The class of the document type to return
   * @return The get response from Elasticsearch
   */
  public <T> GetResponse<T> getV2(Function<GetRequest.Builder, ObjectBuilder<GetRequest>> fn, Class<T> tDocumentClass) {
    return execute(() -> elasticsearchClient.get(fn, tDocumentClass));
  }

  public GetIndexResponse getIndexV2(String indexName) {
    return execute(() -> elasticsearchClient.indices().get(req -> req.index(indexName)));
  }

  public GetIndexResponse getIndexV2(List<String> indexNames) {
    return execute(() -> elasticsearchClient.indices().get(req -> req.index(indexNames)));
  }

  /**
   * Check if an index exists using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the exists request using the new API builder
   * @return True if the index exists, false otherwise
   */
  public boolean indexExistsV2(Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().exists(fn).value());
  }

  /**
   * Create an index using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the create index request using the new API builder
   * @return The create index response from Elasticsearch
   */
  public co.elastic.clients.elasticsearch.indices.CreateIndexResponse createIndexV2(
    Function<co.elastic.clients.elasticsearch.indices.CreateIndexRequest.Builder,
      ObjectBuilder<co.elastic.clients.elasticsearch.indices.CreateIndexRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().create(fn));
  }

  public DeleteIndexResponse deleteIndexV2(String indexName) {
    return deleteIndexV2(List.of(indexName));
  }

  public DeleteIndexResponse deleteIndexV2(List<String> indexNames) {
    return execute(() -> elasticsearchClient.indices().delete(idr -> idr.index(indexNames)));
  }

  /**
   * Update mapping using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the put mapping request using the new API builder
   * @return The put mapping response from Elasticsearch
   */
  public PutMappingResponse putMappingV2(Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().putMapping(fn));
  }

  /**
   * @deprecated Use {@link #clusterHealthV2(java.util.function.Function)} instead. This method uses the old Elasticsearch API.
   */
  @Deprecated(since = "2025.6", forRemoval = true)
  public ClusterHealthResponse clusterHealth(ClusterHealthRequest clusterHealthRequest) {
    return execute(() -> restHighLevelClient.cluster().health(clusterHealthRequest, RequestOptions.DEFAULT),
      () -> computeDetailsAsString(clusterHealthRequest));
  }

  /**
   * Get cluster health using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the cluster health request using the new API builder
   * @return The cluster health response from Elasticsearch
   */
  public HealthResponse clusterHealthV2(Function<HealthRequest.Builder, ObjectBuilder<HealthRequest>> fn) {
    return execute(() -> elasticsearchClient.cluster().health(fn));
  }

  /**
   * @deprecated Use {@link #waitForStatusV2(HealthStatus)} instead. This method uses the old Elasticsearch API.
   */
  @Deprecated(since = "2025.6", forRemoval = true)
  public void waitForStatus(ClusterHealthStatus clusterHealthStatus) {
    clusterHealth(new ClusterHealthRequest().waitForEvents(Priority.LANGUID).waitForStatus(clusterHealthStatus));
  }

  /**
   * Wait for the cluster to reach a specific health status using the new Elasticsearch Java API Client (8.x).
   *
   * @param healthStatus The health status to wait for
   */
  public void waitForStatusV2(HealthStatus healthStatus) {
    clusterHealthV2(req -> req.waitForStatus(healthStatus));
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-nodes-stats.html
  public NodeStatsResponse nodesStats() {
    return execute(() -> {
      Request request = new Request("GET", "/_nodes/stats/fs,process,jvm,indices,breaker");
      Response response = restClient.performRequest(request);
      return NodeStatsResponse.toNodeStatsResponse(gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class));
    });
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-stats.html
  public IndicesStatsResponse indicesStats(String... indices) {
    return execute(() -> {
      Request request = new Request("GET", "/" + (indices.length > 0 ? (String.join(",", indices) + "/") : "") + "_stats");
      request.addParameter("level", "shards");
      Response response = restClient.performRequest(request);
      return IndicesStatsResponse.toIndicesStatsResponse(gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class));
    }, () -> computeDetailsAsString(indices));
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-stats.html
  public ClusterStatsResponse clusterStats() {
    return execute(() -> {
      Request request = new Request("GET", "/_cluster/stats");
      Response response = restClient.performRequest(request);
      return ClusterStatsResponse.toClusterStatsResponse(gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class));
    });
  }

  /**
   * Get index settings using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the get settings request using the new API builder
   * @return The get settings response from Elasticsearch
   */
  public co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse getSettingsV2(
    Function<co.elastic.clients.elasticsearch.indices.GetIndicesSettingsRequest.Builder,
      ObjectBuilder<co.elastic.clients.elasticsearch.indices.GetIndicesSettingsRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().getSettings(fn));
  }

  /**
   * Get index mapping using the new Elasticsearch Java API Client (8.x).
   *
   * @param fn A function that configures the get mapping request using the new API builder
   * @return The get mapping response from Elasticsearch
   */
  public GetMappingResponse getMappingV2(Function<GetMappingRequest.Builder, ObjectBuilder<GetMappingRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().getMapping(fn));
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

    MinimalRestHighLevelClient(RestClient restClient) {
      super(restClient, RestClient::close, Lists.newArrayList(), true);
    }

    @NotNull
    static RestClientBuilder buildHttpClient(@Nullable String searchPassword, @Nullable String keyStorePath,
      @Nullable String keyStorePassword, HttpHost[] hosts) {
      return RestClient.builder(hosts)
        .setRequestConfigCallback(r -> r
          .setConnectTimeout(CONNECT_TIMEOUT)
          .setSocketTimeout(SOCKET_TIMEOUT))
        .setHttpClientConfigCallback(httpClientBuilder -> {
          if (searchPassword != null) {
            BasicCredentialsProvider provider = getBasicCredentialsProvider(searchPassword);
            httpClientBuilder.setDefaultCredentialsProvider(provider);
          }

          if (keyStorePath != null) {
            SSLContext sslContext = getSSLContext(keyStorePath, keyStorePassword);
            httpClientBuilder.setSSLContext(sslContext);
          }

          return httpClientBuilder;
        });
    }

    private static BasicCredentialsProvider getBasicCredentialsProvider(String searchPassword) {
      BasicCredentialsProvider provider = new BasicCredentialsProvider();
      provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(ES_USERNAME, searchPassword));
      return provider;
    }

    private static SSLContext getSSLContext(String keyStorePath, @Nullable String keyStorePassword) {
      try {
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        try (InputStream is = Files.newInputStream(Paths.get(keyStorePath))) {
          keyStore.load(is, keyStorePassword == null ? null : keyStorePassword.toCharArray());
        }
        SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(keyStore, null);
        return sslBuilder.build();
      } catch (IOException | GeneralSecurityException e) {
        throw new IllegalStateException("Failed to setup SSL context on ES client", e);
      }
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
