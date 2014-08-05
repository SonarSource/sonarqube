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
package org.sonar.search;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.LoggerFactory;
import org.sonar.process.ConfigurationUtils;
import org.sonar.process.MinimumViableEnvironment;
import org.sonar.process.MonitoredProcess;
import org.sonar.process.Props;
import org.sonar.search.script.ListUpdate;

import java.io.File;

public class SearchServer extends MonitoredProcess {

  public static final String ES_DEBUG_PROPERTY = "esDebug";
  public static final String ES_PORT_PROPERTY = "sonar.search.port";
  public static final String ES_CLUSTER_PROPERTY = "sonar.cluster.name";
  public static final String ES_CLUSTER_INNET = "sonar.cluster.master";


  private Node node;

  SearchServer(Props props) throws Exception {
    super(props);
    new MinimumViableEnvironment().check();
  }

  @Override
  protected boolean doIsReady() {
    return (node.client().admin().cluster().prepareHealth()
      .setWaitForYellowStatus()
      .setTimeout(TimeValue.timeValueSeconds(3L))
      .get()
      .getStatus() != ClusterHealthStatus.RED);
  }

  @Override
  protected void doStart() {

    String homeDir = props.of("sonar.path.home");
    String dataDir = props.of("sonar.path.data", homeDir + "/data");
    String logDir = props.of("sonar.path.logs", homeDir + "/logs");
    String tempDir = props.of("sonar.path.temp", homeDir + "/temp");
    Integer port = props.intOf(ES_PORT_PROPERTY);
    String clusterName = props.of(ES_CLUSTER_PROPERTY);

    LoggerFactory.getLogger(SearchServer.class).info("Starting ES[{}] on port: {}", clusterName, port);

    ImmutableSettings.Builder esSettings = ImmutableSettings.settingsBuilder()

      .put("discovery.zen.ping.multicast.enabled", "false")

      .put("index.merge.policy.max_merge_at_once", "200")
      .put("index.merge.policy.segments_per_tier", "200")
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("index.store.type", "mmapfs")
      .put("indices.store.throttle.type", "merge")
      .put("indices.store.throttle.max_bytes_per_sec", "200mb")

      .put("script.default_lang", "native")
      .put("script.native." + ListUpdate.NAME + ".type", ListUpdate.UpdateListScriptFactory.class.getName())

      .put("cluster.name", clusterName)
      .put("node.name", "sonarqube-" + System.currentTimeMillis())

      .put("transport.tcp.port", port)
      .put("path.data", new File(dataDir, "es").getAbsolutePath())
      .put("path.work", new File(tempDir).getAbsolutePath())
      .put("path.logs", new File(logDir).getAbsolutePath());

    if (StringUtils.isNotEmpty(props.of(ES_CLUSTER_INNET, null))) {

      System.out.println("props.of(ES_CLUSTER_INNET, null) = " + props.of(ES_CLUSTER_INNET, null));
      esSettings.put("discovery.zen.ping.unicast.hosts", props.of(ES_CLUSTER_INNET));
      esSettings.put("discovery.zen.minimum_master_nodes", "2");

    }

    initAnalysis(esSettings);

    if (props.booleanOf(ES_DEBUG_PROPERTY, false)) {
      esSettings
        .put("http.enabled", true)
        .put("http.port", 9200);
    } else {
      esSettings.put("http.enabled", false);
    }

    node = NodeBuilder.nodeBuilder()
      .settings(esSettings)
      .build().start();

    while (node != null && !node.isClosed()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // TODO
      }
    }
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

  @Override
  protected void doTerminate() {
    if (node != null && !node.isClosed()) {
      node.close();
      node = null;
    }
  }

  public static void main(String... args) throws Exception {
    Props props = ConfigurationUtils.loadPropsFromCommandLineArgs(args);
    new SearchServer(props).start();
  }
}
