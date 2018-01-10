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
package org.sonar.elasticsearch.test;

import com.carrotsearch.hppc.ObjectArrayList;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.IOUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.stats.CommonStatsFlags;
import org.elasticsearch.action.admin.indices.stats.CommonStatsFlags.Flag;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.cluster.routing.allocation.DiskThresholdSettings;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.env.ShardLockObtainFailedException;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.engine.CommitStats;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.IndexTemplateMissingException;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.indices.fielddata.cache.IndicesFieldDataCache;
import org.elasticsearch.indices.recovery.RecoverySettings;
import org.elasticsearch.node.MockNode;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeService;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.test.NodeConfigurationSource;
import org.elasticsearch.test.transport.MockTransportService;
import org.elasticsearch.transport.MockTransportClient;
import org.elasticsearch.transport.TransportService;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * InternalTestCluster manages a set of JVM private nodes and allows convenient access to them.
 * The cluster supports randomized configuration such that nodes started in the cluster will
 * automatically load asserting services tracking resources like file handles or open searchers.
 * <p>
 * The Cluster is bound to a test lifecycle where tests must call {@link #beforeTest(java.util.Random, double)} and
 * {@link #afterTest()} to initialize and reset the cluster in order to be more reproducible. The term "more" relates
 * to the async nature of Elasticsearch in combination with randomized testing. Once Threads and asynchronous calls
 * are involved reproducibility is very limited.
 * </p>
 */
public final class EsTestCluster {

  protected final Logger logger = Loggers.getLogger(getClass());

  protected Random random;

  private double transportClientRatio = 0.0;

  /* sorted map to make traverse order reproducible, concurrent since we do checks on it not within a sync block */
  private final NavigableMap<String, NodeAndClient> nodes = new TreeMap<>();

  private final Set<Path> dataDirToClean = new HashSet<>();

  private final String clusterName;

  private final AtomicBoolean open = new AtomicBoolean(true);

  private final Settings defaultSettings;

  private AtomicInteger nextNodeId = new AtomicInteger(0);

  /*
   * Each shared node has a node seed that is used to start up the node and get default settings
   * this is important if a node is randomly shut down in a test since the next test relies on a
   * fully shared cluster to be more reproducible
   */
  private final long[] sharedNodesSeeds;

  private final int numSharedDataNodes;

  private final NodeConfigurationSource nodeConfigurationSource;

  private final ExecutorService executor;

  private final Collection<Class<? extends Plugin>> mockPlugins;

  /**
   * All nodes started by the cluster will have their name set to nodePrefix followed by a positive number
   */
  private final String nodePrefix;
  private final Path baseDir;

  private Function<Client, Client> clientWrapper;

  public EsTestCluster(long clusterSeed, Path baseDir,
    int numDataNodes, String clusterName, NodeConfigurationSource nodeConfigurationSource,
    String nodePrefix, Collection<Class<? extends Plugin>> mockPlugins, Function<Client, Client> clientWrapper) {
    this.clientWrapper = clientWrapper;
    this.baseDir = baseDir;
    this.clusterName = clusterName;
    if (numDataNodes < 0) {
      throw new IllegalArgumentException("number of data nodes must be >= 0");
    }

    Random random = new Random(clusterSeed);

    this.numSharedDataNodes = numDataNodes;
    assert this.numSharedDataNodes >= 1;

    this.nodePrefix = nodePrefix;

    assert nodePrefix != null;
    this.mockPlugins = mockPlugins;

    sharedNodesSeeds = new long[numSharedDataNodes];
    for (int i = 0; i < sharedNodesSeeds.length; i++) {
      sharedNodesSeeds[i] = random.nextLong();
    }

    logger.info("Setup InternalTestCluster [{}] with seed [{}] using " +
      "[{}] (data) nodes",
      clusterName, clusterSeed,
      numSharedDataNodes);
    this.nodeConfigurationSource = nodeConfigurationSource;
    Builder builder = Settings.builder();
    builder.put(NodeEnvironment.MAX_LOCAL_STORAGE_NODES_SETTING.getKey(), Integer.MAX_VALUE);
    builder.put(Environment.PATH_SHARED_DATA_SETTING.getKey(), baseDir.resolve("custom"));
    builder.put(Environment.PATH_HOME_SETTING.getKey(), baseDir);
    builder.put(Environment.PATH_REPO_SETTING.getKey(), baseDir.resolve("repos"));
    if (Strings.hasLength(System.getProperty("tests.es.logger.level"))) {
      builder.put("logger.level", System.getProperty("tests.es.logger.level"));
    }
    if (Strings.hasLength(System.getProperty("es.logger.prefix"))) {
      builder.put("logger.prefix", System.getProperty("es.logger.prefix"));
    }
    builder.put("action.auto_create_index", false);
    // Default the watermarks to absurdly low to prevent the tests
    // from failing on nodes without enough disk space
    builder.put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING.getKey(), "1b");
    builder.put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING.getKey(), "1b");
    // Some tests make use of scripting quite a bit, so increase the limit for integration tests
    builder.put(ScriptService.SCRIPT_MAX_COMPILATIONS_PER_MINUTE.getKey(), 1000);
    // always reduce this - it can make tests really slow
    builder.put(RecoverySettings.INDICES_RECOVERY_RETRY_DELAY_STATE_SYNC_SETTING.getKey(), TimeValue.timeValueMillis(randomIntBetween(random, 20, 50)));
    defaultSettings = builder.build();
    executor = EsExecutors.newScaling("test runner", 0, Integer.MAX_VALUE, 0, TimeUnit.SECONDS, EsExecutors.daemonThreadFactory("test_" + clusterName),
      new ThreadContext(Settings.EMPTY));
  }

  /**
   * A random integer from <code>min</code> to <code>max</code> (inclusive).
   */
  private static int randomIntBetween(Random r, int min, int max) {
    assert max >= min : "max must be >= min: " + min + ", " + max;
    long range = (long) max - (long) min;
    if (range < Integer.MAX_VALUE) {
      return min + r.nextInt(1 + (int) range);
    } else {
      return min + (int) Math.round(r.nextDouble() * range);
    }
  }

  private Settings getSettings(int nodeOrdinal, Settings others) {
    Builder builder = Settings.builder().put(defaultSettings);
    Settings settings = nodeConfigurationSource.nodeSettings(nodeOrdinal);
    if (settings != null) {
      if (settings.get(ClusterName.CLUSTER_NAME_SETTING.getKey()) != null) {
        throw new IllegalStateException("Tests must not set a '" + ClusterName.CLUSTER_NAME_SETTING.getKey() + "' as a node setting set '"
          + ClusterName.CLUSTER_NAME_SETTING.getKey() + "': [" + settings.get(ClusterName.CLUSTER_NAME_SETTING.getKey()) + "]");
      }
      builder.put(settings);
    }
    if (others != null) {
      builder.put(others);
    }
    builder.put(ClusterName.CLUSTER_NAME_SETTING.getKey(), clusterName);
    return builder.build();
  }

  private Collection<Class<? extends Plugin>> getPlugins() {
    Set<Class<? extends Plugin>> plugins = new HashSet<>(nodeConfigurationSource.nodePlugins());
    plugins.addAll(mockPlugins);
    return plugins;
  }

  private void ensureOpen() {
    if (!open.get()) {
      throw new RuntimeException("Cluster is already closed");
    }
  }

  private synchronized NodeAndClient getOrBuildRandomNode() {
    ensureOpen();
    NodeAndClient randomNodeAndClient = getRandomNodeAndClient();
    if (randomNodeAndClient != null) {
      return randomNodeAndClient;
    }
    NodeAndClient buildNode = buildNode();
    buildNode.startNode();
    publishNode(buildNode);
    return buildNode;
  }

  private synchronized NodeAndClient getRandomNodeAndClient() {
    return getRandomNodeAndClient(nc -> true);
  }

  private synchronized NodeAndClient getRandomNodeAndClient(Predicate<NodeAndClient> predicate) {
    ensureOpen();
    Collection<NodeAndClient> values = nodes.values().stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    if (!values.isEmpty()) {
      int whichOne = random.nextInt(values.size());
      for (NodeAndClient nodeAndClient : values) {
        if (whichOne-- == 0) {
          return nodeAndClient;
        }
      }
    }
    return null;
  }

  private NodeAndClient buildNode() {
    int ord = nextNodeId.getAndIncrement();
    return buildNode(ord, random.nextLong(), null, false);
  }

  private NodeAndClient buildNode(int nodeId, long seed, Settings settings, boolean reuseExisting) {
    assert Thread.holdsLock(this);
    ensureOpen();
    settings = getSettings(nodeId, settings);
    Collection<Class<? extends Plugin>> plugins = getPlugins();
    String name = buildNodeName(nodeId);
    if (reuseExisting && nodes.containsKey(name)) {
      return nodes.get(name);
    } else {
      assert reuseExisting || !nodes.containsKey(name) : "node name [" + name + "] already exists but not allowed to use it";
    }
    Settings finalSettings = Settings.builder()
      .put(Environment.PATH_HOME_SETTING.getKey(), baseDir) // allow overriding path.home
      .put(settings)
      .put("node.name", name)
      .put(NodeEnvironment.NODE_ID_SEED_SETTING.getKey(), seed)
      .build();
    MockNode node = new MockNode(finalSettings, plugins);
    return new NodeAndClient(name, node, nodeId);
  }

  private String buildNodeName(int id) {
    return nodePrefix + id;
  }

  /**
   * Returns a client connected to any node in the cluster
   */
  public synchronized Client client() {
    ensureOpen();
    /* Randomly return a client to one of the nodes in the cluster */
    return getOrBuildRandomNode().client(random);
  }

  /**
   * Returns a node client to a given node.
   */
  public synchronized Client client(String nodeName) {
    ensureOpen();
    NodeAndClient nodeAndClient = nodes.get(nodeName);
    if (nodeAndClient != null) {
      return nodeAndClient.client(random);
    }
    Assert.fail("No node found with name: [" + nodeName + "]");
    return null; // can't happen
  }

  /**
   * Returns a random node that applies to the given predicate.
   * The predicate can filter nodes based on the nodes settings.
   * If all nodes are filtered out this method will return <code>null</code>
   */
  public synchronized Client client(final Predicate<Settings> filterPredicate) {
    ensureOpen();
    final NodeAndClient randomNodeAndClient = getRandomNodeAndClient(nodeAndClient -> filterPredicate.test(nodeAndClient.node.settings()));
    if (randomNodeAndClient != null) {
      return randomNodeAndClient.client(random);
    }
    return null;
  }

  /**
   * Closes the current cluster
   */
  public synchronized void close() {
    if (this.open.compareAndSet(true, false)) {
      IOUtils.closeWhileHandlingException(nodes.values());
      nodes.clear();
      executor.shutdownNow();
    }
  }

  private final class NodeAndClient implements Closeable {
    private MockNode node;
    private Client nodeClient;
    private Client transportClient;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final String name;
    private final int nodeAndClientId;

    NodeAndClient(String name, MockNode node, int nodeAndClientId) {
      this.node = node;
      this.name = name;
      this.nodeAndClientId = nodeAndClientId;
      markNodeDataDirsAsNotEligableForWipe(node);
    }

    Node node() {
      if (closed.get()) {
        throw new RuntimeException("already closed");
      }
      return node;
    }

    int nodeAndClientId() {
      return nodeAndClientId;
    }

    Client client(Random random) {
      if (closed.get()) {
        throw new RuntimeException("already closed");
      }
      double nextDouble = random.nextDouble();
      if (nextDouble < transportClientRatio) {
        if (logger.isTraceEnabled()) {
          logger.trace("Using transport client for node [{}] sniff: [{}]", node.settings().get("node.name"), false);
        }
        return getOrBuildTransportClient();
      } else {
        return getOrBuildNodeClient();
      }
    }

    Client nodeClient() {
      if (closed.get()) {
        throw new RuntimeException("already closed");
      }
      return getOrBuildNodeClient();
    }

    private Client getOrBuildNodeClient() {
      if (nodeClient == null) {
        nodeClient = node.client();
      }
      return clientWrapper.apply(nodeClient);
    }

    private Client getOrBuildTransportClient() {
      if (transportClient == null) {
        /*
         * no sniff client for now - doesn't work will all tests since it might throw NoNodeAvailableException if nodes are shut down.
         * we first need support of transportClientRatio as annotations or so
         */
        Collection<Class<? extends Plugin>> plugins = new ArrayList<>();
        plugins.addAll(nodeConfigurationSource.transportClientPlugins());
        plugins.addAll(mockPlugins);
        transportClient = new TransportClientFactory(false, nodeConfigurationSource.transportClientSettings(), baseDir, plugins)
          .client(node, clusterName);
      }
      return clientWrapper.apply(transportClient);
    }

    void resetClient() {
      if (!closed.get()) {
        Releasables.close(nodeClient, transportClient);
        nodeClient = null;
        transportClient = null;
      }
    }

    void startNode() {
      try {
        node.start();
      } catch (NodeValidationException e) {
        throw new RuntimeException(e);
      }
    }

    void closeNode() throws IOException {
      markNodeDataDirsAsPendingForWipe(node);
      node.close();
    }

    @Override
    public void close() throws IOException {
      try {
        resetClient();
      } finally {
        closed.set(true);
        closeNode();
      }
    }
  }

  private static final String TRANSPORT_CLIENT_PREFIX = "transport_client_";

  static class TransportClientFactory {
    private final boolean sniff;
    private final Settings settings;
    private final Path baseDir;
    private final Collection<Class<? extends Plugin>> plugins;

    TransportClientFactory(boolean sniff, Settings settings, Path baseDir, Collection<Class<? extends Plugin>> plugins) {
      this.sniff = sniff;
      this.settings = settings != null ? settings : Settings.EMPTY;
      this.baseDir = baseDir;
      this.plugins = plugins;
    }

    public Client client(Node node, String clusterName) {
      TransportAddress addr = node.injector().getInstance(TransportService.class).boundAddress().publishAddress();
      Settings nodeSettings = node.settings();
      Builder builder = Settings.builder()
        .put("client.transport.nodes_sampler_interval", "1s")
        .put(Environment.PATH_HOME_SETTING.getKey(), baseDir)
        .put("node.name", TRANSPORT_CLIENT_PREFIX + node.settings().get("node.name"))
        .put(ClusterName.CLUSTER_NAME_SETTING.getKey(), clusterName)
        .put("client.transport.sniff", sniff)
        .put("logger.prefix", nodeSettings.get("logger.prefix", ""))
        .put("logger.level", nodeSettings.get("logger.level", "INFO"))
        .put(settings);
      if (NetworkModule.TRANSPORT_TYPE_SETTING.exists(settings)) {
        builder.put(NetworkModule.TRANSPORT_TYPE_KEY, NetworkModule.TRANSPORT_TYPE_SETTING.get(settings));
      }
      TransportClient client = new MockTransportClient(builder.build(), plugins);
      client.addTransportAddress(addr);
      return client;
    }
  }

  public synchronized void beforeTest(Random random, double transportClientRatio) throws IOException {
    assert transportClientRatio >= 0.0 && transportClientRatio <= 1.0;
    logger.debug("Reset test cluster with transport client ratio: [{}]", transportClientRatio);
    this.transportClientRatio = transportClientRatio;
    this.random = new Random(random.nextLong());
    reset(true);
  }

  private synchronized void reset(boolean wipeData) throws IOException {
    // clear all rules for mock transport services
    for (NodeAndClient nodeAndClient : nodes.values()) {
      TransportService transportService = nodeAndClient.node.injector().getInstance(TransportService.class);
      if (transportService instanceof MockTransportService) {
        final MockTransportService mockTransportService = (MockTransportService) transportService;
        mockTransportService.clearAllRules();
        mockTransportService.clearTracers();
      }
    }
    randomlyResetClients();
    final int newSize = sharedNodesSeeds.length;
    if (nextNodeId.get() == newSize && nodes.size() == newSize) {
      if (wipeData) {
        wipePendingDataDirectories();
      }
      logger.debug("Cluster hasn't changed - moving out - nodes: [{}] nextNodeId: [{}] numSharedNodes: [{}]", nodes.keySet(), nextNodeId.get(), newSize);
      return;
    }
    logger.debug("Cluster is NOT consistent - restarting shared nodes - nodes: [{}] nextNodeId: [{}] numSharedNodes: [{}]", nodes.keySet(), nextNodeId.get(), newSize);

    // trash all nodes with id >= sharedNodesSeeds.length - they are non shared

    for (Iterator<NodeAndClient> iterator = nodes.values().iterator(); iterator.hasNext();) {
      NodeAndClient nodeAndClient = iterator.next();
      if (nodeAndClient.nodeAndClientId() >= sharedNodesSeeds.length) {
        logger.debug("Close Node [{}] not shared", nodeAndClient.name);
        nodeAndClient.close();
        iterator.remove();
      }
    }

    // clean up what the nodes left that is unused
    if (wipeData) {
      wipePendingDataDirectories();
    }

    // start any missing node
    assert newSize == numSharedDataNodes;
    for (int i = 0; i < numSharedDataNodes; i++) {
      final Settings.Builder settings = Settings.builder();
      NodeAndClient nodeAndClient = buildNode(i, sharedNodesSeeds[i], settings.build(), true);
      nodeAndClient.startNode();
      publishNode(nodeAndClient);
    }

    nextNodeId.set(newSize);
    assert size() == newSize;
    if (newSize > 0) {
      ClusterHealthResponse response = client().admin().cluster().prepareHealth()
        .setWaitForNodes(Integer.toString(newSize)).get();
      if (response.isTimedOut()) {
        logger.warn("failed to wait for a cluster of size [{}], got [{}]", newSize, response);
        throw new IllegalStateException("cluster failed to reach the expected size of [" + newSize + "]");
      }
    }
    logger.debug("Cluster is consistent again - nodes: [{}] nextNodeId: [{}] numSharedNodes: [{}]", nodes.keySet(), nextNodeId.get(), newSize);
  }

  /**
   * This method should be executed during tear down, after each test (but after assertAfterTest)
   */
  public synchronized void afterTest() throws IOException {
    wipePendingDataDirectories();
    randomlyResetClients(); /* reset all clients - each test gets its own client based on the Random instance created above. */
  }

  public void beforeIndexDeletion() {
    // Check that the operations counter on index shard has reached 0.
    // The assumption here is that after a test there are no ongoing write operations.
    // test that have ongoing write operations after the test (for example because ttl is used
    // and not all docs have been purged after the test) and inherit from
    // ElasticsearchIntegrationTest must override beforeIndexDeletion() to avoid failures.
    assertShardIndexCounter();
    // check that shards that have same sync id also contain same number of documents
    assertSameSyncIdSameDocs();
  }

  private void assertSameSyncIdSameDocs() {
    Map<String, Long> docsOnShards = new HashMap<>();
    final Collection<NodeAndClient> nodesAndClients = nodes.values();
    for (NodeAndClient nodeAndClient : nodesAndClients) {
      IndicesService indexServices = getInstance(IndicesService.class, nodeAndClient.name);
      for (IndexService indexService : indexServices) {
        for (IndexShard indexShard : indexService) {
          CommitStats commitStats = indexShard.commitStats();
          if (commitStats != null) { // null if the engine is closed or if the shard is recovering
            String syncId = commitStats.getUserData().get(Engine.SYNC_COMMIT_ID);
            if (syncId != null) {
              long liveDocsOnShard = commitStats.getNumDocs();
              if (docsOnShards.get(syncId) != null) {
                assertThat(
                  "sync id is equal but number of docs does not match on node " + nodeAndClient.name + ". expected " + docsOnShards.get(syncId) + " but got " + liveDocsOnShard,
                  docsOnShards.get(syncId), equalTo(liveDocsOnShard));
              } else {
                docsOnShards.put(syncId, liveDocsOnShard);
              }
            }
          }
        }
      }
    }
  }

  private void assertShardIndexCounter() {
    final Collection<NodeAndClient> nodesAndClients = nodes.values();
    for (NodeAndClient nodeAndClient : nodesAndClients) {
      IndicesService indexServices = getInstance(IndicesService.class, nodeAndClient.name);
      for (IndexService indexService : indexServices) {
        for (IndexShard indexShard : indexService) {
          assertThat("index shard counter on shard " + indexShard.shardId() + " on node " + nodeAndClient.name + " not 0", indexShard.getActiveOperationsCount(), equalTo(0));
        }
      }
    }
  }

  private void randomlyResetClients() throws IOException {
    // only reset the clients on nightly tests, it causes heavy load...
    // if (RandomizedTest.isNightly() && rarely(random)) {
    final Collection<NodeAndClient> nodesAndClients = nodes.values();
    for (NodeAndClient nodeAndClient : nodesAndClients) {
      nodeAndClient.resetClient();
    }
    // }
  }

  private void wipePendingDataDirectories() {
    assert Thread.holdsLock(this);
    if (!dataDirToClean.isEmpty()) {
      try {
        for (Path path : dataDirToClean) {
          try {
            FileSystemUtils.deleteSubDirectories(path);
            logger.info("Successfully wiped data directory for node location: {}", path);
          } catch (IOException e) {
            logger.info("Failed to wipe data directory for node location: {}", path);
          }
        }
      } finally {
        dataDirToClean.clear();
      }
    }
  }

  private void markNodeDataDirsAsPendingForWipe(Node node) {
    assert Thread.holdsLock(this);
    NodeEnvironment nodeEnv = node.getNodeEnvironment();
    if (nodeEnv.hasNodeFile()) {
      dataDirToClean.addAll(Arrays.asList(nodeEnv.nodeDataPaths()));
    }
  }

  private void markNodeDataDirsAsNotEligableForWipe(Node node) {
    assert Thread.holdsLock(this);
    NodeEnvironment nodeEnv = node.getNodeEnvironment();
    if (nodeEnv.hasNodeFile()) {
      dataDirToClean.removeAll(Arrays.asList(nodeEnv.nodeDataPaths()));
    }
  }

  /**
   * Returns a reference to the given nodes instances of the given class &gt;T&lt;
   */
  public synchronized <T> T getInstance(Class<T> clazz, final String node) {
    return getInstance(clazz, nc -> node == null || node.equals(nc.name));
  }

  private synchronized <T> T getInstance(Class<T> clazz, Predicate<NodeAndClient> predicate) {
    NodeAndClient randomNodeAndClient = getRandomNodeAndClient(predicate);
    assert randomNodeAndClient != null;
    return getInstanceFromNode(clazz, randomNodeAndClient.node);
  }

  private synchronized <T> T getInstanceFromNode(Class<T> clazz, Node node) {
    return node.injector().getInstance(clazz);
  }

  /**
   * Returns the number of nodes in the cluster.
   */
  public synchronized int size() {
    return this.nodes.size();
  }

  private synchronized void publishNode(NodeAndClient nodeAndClient) {
    assert !nodeAndClient.node().isClosed();
    nodes.put(nodeAndClient.name, nodeAndClient);
  }


  /**
   * Returns an {@link Iterable} over all clients in this test cluster
   */
  public synchronized Iterable<Client> getClients() {
    ensureOpen();
    return () -> {
      ensureOpen();
      final Iterator<NodeAndClient> iterator = nodes.values().iterator();
      return new Iterator<Client>() {

        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }

        @Override
        public Client next() {
          return iterator.next().client(random);
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException("");
        }

      };
    };
  }

  /**
   * Ensures that any breaker statistics are reset to 0.
   *
   * The implementation is specific to the test cluster, because the act of
   * checking some breaker stats can increase them.
   */
  private void ensureEstimatedStats() {
    if (size() > 0) {
      // Checks that the breakers have been reset without incurring a
      // network request, because a network request can increment one
      // of the breakers
      for (NodeAndClient nodeAndClient : nodes.values()) {
        final IndicesFieldDataCache fdCache = getInstanceFromNode(IndicesService.class, nodeAndClient.node).getIndicesFieldDataCache();
        // Clean up the cache, ensuring that entries' listeners have been called
        fdCache.getCache().refresh();

        final String name = nodeAndClient.name;
        final CircuitBreakerService breakerService = getInstanceFromNode(CircuitBreakerService.class, nodeAndClient.node);
        CircuitBreaker fdBreaker = breakerService.getBreaker(CircuitBreaker.FIELDDATA);
        assertThat("Fielddata breaker not reset to 0 on node: " + name, fdBreaker.getUsed(), equalTo(0L));
        // Anything that uses transport or HTTP can increase the
        // request breaker (because they use bigarrays), because of
        // that the breaker can sometimes be incremented from ping
        // requests from other clusters because Jenkins is running
        // multiple ES testing jobs in parallel on the same machine.
        // To combat this we check whether the breaker has reached 0
        // in an assertBusy loop, so it will try for 10 seconds and
        // fail if it never reached 0
        try {
          assertBusy(new Runnable() {
            @Override
            public void run() {
              CircuitBreaker reqBreaker = breakerService.getBreaker(CircuitBreaker.REQUEST);
              assertThat("Request breaker not reset to 0 on node: " + name, reqBreaker.getUsed(), equalTo(0L));
            }
          });
        } catch (Exception e) {
          fail("Exception during check for request breaker reset to 0: " + e);
        }

        NodeService nodeService = getInstanceFromNode(NodeService.class, nodeAndClient.node);
        CommonStatsFlags flags = new CommonStatsFlags(Flag.FieldData, Flag.QueryCache, Flag.Segments);
        NodeStats stats = nodeService.stats(flags, false, false, false, false, false, false, false, false, false, false, false);
        assertThat("Fielddata size must be 0 on node: " + stats.getNode(), stats.getIndices().getFieldData().getMemorySizeInBytes(), equalTo(0L));
        assertThat("Query cache size must be 0 on node: " + stats.getNode(), stats.getIndices().getQueryCache().getMemorySizeInBytes(), equalTo(0L));
        assertThat("FixedBitSet cache size must be 0 on node: " + stats.getNode(), stats.getIndices().getSegments().getBitsetMemoryInBytes(), equalTo(0L));
      }
    }
  }

  /**
   * This method checks all the things that need to be checked after each test
   */
  public void assertAfterTest() {
    ensureEstimatedStats();
    assertRequestsFinished();
    for (NodeAndClient nodeAndClient : nodes.values()) {
      NodeEnvironment env = nodeAndClient.node().getNodeEnvironment();
      Set<ShardId> shardIds = env.lockedShards();
      for (ShardId id : shardIds) {
        try {
          env.shardLock(id, TimeUnit.SECONDS.toMillis(5)).close();
        } catch (ShardLockObtainFailedException ex) {
          fail("Shard " + id + " is still locked after 5 sec waiting");
        }
      }
    }
  }

  private void assertRequestsFinished() {
    if (size() > 0) {
      for (NodeAndClient nodeAndClient : nodes.values()) {
        CircuitBreaker inFlightRequestsBreaker = getInstance(CircuitBreakerService.class, nodeAndClient.name)
          .getBreaker(CircuitBreaker.IN_FLIGHT_REQUESTS);
        try {
          // see #ensureEstimatedStats()
          assertBusy(() -> {
            // ensure that our size accounting on transport level is reset properly
            long bytesUsed = inFlightRequestsBreaker.getUsed();
            assertThat("All incoming requests on node [" + nodeAndClient.name + "] should have finished. Expected 0 but got " +
              bytesUsed, bytesUsed, equalTo(0L));
          });
        } catch (Exception e) {
          logger.error("Could not assert finished requests within timeout", e);
          fail("Could not assert finished requests within timeout on node [" + nodeAndClient.name + "]");
        }
      }
    }
  }

  /**
   * Simple interface that allows to wait for an async operation to finish
   *
   * @param <T> the result of the async execution
   */
  public interface Async<T> {
    T get() throws ExecutionException, InterruptedException;
  }

  /**
   * Wipes any data that a test can leave behind: indices, templates (except exclude templates) and repositories
   */
  public void wipe(Set<String> excludeTemplates) {
    wipeIndices("_all");
    wipeAllTemplates(excludeTemplates);
    wipeRepositories();
  }

  /**
   * Deletes the given indices from the tests cluster. If no index name is passed to this method
   * all indices are removed.
   */
  public void wipeIndices(String... indices) {
    assert indices != null && indices.length > 0;
    if (size() > 0) {
      try {
        assertAcked(client().admin().indices().prepareDelete(indices));
      } catch (IndexNotFoundException e) {
        // ignore
      } catch (IllegalArgumentException e) {
        // Happens if `action.destructive_requires_name` is set to true
        // which is the case in the CloseIndexDisableCloseAllTests
        if ("_all".equals(indices[0])) {
          ClusterStateResponse clusterStateResponse = client().admin().cluster().prepareState().execute().actionGet();
          ObjectArrayList<String> concreteIndices = new ObjectArrayList<>();
          for (IndexMetaData indexMetaData : clusterStateResponse.getState().metaData()) {
            concreteIndices.add(indexMetaData.getIndex().getName());
          }
          if (!concreteIndices.isEmpty()) {
            assertAcked(client().admin().indices().prepareDelete(concreteIndices.toArray(String.class)));
          }
        }
      }
    }
  }

  public void assertAcked(DeleteIndexRequestBuilder builder) {
    DeleteIndexResponse response = builder.get();
    MatcherAssert.assertThat("Delete Index failed - not acked", response.isAcknowledged(), CoreMatchers.equalTo(true));
  }

  /**
   * Removes all templates, except the templates defined in the exclude
   */
  public void wipeAllTemplates(Set<String> exclude) {
    if (size() > 0) {
      GetIndexTemplatesResponse response = client().admin().indices().prepareGetTemplates().get();
      for (IndexTemplateMetaData indexTemplate : response.getIndexTemplates()) {
        if (exclude.contains(indexTemplate.getName())) {
          continue;
        }
        try {
          client().admin().indices().prepareDeleteTemplate(indexTemplate.getName()).execute().actionGet();
        } catch (IndexTemplateMissingException e) {
          // ignore
        }
      }
    }
  }

  /**
   * Deletes index templates, support wildcard notation.
   * If no template name is passed to this method all templates are removed.
   */
  public void wipeTemplates(String... templates) {
    if (size() > 0) {
      // if nothing is provided, delete all
      if (templates.length == 0) {
        templates = new String[] {"*"};
      }
      for (String template : templates) {
        try {
          client().admin().indices().prepareDeleteTemplate(template).execute().actionGet();
        } catch (IndexTemplateMissingException e) {
          // ignore
        }
      }
    }
  }

  /**
   * Deletes repositories, supports wildcard notation.
   */
  public void wipeRepositories(String... repositories) {
    if (size() > 0) {
      // if nothing is provided, delete all
      if (repositories.length == 0) {
        repositories = new String[] {"*"};
      }
      for (String repository : repositories) {
        try {
          client().admin().cluster().prepareDeleteRepository(repository).execute().actionGet();
        } catch (RepositoryMissingException ex) {
          // ignore
        }
      }
    }
  }

  /**
   * Runs the code block for 10 seconds waiting for no assertion to trip.
   */
  public static void assertBusy(Runnable codeBlock) throws Exception {
    assertBusy(codeBlock, 10, TimeUnit.SECONDS);
  }

  /**
   * Runs the code block for the provided interval, waiting for no assertions to trip.
   */
  private static void assertBusy(Runnable codeBlock, long maxWaitTime, TimeUnit unit) throws Exception {
    long maxTimeInMillis = TimeUnit.MILLISECONDS.convert(maxWaitTime, unit);
    long iterations = Math.max(Math.round(Math.log10(maxTimeInMillis) / Math.log10(2)), 1);
    long timeInMillis = 1;
    long sum = 0;
    List<AssertionError> failures = new ArrayList<>();
    for (int i = 0; i < iterations; i++) {
      try {
        codeBlock.run();
        return;
      } catch (AssertionError e) {
        failures.add(e);
      }
      sum += timeInMillis;
      Thread.sleep(timeInMillis);
      timeInMillis *= 2;
    }
    timeInMillis = maxTimeInMillis - sum;
    Thread.sleep(Math.max(timeInMillis, 0));
    try {
      codeBlock.run();
    } catch (AssertionError e) {
      for (AssertionError failure : failures) {
        e.addSuppressed(failure);
      }
      throw e;
    }
  }

}
