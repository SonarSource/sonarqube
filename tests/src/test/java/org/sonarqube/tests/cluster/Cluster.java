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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;
import util.ItUtils;

import static java.util.stream.Collectors.joining;

class Cluster implements AutoCloseable {

  @Nullable
  private final String clusterName;

  private final List<Node> nodes = new ArrayList<>();
  private final String systemPassCode = "fooBar2000";

  Cluster(@Nullable String name) {
    this.clusterName = name;
  }

  Node startNode(NodeConfig config, Consumer<OrchestratorBuilder> consumer) {
    Node node = addNode(config, consumer);
    node.start();
    return node;
  }

  Node addNode(NodeConfig config, Consumer<OrchestratorBuilder> consumer) {
    OrchestratorBuilder builder = newOrchestratorBuilder(config);
    builder.setServerProperty("sonar.web.systemPasscode", systemPassCode);

    switch (config.getType()) {
      case SEARCH:
        builder
          .setServerProperty("sonar.cluster.node.type", "search")
          .setServerProperty("sonar.search.host", config.getAddress().getHostAddress())
          .setServerProperty("sonar.search.port", "" + config.getSearchPort().get())
          .setServerProperty("sonar.search.javaOpts", "-Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError");
        break;
      case APPLICATION:
        builder
          .setServerProperty("sonar.cluster.node.type", "application")
          .setServerProperty("sonar.web.host", config.getAddress().getHostAddress())
          .setServerProperty("sonar.web.port", "" + config.getWebPort().get())
          .setServerProperty("sonar.web.javaOpts", "-Xmx128m -Xms16m -XX:+HeapDumpOnOutOfMemoryError")
          .setServerProperty("sonar.auth.jwtBase64Hs256Secret", "HrPSavOYLNNrwTY+SOqpChr7OwvbR/zbDLdVXRN0+Eg=")
          .setServerProperty("sonar.ce.javaOpts", "-Xmx32m -Xms16m -XX:+HeapDumpOnOutOfMemoryError");
        break;
    }
    consumer.accept(builder);
    Orchestrator orchestrator = builder.build();
    Node node = new Node(config, orchestrator, systemPassCode);
    nodes.add(node);
    return node;
  }

  Stream<Node> getNodes() {
    return nodes.stream();
  }

  Stream<Node> getAppNodes() {
    return nodes.stream().filter(n -> n.getConfig().getType() == NodeConfig.NodeType.APPLICATION);
  }

  Node getAppNode(int index) {
    return getAppNodes().skip(index).findFirst().orElseThrow(IllegalArgumentException::new);
  }

  Stream<Node> getSearchNodes() {
    return nodes.stream().filter(n -> n.getConfig().getType() == NodeConfig.NodeType.SEARCH);
  }

  Node getSearchNode(int index) {
    return getSearchNodes().skip(index).findFirst().orElseThrow(IllegalArgumentException::new);
  }

  @Override
  public void close() throws Exception {
    // nodes are stopped in order of creation
    for (Node node : nodes) {
      try {
        node.stop();
      } catch (Exception e) {
        LoggerFactory.getLogger(getClass()).error("Fail to stop node", e);
      }
    }
  }

  private OrchestratorBuilder newOrchestratorBuilder(NodeConfig node) {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    builder.setOrchestratorProperty("orchestrator.keepDatabase", "true");
    builder.setServerProperty("sonar.cluster.enabled", "true");
    builder.setServerProperty("sonar.cluster.node.host", node.getAddress().getHostAddress());
    builder.setServerProperty("sonar.cluster.node.port", "" + node.getHzPort());
    builder.setServerProperty("sonar.cluster.hosts", node.getConnectedNodes().stream().map(NodeConfig::getHzHost).collect(joining(",")));
    builder.setServerProperty("sonar.cluster.search.hosts", node.getSearchNodes().stream().map(NodeConfig::getSearchHost).collect(joining(",")));
    if (clusterName != null) {
      builder.setServerProperty("sonar.cluster.name", clusterName);
    }
    if (node.getName().isPresent()) {
      builder.setServerProperty("sonar.cluster.node.name", node.getName().get());
    }
    builder.addPlugin(ItUtils.pluginArtifact("server-plugin"));
    builder.setStartupLogWatcher(logLine -> true);
    return builder;
  }
}
