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

import com.google.common.net.HostAndPort;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.util.NetworkUtils;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static org.sonarqube.tests.cluster.Cluster.NodeType.CE;
import static org.sonarqube.tests.cluster.Cluster.NodeType.ES;
import static org.sonarqube.tests.cluster.Cluster.NodeType.WEB;

public class Cluster {

  protected static final String CLUSTER_ENABLED = "sonar.cluster.enabled";
  protected static final String CLUSTER_CE_DISABLED = "sonar.cluster.ce.disabled";
  protected static final String CLUSTER_SEARCH_DISABLED = "sonar.cluster.search.disabled";
  protected static final String CLUSTER_SEARCH_HOSTS = "sonar.cluster.search.hosts";
  protected static final String CLUSTER_WEB_DISABLED = "sonar.cluster.web.disabled";
  protected static final String CLUSTER_HOSTS = "sonar.cluster.hosts";
  protected static final String CLUSTER_PORT = "sonar.cluster.port";
  protected static final String CLUSTER_NETWORK_INTERFACES = "sonar.cluster.networkInterfaces";
  protected static final String CLUSTER_NAME = "sonar.cluster.name";

  protected static final String SEARCH_HOST = "sonar.search.host";
  protected static final String SEARCH_PORT = "sonar.search.port";
  protected static final String SEARCH_JAVA_OPTS = "sonar.search.javaOpts";

  protected static final String WEB_JAVA_OPTS = "sonar.web.javaOpts";
  protected static final String WEB_PORT = "sonar.web.port";

  protected static final String CE_JAVA_OPTS = "sonar.ce.javaOpts";

  public enum NodeType {
    ES, CE, WEB;

    public static final EnumSet<NodeType> ALL = EnumSet.allOf(NodeType.class);
  }

  private final List<Node> nodes;
  private final ForkJoinPool forkJoinPool = new ForkJoinPool(5);

  private Cluster(List<Node> nodes) {
    this.nodes = nodes;
    assignPorts();
    completeNodesConfiguration();
    buildOrchestrators();
  }

  public void start() throws ExecutionException, InterruptedException {
    forkJoinPool.submit(
      () -> nodes.parallelStream().forEach(
        node -> node.getOrchestrator().start()
      )
    ).get();
  }

  public void stop() throws ExecutionException, InterruptedException {
    forkJoinPool.submit(
      () -> nodes.parallelStream().forEach(
        node -> node.getOrchestrator().stop()
      )
    ).get();
  }

  private void assignPorts() {
    nodes.stream().forEach(
      node -> {
        node.setHzPort(NetworkUtils.getNextAvailablePort(getNonloopbackIPv4Address()));
        if (node.getTypes().contains(ES)) {
          node.setEsPort(NetworkUtils.getNextAvailablePort(getNonloopbackIPv4Address()));
        }
        if (node.getTypes().contains(WEB)) {
          node.setWebPort(NetworkUtils.getNextAvailablePort(getNonloopbackIPv4Address()));
        }
      }
    );
  }

  public static InetAddress getNonloopbackIPv4Address()  {
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

  private void completeNodesConfiguration() {
    String inet = getNonloopbackIPv4Address().getHostAddress();
    String clusterHosts = nodes.stream()
      .map(node -> HostAndPort.fromParts(inet, node.getHzPort()).toString())
      .collect(Collectors.joining(","));
    String elasticsearchHosts = nodes.stream()
      .filter(node -> node.getTypes().contains(ES))
      .map(node -> HostAndPort.fromParts(inet, node.getEsPort()).toString())
      .collect(Collectors.joining(","));

    nodes.stream().forEach(
      node -> {
        node.addProperty(CLUSTER_NETWORK_INTERFACES, inet);
        node.addProperty(CLUSTER_HOSTS, clusterHosts);
        node.addProperty(CLUSTER_PORT, Integer.toString(node.getHzPort()));
        node.addProperty(CLUSTER_SEARCH_HOSTS, elasticsearchHosts);
        node.addProperty(SEARCH_PORT, Integer.toString(node.getEsPort()));
        node.addProperty(SEARCH_HOST, inet);
        node.addProperty(WEB_PORT, Integer.toString(node.getWebPort()));

        if (!node.getTypes().contains(CE)) {
          node.addProperty(CLUSTER_CE_DISABLED, "true");
        }
        if (!node.getTypes().contains(WEB)) {
          node.addProperty(CLUSTER_WEB_DISABLED, "true");
        }
        if (!node.getTypes().contains(ES)) {
          node.addProperty(CLUSTER_SEARCH_DISABLED, "true");
        }
      }
    );
  }

  private void buildOrchestrators() {
    nodes.stream().limit(1).forEach(
      node -> buildOrchestrator(node, false)
    );
    nodes.stream().skip(1).forEach(
      node -> buildOrchestrator(node, true)
    );
  }

  private void buildOrchestrator(Node node, boolean keepDatabase) {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setOrchestratorProperty("orchestrator.keepDatabase", Boolean.toString(keepDatabase))
      .setStartupLogWatcher(new StartupLogWatcherImpl());

    node.getProperties().entrySet().stream().forEach(
      e -> builder.setServerProperty((String) e.getKey(), (String) e.getValue())
    );

    node.setOrchestrator(builder.build());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder  {
    private final List<Node> nodes = new ArrayList<>();

    public Cluster build() {
      return new Cluster(nodes);
    }

    public Builder addNode(EnumSet<NodeType> types) {
      nodes.add(new Node(types));
      return this;
    }
  }

  /**
   * A cluster node
   */
  public static class Node {
    private final EnumSet<NodeType> types;
    private int webPort;
    private int esPort;
    private int hzPort;
    private Orchestrator orchestrator = null;
    private Properties properties = new Properties();

    public Node(EnumSet<NodeType> types) {
      this.types = types;

      // Default properties
      properties.setProperty(CLUSTER_ENABLED, "true");
      properties.setProperty(CLUSTER_NAME, "sonarqube");
      properties.setProperty(CE_JAVA_OPTS, "-Xmx128m -Xms128m -XX:+HeapDumpOnOutOfMemoryError");
      properties.setProperty(WEB_JAVA_OPTS, "-Xmx128m -Xms128m -XX:+HeapDumpOnOutOfMemoryError");
      properties.setProperty(SEARCH_JAVA_OPTS, "-Xmx512m -Xms128m -XX:+HeapDumpOnOutOfMemoryError -Xss256k -Djna.nosys=true " +
        " -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly ");
    }

    public Properties getProperties() {
      return properties;
    }

    public Orchestrator getOrchestrator() {
      return orchestrator;
    }

    private void setOrchestrator(Orchestrator orchestrator) {
      this.orchestrator = orchestrator;
    }

    public EnumSet<NodeType> getTypes() {
      return types;
    }

    public int getWebPort() {
      return webPort;
    }

    public int getEsPort() {
      return esPort;
    }

    public int getHzPort() {
      return hzPort;
    }

    private void setWebPort(int webPort) {
      this.webPort = webPort;
    }

    private void setEsPort(int esPort) {
      this.esPort = esPort;
    }

    private void setHzPort(int hzPort) {
      this.hzPort = hzPort;
    }

    private void addProperty(String key, String value) {
      properties.setProperty(key, value);
    }
  }
}
