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
package org.sonarqube.tests.cluster;

import com.sonar.orchestrator.util.NetworkUtils;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

class NodeConfig {

  enum NodeType {
    SEARCH("search"), APPLICATION("application");

    final String value;

    NodeType(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }
  }

  private final NodeType type;
  @Nullable
  private final String name;
  private final InetAddress address;
  private final int hzPort;
  @Nullable
  private final Integer searchPort;
  @Nullable
  private final Integer webPort;
  private final List<NodeConfig> connectedNodes = new ArrayList<>();
  private final List<NodeConfig> searchNodes = new ArrayList<>();

  private NodeConfig(NodeType type, @Nullable String name) {
    this.type = type;
    this.name = name;
    this.address = getNonLoopbackIpv4Address();
    this.hzPort = NetworkUtils.getNextAvailablePort(this.address);
    this.connectedNodes.add(this);
    switch (type) {
      case SEARCH:
        this.searchPort = NetworkUtils.getNextAvailablePort(this.address);
        this.webPort = null;
        this.searchNodes.add(this);
        break;
      case APPLICATION:
        this.searchPort = null;
        this.webPort = NetworkUtils.getNextAvailablePort(this.address);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  NodeType getType() {
    return type;
  }

  Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  InetAddress getAddress() {
    return address;
  }

  int getHzPort() {
    return hzPort;
  }

  Optional<Integer> getSearchPort() {
    return Optional.ofNullable(searchPort);
  }

  Optional<Integer> getWebPort() {
    return Optional.ofNullable(webPort);
  }

  String getHzHost() {
    return address.getHostAddress() + ":" + hzPort;
  }

  String getSearchHost() {
    return address.getHostAddress() + ":" + searchPort;
  }

  NodeConfig addConnectionToBus(NodeConfig... configs) {
    connectedNodes.addAll(Arrays.asList(configs));
    return this;
  }

  NodeConfig addConnectionToSearch(NodeConfig... configs) {
    Arrays.stream(configs).forEach(config -> {
      checkArgument(config.getType() == NodeType.SEARCH);
      searchNodes.add(config);
    });
    return this;
  }

  List<NodeConfig> getConnectedNodes() {
    return connectedNodes;
  }

  List<NodeConfig> getSearchNodes() {
    return searchNodes;
  }

  static NodeConfig newApplicationConfig(String name) {
    return new NodeConfig(NodeType.APPLICATION, name);
  }

  static NodeConfig newSearchConfig(String name) {
    return new NodeConfig(NodeType.SEARCH, name);
  }

  /**
   * See property sonar.cluster.hosts
   */
  static void interconnectBus(NodeConfig... configs) {
    Arrays.stream(configs).forEach(config -> Arrays.stream(configs).filter(c -> c != config).forEach(config::addConnectionToBus));
  }

  /**
   * See property sonar.cluster.search.hosts
   */
  static void interconnectSearch(NodeConfig... configs) {
    Arrays.stream(configs).forEach(config -> Arrays.stream(configs)
      .filter(c -> c.getType() == NodeType.SEARCH)
      .forEach(config::addConnectionToSearch));
  }

  private static InetAddress getNonLoopbackIpv4Address() {
    try {
      Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
      for (NetworkInterface networkInterface : Collections.list(nets)) {
        if (!networkInterface.isLoopback() && networkInterface.isUp() && !isBlackListed(networkInterface)) {
          Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
          while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddress = inetAddresses.nextElement();
            if (inetAddress instanceof Inet4Address) {
              return inetAddress;
            }
          }
        }
      }
    } catch (SocketException se) {
      throw new RuntimeException("Cannot find a non loopback card required for tests", se);
    }
    throw new RuntimeException("Cannot find a non loopback card required for tests");
  }

  private static boolean isBlackListed(NetworkInterface networkInterface) {
    return networkInterface.getName().startsWith("docker") ||
      networkInterface.getName().startsWith("vboxnet");
  }
}
