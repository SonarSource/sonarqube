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
package org.sonar.application.config;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.process.MessageException;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.cluster.NodeType;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.process.ProcessProperties.Property.AUTH_JWT_SECRET;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HZ_PORT;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_WEB_STARTUP_LEADER;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;

public class ClusterSettings implements Consumer<Props> {

  private final NetworkUtils network;

  public ClusterSettings(NetworkUtils network) {
    this.network = network;
  }

  @Override
  public void accept(Props props) {
    if (isClusterEnabled(props)) {
      checkClusterProperties(props);
    }
  }

  private void checkClusterProperties(Props props) {
    // for internal use
    if (props.value(CLUSTER_WEB_STARTUP_LEADER.getKey()) != null) {
      throw new MessageException(format("Property [%s] is forbidden", CLUSTER_WEB_STARTUP_LEADER.getKey()));
    }

    NodeType nodeType = toNodeType(props);
    switch (nodeType) {
      case APPLICATION:
        ensureNotH2(props);
        requireValue(props, AUTH_JWT_SECRET.getKey());
        ensureNotLoopbackAddresses(props, CLUSTER_HZ_HOSTS.getKey());
        break;
      case SEARCH:
        requireValue(props, SEARCH_HOST.getKey());
        ensureLocalButNotLoopbackAddress(props, SEARCH_HOST.getKey());
        if (props.contains(CLUSTER_NODE_HZ_PORT.getKey())) {
          LoggerFactory.getLogger(getClass()).warn("Property {} is ignored on search nodes since 7.2", CLUSTER_NODE_HZ_PORT.getKey());
        }
        break;
      default:
        throw new UnsupportedOperationException("Unknown value: " + nodeType);
    }
    requireValue(props, CLUSTER_NODE_HOST.getKey());
    ensureLocalButNotLoopbackAddress(props, CLUSTER_NODE_HOST.getKey());
    ensureNotLoopbackAddresses(props, CLUSTER_SEARCH_HOSTS.getKey());
  }

  private static NodeType toNodeType(Props props) {
    String nodeTypeValue = requireValue(props, CLUSTER_NODE_TYPE.getKey());
    if (!NodeType.isValid(nodeTypeValue)) {
      throw new MessageException(format("Invalid value for property %s: [%s], only [%s] are allowed", CLUSTER_NODE_TYPE.getKey(), nodeTypeValue,
        Arrays.stream(NodeType.values()).map(NodeType::getValue).collect(joining(", "))));
    }
    return NodeType.parse(nodeTypeValue);
  }

  private static String requireValue(Props props, String key) {
    String value = props.value(key);
    if (isBlank(value)) {
      throw new MessageException(format("Property %s is mandatory", key));
    }
    return value;
  }

  private static void ensureNotH2(Props props) {
    String jdbcUrl = props.value(JDBC_URL.getKey());
    if (isBlank(jdbcUrl) || jdbcUrl.startsWith("jdbc:h2:")) {
      throw new MessageException("Embedded database is not supported in cluster mode");
    }
  }

  private void ensureNotLoopbackAddresses(Props props, String propertyKey) {
    stream(requireValue(props, propertyKey).split(","))
      .filter(StringUtils::isNotBlank)
      .map(StringUtils::trim)
      .map(s -> StringUtils.substringBefore(s, ":"))
      .forEach(ip -> {
        try {
          if (network.isLoopbackInetAddress(network.toInetAddress(ip))) {
            throw new MessageException(format("Property %s must not be a loopback address: %s", propertyKey, ip));
          }
        } catch (UnknownHostException e) {
          throw new MessageException(format("Property %s must not a valid address: %s [%s]", propertyKey, ip, e.getMessage()));
        }
      });
  }

  private void ensureLocalButNotLoopbackAddress(Props props, String propertyKey) {
    String propertyValue = props.nonNullValue(propertyKey).trim();
    try {
      InetAddress address = network.toInetAddress(propertyValue);
      if (!network.isLocalInetAddress(address) || network.isLoopbackInetAddress(address)) {
        throw new MessageException(format("Property %s must be a local non-loopback address: %s", propertyKey, propertyValue));
      }
    } catch (UnknownHostException | SocketException e) {
      throw new MessageException(format("Property %s must be a local non-loopback address: %s [%s]", propertyKey, propertyValue, e.getMessage()));
    }
  }

  public static boolean isClusterEnabled(AppSettings settings) {
    return isClusterEnabled(settings.getProps());
  }

  private static boolean isClusterEnabled(Props props) {
    return props.valueAsBoolean(CLUSTER_ENABLED.getKey());
  }

  /**
   * Hazelcast must be started when cluster is activated on all nodes but search ones
   */
  public static boolean shouldStartHazelcast(AppSettings appSettings) {
    return isClusterEnabled(appSettings.getProps()) && toNodeType(appSettings.getProps()).equals(NodeType.APPLICATION);
  }

  public static List<ProcessId> getEnabledProcesses(AppSettings settings) {
    if (!isClusterEnabled(settings)) {
      return asList(ProcessId.ELASTICSEARCH, ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);
    }
    NodeType nodeType = NodeType.parse(settings.getValue(CLUSTER_NODE_TYPE.getKey()).orElse(""));
    switch (nodeType) {
      case APPLICATION:
        return asList(ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);
      case SEARCH:
        return singletonList(ProcessId.ELASTICSEARCH);
      default:
        throw new IllegalArgumentException("Unexpected node type " + nodeType);
    }
  }

  public static boolean isLocalElasticsearchEnabled(AppSettings settings) {
    // elasticsearch is enabled on "search" nodes, but disabled on "application" nodes
    if (isClusterEnabled(settings.getProps())) {
      return NodeType.parse(settings.getValue(CLUSTER_NODE_TYPE.getKey()).orElse("")) == NodeType.SEARCH;
    }

    // elasticsearch is enabled in standalone mode
    return true;
  }
}
