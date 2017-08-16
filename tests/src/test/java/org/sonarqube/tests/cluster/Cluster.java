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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;

import static org.sonarqube.tests.cluster.Cluster.NodeType.APPLICATION;
import static org.sonarqube.tests.cluster.Cluster.NodeType.SEARCH;

public class Cluster {

  protected static final String CLUSTER_ENABLED = "sonar.cluster.enabled";
  protected static final String CLUSTER_NODE_TYPE = "sonar.cluster.node.type";
  protected static final String CLUSTER_SEARCH_HOSTS = "sonar.cluster.search.hosts";
  protected static final String CLUSTER_HOSTS = "sonar.cluster.hosts";
  protected static final String CLUSTER_NODE_PORT = "sonar.cluster.node.port";
  protected static final String CLUSTER_NODE_HOST = "sonar.cluster.node.host";
  protected static final String CLUSTER_NAME = "sonar.cluster.name";

  protected static final String SEARCH_HOST = "sonar.search.host";
  protected static final String SEARCH_PORT = "sonar.search.port";
  protected static final String SEARCH_JAVA_OPTS = "sonar.search.javaOpts";

  protected static final String WEB_JAVA_OPTS = "sonar.web.javaOpts";
  protected static final String WEB_PORT = "sonar.web.port";

  protected static final String CE_JAVA_OPTS = "sonar.ce.javaOpts";

  public enum NodeType {
    SEARCH("search"), APPLICATION("application");

    final String value;

    NodeType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

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

  public void stopAll(Predicate<Node> predicate) throws ExecutionException, InterruptedException {
    forkJoinPool.submit(
      () -> nodes.parallelStream()
      .filter(predicate)
      .forEach(n -> n.getOrchestrator().stop())
    ).get();
  }

  public void startAll(Predicate<Node> predicate) throws ExecutionException, InterruptedException {
    forkJoinPool.submit(
      () -> nodes.parallelStream()
      .filter(predicate)
      .forEach(n -> n.getOrchestrator().start())
    ).get();
  }

  public List<Node> getNodes() {
    return Collections.unmodifiableList(nodes);
  }

  private void assignPorts() {
    nodes.stream().forEach(
      node -> {
        node.setHzPort(NetworkUtils.getNextAvailablePort(getNonloopbackIPv4Address()));
        if (node.getType() == SEARCH) {
          node.setEsPort(NetworkUtils.getNextAvailablePort(getNonloopbackIPv4Address()));
        } else if (node.getType() == APPLICATION) {
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
      .filter(node -> node.getType() == SEARCH)
      .map(node -> HostAndPort.fromParts(inet, node.getEsPort()).toString())
      .collect(Collectors.joining(","));

    nodes.forEach(
      node -> {
        node.addProperty(CLUSTER_NODE_HOST, inet);
        node.addProperty(CLUSTER_HOSTS, clusterHosts);
        node.addProperty(CLUSTER_NODE_PORT, Integer.toString(node.getHzPort() == null ? -1 : node.getHzPort()));
        node.addProperty(CLUSTER_SEARCH_HOSTS, elasticsearchHosts);
        node.addProperty(SEARCH_PORT, Integer.toString(node.getEsPort() == null ? -1 : node.getEsPort()));
        node.addProperty(SEARCH_HOST, inet);
        node.addProperty(WEB_PORT, Integer.toString(node.getWebPort() == null ? -1 : node.getWebPort()));
        node.addProperty(CLUSTER_NODE_TYPE, node.getType().getValue());
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

    public Builder addNode(NodeType type) {
      nodes.add(new Node(type));
      return this;
    }
  }

  /**
   * A cluster node
   */
  public static class Node {
    private final NodeType type;
    private Integer webPort;
    private Integer esPort;
    private Integer hzPort;
    private Orchestrator orchestrator = null;
    private Properties properties = new Properties();

    public Node(NodeType type) {
      this.type = type;

      // Default properties
      properties.setProperty(CLUSTER_ENABLED, "true");
      properties.setProperty(CLUSTER_NAME, "sonarqube");
      properties.setProperty(CE_JAVA_OPTS, "-Xmx256m");
      properties.setProperty(WEB_JAVA_OPTS, "-Xmx256m");
      properties.setProperty(SEARCH_JAVA_OPTS,  "-Xmx256m -Xms256m " +
          "-XX:+UseConcMarkSweepGC " +
          "-XX:CMSInitiatingOccupancyFraction=75 " +
          "-XX:+UseCMSInitiatingOccupancyOnly " +
          "-XX:+AlwaysPreTouch " +
          "-server " +
          "-Xss1m " +
          "-Djava.awt.headless=true " +
          "-Dfile.encoding=UTF-8 " +
          "-Djna.nosys=true " +
          "-Djdk.io.permissionsUseCanonicalPath=true " +
          "-Dio.netty.noUnsafe=true " +
          "-Dio.netty.noKeySetOptimization=true " +
          "-Dio.netty.recycler.maxCapacityPerThread=0 " +
          "-Dlog4j.shutdownHookEnabled=false " +
          "-Dlog4j2.disable.jmx=true " +
          "-Dlog4j.skipJansi=true " +
          "-XX:+HeapDumpOnOutOfMemoryError");
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

    public NodeType getType() {
      return type;
    }

    @CheckForNull
    public Integer getWebPort() {
      return webPort;
    }

    @CheckForNull
    public Integer getEsPort() {
      return esPort;
    }

    @CheckForNull
    public Integer getHzPort() {
      return hzPort;
    }

    private void setWebPort(Integer webPort) {
      this.webPort = webPort;
    }

    private void setEsPort(Integer esPort) {
      this.esPort = esPort;
    }

    private void setHzPort(Integer hzPort) {
      this.hzPort = hzPort;
    }

    private void addProperty(String key, String value) {
      properties.setProperty(key, value);
    }
  }
}
