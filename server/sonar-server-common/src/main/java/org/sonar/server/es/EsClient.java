/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.ClearScrollRequest;
import co.elastic.clients.elasticsearch.core.ClearScrollResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.elasticsearch.indices.ClearCacheRequest;
import co.elastic.clients.elasticsearch.indices.ClearCacheResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.ForcemergeRequest;
import co.elastic.clients.elasticsearch.indices.ForcemergeResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.elasticsearch.indices.RefreshResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.util.ObjectBuilder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.es.response.ClusterStatsResponse;
import org.sonar.server.es.response.IndicesStatsResponse;
import org.sonar.server.es.response.NodeStatsResponse;

/**
 * Wrapper to connect to Elasticsearch node. Handles correctly errors (logging + exceptions
 * with context) and profiling of requests.
 */
public class EsClient implements Closeable {
  public static final Logger LOGGER = Loggers.get("es");
  private static final String ES_USERNAME = "elastic";

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
    this.restClient = restClient;
    ObjectMapper objectMapper = new ObjectMapper()
      .configure(SerializationFeature.INDENT_OUTPUT, false)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    RestClientTransport transport = new RestClientTransport(this.restClient, new JacksonJsonpMapper(objectMapper));
    this.elasticsearchClient = new ElasticsearchClient(transport);
    this.gson = new GsonBuilder().create();
  }

  public BulkResponse bulkV2(Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>> fn) {
    return execute(() -> elasticsearchClient.bulk(fn));
  }

  public <T> SearchResponse<T> searchV2(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Class<T> tDocumentClass) {
    return execute(() -> elasticsearchClient.search(fn, tDocumentClass));
  }

  public ScrollResponse<Void> scrollV2(Function<ScrollRequest.Builder, ObjectBuilder<ScrollRequest>> fn) {
    return execute(() -> elasticsearchClient.scroll(fn, Void.class));
  }

  public ClearScrollResponse clearScrollV2(Function<ClearScrollRequest.Builder, ObjectBuilder<ClearScrollRequest>> fn) {
    return execute(() -> elasticsearchClient.clearScroll(fn));
  }

  public DeleteResponse deleteV2(Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>> fn) {
    return execute(() -> elasticsearchClient.delete(fn));
  }

  public DeleteByQueryResponse deleteByQueryV2(Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>> fn) {
    return execute(() -> elasticsearchClient.deleteByQuery(fn));
  }

  public AnalyzeResponse analyzeV2(Function<AnalyzeRequest.Builder, ObjectBuilder<AnalyzeRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().analyze(fn));
  }

  public RefreshResponse refreshV2(Index... indices) {
    List<String> indexNames = Arrays.stream(indices).map(Index::getName).toList();
    return execute(() -> elasticsearchClient.indices().refresh(rr -> rr.index(indexNames)));
  }

  public ForcemergeResponse forcemergeV2(Function<ForcemergeRequest.Builder, ObjectBuilder<ForcemergeRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().forcemerge(fn));
  }

  public PutIndicesSettingsResponse putSettingsV2(
    Function<PutIndicesSettingsRequest.Builder, ObjectBuilder<PutIndicesSettingsRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().putSettings(fn));
  }

  public ClearCacheResponse clearCacheV2(Function<ClearCacheRequest.Builder, ObjectBuilder<ClearCacheRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().clearCache(fn));
  }

  public <T> IndexResponse indexV2(Function<IndexRequest.Builder<T>, ObjectBuilder<IndexRequest<T>>> fn) {
    return execute(() -> elasticsearchClient.index(fn));
  }

  public <T> GetResponse<T> getV2(Function<GetRequest.Builder, ObjectBuilder<GetRequest>> fn, Class<T> tDocumentClass) {
    return execute(() -> elasticsearchClient.get(fn, tDocumentClass));
  }

  public GetIndexResponse getIndexV2(String indexName) {
    return execute(() -> elasticsearchClient.indices().get(req -> req.index(indexName)));
  }

  public GetIndexResponse getIndexV2(List<String> indexNames) {
    return execute(() -> elasticsearchClient.indices().get(req -> req.index(indexNames)));
  }

  public boolean indexExistsV2(Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().exists(fn).value());
  }

  public CreateIndexResponse createIndexV2(Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().create(fn));
  }

  public DeleteIndexResponse deleteIndexV2(String indexName) {
    return deleteIndexV2(List.of(indexName));
  }

  public DeleteIndexResponse deleteIndexV2(List<String> indexNames) {
    return execute(() -> elasticsearchClient.indices().delete(idr -> idr.index(indexNames)));
  }

  public PutMappingResponse putMappingV2(Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().putMapping(fn));
  }

  public HealthResponse clusterHealthV2(Function<HealthRequest.Builder, ObjectBuilder<HealthRequest>> fn) {
    return execute(() -> elasticsearchClient.cluster().health(fn));
  }

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
    });
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-stats.html
  public ClusterStatsResponse clusterStats() {
    return execute(() -> {
      Request request = new Request("GET", "/_cluster/stats");
      Response response = restClient.performRequest(request);
      return ClusterStatsResponse.toClusterStatsResponse(gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class));
    });
  }

  public GetIndicesSettingsResponse getSettingsV2(Function<GetIndicesSettingsRequest.Builder, ObjectBuilder<GetIndicesSettingsRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().getSettings(fn));
  }

  public GetMappingResponse getMappingV2(Function<GetMappingRequest.Builder, ObjectBuilder<GetMappingRequest>> fn) {
    return execute(() -> elasticsearchClient.indices().getMapping(fn));
  }

  @Override
  public void close() {
    try {
      restClient.close();
    } catch (IOException e) {
      throw new ElasticsearchException("Could not close ES Rest client", e);
    }
  }

  /**
   * Internal usage only - exposes the new ES8 Java API client for components that need direct
   * access (e.g. {@link co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester}).
   */
  ElasticsearchClient nativeClientV2() {
    return elasticsearchClient;
  }

  /**
   * Internal usage only - exposes the underlying low-level REST client.
   * Used by tests to inspect the configured nodes.
   */
  RestClient nativeRestClient() {
    return restClient;
  }

  @NotNull
  static RestClientBuilder buildHttpClient(@Nullable String searchPassword, @Nullable String keyStorePath,
    @Nullable String keyStorePassword, HttpHost[] hosts) {
    return RestClient.builder(hosts)
      .setRequestConfigCallback(r -> r
        .setConnectTimeout(5000)
        .setSocketTimeout(60000))
      .setHttpClientConfigCallback(httpClientBuilder -> {
        if (searchPassword != null) {
          BasicCredentialsProvider provider = new BasicCredentialsProvider();
          provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(ES_USERNAME, searchPassword));
          httpClientBuilder.setDefaultCredentialsProvider(provider);
        }
        if (keyStorePath != null) {
          httpClientBuilder.setSSLContext(buildSslContext(keyStorePath, keyStorePassword));
        }
        return httpClientBuilder;
      });
  }

  private static SSLContext buildSslContext(String keyStorePath, @Nullable String keyStorePassword) {
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
