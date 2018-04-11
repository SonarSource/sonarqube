/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.routing.allocation.DiskThresholdSettings;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.discovery.DiscoveryModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.indices.recovery.RecoverySettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.junit.rules.ExternalResource;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.view.index.ViewIndexDefinition;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.sonar.server.es.DefaultIndexSettings.REFRESH_IMMEDIATE;

public class EsTester extends ExternalResource {

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
      Configuration config = new MapSettings().asConfig();
      List<IndexDefinitions.Index> createdIndices = createIndices(
        new ComponentIndexDefinition(config),
        IssueIndexDefinition.createForTest(),
        new ProjectMeasuresIndexDefinition(config),
        RuleIndexDefinition.createForTest(),
        new TestIndexDefinition(config),
        new UserIndexDefinition(config),
        new ViewIndexDefinition(config));

      CORE_INDICES_CREATED.set(true);
      createdIndices.stream().map(IndexDefinitions.Index::getName).forEach(CORE_INDICES_NAMES::add);
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

  @Override
  protected void after() {
    if (isCustom) {
      // delete non-core indices
      String[] existingIndices = SHARED_NODE.client().admin().indices().prepareGetIndex().get().getIndices();
      Stream.of(existingIndices)
        .filter(i -> !CORE_INDICES_NAMES.contains(i))
        .forEach(EsTester::deleteIndexIfExists);
    }
    BulkIndexer.delete(client(), new IndexType("_all", ""), client().prepareSearch("_all").setQuery(matchAllQuery()));
  }

  public EsClient client() {
    return new EsClient(SHARED_NODE.client());
  }

  public void putDocuments(String index, String type, BaseDoc... docs) {
    putDocuments(new IndexType(index, type), docs);
  }

  public void putDocuments(IndexType indexType, BaseDoc... docs) {
    try {
      BulkRequestBuilder bulk = SHARED_NODE.client().prepareBulk()
        .setRefreshPolicy(REFRESH_IMMEDIATE);
      for (BaseDoc doc : docs) {
        bulk.add(new IndexRequest(indexType.getIndex(), indexType.getType(), doc.getId())
          .parent(doc.getParent())
          .routing(doc.getRouting())
          .source(doc.getFields()));
      }
      BulkResponse bulkResponse = bulk.get();
      if (bulkResponse.hasFailures()) {
        throw new IllegalStateException(bulkResponse.buildFailureMessage());
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void putDocuments(IndexType indexType, Map<String, Object>... docs) {
    try {
      BulkRequestBuilder bulk = SHARED_NODE.client().prepareBulk()
        .setRefreshPolicy(REFRESH_IMMEDIATE);
      for (Map<String, Object> doc : docs) {
        bulk.add(new IndexRequest(indexType.getIndex(), indexType.getType())
          .source(doc));
      }
      BulkResponse bulkResponse = bulk.get();
      if (bulkResponse.hasFailures()) {
        throw new IllegalStateException(bulkResponse.buildFailureMessage());
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public long countDocuments(String index, String type) {
    return countDocuments(new IndexType(index, type));
  }

  public long countDocuments(IndexType indexType) {
    return client().prepareSearch(indexType).setSize(0).get().getHits().getTotalHits();
  }

  /**
   * Get all the indexed documents (no paginated results). Results are converted to BaseDoc objects.
   * Results are not sorted.
   */
  public <E extends BaseDoc> List<E> getDocuments(IndexType indexType, final Class<E> docClass) {
    List<SearchHit> hits = getDocuments(indexType);
    return new ArrayList<>(Collections2.transform(hits, input -> {
      try {
        return (E) ConstructorUtils.invokeConstructor(docClass, input.getSource());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }));
  }

  /**
   * Get all the indexed documents (no paginated results). Results are not sorted.
   */
  public List<SearchHit> getDocuments(IndexType indexType) {
    SearchRequestBuilder req = SHARED_NODE.client().prepareSearch(indexType.getIndex()).setTypes(indexType.getType()).setQuery(matchAllQuery());
    EsUtils.optimizeScrollRequest(req);
    req.setScroll(new TimeValue(60000))
      .setSize(100);

    SearchResponse response = req.get();
    List<SearchHit> result = newArrayList();
    while (true) {
      Iterables.addAll(result, response.getHits());
      response = SHARED_NODE.client().prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
      // Break condition: No hits are returned
      if (response.getHits().getHits().length == 0) {
        break;
      }
    }
    return result;
  }

  /**
   * Get a list of a specific field from all indexed documents.
   */
  public <T> List<T> getDocumentFieldValues(IndexType indexType, final String fieldNameToReturn) {
    return getDocuments(indexType)
      .stream()
      .map(input -> (T) input.getSourceAsMap().get(fieldNameToReturn))
      .collect(Collectors.toList());
  }

  public List<String> getIds(IndexType indexType) {
    return getDocuments(indexType).stream().map(SearchHit::getId).collect(Collectors.toList());
  }

  public void lockWrites(IndexType index) {
    setIndexSettings(index.getIndex(), ImmutableMap.of("index.blocks.write", "true"));
  }

  public void unlockWrites(IndexType index) {
    setIndexSettings(index.getIndex(), ImmutableMap.of("index.blocks.write", "false"));
  }

  private void setIndexSettings(String index, Map<String, Object> settings) {
    UpdateSettingsResponse response = SHARED_NODE.client().admin().indices()
      .prepareUpdateSettings(index)
      .setSettings(settings)
      .get();
    checkState(response.isAcknowledged());
  }

  private static void deleteIndexIfExists(String name) {
    try {
      DeleteIndexResponse response = SHARED_NODE.client().admin().indices().prepareDelete(name).get();
      checkState(response.isAcknowledged(), "Fail to drop the index " + name);
    } catch (IndexNotFoundException e) {
      // ignore
    }
  }

  private static List<IndexDefinitions.Index> createIndices(IndexDefinition... definitions) {
    IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();
    Stream.of(definitions).forEach(d -> d.define(context));

    List<IndexDefinitions.Index> result = new ArrayList<>();
    for (NewIndex newIndex : context.getIndices().values()) {
      IndexDefinitions.Index index = new IndexDefinitions.Index(newIndex);

      deleteIndexIfExists(index.getName());

      // create index
      Settings.Builder settings = Settings.builder();
      settings.put(index.getSettings());
      CreateIndexResponse indexResponse = SHARED_NODE.client().admin().indices()
        .prepareCreate(index.getName())
        .setSettings(settings)
        .get();
      if (!indexResponse.isAcknowledged()) {
        throw new IllegalStateException("Failed to create index " + index.getName());
      }
      SHARED_NODE.client().admin().cluster().prepareHealth(index.getName()).setWaitForStatus(ClusterHealthStatus.YELLOW).get();

      // create types
      for (Map.Entry<String, IndexDefinitions.IndexType> entry : index.getTypes().entrySet()) {
        PutMappingResponse mappingResponse = SHARED_NODE.client().admin().indices().preparePutMapping(index.getName())
          .setType(entry.getKey())
          .setSource(entry.getValue().getAttributes())
          .get();
        if (!mappingResponse.isAcknowledged()) {
          throw new IllegalStateException("Failed to create type " + entry.getKey());
        }
      }
      SHARED_NODE.client().admin().cluster().prepareHealth(index.getName()).setWaitForStatus(ClusterHealthStatus.YELLOW).get();
      result.add(index);
    }
    return result;
  }

  private static Node createNode() {
    try {
      Path tempDir = Files.createTempDirectory("EsTester");
      tempDir.toFile().deleteOnExit();
      Settings settings = Settings.builder()
        .put(Environment.PATH_HOME_SETTING.getKey(), tempDir)
        .put("node.name", "EsTester")
        .put(NodeEnvironment.MAX_LOCAL_STORAGE_NODES_SETTING.getKey(), Integer.MAX_VALUE)
        .put("logger.level", "INFO")
        .put("action.auto_create_index", false)
        // Default the watermarks to absurdly low to prevent the tests
        // from failing on nodes without enough disk space
        .put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING.getKey(), "1b")
        .put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING.getKey(), "1b")
        // always reduce this - it can make tests really slow
        .put(RecoverySettings.INDICES_RECOVERY_RETRY_DELAY_STATE_SYNC_SETTING.getKey(), TimeValue.timeValueMillis(20))
        .put(NetworkModule.TRANSPORT_TYPE_KEY, "local")
        .put(NetworkModule.HTTP_ENABLED.getKey(), false)
        .put(DiscoveryModule.DISCOVERY_TYPE_SETTING.getKey(), "single-node")
        .build();
      Node node = new Node(settings);
      return node.start();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to start embedded Elasticsearch", e);
    }
  }
}
