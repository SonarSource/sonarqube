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
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ServerFileSystem;

import java.io.File;

/**
 * ElasticSearch Node used to connect to index.
 */
public class ESNode implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(ESNode.class);

  private static final String HTTP_ENABLED = "http.enabled";
  static final String DATA_DIR = "data/es";

  private static final String DEFAULT_HEALTH_TIMEOUT = "30s";

  private final ServerFileSystem fileSystem;
  private final Settings settings;
  private final String healthTimeout;

  // available only after startup
  private Node node;

  public ESNode(ServerFileSystem fileSystem, Settings settings) {
    this(fileSystem, settings, DEFAULT_HEALTH_TIMEOUT);
  }

  @VisibleForTesting
  ESNode(ServerFileSystem fileSystem, Settings settings, String healthTimeout) {
    this.fileSystem = fileSystem;
    this.settings = settings;
    this.healthTimeout = healthTimeout;
  }

  @Override
  public void start() {
    initLogging();

    IndexProperties.ES_TYPE type = settings.hasKey(IndexProperties.TYPE) ?
      IndexProperties.ES_TYPE.valueOf(settings.getString(IndexProperties.TYPE)) :
      IndexProperties.ES_TYPE.DATA;

    ImmutableSettings.Builder esSettings = ImmutableSettings.settingsBuilder();

    switch (type) {
      case MEMORY:
        initMemoryES(esSettings);
        break;
      case TRANSPORT:
        initTransportES(esSettings);
        break;
      case DATA:
      default:
        initDataES(esSettings);
        break;
    }

    initAnalysis(esSettings);
    initDirs(esSettings);
    initRestConsole(esSettings);
    initNetwork(esSettings);

    node = NodeBuilder.nodeBuilder()
      .settings(esSettings)
      .node();
    node.start();


    if (
      node.client().admin().cluster().prepareHealth()
        .setWaitForYellowStatus()
        .get(TimeValue.timeValueMillis(3000))
        .getStatus() == ClusterHealthStatus.RED) {
      throw new IllegalStateException(
        String.format("Elasticsearch index is corrupt, please delete directory '%s/%s' and relaunch the SonarQube server.", fileSystem.getHomeDir().getAbsolutePath(), DATA_DIR));
    }

    addIndexTemplates();

    LOG.info("Elasticsearch started");

  }

  private void initMemoryES(ImmutableSettings.Builder builder) {
    builder
      .put("node.name", "node-test-" + System.currentTimeMillis())
      .put("node.data", true)
      .put("cluster.name", "cluster-test-" + NetworkUtils.getLocalAddress().getHostName())
      .put("index.store.type", "memory")
      .put("index.store.fs.memory.enabled", "true")
      .put("gateway.type", "none")
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("cluster.routing.schedule", "50ms")
      .put("node.local", true);
  }

  private void initTransportES(ImmutableSettings.Builder builder) {
    throw new IllegalStateException("Not implemented yet");
  }

  private void initDataES(ImmutableSettings.Builder builder) {
    builder
      .put("node.name", "sonarqube-" + System.currentTimeMillis())
      .put("node.data", true)
      .put("node.local", true)
      .put("cluster.name", "sonarqube")
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0");
  }

  private void addIndexTemplates() {
    PutIndexTemplateResponse response = node.client().admin().indices()
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

      .put("index.analysis.analyzer.sortable.type", "custom")
      .put("index.analysis.analyzer.sortable.tokenizer", "keyword")
      .putArray("index.analysis.analyzer.sortable.filter", "trim", "lowercase", "truncate")

      .put("index.analysis.analyzer.string_gram.type", "custom")
      .put("index.analysis.analyzer.string_gram.tokenizer", "whitespace")
      .putArray("index.analysis.analyzer.string_gram.filter", "lowercase", "code_gram")

      .put("index.analysis.filter.code_gram.type", "edgeNGram")
      .put("index.analysis.filter.code_gram.min_gram", 2)
      .put("index.analysis.filter.code_gram.max_gram", 15)
      .putArray("index.analysis.filter.code_gram.token_chars", "letter", "digit", "punctuation", "symbol")

      .put("index.analysis.analyzer.path_analyzer.type", "custom")
      .put("index.analysis.analyzer.path_analyzer.tokenizer", "path_hierarchy");

  }

  private void initNetwork(ImmutableSettings.Builder esSettings) {
    esSettings.put("network.bind_host", "127.0.0.1");
  }

  private void initRestConsole(ImmutableSettings.Builder esSettings) {
    int httpPort = settings.getInt(IndexProperties.HTTP_PORT);
    if (httpPort > 0) {
      LOG.warn("Elasticsearch HTTP console enabled on port {}. Only for debugging purpose.", httpPort);
      esSettings.put(HTTP_ENABLED, true);
      esSettings.put("http.host", "127.0.0.1");
      esSettings.put("http.port", httpPort);
    } else {
      esSettings.put(HTTP_ENABLED, false);
    }
  }

  private void initDirs(ImmutableSettings.Builder esSettings) {
    File esDir = new File(fileSystem.getHomeDir(), DATA_DIR);
    try {
      FileUtils.forceMkdir(esDir);
      esSettings.put("path.home", esDir.getAbsolutePath());
      LOG.debug("Elasticsearch data stored in {}", esDir.getAbsolutePath());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create directory " + esDir.getAbsolutePath(), e);
    }
  }

  @Override
  public void stop() {
    if (node != null) {
      node.close();
      node = null;
    }
  }

  public Client client() {
    if (node == null) {
      throw new IllegalStateException("Elasticsearch is not started");
    }
    return node.client();
  }
}
