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
package org.sonar.search;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

public class EsSettings implements EsSettingsMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(EsSettings.class);
  public static final String PROP_MARVEL_HOSTS = "sonar.search.marvelHosts";
  public static final String CLUSTER_SEARCH_NODE_NAME = "sonar.cluster.search.nodeName";
  public static final String STANDALONE_NODE_NAME = "sonarqube";

  private final Props props;

  private final boolean clusterEnabled;
  private final String clusterName;
  private final String nodeName;

  EsSettings(Props props) {
    this.props = props;

    this.clusterName = props.nonNullValue(ProcessProperties.CLUSTER_NAME);
    this.clusterEnabled = props.valueAsBoolean(ProcessProperties.CLUSTER_ENABLED);
    if (this.clusterEnabled) {
      this.nodeName = props.value(CLUSTER_SEARCH_NODE_NAME, "sonarqube-" + UUID.randomUUID().toString());
    } else {
      this.nodeName = STANDALONE_NODE_NAME;
    }
  }

  @Override
  public int getHttpPort() {
    return props.valueAsInt(ProcessProperties.SEARCH_HTTP_PORT, -1);
  }

  @Override
  public String getClusterName() {
    return clusterName;
  }

  @Override
  public String getNodeName() {
    return nodeName;
  }

  Settings build() {
    Settings.Builder builder = Settings.builder();
    configureFileSystem(builder);
    configureNetwork(builder);
    configureCluster(builder);
    configureMarvel(builder);
    return builder.build();
  }

  private void configureFileSystem(Settings.Builder builder) {
    File homeDir = props.nonNullValueAsFile(ProcessProperties.PATH_HOME);
    File dataDir;
    File logDir;

    // data dir
    String dataPath = props.value(ProcessProperties.PATH_DATA);
    if (StringUtils.isNotEmpty(dataPath)) {
      dataDir = new File(dataPath, "es");
    } else {
      dataDir = new File(homeDir, "data/es");
    }
    builder.put("path.data", dataDir.getAbsolutePath());

    String tempPath = props.value(ProcessProperties.PATH_TEMP);
    builder.put("path.home", new File(tempPath, "es"));

    // log dir
    String logPath = props.value(ProcessProperties.PATH_LOGS);
    if (StringUtils.isNotEmpty(logPath)) {
      logDir = new File(logPath);
    } else {
      logDir = new File(homeDir, "log");
    }
    builder.put("path.logs", logDir.getAbsolutePath());
  }

  private void configureNetwork(Settings.Builder builder) {
    InetAddress host = readHost();
    int port = Integer.parseInt(props.nonNullValue(ProcessProperties.SEARCH_PORT));
    LOGGER.info("Elasticsearch listening on {}:{}", host, port);

    // disable multicast
    builder.put("discovery.zen.ping.multicast.enabled", "false")
      .put("transport.tcp.port", port)
      .put("transport.host", host.getHostAddress())
      .put("network.host", host.getHostAddress());

    // Elasticsearch sets the default value of TCP reuse address to true only on non-MSWindows machines, but why ?
    builder.put("network.tcp.reuse_address", true);

    int httpPort = getHttpPort();
    if (httpPort < 0) {
      // standard configuration
      builder.put("http.enabled", false);
    } else {
      LOGGER.warn("Elasticsearch HTTP connector is enabled on port {}. MUST NOT BE USED FOR PRODUCTION", httpPort);
      // see https://github.com/lmenezes/elasticsearch-kopf/issues/195
      builder.put("http.cors.enabled", true)
        .put("http.cors.allow-origin", "*")
        .put("http.enabled", true)
        .put("http.host", host.getHostAddress())
        .put("http.port", httpPort);
    }
  }

  private InetAddress readHost() {
    String hostProperty = props.nonNullValue(ProcessProperties.SEARCH_HOST);
    try {
      return InetAddress.getByName(hostProperty);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Can not resolve host [" + hostProperty + "]. Please check network settings and property " + ProcessProperties.SEARCH_HOST, e);
    }
  }

  void configureIndexDefaults(Settings.Builder builder) {
    configureIndexDefaultsForCluster(builder);
    builder
      .put("index.number_of_shards", "1")
      .put("index.refresh_interval", "30s")
      .put("action.auto_create_index", false)
      .put("index.mapper.dynamic", false);
  }

  private void configureIndexDefaultsForCluster(Settings.Builder builder) {
    builder.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, computeReplicationFactor());
  }

  private int computeReplicationFactor() {
    if (clusterEnabled) {
      return props.valueAsInt(ProcessProperties.SEARCH_REPLICAS, 1);
    }
    return 0;
  }

  private void configureCluster(Settings.Builder builder) {
    // Default value in a standalone mode, not overridable

    int minimumMasterNodes = 1;
    String initialStateTimeOut = "30s";

    if (clusterEnabled) {
      minimumMasterNodes = props.valueAsInt(ProcessProperties.SEARCH_MINIMUM_MASTER_NODES, 2);
      initialStateTimeOut = props.value(ProcessProperties.SEARCH_INITIAL_STATE_TIMEOUT, "120s");

      String hosts = props.value(ProcessProperties.CLUSTER_SEARCH_HOSTS, "");
      LOGGER.info("Elasticsearch cluster enabled. Connect to hosts [{}]", hosts);
      builder.put("discovery.zen.ping.unicast.hosts", hosts);
    }

    builder.put("discovery.zen.minimum_master_nodes", minimumMasterNodes)
      .put("discovery.initial_state_timeout", initialStateTimeOut)
      .put("cluster.name", getClusterName())
      .put("cluster.routing.allocation.awareness.attributes", "rack_id")
      .put("node.rack_id", nodeName)
      .put("node.name", nodeName)
      .put("node.data", true)
      .put("node.master", true);
  }

  private void configureMarvel(Settings.Builder builder) {
    Set<String> marvels = new TreeSet<>();
    marvels.addAll(Arrays.asList(StringUtils.split(props.value(PROP_MARVEL_HOSTS, ""), ",")));

    // If we're collecting indexing data send them to the Marvel host(s)
    if (!marvels.isEmpty()) {
      String hosts = StringUtils.join(marvels, ",");
      LOGGER.info("Elasticsearch Marvel is enabled for %s", hosts);
      builder.put("marvel.agent.exporter.es.hosts", hosts);
    }
  }
}
