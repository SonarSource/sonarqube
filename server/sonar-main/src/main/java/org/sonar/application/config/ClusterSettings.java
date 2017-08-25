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
package org.sonar.application.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang.StringUtils;
import org.sonar.process.MessageException;
import org.sonar.process.NodeType;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

import static com.google.common.net.InetAddresses.forString;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.cluster.ClusterProperties.CLUSTER_ENABLED;
import static org.sonar.cluster.ClusterProperties.CLUSTER_HOSTS;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_HOST;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_TYPE;
import static org.sonar.cluster.ClusterProperties.CLUSTER_SEARCH_HOSTS;
import static org.sonar.cluster.ClusterProperties.CLUSTER_WEB_LEADER;
import static org.sonar.process.ProcessProperties.JDBC_URL;
import static org.sonar.process.ProcessProperties.SEARCH_HOST;

public class ClusterSettings implements Consumer<Props> {

  @Override
  public void accept(Props props) {
    if (isClusterEnabled(props)) {
      checkProperties(props);
    }
  }

  private static void checkProperties(Props props) {
    // Cluster web leader is not allowed
    if (props.value(CLUSTER_WEB_LEADER) != null) {
      throw new MessageException(format("Property [%s] is forbidden", CLUSTER_WEB_LEADER));
    }

    // Mandatory properties
    ensureMandatoryProperty(props, CLUSTER_NODE_TYPE);
    String nodeTypeValue = props.nonNullValue(CLUSTER_NODE_TYPE);
    if (!NodeType.isValid(nodeTypeValue)) {
      throw new MessageException(format("Invalid value for property [%s]: [%s], only [%s] are allowed", CLUSTER_NODE_TYPE, nodeTypeValue,
        Arrays.stream(NodeType.values()).map(NodeType::getValue).collect(joining(", "))));
    }
    ensureMandatoryProperty(props, CLUSTER_HOSTS);
    ensureMandatoryProperty(props, CLUSTER_SEARCH_HOSTS);

    NodeType nodeType = NodeType.parse(nodeTypeValue);
    switch (nodeType) {
      case APPLICATION:
        ensureNotH2(props);
        ensureMandatoryProperty(props, "sonar.auth.jwtBase64Hs256Secret");
        break;
      case SEARCH:
        ensureMandatoryProperty(props, SEARCH_HOST);
        ensureNotLoopback(props, SEARCH_HOST);
        break;
      default:
        throw new IllegalArgumentException("Unsupported node type: " + nodeType);
    }

    // Loopback interfaces are forbidden for the ports accessed
    // by other nodes of cluster
    ensureNotLoopback(props, CLUSTER_HOSTS);
    ensureNotLoopback(props, CLUSTER_NODE_HOST);
    ensureNotLoopback(props, CLUSTER_SEARCH_HOSTS);

    ensureLocalAddress(props, SEARCH_HOST);
    ensureLocalAddress(props, CLUSTER_NODE_HOST);
  }

  private static void ensureNotH2(Props props) {
    String jdbcUrl = props.value(JDBC_URL);
    if (isBlank(jdbcUrl) || jdbcUrl.startsWith("jdbc:h2:")) {
      throw new MessageException("Embedded database is not supported in cluster mode");
    }
  }

  private static void ensureMandatoryProperty(Props props, String key) {
    if (isBlank(props.value(key))) {
      throw new MessageException(format("Property [%s] is mandatory", key));
    }
  }

  @VisibleForTesting
  private static void ensureNotLoopback(Props props, String key) {
    String ipList = props.value(key);
    if (ipList == null) {
      return;
    }

    stream(ipList.split(","))
      .filter(StringUtils::isNotBlank)
      .map(StringUtils::trim)
      .forEach(ip -> {
        InetAddress inetAddress = convertToInetAddress(ip, key);
        if (inetAddress.isLoopbackAddress()) {
          throw new MessageException(format("The interface address [%s] of [%s] must not be a loopback address", ip, key));
        }
      });
  }

  private static void ensureLocalAddress(Props props, String key) {
    String ipList = props.value(key);

    if (ipList == null) {
      return;
    }

    stream(ipList.split(","))
      .filter(StringUtils::isNotBlank)
      .map(StringUtils::trim)
      .forEach(ip -> {
        InetAddress inetAddress = convertToInetAddress(ip, key);
        try {
          if (NetworkInterface.getByInetAddress(inetAddress) == null) {
            throw new MessageException(format("The interface address [%s] of [%s] is not a local address", ip, key));
          }
        } catch (SocketException e) {
          throw new MessageException(format("The interface address [%s] of [%s] is not a local address", ip, key));
        }
      });
  }

  private static InetAddress convertToInetAddress(String text, String key) {
    InetAddress inetAddress;
    HostAndPort hostAndPort = HostAndPort.fromString(text);
    if (!InetAddresses.isInetAddress(hostAndPort.getHostText())) {
      try {
        inetAddress = InetAddress.getByName(hostAndPort.getHostText());
      } catch (UnknownHostException e) {
        throw new MessageException(format("The interface address [%s] of [%s] cannot be resolved : %s", text, key, e.getMessage()));
      }
    } else {
      inetAddress = forString(hostAndPort.getHostText());
    }

    return inetAddress;
  }

  public static boolean isClusterEnabled(AppSettings settings) {
    return isClusterEnabled(settings.getProps());
  }

  private static boolean isClusterEnabled(Props props) {
    return props.valueAsBoolean(CLUSTER_ENABLED);
  }

  public static List<ProcessId> getEnabledProcesses(AppSettings settings) {
    if (!isClusterEnabled(settings)) {
      return asList(ProcessId.ELASTICSEARCH, ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);
    }
    NodeType nodeType = NodeType.parse(settings.getValue(CLUSTER_NODE_TYPE).orElse(null));
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
      return NodeType.parse(settings.getValue(CLUSTER_NODE_TYPE).orElse(null)) == NodeType.SEARCH;
    }

    // elasticsearch is enabled in standalone mode
    return true;
  }
}
