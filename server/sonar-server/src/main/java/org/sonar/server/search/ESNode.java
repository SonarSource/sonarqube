/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.search;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsNodeResponse;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsNodes;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.StopWatch;
import org.sonar.server.search.es.ListUpdate;
import org.sonar.server.search.es.ListUpdate.UpdateListScriptFactory;

import javax.annotation.CheckForNull;

import java.io.File;
import java.net.InetAddress;

/**
 * ElasticSearch Node used to connect to index.
 */
public class ESNode implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(ESNode.class);
  private static final String HTTP_ENABLED = "http.enabled";
  private static final String DEFAULT_HEALTH_TIMEOUT = "30s";

  private final Settings settings;
  private final String healthTimeout;

  // available only after startup
  private Client client;
  private Node node;
  protected final Profiling profiling;

  public ESNode(Settings settings) {
    this(settings, DEFAULT_HEALTH_TIMEOUT);
  }

  @VisibleForTesting
  ESNode(Settings settings, String healthTimeout) {
    this.settings = settings;
    this.healthTimeout = healthTimeout;
    this.profiling = new Profiling(settings);
  }

  @Override
  public void start() {
    initLogging();

    String typeValue = settings.getString(IndexProperties.TYPE);
    IndexProperties.ES_TYPE type =
      typeValue != null ?
        IndexProperties.ES_TYPE.valueOf(typeValue) :
        IndexProperties.ES_TYPE.DATA;

    ImmutableSettings.Builder esSettings = ImmutableSettings.settingsBuilder()
      .put("index.merge.policy.max_merge_at_once", "200")
      .put("index.merge.policy.segments_per_tier", "200")

      .put("indices.store.throttle.type", "merge")
      .put("indices.store.throttle.max_bytes_per_sec", "200mb")

      .put("script.default_lang", "native")
      .put("script.native." + ListUpdate.NAME + ".type", UpdateListScriptFactory.class.getName())

      .put("cluster.name", StringUtils.defaultIfBlank(settings.getString(IndexProperties.CLUSTER_NAME), "sonarqube"))
      .put("path.home", esHomeDir().getAbsolutePath());


    initAnalysis(esSettings);

    if (IndexProperties.ES_TYPE.TRANSPORT.equals(type)) {
      initRemoteClient(esSettings);
    } else {
      initLocalClient(type, esSettings);
    }

    if (getClusterHealthStatus() == ClusterHealthStatus.RED) {
      throw new IllegalStateException(String.format("Elasticsearch index is corrupt, please delete directory '%s' " +
        "and relaunch the SonarQube server.", esDataDir()));
    }

    addIndexTemplates();

    LOG.info("Elasticsearch started");
  }

  public NodeHealth getNodeHealth() {
    NodeHealth health = new NodeHealth();
    ClusterStatsResponse clusterStatsResponse = client().admin().cluster().prepareClusterStats().get();

    // Cluster health
    health.setClusterAvailable(clusterStatsResponse.getStatus() != ClusterHealthStatus.RED);

    ClusterStatsNodes nodesStats = clusterStatsResponse.getNodesStats();

    // JVM Heap Usage
    health.setJvmHeapMax(nodesStats.getJvm().getHeapMax().bytes());
    health.setJvmHeapUsed(nodesStats.getJvm().getHeapUsed().bytes());

    // OS Memory Usage ?

    // Disk Usage
    health.setFsTotal(nodesStats.getFs().getTotal().bytes());
    health.setFsAvailable(nodesStats.getFs().getAvailable().bytes());

    // Ping ?

    // Threads
    health.setJvmThreads(nodesStats.getJvm().getThreads());

    // CPU
    health.setProcessCpuPercent(nodesStats.getProcess().getCpuPercent());

    // Open Files
    health.setOpenFiles(nodesStats.getProcess().getAvgOpenFileDescriptors());

    // Uptime
    health.setJvmUptimeMillis(nodesStats.getJvm().getMaxUpTime().getMillis());

    return health;
  }

  private ClusterHealthStatus getClusterHealthStatus() {
    return client.admin().cluster().prepareHealth()
      .setWaitForYellowStatus()
      .setTimeout(healthTimeout)
      .get()
      .getStatus();
  }

  private void initRemoteClient(ImmutableSettings.Builder esSettings) {
    int port = settings.getInt(IndexProperties.NODE_PORT);
    client = new TransportClient(esSettings)
      .addTransportAddress(new InetSocketTransportAddress("localhost",
        port));
    LOG.info("Elasticsearch port: " + port);
  }

  @CheckForNull
  private NodeInfo getLocalNodeInfoByHostName(String hostname) {
    for (ClusterStatsNodeResponse nodeResp : client().admin().cluster().prepareClusterStats().get().getNodes()) {
      if (hostname.equals(nodeResp.nodeInfo().getHostname())) {
        return nodeResp.nodeInfo();
      }
    }
    return null;
  }

  @CheckForNull
  private NodeInfo getLocalNodeInfoByNodeName(String nodeName) {
    return client().admin().cluster().prepareClusterStats().get().getNodesMap().get(nodeName).nodeInfo();
  }

  @CheckForNull
  private NodeInfo getLocalNodeInfo() {
    if (settings.hasKey(IndexProperties.NODE_NAME)) {
      return getLocalNodeInfoByNodeName(settings.getString(IndexProperties.NODE_NAME));
    } else {
      try {
        String LocalhostName = InetAddress.getLocalHost().getHostName();
        return getLocalNodeInfoByHostName(LocalhostName);
      } catch (Exception exception) {
        throw new IllegalStateException("Could not get localhost hostname", exception);
      }
    }
  }

  private void initLocalClient(IndexProperties.ES_TYPE type, ImmutableSettings.Builder esSettings) {
    if (IndexProperties.ES_TYPE.MEMORY.equals(type)) {
      initMemoryES(esSettings);
    }

    initNetwork(esSettings);

    node = NodeBuilder.nodeBuilder()
      .settings(esSettings)
      .node();
    node.start();

    client = node.client();
  }

  private void initMemoryES(ImmutableSettings.Builder builder) {
    builder
      .put("node.name", "node-mem-" + System.currentTimeMillis())
      .put("node.data", true)
      .put("cluster.name", "cluster-mem-" + NetworkUtils.getLocalAddress().getHostName())
      .put("index.store.type", "memory")
      .put("index.store.fs.memory.enabled", "true")
      .put("gateway.type", "none")
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("node.local", true)

      .put("index.search.slowlog.threshold.query.warn", "10ms")
        // Cannot use anything else but warn
      .put("index.search.slowlog.threshold.query.info", "10ms")
      .put("index.search.slowlog.threshold.query.debug", "10ms")
      .put("index.search.slowlog.threshold.query.trace", "10ms")

      .put("index.search.slowlog.threshold.fetch.warn", "10ms")
        // Cannot use anything else but warn
      .put("index.search.slowlog.threshold.fetch.info", "10ms")
      .put("index.search.slowlog.threshold.fetch.debug", "10ms")
      .put("index.search.slowlog.threshold.fetch.trace", "10ms")

      .put("index.indexing.slowlog.threshold.index.warn", "10ms")
        // Cannot use anything else but warn
      .put("index.indexing.slowlog.threshold.index.info", "10ms")
      .put("index.indexing.slowlog.threshold.index.debug", "10ms")
      .put("index.indexing.slowlog.threshold.index.trace", "10ms")

      .put("path.data", esDataDir().getAbsolutePath())

      .put("path.logs", esLogDir().getAbsolutePath());

    int httpPort = settings.getInt(IndexProperties.HTTP_PORT);
    if (httpPort > 0) {
      LOG.warn("Elasticsearch HTTP console enabled on port {}. Only for debugging purpose.", httpPort);
      builder.put(HTTP_ENABLED, true);
      builder.put("http.host", "127.0.0.1");
      builder.put("http.port", httpPort);
    } else {
      builder.put(HTTP_ENABLED, false);
    }
  }

  private void addIndexTemplates() {
    client.admin().indices()
      .preparePutTemplate("default")
      .setTemplate("*")
      .addMapping("_default_", "{\"dynamic\": \"strict\"}")
      .get();
  }

  private void initLogging() {
    ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
  }

  private void initAnalysis(ImmutableSettings.Builder esSettings) {
    esSettings
      .put("index.mapper.dynamic", false)

        // Sortable text analyzer
      .put("index.analysis.analyzer.sortable.type", "custom")
      .put("index.analysis.analyzer.sortable.tokenizer", "keyword")
      .putArray("index.analysis.analyzer.sortable.filter", "trim", "lowercase", "truncate")

        // Edge NGram index-analyzer
      .put("index.analysis.analyzer.index_grams.type", "custom")
      .put("index.analysis.analyzer.index_grams.tokenizer", "whitespace")
      .putArray("index.analysis.analyzer.index_grams.filter", "trim", "lowercase", "gram_filter")

        // Edge NGram search-analyzer
      .put("index.analysis.analyzer.search_grams.type", "custom")
      .put("index.analysis.analyzer.search_grams.tokenizer", "whitespace")
      .putArray("index.analysis.analyzer.search_grams.filter", "trim", "lowercase")

        // Word index-analyzer
      .put("index.analysis.analyzer.index_words.type", "custom")
      .put("index.analysis.analyzer.index_words.tokenizer", "standard")
      .putArray("index.analysis.analyzer.index_words.filter",
        "standard", "word_filter", "lowercase", "stop", "asciifolding", "porter_stem")

        // Word search-analyzer
      .put("index.analysis.analyzer.search_words.type", "custom")
      .put("index.analysis.analyzer.search_words.tokenizer", "standard")
      .putArray("index.analysis.analyzer.search_words.filter",
        "standard", "lowercase", "stop", "asciifolding", "porter_stem")

        // Edge NGram filter
      .put("index.analysis.filter.gram_filter.type", "edgeNGram")
      .put("index.analysis.filter.gram_filter.min_gram", 2)
      .put("index.analysis.filter.gram_filter.max_gram", 15)
      .putArray("index.analysis.filter.gram_filter.token_chars", "letter", "digit", "punctuation", "symbol")

        // Word filter
      .put("index.analysis.filter.word_filter.type", "word_delimiter")
      .put("index.analysis.filter.word_filter.generate_word_parts", true)
      .put("index.analysis.filter.word_filter.catenate_words", true)
      .put("index.analysis.filter.word_filter.catenate_numbers", true)
      .put("index.analysis.filter.word_filter.catenate_all", true)
      .put("index.analysis.filter.word_filter.split_on_case_change", true)
      .put("index.analysis.filter.word_filter.preserve_original", true)
      .put("index.analysis.filter.word_filter.split_on_numerics", true)
      .put("index.analysis.filter.word_filter.stem_english_possessive", true)

        // Path Analyzer
      .put("index.analysis.analyzer.path_analyzer.type", "custom")
      .put("index.analysis.analyzer.path_analyzer.tokenizer", "path_hierarchy");

  }

  private void initNetwork(ImmutableSettings.Builder esSettings) {
    esSettings.put("network.bind_host", "127.0.0.1");
  }

  private File esHomeDir() {
    if (!settings.hasKey("sonar.path.home")) {
      throw new IllegalStateException("property 'sonar.path.home' is required");
    }
    return new File(settings.getString("sonar.path.home"));
  }

  private File esDataDir() {
    if (settings.hasKey("sonar.path.data")) {
      return new File(settings.getString("sonar.path.data"), "es");
    } else {
      return new File(settings.getString("sonar.path.home"), "data/es");
    }
  }

  private File esLogDir() {
    if (settings.hasKey("sonar.path.log")) {
      return new File(settings.getString("sonar.path.log"));
    } else {
      return new File(settings.getString("sonar.path.home"), "log");
    }
  }

  @Override
  public void stop() {
    if (client != null) {
      client.close();
      client = null;
    }
    if (node != null) {
      node.close();
      node = null;
    }
  }

  public Client client() {
    if (client == null) {
      throw new IllegalStateException("Elasticsearch is not started");
    }
    return client;
  }

  public <K extends ActionResponse> K execute(ActionRequestBuilder request) {
    StopWatch basicProfile = profiling.start("search", Profiling.Level.BASIC);
    StopWatch fullProfile = profiling.start("search", Profiling.Level.FULL);
    ListenableActionFuture acc = request.execute();
    try {

      K response = (K) acc.get();

      if (profiling.isProfilingEnabled(Profiling.Level.BASIC)) {
        if (ToXContent.class.isAssignableFrom(request.getClass())) {
          XContentBuilder debugResponse = XContentFactory.jsonBuilder();
          debugResponse.startObject();
          ((ToXContent) request).toXContent(debugResponse, ToXContent.EMPTY_PARAMS);
          debugResponse.endObject();
          fullProfile.stop("ES Request: %s", debugResponse.string());
        } else {
          fullProfile.stop("ES Request: %s", request.toString().replaceAll("\n", ""));
        }
      }

      if (profiling.isProfilingEnabled(Profiling.Level.FULL)) {
        if (ToXContent.class.isAssignableFrom(response.getClass())) {
          XContentBuilder debugResponse = XContentFactory.jsonBuilder();
          debugResponse.startObject();
          ((ToXContent) response).toXContent(debugResponse, ToXContent.EMPTY_PARAMS);
          debugResponse.endObject();
          fullProfile.stop("ES Response: %s", debugResponse.string());
        } else {
          fullProfile.stop("ES Response: %s", response.toString());
        }
      }

      return response;
    } catch (Exception e) {
      throw new IllegalStateException("ES error: ", e);
    }
  }
}
