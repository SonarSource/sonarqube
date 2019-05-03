/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.application.es;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Props;
import org.sonar.process.System2;

import static java.lang.String.valueOf;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HTTP_PORT;
import static org.sonar.process.ProcessProperties.Property.SEARCH_INITIAL_STATE_TIMEOUT;
import static org.sonar.process.ProcessProperties.Property.SEARCH_MINIMUM_MASTER_NODES;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;

public class EsSettings {

  private static final Logger LOGGER = LoggerFactory.getLogger(EsSettings.class);
  private static final String STANDALONE_NODE_NAME = "sonarqube";
  private static final String SECCOMP_PROPERTY = "bootstrap.system_call_filter";
  private static final String ALLOW_MMAP = "node.store.allow_mmapfs";

  private final Props props;
  private final EsInstallation fileSystem;

  private final boolean clusterEnabled;
  private final String clusterName;
  private final String nodeName;

  public EsSettings(Props props, EsInstallation fileSystem, System2 system2) {
    this.props = props;
    this.fileSystem = fileSystem;

    this.clusterName = props.nonNullValue(CLUSTER_NAME.getKey());
    this.clusterEnabled = props.valueAsBoolean(CLUSTER_ENABLED.getKey());
    if (this.clusterEnabled) {
      this.nodeName = props.value(CLUSTER_NODE_NAME.getKey(), "sonarqube-" + UUID.randomUUID().toString());
    } else {
      this.nodeName = STANDALONE_NODE_NAME;
    }
    String esJvmOptions = system2.getenv("ES_JVM_OPTIONS");
    if (esJvmOptions != null && !esJvmOptions.trim().isEmpty()) {
      LOGGER.warn("ES_JVM_OPTIONS is defined but will be ignored. " +
        "Use sonar.search.javaOpts and/or sonar.search.javaAdditionalOpts in sonar.properties to specify jvm options for Elasticsearch");
    }
  }

  public Map<String, String> build() {
    Map<String, String> builder = new HashMap<>();
    configureFileSystem(builder);
    configureNetwork(builder);
    configureCluster(builder);
    configureOthers(builder);
    return builder;
  }

  private void configureFileSystem(Map<String, String> builder) {
    builder.put("path.data", fileSystem.getDataDirectory().getAbsolutePath());
    builder.put("path.logs", fileSystem.getLogDirectory().getAbsolutePath());
  }

  private void configureNetwork(Map<String, String> builder) {
    InetAddress host = readHost();
    int port = Integer.parseInt(props.nonNullValue(SEARCH_PORT.getKey()));
    LOGGER.info("Elasticsearch listening on {}:{}", host, port);

    builder.put("transport.tcp.port", valueOf(port));
    builder.put("transport.host", valueOf(host.getHostAddress()));
    builder.put("network.host", valueOf(host.getHostAddress()));

    // Elasticsearch sets the default value of TCP reuse address to true only on non-MSWindows machines, but why ?
    builder.put("network.tcp.reuse_address", valueOf(true));

    int httpPort = props.valueAsInt(SEARCH_HTTP_PORT.getKey(), -1);
    if (httpPort < 0) {
      // standard configuration
      builder.put("http.enabled", valueOf(false));
    } else {
      LOGGER.warn("Elasticsearch HTTP connector is enabled on port {}. MUST NOT BE USED FOR PRODUCTION", httpPort);
      // see https://github.com/lmenezes/elasticsearch-kopf/issues/195
      builder.put("http.cors.enabled", valueOf(true));
      builder.put("http.cors.allow-origin", "*");
      builder.put("http.enabled", valueOf(true));
      builder.put("http.host", host.getHostAddress());
      builder.put("http.port", valueOf(httpPort));
    }
  }

  private InetAddress readHost() {
    String hostProperty = props.nonNullValue(SEARCH_HOST.getKey());
    try {
      return InetAddress.getByName(hostProperty);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Can not resolve host [" + hostProperty + "]. Please check network settings and property " + SEARCH_HOST.getKey(), e);
    }
  }

  private void configureCluster(Map<String, String> builder) {
    // Default value in a standalone mode, not overridable

    int minimumMasterNodes = 1;
    String initialStateTimeOut = "30s";

    if (clusterEnabled) {
      minimumMasterNodes = props.valueAsInt(SEARCH_MINIMUM_MASTER_NODES.getKey(), 2);
      initialStateTimeOut = props.value(SEARCH_INITIAL_STATE_TIMEOUT.getKey(), "120s");

      String hosts = props.value(CLUSTER_SEARCH_HOSTS.getKey(), "");
      LOGGER.info("Elasticsearch cluster enabled. Connect to hosts [{}]", hosts);
      builder.put("discovery.zen.ping.unicast.hosts", hosts);
    }

    builder.put("discovery.zen.minimum_master_nodes", valueOf(minimumMasterNodes));
    builder.put("discovery.initial_state_timeout", initialStateTimeOut);
    builder.put("cluster.name", clusterName);
    builder.put("cluster.routing.allocation.awareness.attributes", "rack_id");
    builder.put("node.attr.rack_id", nodeName);
    builder.put("node.name", nodeName);
    builder.put("node.data", valueOf(true));
    builder.put("node.master", valueOf(true));
  }

  private void configureOthers(Map<String, String> builder) {
    builder.put("action.auto_create_index", String.valueOf(false));
    if (props.value("sonar.search.javaAdditionalOpts", "").contains("-D" + SECCOMP_PROPERTY + "=false")) {
      builder.put(SECCOMP_PROPERTY, "false");
    }

    // to be used with HA QA, where we can't easily set mmap size when running with docker.
    if (props.value("sonar.search.javaAdditionalOpts", "").contains("-D" + ALLOW_MMAP + "=false")) {
      builder.put(ALLOW_MMAP, "false");
    }
  }
}
