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
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.StopWatch;

import javax.annotation.CheckForNull;
import java.io.File;
import java.net.InetAddress;

/**
 * ElasticSearch Node used to connect to index.
 */
public class SearchClient extends TransportClient {

  private static final Logger LOG = LoggerFactory.getLogger(SearchClient.class);
  private static final String DEFAULT_HEALTH_TIMEOUT = "30s";

  private final Settings settings;
  private final String healthTimeout;

  protected final Profiling profiling;

  public SearchClient(Settings settings) {
    this(settings, DEFAULT_HEALTH_TIMEOUT);
  }

  @VisibleForTesting
  SearchClient(Settings settings, String healthTimeout) {
    super(ImmutableSettings.settingsBuilder()
        .put("network.bind_host", "localhost")
        .put("node.rack_id", StringUtils.defaultIfEmpty(settings.getString(IndexProperties.NODE_NAME), "unknown"))
        .put("cluster.name", StringUtils.defaultIfBlank(settings.getString(IndexProperties.CLUSTER_NAME), "sonarqube"))
        .build()
    );
    initLogging();
    this.addTransportAddress(new InetSocketTransportAddress("localhost",
      settings.getInt(IndexProperties.NODE_PORT)));
    this.settings = settings;
    this.healthTimeout = healthTimeout;
    this.profiling = new Profiling(settings);
  }

  public NodeHealth getNodeHealth() {
    NodeHealth health = new NodeHealth();
    ClusterStatsResponse clusterStatsResponse = this.admin().cluster().prepareClusterStats().get();

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
    return this.admin().cluster().prepareHealth()
      .setWaitForYellowStatus()
      .setTimeout(healthTimeout)
      .get()
      .getStatus();
  }

  @CheckForNull
  private NodeInfo getLocalNodeInfoByHostName(String hostname) {
    for (ClusterStatsNodeResponse nodeResp : this.admin().cluster().prepareClusterStats().get().getNodes()) {
      if (hostname.equals(nodeResp.nodeInfo().getHostname())) {
        return nodeResp.nodeInfo();
      }
    }
    return null;
  }

  @CheckForNull
  private NodeInfo getLocalNodeInfoByNodeName(String nodeName) {
    return this.admin().cluster().prepareClusterStats().get().getNodesMap().get(nodeName).nodeInfo();
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

  // TODO that has nothing to do here!!!
  private void addIndexTemplates() {
    this.admin().indices()
      .preparePutTemplate("default")
      .setTemplate("*")
      .addMapping("_default_", "{\"dynamic\": \"strict\"}")
      .get();
  }

  private void initLogging() {
    ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
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



  static public ImmutableSettings.Builder getGlobalIndexSettings(ImmutableSettings.Builder builder) {
    return builder

      // Disable MCast
      .put("discovery.zen.ping.multicast.enabled", "false")

        // Disable dynamic mapping
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

}
