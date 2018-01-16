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

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.discovery.DiscoveryModule;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.ParentJoinPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.test.NodeConfigurationSource;
import org.junit.rules.ExternalResource;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.config.ConfigurationProvider;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.elasticsearch.test.EsTestCluster;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertNull;
import static org.elasticsearch.test.XContentTestUtils.convertToMap;
import static org.elasticsearch.test.XContentTestUtils.differenceBetweenMapsIgnoringArrayOrder;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoTimeout;
import static org.junit.Assert.assertEquals;
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

  private static final Set<String> NO_TEMPLATES_SURVIVING_WIPE = Collections.emptySet();
  private static EsTestCluster cluster;
  private final List<IndexDefinition> indexDefinitions;

  public EsTester(IndexDefinition... defs) {
    this.indexDefinitions = asList(defs);
  }

  public void init() {
    Path tempDirectory;
    try {
      tempDirectory = Files.createTempDirectory("es-unit-test");
      tempDirectory.toFile().deleteOnExit();
      cluster = new EsTestCluster(new Random().nextLong(), tempDirectory, 1, "test cluster", getNodeConfigSource(), "node-",
        Collections.singletonList(ParentJoinPlugin.class), i -> i);
      Random random = new Random();
      cluster.beforeTest(random, random.nextDouble());
      cluster.wipe(NO_TEMPLATES_SURVIVING_WIPE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private NodeConfigurationSource getNodeConfigSource() {
    Settings.Builder networkSettings = Settings.builder();
    networkSettings.put(NetworkModule.TRANSPORT_TYPE_KEY, "local");

    return new NodeConfigurationSource() {
      @Override
      public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
          .put(NetworkModule.HTTP_ENABLED.getKey(), false)
          .put(DiscoveryModule.DISCOVERY_TYPE_SETTING.getKey(), "single-node")
          .put(networkSettings.build())
          .build();
      }

      @Override
      public Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.emptyList();
      }

      @Override
      public Settings transportClientSettings() {
        return Settings.builder().put(networkSettings.build()).build();
      }

      @Override
      public Collection<Class<? extends Plugin>> transportClientPlugins() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public void before() {
    if (cluster == null) {
      init();
    }

    if (!indexDefinitions.isEmpty()) {
      EsClient esClient = new NonClosingEsClient(cluster.client());
      ComponentContainer container = new ComponentContainer();
      container.addSingleton(new MapSettings());
      container.addSingleton(new ConfigurationProvider());
      container.addSingletons(indexDefinitions);
      container.addSingleton(esClient);
      container.addSingleton(IndexDefinitions.class);
      container.addSingleton(IndexCreator.class);
      container.addSingleton(MetadataIndex.class);
      container.addSingleton(MetadataIndexDefinition.class);
      container.addSingleton(TestEsDbCompatibility.class);

      Logger logger = Loggers.get(IndexCreator.class);
      LoggerLevel oldLevel = logger.getLevel();
      if (oldLevel == LoggerLevel.INFO) {
        logger.setLevel(LoggerLevel.WARN);
      }

      try {
        container.startComponents();
      } finally {
        logger.setLevel(oldLevel);
      }

      container.stopComponents();
      client().close();
    }
  }

  public static class NonClosingEsClient extends EsClient {
    NonClosingEsClient(Client nativeClient) {
      super(nativeClient);
    }

    @Override
    public void close() {
      // do nothing
    }
  }

  @Override
  public void after() {
    try {
      afterTest();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void afterTest() throws Exception {
    if (cluster != null) {
      MetaData metaData = cluster.client().admin().cluster().prepareState().execute().actionGet().getState().getMetaData();
      assertEquals("test leaves persistent cluster metadata behind: " + metaData.persistentSettings().getAsMap(),
        0,
        metaData.persistentSettings().getAsMap().size());
      assertEquals("test leaves transient cluster metadata behind: " + metaData.transientSettings().getAsMap(), 0, metaData
        .transientSettings().getAsMap().size());
      ensureClusterSizeConsistency();
      ensureClusterStateConsistency();
      cluster.beforeIndexDeletion();
      cluster.wipe(NO_TEMPLATES_SURVIVING_WIPE); // wipe after to make sure we fail in the test that didn't ack the delete
      cluster.assertAfterTest();
    }
  }

  private void ensureClusterSizeConsistency() {
    if (cluster != null) { // if static init fails the cluster can be null
      // logger.trace("Check consistency for [{}] nodes", cluster().size());
      assertNoTimeout(cluster.client().admin().cluster().prepareHealth().setWaitForNodes(Integer.toString(cluster.size())).get());
    }
  }

  /**
   * Verifies that all nodes that have the same version of the cluster state as master have same cluster state
   */
  private void ensureClusterStateConsistency() throws IOException {
    if (cluster != null) {
      ClusterState masterClusterState = cluster.client().admin().cluster().prepareState().all().get().getState();
      Map<String, Object> masterStateMap = convertToMap(masterClusterState);
      int masterClusterStateSize = ClusterState.Builder.toBytes(masterClusterState).length;
      String masterId = masterClusterState.nodes().getMasterNodeId();
      for (Client client : cluster.getClients()) {
        ClusterState localClusterState = client.admin().cluster().prepareState().all().setLocal(true).get().getState();
        final Map<String, Object> localStateMap = convertToMap(localClusterState);
        final int localClusterStateSize = ClusterState.Builder.toBytes(localClusterState).length;
        // Check that the non-master node has the same version of the cluster state as the master and
        // that the master node matches the master (otherwise there is no requirement for the cluster state to match)
        if (masterClusterState.version() == localClusterState.version() && masterId.equals(localClusterState.nodes().getMasterNodeId())) {
          try {
            assertEquals("clusterstate UUID does not match", masterClusterState.stateUUID(), localClusterState.stateUUID());
            // We cannot compare serialization bytes since serialization order of maps is not guaranteed
            // but we can compare serialization sizes - they should be the same
            assertEquals("clusterstate size does not match", masterClusterStateSize, localClusterStateSize);
            // Compare JSON serialization
            assertNull("clusterstate JSON serialization does not match", differenceBetweenMapsIgnoringArrayOrder(masterStateMap, localStateMap));
          } catch (AssertionError error) {
            // logger.error("Cluster state from master:\n{}\nLocal cluster state:\n{}", masterClusterState.toString(), localClusterState.toString());
            throw error;
          }
        }
      }
    }

  }

  public void deleteIndex(String indexName) {
    cluster.wipeIndices(indexName);
  }

  public void putDocuments(String index, String type, BaseDoc... docs) {
    putDocuments(new IndexType(index, type), docs);
  }

  public void putDocuments(IndexType indexType, BaseDoc... docs) {
    try {
      BulkRequestBuilder bulk = cluster.client().prepareBulk()
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

  public void putDocuments(IndexType indexType, Map<String,Object>... docs) {
    try {
      BulkRequestBuilder bulk = cluster.client().prepareBulk()
        .setRefreshPolicy(REFRESH_IMMEDIATE);
      for (Map<String,Object> doc : docs) {
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
    return newArrayList(Collections2.transform(hits, input -> {
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
    Client client = cluster.client();
    SearchRequestBuilder req = client.prepareSearch(indexType.getIndex()).setTypes(indexType.getType()).setQuery(QueryBuilders.matchAllQuery());
    EsUtils.optimizeScrollRequest(req);
    req.setScroll(new TimeValue(60000))
      .setSize(100);

    SearchResponse response = req.get();
    List<SearchHit> result = newArrayList();
    while (true) {
      Iterables.addAll(result, response.getHits());
      response = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
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
    return newArrayList(Iterables.transform(getDocuments(indexType), new Function<SearchHit, T>() {
      @Override
      public T apply(SearchHit input) {
        return (T) input.sourceAsMap().get(fieldNameToReturn);
      }
    }));
  }

  public List<String> getIds(IndexType indexType) {
    return getDocuments(indexType).stream().map(SearchHit::id).collect(Collectors.toList());
  }

  public EsClient client() {
    // EsClient which do not hold any reference to client returned by cluster and does not close them, to avoid leaks
    return new EsClient() {
      @Override
      public Client nativeClient() {
        return cluster.client();
      }

      @Override
      public void close() {
        // do nothing
      }
    };
  }

  public EsTester lockWrites(IndexType index) {
    return setIndexSettings(index.getIndex(), ImmutableMap.of("index.blocks.write", "true"));
  }

  public EsTester unlockWrites(IndexType index) {
    return setIndexSettings(index.getIndex(), ImmutableMap.of("index.blocks.write", "false"));
  }

  private EsTester setIndexSettings(String index, Map<String, Object> settings) {
    UpdateSettingsResponse response = client().nativeClient().admin().indices()
      .prepareUpdateSettings(index)
      .setSettings(settings)
      .get();
    checkState(response.isAcknowledged());
    return this;
  }
}
