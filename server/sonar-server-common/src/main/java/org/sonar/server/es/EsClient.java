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
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransportBase;
import co.elastic.clients.transport.TransportOptions;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
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
  private static final int MAX_CONN_PER_ROUTE = 20;
  private static final int MAX_CONN_TOTAL = 60;

  private final ElasticsearchClient elasticsearchClient;
  private final SynchronousEsHttpClient httpClient;

  private final Gson gson;

  public EsClient(HttpHost... hosts) {
    this(new SynchronousEsHttpClient(buildClassicHttpClient(null, null, null), List.of(hosts)));
  }

  public EsClient(@Nullable String searchPassword, @Nullable String keyStorePath, @Nullable String keyStorePassword, HttpHost... hosts) {
    this(new SynchronousEsHttpClient(buildClassicHttpClient(searchPassword, keyStorePath, keyStorePassword), List.of(hosts)));
  }

  EsClient(SynchronousEsHttpClient httpClient) {
    this.httpClient = httpClient;
    ObjectMapper objectMapper = new ObjectMapper()
      .configure(SerializationFeature.INDENT_OUTPUT, false)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    ElasticsearchTransportBase transport = new ElasticsearchTransportBase(httpClient, (TransportOptions) null, new JacksonJsonpMapper(objectMapper)) {
    };
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

  public void putClusterSettings(Map<String, Object> settings) {
    Map<String, JsonData> jsonSettings = new HashMap<>();
    settings.forEach((key, value) -> {
      if (value != null) {
        jsonSettings.put(key, JsonData.of(value));
      }
    });
    if (jsonSettings.isEmpty()) {
      return;
    }
    execute(() -> elasticsearchClient.cluster().putSettings(req -> req
      .transient_(jsonSettings)
    ));
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
      String body = httpClient.rawGet("/_nodes/stats/fs,process,jvm,indices,breaker", Map.of());
      return NodeStatsResponse.toNodeStatsResponse(gson.fromJson(body, JsonObject.class));
    });
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-stats.html
  public IndicesStatsResponse indicesStats(String... indices) {
    return execute(() -> {
      String path = indices.length == 0 ? "/_stats" : String.format("/%s/_stats", String.join(",", indices));
      String body = httpClient.rawGet(path, Map.of("level", "shards"));
      return IndicesStatsResponse.toIndicesStatsResponse(gson.fromJson(body, JsonObject.class));
    });
  }

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-stats.html
  public ClusterStatsResponse clusterStats() {
    return execute(() -> {
      String body = httpClient.rawGet("/_cluster/stats", Map.of());
      return ClusterStatsResponse.toClusterStatsResponse(gson.fromJson(body, JsonObject.class));
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
      httpClient.close();
    } catch (IOException e) {
      throw new ElasticsearchException("Could not close ES Rest client", e);
    }
  }

  /**
   * Internal usage only - exposes the new ES Java API client for components that need direct
   * access (e.g. {@link co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester}).
   */
  ElasticsearchClient nativeClientV2() {
    return elasticsearchClient;
  }

  /**
   * Internal usage only - exposes the underlying synchronous HTTP client.
   * Used by tests to inspect the configured nodes.
   */
  SynchronousEsHttpClient nativeHttpClient() {
    return httpClient;
  }

  static CloseableHttpClient buildClassicHttpClient(@Nullable String searchPassword, @Nullable String keyStorePath,
    @Nullable String keyStorePassword) {
    RequestConfig requestConfig = RequestConfig.custom()
      .setConnectTimeout(Timeout.ofMilliseconds(5_000))
      .setResponseTimeout(Timeout.ofMilliseconds(60_000))
      .build();
    PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
      .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
      .setMaxConnTotal(MAX_CONN_TOTAL);
    if (keyStorePath != null) {
      connectionManagerBuilder.setSSLSocketFactory(
        SSLConnectionSocketFactoryBuilder.create()
          .setSslContext(buildSslContext(keyStorePath, keyStorePassword))
          .build());
    }
    HttpClientBuilder clientBuilder = HttpClients.custom()
      .setDefaultRequestConfig(requestConfig)
      .setConnectionManager(connectionManagerBuilder.build());
    if (searchPassword != null) {
      String encoded = Base64.getEncoder().encodeToString((ES_USERNAME + ":" + searchPassword).getBytes(StandardCharsets.UTF_8));
      String headerValue = "Basic " + encoded;
      clientBuilder.addRequestInterceptorFirst((request, entity, context) -> {
        if (!request.containsHeader("Authorization")) {
          request.addHeader("Authorization", headerValue);
        }
      });
    }
    return clientBuilder.build();
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
