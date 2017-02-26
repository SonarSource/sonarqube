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

package org.sonar.application;

import com.hazelcast.core.HazelcastException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ClusterTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_cluster() throws Exception {
    Properties properties = new Properties();
    properties.put(ClusterParameters.ENABLED.getName(), "true");
    ClusterProperties clusterProperties = toClusterProperties(properties);

    try (Cluster cluster = new Cluster(clusterProperties)) {
      assertThat(
        cluster.isActive()
      ).isEqualTo(true);
    }

    properties.put(ClusterParameters.ENABLED.getName(), "false");
    clusterProperties = toClusterProperties(properties);
    try (Cluster cluster = new Cluster(clusterProperties)) {
      assertThat(
        cluster.isActive()
      ).isEqualTo(false);
    }
  }

  @Test
  public void test_interface() throws Exception {
    String ipAddress = findIPv4Address().getHostAddress();
    int port = findAvailablePort();

    Properties properties = new Properties();
    properties.put(ClusterParameters.INTERFACES.getName(), ipAddress);
    properties.put(ClusterParameters.PORT.getName(), Integer.toString(port));
    properties.put(ClusterParameters.ENABLED.getName(), "true");
    ClusterProperties clusterProperties = toClusterProperties(properties);

    try (Cluster cluster = new Cluster(clusterProperties)) {
      assertThat(
        cluster.hazelcastInstance.getConfig().getNetworkConfig().getInterfaces().isEnabled()
      ).isEqualTo(true);
      assertThat(
        cluster.hazelcastInstance.getConfig().getNetworkConfig().getInterfaces().getInterfaces()
      ).containsExactly(ipAddress);
      InetSocketAddress localSocket = (InetSocketAddress) cluster.hazelcastInstance.getLocalEndpoint().getSocketAddress();
      assertThat(
        (localSocket).getPort()
      ).isEqualTo(port);
      assertThat(
        (localSocket).getAddress().getHostAddress()
      ).isEqualTo(ipAddress);
    }
  }

  @Test
  public void test_with_already_used_port() throws IOException {
    InetAddress ipAddress = findIPv4Address();

    Cluster cluster = null;

    try (ServerSocket socket = new ServerSocket(0, 50, ipAddress)) {
      Properties properties = new Properties();
      properties.put(ClusterParameters.INTERFACES.getName(), ipAddress.getHostAddress());
      properties.put(ClusterParameters.PORT.getName(), Integer.toString(socket.getLocalPort()));
      properties.put(ClusterParameters.ENABLED.getName(), "true");
      ClusterProperties clusterProperties = toClusterProperties(properties);

      expectedException.expect(HazelcastException.class);
      cluster = new Cluster(clusterProperties);
    } finally {
      if (cluster != null) {
        cluster.close();
      }
    }
  }

  @Test
  public void test_adding_port_to_members() throws Exception {
    InetAddress ipAddress = findIPv4Address();

    Properties properties = new Properties();
    properties.put(ClusterParameters.ENABLED.getName(), "true");
    properties.put(ClusterParameters.MEMBERS.getName(), ipAddress.getHostAddress());
    ClusterProperties clusterProperties = toClusterProperties(properties);

    try (Cluster cluster = new Cluster(clusterProperties)) {
      assertThat(
        cluster.hazelcastInstance.getConfig().getNetworkConfig().getJoin().getTcpIpConfig().getMembers()
      ).containsExactly(ipAddress.getHostAddress() + ":9003");
    }
  }

  private ClusterProperties toClusterProperties(Properties properties) {
    return new ClusterProperties(new Props(properties));
  }

  private InetAddress findIPv4Address() throws SocketException {
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();
      if (!networkInterface.isVirtual()
        && !networkInterface.isLoopback()
        && !networkInterface.getName().startsWith("docker")) {
        Enumeration<InetAddress> ips = networkInterface.getInetAddresses();
        while (ips.hasMoreElements()) {
          InetAddress ip = ips.nextElement();

          if (ip instanceof Inet4Address) {
            return ip;
          }
        }
      }
    }

    fail("Missing IPv4 address");
    return null;
  }

  private int findAvailablePort() throws IOException {
    try (ServerSocket ignored = new ServerSocket(0)) {
      return ignored.getLocalPort();
    }
  }
}
