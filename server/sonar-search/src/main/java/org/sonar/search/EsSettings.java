/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.search;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

public class EsSettings implements EsSettingsMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(EsSettings.class);

  public static final String PROP_MARVEL_HOSTS = "sonar.search.marvelHosts";

  private final Props props;
  private final Set<String> masterHosts = new LinkedHashSet<>();

  EsSettings(Props props) {
    this.props = props;
    masterHosts.addAll(Arrays.asList(StringUtils.split(props.value(ProcessProperties.CLUSTER_MASTER_HOST, ""), ",")));
  }

  boolean inCluster() {
    return props.valueAsBoolean(ProcessProperties.CLUSTER_ACTIVATE, false);
  }

  boolean isMaster() {
    return props.valueAsBoolean(ProcessProperties.CLUSTER_MASTER, false);
  }

  @Override
  public int getHttpPort() {
    return props.valueAsInt(ProcessProperties.SEARCH_HTTP_PORT, -1);
  }

  @Override
  public String getClusterName() {
    return props.value(ProcessProperties.CLUSTER_NAME);
  }

  @Override
  public String getNodeName() {
    return props.value(ProcessProperties.CLUSTER_NODE_NAME);
  }

  Settings build() {
    ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();
    configureFileSystem(builder);
    configureIndexDefaults(builder);
    configureNetwork(builder);
    configureCluster(builder);
    configureMarvel(builder);
    return builder.build();
  }

  private void configureFileSystem(ImmutableSettings.Builder builder) {
    File homeDir = props.nonNullValueAsFile(ProcessProperties.PATH_HOME);
    File dataDir;
    File workDir;
    File logDir;

    // data dir
    String dataPath = props.value(ProcessProperties.PATH_DATA);
    if (StringUtils.isNotEmpty(dataPath)) {
      dataDir = new File(dataPath, "es");
    } else {
      dataDir = new File(homeDir, "data/es");
    }
    builder.put("path.data", dataDir.getAbsolutePath());

    // working dir
    String workPath = props.value(ProcessProperties.PATH_TEMP);
    if (StringUtils.isNotEmpty(workPath)) {
      workDir = new File(workPath);
    } else {
      workDir = new File(homeDir, "temp");
    }
    builder.put("path.work", workDir.getAbsolutePath());
    builder.put("path.plugins", workDir.getAbsolutePath());

    // log dir
    String logPath = props.value(ProcessProperties.PATH_LOGS);
    if (StringUtils.isNotEmpty(logPath)) {
      logDir = new File(logPath);
    } else {
      logDir = new File(homeDir, "log");
    }
    builder.put("path.logs", logDir.getAbsolutePath());
  }

  private void configureNetwork(ImmutableSettings.Builder builder) {
    // the following properties can't be null as default values are defined by app process
    String host = props.nonNullValue(ProcessProperties.SEARCH_HOST);
    int port = Integer.parseInt(props.nonNullValue(ProcessProperties.SEARCH_PORT));
    LOGGER.info("Elasticsearch listening on {}:{}", host, port);

    // disable multicast
    builder.put("discovery.zen.ping.multicast.enabled", "false");
    builder.put("transport.tcp.port", port);
    builder.put("transport.host", host);

    // Elasticsearch sets the default value of TCP reuse address to true only on non-MSWindows machines, but why ?
    builder.put("network.tcp.reuse_address", true);

    int httpPort = getHttpPort();
    if (httpPort < 0) {
      // standard configuration
      builder.put("http.enabled", false);
    } else {
      LOGGER.warn(String.format(
        "Elasticsearch HTTP connector is enabled on port %d. MUST NOT BE USED FOR PRODUCTION", httpPort));
      // see https://github.com/lmenezes/elasticsearch-kopf/issues/195
      builder.put("http.cors.enabled", true);
      builder.put("http.enabled", true);
      builder.put("http.host", host);
      builder.put("http.port", httpPort);
    }
  }

  private static void configureIndexDefaults(ImmutableSettings.Builder builder) {
    builder
      .put("index.number_of_shards", "1")
      .put("index.refresh_interval", "30s")
      .put("action.auto_create_index", false)
      .put("index.mapper.dynamic", false);
  }

  private void configureCluster(ImmutableSettings.Builder builder) {
    int replicationFactor = 0;
    if (inCluster()) {
      replicationFactor = 1;
      if (isMaster()) {
        LOGGER.info("Elasticsearch cluster enabled. Master node.");
        builder.put("node.master", true);
      } else if (!masterHosts.isEmpty()) {
        LOGGER.info("Elasticsearch cluster enabled. Node connecting to master: {}", masterHosts);
        builder.put("discovery.zen.ping.unicast.hosts", StringUtils.join(masterHosts, ","));
        builder.put("node.master", false);
        builder.put("discovery.zen.minimum_master_nodes", 1);
      } else {
        throw new MessageException(String.format("Not an Elasticsearch master nor slave. Please check properties %s and %s",
          ProcessProperties.CLUSTER_MASTER, ProcessProperties.CLUSTER_MASTER_HOST));
      }
    }
    builder.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, replicationFactor);
    builder.put("cluster.name", getClusterName());
    builder.put("cluster.routing.allocation.awareness.attributes", "rack_id");
    String nodeName = getNodeName();
    builder.put("node.rack_id", nodeName);
    builder.put("node.name", nodeName);
  }

  private void configureMarvel(ImmutableSettings.Builder builder) {
    Set<String> marvels = new TreeSet<>();
    marvels.addAll(Arrays.asList(StringUtils.split(props.value(PROP_MARVEL_HOSTS, ""), ",")));

    // If we're collecting indexing data send them to the Marvel host(s)
    if (!marvels.isEmpty()) {
      String hosts = StringUtils.join(marvels, ",");
      LOGGER.info(String.format("Elasticsearch Marvel is enabled for %s", hosts));
      builder.put("marvel.agent.exporter.es.hosts", hosts);
    }
  }
}
