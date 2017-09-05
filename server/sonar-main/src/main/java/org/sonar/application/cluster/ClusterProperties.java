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

package org.sonar.application.cluster;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.cluster.NodeType;

import static org.sonar.cluster.ClusterProperties.CLUSTER_HOSTS;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_HOST;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_NAME;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_PORT;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_TYPE;

/**
 * Properties of the cluster configuration
 */
final class ClusterProperties {
  private static final String DEFAULT_PORT = "9003";
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProperties.class);
  static final String HAZELCAST_CLUSTER_NAME = "sonarqube";

  private final int port;
  private final List<String> hosts;
  private final List<String> networkInterfaces;
  private final NodeType nodeType;
  private final String nodeName;

  ClusterProperties(AppSettings appSettings) {
    port = appSettings.getProps().valueAsInt(CLUSTER_NODE_PORT);
    networkInterfaces = extractNetworkInterfaces(appSettings.getProps().value(CLUSTER_NODE_HOST, ""));
    hosts = extractHosts(appSettings.getProps().value(CLUSTER_HOSTS, ""));
    nodeType = NodeType.parse(appSettings.getProps().value(CLUSTER_NODE_TYPE));
    nodeName = appSettings.getProps().value(CLUSTER_NODE_NAME, "sonarqube-" + UUID.randomUUID().toString());
  }

  int getPort() {
    return port;
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  List<String> getHosts() {
    return hosts;
  }

  List<String> getNetworkInterfaces() {
    return networkInterfaces;
  }

  public String getNodeName() {
    return nodeName;
  }

  void validate() {
    // Test validity of port
    checkArgument(
      port > 0 && port < 65_536,
      "Cluster port have been set to %d which is outside the range [1-65535].",
      port);

    // Test the networkInterfaces parameter
    try {
      List<String> localInterfaces = findAllLocalIPs();

      networkInterfaces.forEach(
        inet -> checkArgument(
          StringUtils.isEmpty(inet) || localInterfaces.contains(inet),
          "Interface %s is not available on this machine.",
          inet));
    } catch (SocketException e) {
      LOGGER.warn("Unable to retrieve network networkInterfaces. Interfaces won't be checked", e);
    }
  }

  private static List<String> extractHosts(final String hosts) {
    List<String> result = new ArrayList<>();
    for (String host : hosts.split(",")) {
      if (StringUtils.isNotEmpty(host)) {
        if (!host.contains(":")) {
          result.add(
            String.format("%s:%s", host, DEFAULT_PORT));
        } else {
          result.add(host);
        }
      }
    }
    return result;
  }

  private static List<String> extractNetworkInterfaces(final String networkInterfaces) {
    List<String> result = new ArrayList<>();
    for (String iface : networkInterfaces.split(",")) {
      if (StringUtils.isNotEmpty(iface)) {
        result.add(iface);
      }
    }
    return result;
  }

  private static List<String> findAllLocalIPs() throws SocketException {
    Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
    List<String> localInterfaces = new ArrayList<>();

    while (netInterfaces.hasMoreElements()) {
      NetworkInterface networkInterface = netInterfaces.nextElement();
      Enumeration<InetAddress> ips = networkInterface.getInetAddresses();
      while (ips.hasMoreElements()) {
        InetAddress ip = ips.nextElement();
        localInterfaces.add(ip.getHostAddress());
      }
    }
    return localInterfaces;
  }

  private static void checkArgument(boolean expression,
    @Nullable String messageTemplate,
    @Nullable Object... args) {
    if (!expression) {
      throw new IllegalArgumentException(String.format(messageTemplate, args));
    }
  }
}
