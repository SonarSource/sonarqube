/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.IOUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
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
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNode.Role;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.DiskThresholdSettings;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.discovery.DiscoverySettings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.env.ShardLockObtainFailedException;
import org.elasticsearch.http.HttpServerTransport;
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
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.node.service.NodeService;
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
import org.sonar.server.es.EsTester;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoTimeout;
import static org.hamcrest.Matchers.equalTo;
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
 * are involved reproducibility is very limited. This class should only be used through {@code ESIntegTestCase}.
 * </p>
 */
public final class EsTestCluster {

  protected final Logger logger = Loggers.getLogger(getClass());
  private final long seed;

  protected Random random;

  protected double transportClientRatio = 0.0;

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
    // assertTrue(Version.CURRENT.after(VersionUtils.getPreviousVersion()));
    // /*
    // * If possible we fetch the NamedWriteableRegistry from the test cluster. That is the only way to make sure that we properly handle
    // * when plugins register names. If not possible we'll try and set up a registry based on whatever SearchModule registers. But that
    // * is a hack at best - it only covers some things. If you end up with errors below and get to this comment I'm sorry. Please find
    // * a way that sucks less.
    // */
    // NamedWriteableRegistry registry = this.getInstance(NamedWriteableRegistry.class);
    // assertVersionSerializable(randomVersion(random()), response, registry);
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

  public static final int DEFAULT_LOW_NUM_MASTER_NODES = 1;
  public static final int DEFAULT_HIGH_NUM_MASTER_NODES = 3;

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

  // if set to 0, data nodes will also assume the master role
  private final int numSharedDedicatedMasterNodes;

  private final int numSharedDataNodes;

  private final int numSharedCoordOnlyNodes;

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
    boolean randomlyAddDedicatedMasters,
    int numDataNodes, String clusterName, NodeConfigurationSource nodeConfigurationSource, int numClientNodes,
    String nodePrefix, Collection<Class<? extends Plugin>> mockPlugins, Function<Client, Client> clientWrapper) {
    this.seed = clusterSeed;
    this.clientWrapper = clientWrapper;
    this.baseDir = baseDir;
    this.clusterName = clusterName;
    if (numDataNodes < 0) {
      throw new IllegalArgumentException("number of data nodes must be >= 0");
    }

    Random random = new Random(clusterSeed);

    boolean useDedicatedMasterNodes = randomlyAddDedicatedMasters ? random.nextBoolean() : false;

    this.numSharedDataNodes = numDataNodes;
    assert this.numSharedDataNodes >= 0;

    if (numSharedDataNodes == 0) {
      this.numSharedCoordOnlyNodes = 0;
      this.numSharedDedicatedMasterNodes = 0;
    } else {
      if (useDedicatedMasterNodes) {
        if (random.nextBoolean()) {
          // use a dedicated master, but only low number to reduce overhead to tests
          this.numSharedDedicatedMasterNodes = DEFAULT_LOW_NUM_MASTER_NODES;
        } else {
          this.numSharedDedicatedMasterNodes = DEFAULT_HIGH_NUM_MASTER_NODES;
        }
      } else {
        this.numSharedDedicatedMasterNodes = 0;
      }
      this.numSharedCoordOnlyNodes = numClientNodes;
    }
    assert this.numSharedCoordOnlyNodes >= 0;

    this.nodePrefix = nodePrefix;

    assert nodePrefix != null;
    this.mockPlugins = mockPlugins;

    sharedNodesSeeds = new long[numSharedDedicatedMasterNodes + numSharedDataNodes + numSharedCoordOnlyNodes];
    for (int i = 0; i < sharedNodesSeeds.length; i++) {
      sharedNodesSeeds[i] = random.nextLong();
    }

    logger.info("Setup InternalTestCluster [{}] with seed [{}] using [{}] dedicated masters, " +
      "[{}] (data) nodes and [{}] coord only nodes",
      clusterName, clusterSeed,
      numSharedDedicatedMasterNodes, numSharedDataNodes, numSharedCoordOnlyNodes);
    this.nodeConfigurationSource = nodeConfigurationSource;
    Builder builder = Settings.builder();
    if (random.nextInt(5) == 0) { // sometimes set this
      // randomize (multi/single) data path, special case for 0, don't set it at all...
      final int numOfDataPaths = random.nextInt(5);
      if (numOfDataPaths > 0) {
        StringBuilder dataPath = new StringBuilder();
        for (int i = 0; i < numOfDataPaths; i++) {
          dataPath.append(baseDir.resolve("d" + i).toAbsolutePath()).append(',');
        }
        builder.put(Environment.PATH_DATA_SETTING.getKey(), dataPath.toString());
      }
    }
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
    // Default the watermarks to absurdly low to prevent the tests
    // from failing on nodes without enough disk space
    builder.put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING.getKey(), "1b");
    builder.put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING.getKey(), "1b");
    // Some tests make use of scripting quite a bit, so increase the limit for integration tests
    builder.put(ScriptService.SCRIPT_MAX_COMPILATIONS_PER_MINUTE.getKey(), 1000);
    // if (random.nextInt(100) <= 90) {
    // builder.put(ThrottlingAllocationDecider.CLUSTER_ROUTING_ALLOCATION_NODE_CONCURRENT_INCOMING_RECOVERIES_SETTING.getKey(),
    // RandomInts.randomIntBetween(random, 2, 5));
    // builder.put(ThrottlingAllocationDecider.CLUSTER_ROUTING_ALLOCATION_NODE_CONCURRENT_OUTGOING_RECOVERIES_SETTING.getKey(),
    // RandomInts.randomIntBetween(random, 2, 5));
    // }
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

  /**
   * Returns the cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

  public String[] getNodeNames() {
    return nodes.keySet().toArray(Strings.EMPTY_ARRAY);
  }

  private Settings getSettings(int nodeOrdinal, long nodeSeed, Settings others) {
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

  /**
   * Ensures that at least <code>n</code> data nodes are present in the cluster.
   * if more nodes than <code>n</code> are present this method will not
   * stop any of the running nodes.
   */
  public void ensureAtLeastNumDataNodes(int n) {
    final List<Async<String>> asyncs = new ArrayList<>();
    synchronized (this) {
      int size = numDataNodes();
      for (int i = size; i < n; i++) {
        logger.info("increasing cluster size from {} to {}", size, n);
        if (numSharedDedicatedMasterNodes > 0) {
          asyncs.add(startDataOnlyNodeAsync());
        } else {
          asyncs.add(startNodeAsync());
        }
      }
    }
    try {
      for (Async<String> async : asyncs) {
        async.get();
      }
    } catch (Exception e) {
      throw new ElasticsearchException("failed to start nodes", e);
    }
    if (!asyncs.isEmpty()) {
      synchronized (this) {
        assertNoTimeout(client().admin().cluster().prepareHealth().setWaitForNodes(Integer.toString(nodes.size())).get());
      }
    }
  }

  /**
   * Ensures that at most <code>n</code> are up and running.
   * If less nodes that <code>n</code> are running this method
   * will not start any additional nodes.
   */
  public synchronized void ensureAtMostNumDataNodes(int n) throws IOException {
    int size = numDataNodes();
    if (size <= n) {
      return;
    }
    // prevent killing the master if possible and client nodes
    final Stream<NodeAndClient> collection = n == 0 ? nodes.values().stream()
      : nodes.values().stream().filter(new DataNodePredicate().and(new MasterNodePredicate(getMasterName()).negate()));
    final Iterator<NodeAndClient> values = collection.iterator();

    logger.info("changing cluster size from {} data nodes to {}", size, n);
    Set<NodeAndClient> nodesToRemove = new HashSet<>();
    int numNodesAndClients = 0;
    while (values.hasNext() && numNodesAndClients++ < size - n) {
      NodeAndClient next = values.next();
      nodesToRemove.add(next);
      // removeDisruptionSchemeFromNode(next);
      next.close();
    }
    for (NodeAndClient toRemove : nodesToRemove) {
      nodes.remove(toRemove.name);
    }
    if (!nodesToRemove.isEmpty() && size() > 0) {
      assertNoTimeout(client().admin().cluster().prepareHealth().setWaitForNodes(Integer.toString(nodes.size())).get());
    }
  }

  private NodeAndClient buildNode(Settings settings) {
    int ord = nextNodeId.getAndIncrement();
    return buildNode(ord, random.nextLong(), settings, false);
  }

  private NodeAndClient buildNode() {
    int ord = nextNodeId.getAndIncrement();
    return buildNode(ord, random.nextLong(), null, false);
  }

  private NodeAndClient buildNode(int nodeId, long seed, Settings settings, boolean reuseExisting) {
    assert Thread.holdsLock(this);
    ensureOpen();
    settings = getSettings(nodeId, seed, settings);
    Collection<Class<? extends Plugin>> plugins = getPlugins();
    String name = buildNodeName(nodeId, settings);
    if (reuseExisting && nodes.containsKey(name)) {
      return nodes.get(name);
    } else {
      assert reuseExisting == true || nodes.containsKey(name) == false : "node name [" + name + "] already exists but not allowed to use it";
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

  private String buildNodeName(int id, Settings settings) {
    String prefix = nodePrefix;
    prefix = prefix + getRoleSuffix(settings);
    return prefix + id;
  }

  /**
   * returns a suffix string based on the node role. If no explicit role is defined, the suffix will be empty
   */
  private String getRoleSuffix(Settings settings) {
    String suffix = "";
    if (Node.NODE_MASTER_SETTING.exists(settings) && Node.NODE_MASTER_SETTING.get(settings)) {
      suffix = suffix + Role.MASTER.getAbbreviation();
    }
    if (Node.NODE_DATA_SETTING.exists(settings) && Node.NODE_DATA_SETTING.get(settings)) {
      suffix = suffix + Role.DATA.getAbbreviation();
    }
    if (Node.NODE_MASTER_SETTING.exists(settings) && Node.NODE_MASTER_SETTING.get(settings) == false &&
      Node.NODE_DATA_SETTING.exists(settings) && Node.NODE_DATA_SETTING.get(settings) == false) {
      suffix = suffix + "c";
    }
    return suffix;
  }

  /**
   * Returns the common node name prefix for this test cluster.
   */
  public String nodePrefix() {
    return nodePrefix;
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
   * Returns a node client to a data node in the cluster.
   * Note: use this with care tests should not rely on a certain nodes client.
   */
  public synchronized Client dataNodeClient() {
    ensureOpen();
    /* Randomly return a client to one of the nodes in the cluster */
    return getRandomNodeAndClient(new DataNodePredicate()).client(random);
  }

  /**
   * Returns a node client to the current master node.
   * Note: use this with care tests should not rely on a certain nodes client.
   */
  public synchronized Client masterClient() {
    ensureOpen();
    NodeAndClient randomNodeAndClient = getRandomNodeAndClient(new MasterNodePredicate(getMasterName()));
    if (randomNodeAndClient != null) {
      return randomNodeAndClient.nodeClient(); // ensure node client master is requested
    }
    Assert.fail("No master client found");
    return null; // can't happen
  }

  /**
   * Returns a node client to random node but not the master. This method will fail if no non-master client is available.
   */
  public synchronized Client nonMasterClient() {
    ensureOpen();
    NodeAndClient randomNodeAndClient = getRandomNodeAndClient(new MasterNodePredicate(getMasterName()).negate());
    if (randomNodeAndClient != null) {
      return randomNodeAndClient.nodeClient(); // ensure node client non-master is requested
    }
    Assert.fail("No non-master client found");
    return null; // can't happen
  }

  /**
   * Returns a client to a coordinating only node
   */
  public synchronized Client coordOnlyNodeClient() {
    ensureOpen();
    NodeAndClient randomNodeAndClient = getRandomNodeAndClient(new NoDataNoMasterNodePredicate());
    if (randomNodeAndClient != null) {
      return randomNodeAndClient.client(random);
    }
    int nodeId = nextNodeId.getAndIncrement();
    Settings settings = getSettings(nodeId, random.nextLong(), Settings.EMPTY);
    startCoordinatingOnlyNode(settings);
    return getRandomNodeAndClient(new NoDataNoMasterNodePredicate()).client(random);
  }

  private synchronized String startCoordinatingOnlyNode(Settings settings) {
    ensureOpen(); // currently unused
    Builder builder = Settings.builder().put(settings).put(Node.NODE_MASTER_SETTING.getKey(), false)
      .put(Node.NODE_DATA_SETTING.getKey(), false).put(Node.NODE_INGEST_SETTING.getKey(), false);
    if (size() == 0) {
      // if we are the first node - don't wait for a state
      builder.put(DiscoverySettings.INITIAL_STATE_TIMEOUT_SETTING.getKey(), 0);
    }
    return startNode(builder);
  }

  /**
   * Returns a transport client
   */
  public synchronized Client transportClient() {
    ensureOpen();
    // randomly return a transport client going to one of the nodes in the cluster
    return getOrBuildRandomNode().transportClient();
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
   * Returns a "smart" node client to a random node in the cluster
   */
  public synchronized Client smartClient() {
    NodeAndClient randomNodeAndClient = getRandomNodeAndClient();
    if (randomNodeAndClient != null) {
      return randomNodeAndClient.nodeClient();
    }
    Assert.fail("No smart client found");
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

    public int nodeAndClientId() {
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

    Client transportClient() {
      if (closed.get()) {
        throw new RuntimeException("already closed");
      }
      return getOrBuildTransportClient();
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
        transportClient = new TransportClientFactory(false, nodeConfigurationSource.transportClientSettings(), baseDir, nodeConfigurationSource.transportClientPlugins())
          .client(node, clusterName);
      }
      return clientWrapper.apply(transportClient);
    }

    void resetClient() throws IOException {
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

    void restart(RestartCallback callback, boolean clearDataIfNeeded) throws Exception {
      assert callback != null;
      resetClient();
      if (!node.isClosed()) {
        closeNode();
      }
      Settings newSettings = callback.onNodeStopped(name);
      if (newSettings == null) {
        newSettings = Settings.EMPTY;
      }
      if (clearDataIfNeeded) {
        clearDataIfNeeded(callback);
      }
      createNewNode(newSettings);
      startNode();
    }

    private void clearDataIfNeeded(RestartCallback callback) throws IOException {
      if (callback.clearData(name)) {
        NodeEnvironment nodeEnv = node.getNodeEnvironment();
        if (nodeEnv.hasNodeFile()) {
          final Path[] locations = nodeEnv.nodeDataPaths();
          logger.debug("removing node data paths: [{}]", Arrays.toString(locations));
          IOUtils.rm(locations);
        }
      }
    }

    private void createNewNode(final Settings newSettings) {
      final long newIdSeed = NodeEnvironment.NODE_ID_SEED_SETTING.get(node.settings()) + 1; // use a new seed to make sure we have new node id
      Settings finalSettings = Settings.builder().put(node.settings()).put(newSettings).put(NodeEnvironment.NODE_ID_SEED_SETTING.getKey(), newIdSeed).build();
      Collection<Class<? extends Plugin>> plugins = node.getClasspathPlugins();
      node = new MockNode(finalSettings, plugins);
      markNodeDataDirsAsNotEligableForWipe(node);
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

  public static final String TRANSPORT_CLIENT_PREFIX = "transport_client_";

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

  public synchronized void beforeTest(Random random, double transportClientRatio) throws IOException, InterruptedException {
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
    assert newSize == numSharedDedicatedMasterNodes + numSharedDataNodes + numSharedCoordOnlyNodes;
    for (int i = 0; i < numSharedDedicatedMasterNodes; i++) {
      final Settings.Builder settings = Settings.builder();
      settings.put(Node.NODE_MASTER_SETTING.getKey(), true).build();
      settings.put(Node.NODE_DATA_SETTING.getKey(), false).build();
      NodeAndClient nodeAndClient = buildNode(i, sharedNodesSeeds[i], settings.build(), true);
      nodeAndClient.startNode();
      publishNode(nodeAndClient);
    }
    for (int i = numSharedDedicatedMasterNodes; i < numSharedDedicatedMasterNodes + numSharedDataNodes; i++) {
      final Settings.Builder settings = Settings.builder();
      if (numSharedDedicatedMasterNodes > 0) {
        // if we don't have dedicated master nodes, keep things default
        settings.put(Node.NODE_MASTER_SETTING.getKey(), false).build();
        settings.put(Node.NODE_DATA_SETTING.getKey(), true).build();
      }
      NodeAndClient nodeAndClient = buildNode(i, sharedNodesSeeds[i], settings.build(), true);
      nodeAndClient.startNode();
      publishNode(nodeAndClient);
    }
    for (int i = numSharedDedicatedMasterNodes + numSharedDataNodes; i < numSharedDedicatedMasterNodes + numSharedDataNodes + numSharedCoordOnlyNodes; i++) {
      final Builder settings = Settings.builder().put(Node.NODE_MASTER_SETTING.getKey(), false)
        .put(Node.NODE_DATA_SETTING.getKey(), false).put(Node.NODE_INGEST_SETTING.getKey(), false);
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
   * Returns a reference to a random node's {@link ClusterService}
   */
  public ClusterService clusterService() {
    return clusterService(null);
  }

  /**
   * Returns a reference to a node's {@link ClusterService}. If the given node is null, a random node will be selected.
   */
  public synchronized ClusterService clusterService(@Nullable String node) {
    return getInstance(ClusterService.class, node);
  }

  /**
   * Returns an Iterable to all instances for the given class &gt;T&lt; across all nodes in the cluster.
   */
  public synchronized <T> Iterable<T> getInstances(Class<T> clazz) {
    List<T> instances = new ArrayList<>(nodes.size());
    for (NodeAndClient nodeAndClient : nodes.values()) {
      instances.add(getInstanceFromNode(clazz, nodeAndClient.node));
    }
    return instances;
  }

  /**
   * Returns an Iterable to all instances for the given class &gt;T&lt; across all data nodes in the cluster.
   */
  public synchronized <T> Iterable<T> getDataNodeInstances(Class<T> clazz) {
    return getInstances(clazz, new DataNodePredicate());
  }

  /**
   * Returns an Iterable to all instances for the given class &gt;T&lt; across all data and master nodes
   * in the cluster.
   */
  public synchronized <T> Iterable<T> getDataOrMasterNodeInstances(Class<T> clazz) {
    return getInstances(clazz, new DataOrMasterNodePredicate());
  }

  private synchronized <T> Iterable<T> getInstances(Class<T> clazz, Predicate<NodeAndClient> predicate) {
    Iterable<NodeAndClient> filteredNodes = nodes.values().stream().filter(predicate)::iterator;
    List<T> instances = new ArrayList<>();
    for (NodeAndClient nodeAndClient : filteredNodes) {
      instances.add(getInstanceFromNode(clazz, nodeAndClient.node));
    }
    return instances;
  }

  /**
   * Returns a reference to the given nodes instances of the given class &gt;T&lt;
   */
  public synchronized <T> T getInstance(Class<T> clazz, final String node) {
    return getInstance(clazz, nc -> node == null || node.equals(nc.name));
  }

  public synchronized <T> T getDataNodeInstance(Class<T> clazz) {
    return getInstance(clazz, new DataNodePredicate());
  }

  private synchronized <T> T getInstance(Class<T> clazz, Predicate<NodeAndClient> predicate) {
    NodeAndClient randomNodeAndClient = getRandomNodeAndClient(predicate);
    assert randomNodeAndClient != null;
    return getInstanceFromNode(clazz, randomNodeAndClient.node);
  }

  /**
   * Returns a reference to a random nodes instances of the given class &gt;T&lt;
   */
  public synchronized <T> T getInstance(Class<T> clazz) {
    return getInstance(clazz, nc -> true);
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

  /**
   * Returns the http addresses of the nodes within the cluster.
   * Can be used to run REST tests against the test cluster.
   */
  public InetSocketAddress[] httpAddresses() {
    List<InetSocketAddress> addresses = new ArrayList<>();
    for (HttpServerTransport httpServerTransport : getInstances(HttpServerTransport.class)) {
      addresses.add(((InetSocketTransportAddress) httpServerTransport.boundAddress().publishAddress()).address());
    }
    return addresses.toArray(new InetSocketAddress[addresses.size()]);
  }

  /**
   * Stops a random data node in the cluster. Returns true if a node was found to stop, false otherwise.
   */
  public synchronized boolean stopRandomDataNode() throws IOException {
    ensureOpen();
    NodeAndClient nodeAndClient = getRandomNodeAndClient(new DataNodePredicate());
    if (nodeAndClient != null) {
      logger.info("Closing random node [{}] ", nodeAndClient.name);
      // removeDisruptionSchemeFromNode(nodeAndClient);
      nodes.remove(nodeAndClient.name);
      nodeAndClient.close();
      return true;
    }
    return false;
  }

  /**
   * Stops a random node in the cluster that applies to the given filter or non if the non of the nodes applies to the
   * filter.
   */
  public synchronized void stopRandomNode(final Predicate<Settings> filter) throws IOException {
    ensureOpen();
    NodeAndClient nodeAndClient = getRandomNodeAndClient(nc -> filter.test(nc.node.settings()));
    if (nodeAndClient != null) {
      logger.info("Closing filtered random node [{}] ", nodeAndClient.name);
      // removeDisruptionSchemeFromNode(nodeAndClient);
      nodes.remove(nodeAndClient.name);
      nodeAndClient.close();
    }
  }

  /**
   * Stops the current master node forcefully
   */
  public synchronized void stopCurrentMasterNode() throws IOException {
    ensureOpen();
    assert size() > 0;
    String masterNodeName = getMasterName();
    assert nodes.containsKey(masterNodeName);
    logger.info("Closing master node [{}] ", masterNodeName);
    // removeDisruptionSchemeFromNode(nodes.get(masterNodeName));
    NodeAndClient remove = nodes.remove(masterNodeName);
    remove.close();
  }

  /**
   * Stops any of the current nodes but not the master node.
   */
  public synchronized void stopRandomNonMasterNode() throws IOException {
    NodeAndClient nodeAndClient = getRandomNodeAndClient(new MasterNodePredicate(getMasterName()).negate());
    if (nodeAndClient != null) {
      logger.info("Closing random non master node [{}] current master [{}] ", nodeAndClient.name, getMasterName());
      // removeDisruptionSchemeFromNode(nodeAndClient);
      nodes.remove(nodeAndClient.name);
      nodeAndClient.close();
    }
  }

  /**
   * Restarts a random node in the cluster
   */
  public void restartRandomNode() throws Exception {
    restartRandomNode(EMPTY_CALLBACK);
  }

  /**
   * Restarts a random node in the cluster and calls the callback during restart.
   */
  public void restartRandomNode(RestartCallback callback) throws Exception {
    restartRandomNode(nc -> true, callback);
  }

  /**
   * Restarts a random data node in the cluster
   */
  public void restartRandomDataNode() throws Exception {
    restartRandomDataNode(EMPTY_CALLBACK);
  }

  /**
   * Restarts a random data node in the cluster and calls the callback during restart.
   */
  public void restartRandomDataNode(RestartCallback callback) throws Exception {
    restartRandomNode(new DataNodePredicate(), callback);
  }

  /**
   * Restarts a random node in the cluster and calls the callback during restart.
   */
  private synchronized void restartRandomNode(Predicate<NodeAndClient> predicate, RestartCallback callback) throws Exception {
    ensureOpen();
    NodeAndClient nodeAndClient = getRandomNodeAndClient(predicate);
    if (nodeAndClient != null) {
      logger.info("Restarting random node [{}] ", nodeAndClient.name);
      nodeAndClient.restart(callback, true);
    }
  }

  /**
   * Restarts a node and calls the callback during restart.
   */
  public synchronized void restartNode(String nodeName, RestartCallback callback) throws Exception {
    ensureOpen();
    NodeAndClient nodeAndClient = nodes.get(nodeName);
    if (nodeAndClient != null) {
      logger.info("Restarting node [{}] ", nodeAndClient.name);
      nodeAndClient.restart(callback, true);
    }
  }

  private synchronized void restartAllNodes(boolean rollingRestart, RestartCallback callback) throws Exception {
    ensureOpen();
    List<NodeAndClient> toRemove = new ArrayList<>();
    try {
      for (NodeAndClient nodeAndClient : nodes.values()) {
        if (!callback.doRestart(nodeAndClient.name)) {
          logger.info("Closing node [{}] during restart", nodeAndClient.name);
          toRemove.add(nodeAndClient);
          // if (activeDisruptionScheme != null) {
          // activeDisruptionScheme.removeFromNode(nodeAndClient.name, this);
          // }
          nodeAndClient.close();
        }
      }
    } finally {
      for (NodeAndClient nodeAndClient : toRemove) {
        nodes.remove(nodeAndClient.name);
      }
    }
    logger.info("Restarting remaining nodes rollingRestart [{}]", rollingRestart);
    if (rollingRestart) {
      int numNodesRestarted = 0;
      for (NodeAndClient nodeAndClient : nodes.values()) {
        callback.doAfterNodes(numNodesRestarted++, nodeAndClient.nodeClient());
        logger.info("Restarting node [{}] ", nodeAndClient.name);
        // if (activeDisruptionScheme != null) {
        // activeDisruptionScheme.removeFromNode(nodeAndClient.name, this);
        // }
        nodeAndClient.restart(callback, true);
        // if (activeDisruptionScheme != null) {
        // activeDisruptionScheme.applyToNode(nodeAndClient.name, this);
        // }
      }
    } else {
      int numNodesRestarted = 0;
      Set[] nodesRoleOrder = new Set[nextNodeId.get()];
      Map<Set<Role>, List<NodeAndClient>> nodesByRoles = new HashMap<>();
      for (NodeAndClient nodeAndClient : nodes.values()) {
        callback.doAfterNodes(numNodesRestarted++, nodeAndClient.nodeClient());
        logger.info("Stopping node [{}] ", nodeAndClient.name);
        // if (activeDisruptionScheme != null) {
        // activeDisruptionScheme.removeFromNode(nodeAndClient.name, this);
        // }
        nodeAndClient.closeNode();
        // delete data folders now, before we start other nodes that may claim it
        nodeAndClient.clearDataIfNeeded(callback);

        DiscoveryNode discoveryNode = getInstanceFromNode(ClusterService.class, nodeAndClient.node()).localNode();
        nodesRoleOrder[nodeAndClient.nodeAndClientId()] = discoveryNode.getRoles();
        nodesByRoles.computeIfAbsent(discoveryNode.getRoles(), k -> new ArrayList<>()).add(nodeAndClient);
      }

      assert nodesByRoles.values().stream().collect(Collectors.summingInt(List::size)) == nodes.size();

      // randomize start up order, but making sure that:
      // 1) A data folder that was assigned to a data node will stay so
      // 2) Data nodes will get the same node lock ordinal range, so custom index paths (where the ordinal is used)
      // will still belong to data nodes
      for (List<NodeAndClient> sameRoleNodes : nodesByRoles.values()) {
        Collections.shuffle(sameRoleNodes, random);
      }

      for (Set roles : nodesRoleOrder) {
        if (roles == null) {
          // if some nodes were stopped, we want have a role for them
          continue;
        }
        NodeAndClient nodeAndClient = nodesByRoles.get(roles).remove(0);
        logger.info("Starting node [{}] ", nodeAndClient.name);
        // if (activeDisruptionScheme != null) {
        // activeDisruptionScheme.removeFromNode(nodeAndClient.name, this);
        // }
        // we already cleared data folders, before starting nodes up
        nodeAndClient.restart(callback, false);
        // if (activeDisruptionScheme != null) {
        // activeDisruptionScheme.applyToNode(nodeAndClient.name, this);
        // }
      }
    }
  }

  public static final RestartCallback EMPTY_CALLBACK = new RestartCallback() {
    @Override
    public Settings onNodeStopped(String node) {
      return null;
    }
  };

  /**
   * Restarts all nodes in the cluster. It first stops all nodes and then restarts all the nodes again.
   */
  public void fullRestart() throws Exception {
    fullRestart(EMPTY_CALLBACK);
  }

  /**
   * Restarts all nodes in a rolling restart fashion ie. only restarts on node a time.
   */
  public void rollingRestart() throws Exception {
    rollingRestart(EMPTY_CALLBACK);
  }

  /**
   * Restarts all nodes in a rolling restart fashion ie. only restarts on node a time.
   */
  public void rollingRestart(RestartCallback function) throws Exception {
    restartAllNodes(true, function);
  }

  /**
   * Restarts all nodes in the cluster. It first stops all nodes and then restarts all the nodes again.
   */
  public void fullRestart(RestartCallback function) throws Exception {
    restartAllNodes(false, function);
  }

  /**
   * Returns the name of the current master node in the cluster.
   */
  public String getMasterName() {
    return getMasterName(null);
  }

  /**
   * Returns the name of the current master node in the cluster and executes the request via the node specified
   * in the viaNode parameter. If viaNode isn't specified a random node will be picked to the send the request to.
   */
  public String getMasterName(@Nullable String viaNode) {
    try {
      Client client = viaNode != null ? client(viaNode) : client();
      ClusterState state = client.admin().cluster().prepareState().execute().actionGet().getState();
      return state.nodes().getMasterNode().getName();
    } catch (Exception e) {
      logger.warn("Can't fetch cluster state", e);
      throw new RuntimeException("Can't get master node " + e.getMessage(), e);
    }
  }

  synchronized Set<String> allDataNodesButN(int numNodes) {
    return nRandomDataNodes(numDataNodes() - numNodes);
  }

  private synchronized Set<String> nRandomDataNodes(int numNodes) {
    assert size() >= numNodes;
    Map<String, NodeAndClient> dataNodes = nodes
      .entrySet()
      .stream()
      .filter(new EntryNodePredicate(new DataNodePredicate()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    final HashSet<String> set = new HashSet<>();
    final Iterator<String> iterator = dataNodes.keySet().iterator();
    for (int i = 0; i < numNodes; i++) {
      assert iterator.hasNext();
      set.add(iterator.next());
    }
    return set;
  }

  /**
   * Returns a set of nodes that have at least one shard of the given index.
   */
  public synchronized Set<String> nodesInclude(String index) {
    if (clusterService().state().routingTable().hasIndex(index)) {
      List<ShardRouting> allShards = clusterService().state().routingTable().allShards(index);
      DiscoveryNodes discoveryNodes = clusterService().state().getNodes();
      Set<String> nodes = new HashSet<>();
      for (ShardRouting shardRouting : allShards) {
        if (shardRouting.assignedToNode()) {
          DiscoveryNode discoveryNode = discoveryNodes.get(shardRouting.currentNodeId());
          nodes.add(discoveryNode.getName());
        }
      }
      return nodes;
    }
    return Collections.emptySet();
  }

  /**
   * Starts a node with default settings and returns it's name.
   */
  public synchronized String startNode() {
    return startNode(Settings.EMPTY);
  }

  /**
   * Starts a node with the given settings builder and returns it's name.
   */
  public synchronized String startNode(Settings.Builder settings) {
    return startNode(settings.build());
  }

  /**
   * Starts a node with the given settings and returns it's name.
   */
  public synchronized String startNode(Settings settings) {
    NodeAndClient buildNode = buildNode(settings);
    buildNode.startNode();
    publishNode(buildNode);
    return buildNode.name;
  }

  public synchronized Async<List<String>> startMasterOnlyNodesAsync(int numNodes) {
    return startMasterOnlyNodesAsync(numNodes, Settings.EMPTY);
  }

  public synchronized Async<List<String>> startMasterOnlyNodesAsync(int numNodes, Settings settings) {
    Settings settings1 = Settings.builder().put(settings).put(Node.NODE_MASTER_SETTING.getKey(), true).put(Node.NODE_DATA_SETTING.getKey(), false).build();
    return startNodesAsync(numNodes, settings1, Version.CURRENT);
  }

  public synchronized Async<List<String>> startDataOnlyNodesAsync(int numNodes) {
    return startDataOnlyNodesAsync(numNodes, Settings.EMPTY);
  }

  public synchronized Async<List<String>> startDataOnlyNodesAsync(int numNodes, Settings settings) {
    Settings settings1 = Settings.builder().put(settings).put(Node.NODE_MASTER_SETTING.getKey(), false).put(Node.NODE_DATA_SETTING.getKey(), true).build();
    return startNodesAsync(numNodes, settings1, Version.CURRENT);
  }

  public synchronized Async<String> startMasterOnlyNodeAsync() {
    return startMasterOnlyNodeAsync(Settings.EMPTY);
  }

  public synchronized Async<String> startMasterOnlyNodeAsync(Settings settings) {
    Settings settings1 = Settings.builder().put(settings).put(Node.NODE_MASTER_SETTING.getKey(), true).put(Node.NODE_DATA_SETTING.getKey(), false).build();
    return startNodeAsync(settings1, Version.CURRENT);
  }

  public synchronized String startMasterOnlyNode(Settings settings) {
    Settings settings1 = Settings.builder().put(settings).put(Node.NODE_MASTER_SETTING.getKey(), true).put(Node.NODE_DATA_SETTING.getKey(), false).build();
    return startNode(settings1);
  }

  public synchronized Async<String> startDataOnlyNodeAsync() {
    return startDataOnlyNodeAsync(Settings.EMPTY);
  }

  public synchronized Async<String> startDataOnlyNodeAsync(Settings settings) {
    Settings settings1 = Settings.builder().put(settings).put(Node.NODE_MASTER_SETTING.getKey(), false).put(Node.NODE_DATA_SETTING.getKey(), true).build();
    return startNodeAsync(settings1, Version.CURRENT);
  }

  public synchronized String startDataOnlyNode(Settings settings) {
    Settings settings1 = Settings.builder().put(settings).put(Node.NODE_MASTER_SETTING.getKey(), false).put(Node.NODE_DATA_SETTING.getKey(), true).build();
    return startNode(settings1);
  }

  /**
   * Starts a node in an async manner with the given settings and returns future with its name.
   */
  public synchronized Async<String> startNodeAsync() {
    return startNodeAsync(Settings.EMPTY, Version.CURRENT);
  }

  /**
   * Starts a node in an async manner with the given settings and returns future with its name.
   */
  public synchronized Async<String> startNodeAsync(final Settings settings) {
    return startNodeAsync(settings, Version.CURRENT);
  }

  /**
   * Starts a node in an async manner with the given settings and version and returns future with its name.
   */
  public synchronized Async<String> startNodeAsync(final Settings settings, final Version version) {
    final NodeAndClient buildNode = buildNode(settings);
    final Future<String> submit = executor.submit(() -> {
      buildNode.startNode();
      publishNode(buildNode);
      return buildNode.name;
    });
    return () -> submit.get();
  }

  /**
   * Starts multiple nodes in an async manner and returns future with its name.
   */
  public synchronized Async<List<String>> startNodesAsync(final int numNodes) {
    return startNodesAsync(numNodes, Settings.EMPTY, Version.CURRENT);
  }

  /**
   * Starts multiple nodes in an async manner with the given settings and returns future with its name.
   */
  public synchronized Async<List<String>> startNodesAsync(final int numNodes, final Settings settings) {
    return startNodesAsync(numNodes, settings, Version.CURRENT);
  }

  /**
   * Starts multiple nodes in an async manner with the given settings and version and returns future with its name.
   */
  public synchronized Async<List<String>> startNodesAsync(final int numNodes, final Settings settings, final Version version) {
    final List<Async<String>> asyncs = new ArrayList<>();
    for (int i = 0; i < numNodes; i++) {
      asyncs.add(startNodeAsync(settings, version));
    }

    return () -> {
      List<String> ids = new ArrayList<>();
      for (Async<String> async : asyncs) {
        ids.add(async.get());
      }
      return ids;
    };
  }

  /**
   * Starts multiple nodes (based on the number of settings provided) in an async manner, with explicit settings for each node.
   * The order of the node names returned matches the order of the settings provided.
   */
  public synchronized Async<List<String>> startNodesAsync(final Settings... settings) {
    List<Async<String>> asyncs = new ArrayList<>();
    for (Settings setting : settings) {
      asyncs.add(startNodeAsync(setting, Version.CURRENT));
    }
    return () -> {
      List<String> ids = new ArrayList<>();
      for (Async<String> async : asyncs) {
        ids.add(async.get());
      }
      return ids;
    };
  }

  private synchronized void publishNode(NodeAndClient nodeAndClient) {
    assert !nodeAndClient.node().isClosed();
    nodes.put(nodeAndClient.name, nodeAndClient);
    // applyDisruptionSchemeToNode(nodeAndClient);
  }

  public void closeNonSharedNodes(boolean wipeData) throws IOException {
    reset(wipeData);
  }

  /**
   * Returns the number of data nodes in the cluster.
   */
  public int numDataNodes() {
    return dataNodeAndClients().size();
  }

  /**
   * Returns the number of data and master eligible nodes in the cluster.
   */
  public int numDataAndMasterNodes() {
    return dataAndMasterNodes().size();
  }

  // public void setDisruptionScheme(ServiceDisruptionScheme scheme) {
  // clearDisruptionScheme();
  // scheme.applyToCluster(this);
  // activeDisruptionScheme = scheme;
  // }
  //
  // public void clearDisruptionScheme() {
  // clearDisruptionScheme(true);
  // }
  //
  // public void clearDisruptionScheme(boolean ensureHealthyCluster) {
  // if (activeDisruptionScheme != null) {
  // TimeValue expectedHealingTime = activeDisruptionScheme.expectedTimeToHeal();
  // logger.info("Clearing active scheme {}, expected healing time {}", activeDisruptionScheme, expectedHealingTime);
  // if (ensureHealthyCluster) {
  // activeDisruptionScheme.removeAndEnsureHealthy(this);
  // } else {
  // activeDisruptionScheme.removeFromCluster(this);
  // }
  // }
  // activeDisruptionScheme = null;
  // }
  //
  // private void applyDisruptionSchemeToNode(NodeAndClient nodeAndClient) {
  // if (activeDisruptionScheme != null) {
  // assert nodes.containsKey(nodeAndClient.name);
  // activeDisruptionScheme.applyToNode(nodeAndClient.name, this);
  // }
  // }
  //
  // private void removeDisruptionSchemeFromNode(NodeAndClient nodeAndClient) {
  // if (activeDisruptionScheme != null) {
  // assert nodes.containsKey(nodeAndClient.name);
  // activeDisruptionScheme.removeFromNode(nodeAndClient.name, this);
  // }
  // }

  private synchronized Collection<NodeAndClient> dataNodeAndClients() {
    return filterNodes(nodes, new DataNodePredicate());
  }

  private synchronized Collection<NodeAndClient> dataAndMasterNodes() {
    return filterNodes(nodes, new DataOrMasterNodePredicate());
  }

  private synchronized Collection<NodeAndClient> filterNodes(Map<String, EsTestCluster.NodeAndClient> map, Predicate<NodeAndClient> predicate) {
    return map
      .values()
      .stream()
      .filter(predicate)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private static final class DataNodePredicate implements Predicate<NodeAndClient> {
    @Override
    public boolean test(NodeAndClient nodeAndClient) {
      return DiscoveryNode.isDataNode(nodeAndClient.node.settings());
    }
  }

  private static final class DataOrMasterNodePredicate implements Predicate<NodeAndClient> {
    @Override
    public boolean test(NodeAndClient nodeAndClient) {
      return DiscoveryNode.isDataNode(nodeAndClient.node.settings()) ||
        DiscoveryNode.isMasterNode(nodeAndClient.node.settings());
    }
  }

  private static final class MasterNodePredicate implements Predicate<NodeAndClient> {
    private final String masterNodeName;

    public MasterNodePredicate(String masterNodeName) {
      this.masterNodeName = masterNodeName;
    }

    @Override
    public boolean test(NodeAndClient nodeAndClient) {
      return masterNodeName.equals(nodeAndClient.name);
    }
  }

  private static final class NoDataNoMasterNodePredicate implements Predicate<NodeAndClient> {
    @Override
    public boolean test(NodeAndClient nodeAndClient) {
      return DiscoveryNode.isMasterNode(nodeAndClient.node.settings()) == false &&
        DiscoveryNode.isDataNode(nodeAndClient.node.settings()) == false;
    }
  }

  private static final class EntryNodePredicate implements Predicate<Map.Entry<String, NodeAndClient>> {
    private final Predicate<NodeAndClient> delegateNodePredicate;

    EntryNodePredicate(Predicate<NodeAndClient> delegateNodePredicate) {
      this.delegateNodePredicate = delegateNodePredicate;
    }

    @Override
    public boolean test(Map.Entry<String, NodeAndClient> entry) {
      return delegateNodePredicate.test(entry.getValue());
    }
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
   * Returns a predicate that only accepts settings of nodes with one of the given names.
   */
  public static Predicate<Settings> nameFilter(String... nodeName) {
    return new NodeNamePredicate(new HashSet<>(Arrays.asList(nodeName)));
  }

  private static final class NodeNamePredicate implements Predicate<Settings> {
    private final HashSet<String> nodeNames;

    public NodeNamePredicate(HashSet<String> nodeNames) {
      this.nodeNames = nodeNames;
    }

    @Override
    public boolean test(Settings settings) {
      return nodeNames.contains(settings.get("node.name"));

    }
  }

  /**
   * An abstract class that is called during {@link #rollingRestart(EsTestCluster.RestartCallback)}
   * and / or {@link #fullRestart(EsTestCluster.RestartCallback)} to execute actions at certain
   * stages of the restart.
   */
  public static class RestartCallback {

    /**
     * Executed once the give node name has been stopped.
     */
    public Settings onNodeStopped(String nodeName) throws Exception {
      return Settings.EMPTY;
    }

    /**
     * Executed for each node before the <tt>n+1</tt> node is restarted. The given client is
     * an active client to the node that will be restarted next.
     */
    public void doAfterNodes(int n, Client client) throws Exception {
    }

    /**
     * If this returns <code>true</code> all data for the node with the given node name will be cleared including
     * gateways and all index data. Returns <code>false</code> by default.
     */
    public boolean clearData(String nodeName) {
      return false;
    }

    /**
     * If this returns <code>false</code> the node with the given node name will not be restarted. It will be
     * closed and removed from the cluster. Returns <code>true</code> by default.
     */
    public boolean doRestart(String nodeName) {
      return true;
    }
  }

  public Settings getDefaultSettings() {
    return defaultSettings;
  }

  /**
   * Ensures that any breaker statistics are reset to 0.
   *
   * The implementation is specific to the test cluster, because the act of
   * checking some breaker stats can increase them.
   */
  public void ensureEstimatedStats() {
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
          EsTester.assertBusy(new Runnable() {
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
  public void assertAfterTest() throws IOException {
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
          EsTester.assertBusy(() -> {
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

}
