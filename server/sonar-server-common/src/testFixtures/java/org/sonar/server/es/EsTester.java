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

import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.analysis.common.CommonAnalysisPlugin;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.routing.allocation.DiskThresholdSettings;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.discovery.DiscoveryModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.http.BindHttpException;
import org.elasticsearch.http.HttpTransportSettings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.indices.recovery.RecoverySettings;
import org.elasticsearch.join.ParentJoinPlugin;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.reindex.ReindexPlugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.Netty4Plugin;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.es.IndexDefinition.IndexDefinitionContext;
import org.sonar.server.es.IndexType.IndexRelationType;
import org.sonar.server.es.newindex.BuiltIndex;
import org.sonar.server.es.newindex.NewIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.view.index.ViewIndexDefinition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.sonar.server.es.Index.ALL_INDICES;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.newindex.DefaultIndexSettings.REFRESH_IMMEDIATE;

public class EsTester extends ExternalResource {

  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 49151;
  private static final int MIN_NON_ROOT_PORT = 1025;
  private static final Logger LOG = LoggerFactory.getLogger(EsTester.class);

  static {
    System.setProperty("log4j.shutdownHookEnabled", "false");
    // we can not shutdown logging when tests are running or the next test that runs within the
    // same JVM will try to initialize logging after a security manager has been installed and
    // this will fail
    System.setProperty("es.log4j.shutdownEnabled", "false");
    System.setProperty("log4j2.disable.jmx", "true");
    System.setProperty("log4j.skipJansi", "true"); // jython has this crazy shaded Jansi version that log4j2 tries to load

    if (!Strings.hasLength(System.getProperty("tests.es.logger.level"))) {
      System.setProperty("tests.es.logger.level", "WARN");
    }
  }

  private static final Node SHARED_NODE = createNode();
  private static final EsClient ES_REST_CLIENT = createEsRestClient(SHARED_NODE);

  private static final AtomicBoolean CORE_INDICES_CREATED = new AtomicBoolean(false);
  private static final Set<String> CORE_INDICES_NAMES = new HashSet<>();

  private final boolean isCustom;

  private EsTester(boolean isCustom) {
    this.isCustom = isCustom;
  }

  /**
   * New instance which contains the core indices (rules, issues, ...).
   */
  public static EsTester create() {
    if (!CORE_INDICES_CREATED.get()) {
      List<BuiltIndex> createdIndices = createIndices(
        ComponentIndexDefinition.createForTest(),
        IssueIndexDefinition.createForTest(),
        ProjectMeasuresIndexDefinition.createForTest(),
        RuleIndexDefinition.createForTest(),
        ViewIndexDefinition.createForTest());

      CORE_INDICES_CREATED.set(true);
      createdIndices.stream().map(t -> t.getMainType().getIndex().getName()).forEach(CORE_INDICES_NAMES::add);
    }
    return new EsTester(false);
  }

  /**
   * New instance which contains the specified indices. Note that
   * core indices may exist.
   */
  public static EsTester createCustom(IndexDefinition... definitions) {
    createIndices(definitions);
    return new EsTester(true);
  }

  public void recreateIndexes() {
    deleteIndexIfExists(ALL_INDICES.getName());
    CORE_INDICES_CREATED.set(false);
    create();
  }

  @Override
  protected void after() {
    if (isCustom) {
      // delete non-core indices
      String[] existingIndices = getIndicesNames();
      Stream.of(existingIndices)
        .filter(i -> !CORE_INDICES_NAMES.contains(i))
        .forEach(EsTester::deleteIndexIfExists);
    }

    deleteAllDocumentsInIndexes();
  }

  private void deleteAllDocumentsInIndexes() {
    try {
      ES_REST_CLIENT.nativeClient()
        .deleteByQuery(new DeleteByQueryRequest(ALL_INDICES.getName()).setQuery(QueryBuilders.matchAllQuery()).setRefresh(true).setWaitForActiveShards(1), RequestOptions.DEFAULT);
      ES_REST_CLIENT.forcemerge(new ForceMergeRequest());
    } catch (IOException e) {
      throw new IllegalStateException("Could not delete data from _all indices", e);
    }
  }

  private static String[] getIndicesNames() {
    String[] existingIndices;
    try {
      existingIndices = ES_REST_CLIENT.nativeClient().indices().get(new GetIndexRequest(ALL_INDICES.getName()), RequestOptions.DEFAULT).getIndices();
    } catch (ElasticsearchStatusException e) {
      if (e.status().getStatus() == 404) {
        existingIndices = new String[0];
      } else {
        throw e;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not get indicies", e);
    }
    return existingIndices;
  }

  private static EsClient createEsRestClient(Node sharedNode) {
    assertThat(sharedNode.isClosed()).isFalse();

    String host = sharedNode.settings().get(HttpTransportSettings.SETTING_HTTP_BIND_HOST.getKey());
    Integer port = sharedNode.settings().getAsInt(HttpTransportSettings.SETTING_HTTP_PORT.getKey(), -1);
    return new EsClient(new HttpHost(host, port));
  }

  public EsClient client() {
    return ES_REST_CLIENT;
  }

  public RestHighLevelClient nativeClient() {
    return ES_REST_CLIENT.nativeClient();
  }

  public void putDocuments(IndexType indexType, BaseDoc... docs) {
    BulkRequest bulk = new BulkRequest()
      .setRefreshPolicy(REFRESH_IMMEDIATE);
    for (BaseDoc doc : docs) {
      bulk.add(doc.toIndexRequest());
    }
    BulkResponse bulkResponse = ES_REST_CLIENT.bulk(bulk);
    if (bulkResponse.hasFailures()) {
      fail("Bulk indexing of documents failed: " + bulkResponse.buildFailureMessage());
    }
  }

  public void putDocuments(IndexType indexType, Map<String, Object>... docs) {
    try {
      BulkRequest bulk = new BulkRequest()
        .setRefreshPolicy(REFRESH_IMMEDIATE);
      for (Map<String, Object> doc : docs) {
        IndexType.IndexMainType mainType = indexType.getMainType();
        bulk.add(new IndexRequest(mainType.getIndex().getName())
          .source(doc));
      }
      BulkResponse bulkResponse = ES_REST_CLIENT.bulk(bulk);
      if (bulkResponse.hasFailures()) {
        throw new IllegalStateException(bulkResponse.buildFailureMessage());
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public long countDocuments(Index index) {
    SearchRequest searchRequest = EsClient.prepareSearch(index.getName())
      .source(new SearchSourceBuilder()
        .query(QueryBuilders.matchAllQuery())
        .size(0));

    return ES_REST_CLIENT.search(searchRequest)
      .getHits().getTotalHits().value;
  }

  public long countDocuments(IndexType indexType) {
    SearchRequest searchRequest = EsClient.prepareSearch(indexType.getMainType())
      .source(new SearchSourceBuilder()
        .query(getDocumentsQuery(indexType))
        .size(0));

    return ES_REST_CLIENT.search(searchRequest)
      .getHits().getTotalHits().value;
  }

  /**
   * Get all the indexed documents (no paginated results). Results are converted to BaseDoc objects.
   * Results are not sorted.
   */
  public <E extends BaseDoc> List<E> getDocuments(IndexType indexType, final Class<E> docClass) {
    List<SearchHit> hits = getDocuments(indexType);
    return new ArrayList<>(Collections2.transform(hits, input -> {
      try {
        return (E) ConstructorUtils.invokeConstructor(docClass, input.getSourceAsMap());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }));
  }

  /**
   * Get all the indexed documents (no paginated results) of the specified type. Results are not sorted.
   */
  public List<SearchHit> getDocuments(IndexType indexType) {
    IndexType.IndexMainType mainType = indexType.getMainType();

    SearchRequest searchRequest = EsClient.prepareSearch(mainType.getIndex().getName())
      .source(new SearchSourceBuilder()
        .query(getDocumentsQuery(indexType)));
    return getDocuments(searchRequest);
  }

  private List<SearchHit> getDocuments(SearchRequest req) {
    req.scroll(new TimeValue(60000));
    req.source()
      .size(100)
      .sort("_doc", SortOrder.ASC);

    SearchResponse response = ES_REST_CLIENT.search(req);
    List<SearchHit> result = newArrayList();
    while (true) {
      Iterables.addAll(result, response.getHits());
      response = ES_REST_CLIENT.scroll(new SearchScrollRequest(response.getScrollId()).scroll(new TimeValue(600000)));
      // Break condition: No hits are returned
      if (response.getHits().getHits().length == 0) {
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(response.getScrollId());
        ES_REST_CLIENT.clearScroll(clearScrollRequest);
        break;
      }
    }
    return result;
  }

  private QueryBuilder getDocumentsQuery(IndexType indexType) {
    if (!indexType.getMainType().getIndex().acceptsRelations()) {
      return matchAllQuery();
    }

    if (indexType instanceof IndexRelationType) {
      return new TermQueryBuilder(FIELD_INDEX_TYPE, ((IndexRelationType) indexType).getName());
    }
    if (indexType instanceof IndexType.IndexMainType) {
      return new TermQueryBuilder(FIELD_INDEX_TYPE, ((IndexType.IndexMainType) indexType).getType());
    }
    throw new IllegalArgumentException("Unsupported IndexType " + indexType.getClass());
  }

  /**
   * Get a list of a specific field from all indexed documents.
   */
  public <T> List<T> getDocumentFieldValues(IndexType indexType, final String fieldNameToReturn) {
    return getDocuments(indexType)
      .stream()
      .map(input -> (T) input.getSourceAsMap().get(fieldNameToReturn))
      .toList();
  }

  public List<String> getIds(IndexType indexType) {
    return getDocuments(indexType).stream().map(SearchHit::getId).toList();
  }

  public void lockWrites(IndexType index) {
    setIndexSettings(index.getMainType().getIndex().getName(), ImmutableMap.of("index.blocks.write", "true"));
  }

  public void unlockWrites(IndexType index) {
    setIndexSettings(index.getMainType().getIndex().getName(), ImmutableMap.of("index.blocks.write", "false"));
  }

  private void setIndexSettings(String index, Map<String, Object> settings) {
    AcknowledgedResponse response = null;
    try {
      response = ES_REST_CLIENT.nativeClient().indices()
        .putSettings(new UpdateSettingsRequest(index).settings(settings), RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new IllegalStateException("Could not update index settings", e);
    }
    checkState(response.isAcknowledged());
  }

  private static void deleteIndexIfExists(String name) {
    try {
      AcknowledgedResponse response = ES_REST_CLIENT.nativeClient().indices().delete(new DeleteIndexRequest(name), RequestOptions.DEFAULT);
      checkState(response.isAcknowledged(), "Fail to drop the index " + name);
    } catch (ElasticsearchStatusException e) {
      if (e.status().getStatus() == 404) {
        // ignore, index not found
      } else {
        throw e;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not delete index", e);
    }
  }

  private static List<BuiltIndex> createIndices(IndexDefinition... definitions) {
    IndexDefinitionContext context = new IndexDefinitionContext();
    Stream.of(definitions).forEach(d -> d.define(context));

    List<BuiltIndex> result = new ArrayList<>();
    for (NewIndex newIndex : context.getIndices().values()) {
      BuiltIndex index = newIndex.build();

      String indexName = index.getMainType().getIndex().getName();
      deleteIndexIfExists(indexName);

      // create index
      Settings.Builder settings = Settings.builder();
      settings.put(index.getSettings());

      CreateIndexResponse indexResponse = createIndex(indexName, settings);

      if (!indexResponse.isAcknowledged()) {
        throw new IllegalStateException("Failed to create index " + indexName);
      }

      waitForClusterYellowStatus(indexName);

      // create types
      String typeName = index.getMainType().getType();
      putIndexMapping(index, indexName, typeName);

      waitForClusterYellowStatus(indexName);

      result.add(index);
    }
    return result;
  }

  private static void waitForClusterYellowStatus(String indexName) {
    try {
      ES_REST_CLIENT.nativeClient().cluster().health(new ClusterHealthRequest(indexName).waitForStatus(ClusterHealthStatus.YELLOW), RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new IllegalStateException("Could not query for index health status");
    }
  }

  private static void putIndexMapping(BuiltIndex index, String indexName, String typeName) {
    try {
      AcknowledgedResponse mappingResponse = ES_REST_CLIENT.nativeClient().indices().putMapping(new PutMappingRequest(indexName)
        .type(typeName)
        .source(index.getAttributes()), RequestOptions.DEFAULT);

      if (!mappingResponse.isAcknowledged()) {
        throw new IllegalStateException("Failed to create type " + typeName);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not query for put index mapping");
    }
  }

  private static CreateIndexResponse createIndex(String indexName, Settings.Builder settings) {
    CreateIndexResponse indexResponse;
    try {
      indexResponse = ES_REST_CLIENT.nativeClient().indices()
        .create(new CreateIndexRequest(indexName).settings(settings), RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new IllegalStateException("Could not create index");
    }
    return indexResponse;
  }

  private static Node createNode() {
    try {
      Path tempDir = Files.createTempDirectory("EsTester");
      tempDir.toFile().deleteOnExit();
      int i = 10;
      while (i > 0) {
        int httpPort = getNextAvailable();
        try {
          Node node = startNode(tempDir, httpPort);
          LOG.info("EsTester running ElasticSearch on HTTP port {}", httpPort);
          return node;
        } catch (BindHttpException e) {
          i--;
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to start embedded Elasticsearch", e);
    }
    throw new IllegalStateException("Failed to find an open port to connect EsTester's Elasticsearch instance after 10 attempts");
  }

  private static Node startNode(Path tempDir, int httpPort) throws NodeValidationException {
    Settings settings = Settings.builder()
      .put(Environment.PATH_HOME_SETTING.getKey(), tempDir)
      .put("node.name", "EsTester")
      .put(NodeEnvironment.MAX_LOCAL_STORAGE_NODES_SETTING.getKey(), Integer.MAX_VALUE)
      .put("logger.level", "INFO")
      .put("action.auto_create_index", false)
      // allows to drop all indices at once using `_all`
      // this parameter will default to true in ES 8.X
      .put("action.destructive_requires_name", false)
      // Default the watermarks to absurdly low to prevent the tests
      // from failing on nodes without enough disk space
      .put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING.getKey(), "1b")
      .put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING.getKey(), "1b")
      .put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_DISK_FLOOD_STAGE_WATERMARK_SETTING.getKey(), "1b")
      // always reduce this - it can make tests really slow
      .put(RecoverySettings.INDICES_RECOVERY_RETRY_DELAY_STATE_SYNC_SETTING.getKey(), TimeValue.timeValueMillis(20))
      .put(HttpTransportSettings.SETTING_HTTP_PORT.getKey(), httpPort)
      .put(HttpTransportSettings.SETTING_HTTP_BIND_HOST.getKey(), "localhost")
      .put(DiscoveryModule.DISCOVERY_TYPE_SETTING.getKey(), "single-node")
      .build();
    Node node = new Node(InternalSettingsPreparer.prepareEnvironment(settings, Collections.emptyMap(), null, null),
      ImmutableList.of(
        CommonAnalysisPlugin.class,
        ReindexPlugin.class,
        // Netty4Plugin provides http and tcp transport
        Netty4Plugin.class,
        // install ParentJoin plugin required to create field of type "join"
        ParentJoinPlugin.class),
      true) {
    };
    return node.start();
  }

  public static int getNextAvailable() {
    Random random = new Random();
    int maxAttempts = 10;
    int i = maxAttempts;
    while (i > 0) {
      int port = MIN_NON_ROOT_PORT + random.nextInt(MAX_PORT - MIN_NON_ROOT_PORT);
      if (available(port)) {
        return port;
      }
      i--;
    }

    throw new NoSuchElementException(format("Could not find an available port in %s attempts", maxAttempts));
  }

  private static boolean available(int port) {
    checkArgument(validPort(port), "Invalid port: %s", port);

    try (ServerSocket ss = new ServerSocket(port)) {
      ss.setReuseAddress(true);
      try (DatagramSocket ds = new DatagramSocket(port)) {
        ds.setReuseAddress(true);
      }
      return true;
    } catch (IOException var13) {
      return false;
    }
  }

  private static boolean validPort(int fromPort) {
    return fromPort >= MIN_PORT && fromPort <= MAX_PORT;
  }

}
