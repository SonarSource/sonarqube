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
package org.sonar.server.health;

import java.util.Random;
import java.util.function.Supplier;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.cluster.health.NodeDetails;
import org.sonar.cluster.health.NodeHealth;
import org.sonar.cluster.health.NodeHealthProvider;

import static java.lang.String.format;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NAME;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_PORT;
import static org.sonar.cluster.health.NodeDetails.newNodeDetailsBuilder;
import static org.sonar.cluster.health.NodeHealth.newNodeHealthBuilder;

public class NodeHealthProviderImpl implements NodeHealthProvider {
  private final HealthChecker healthChecker;
  private final NodeHealth.Builder nodeHealthBuilder;
  private final NodeDetails nodeDetails;
  private final System2 system2;

  public NodeHealthProviderImpl(Configuration configuration, HealthChecker healthChecker, Server server, System2 system2) {
    this.system2 = system2;
    this.healthChecker = healthChecker;
    this.nodeHealthBuilder = newNodeHealthBuilder();
    this.nodeDetails = newNodeDetailsBuilder()
      .setName(computeName(configuration))
      .setType(NodeDetails.Type.APPLICATION)
      .setHost(computeIp(configuration))
      .setPort(computePort(configuration))
      .setStarted(server.getStartedAt().getTime())
      .build();
  }

  private static String computeName(Configuration configuration) {
    String clusterName = configuration.get(CLUSTER_NAME)
      .orElseThrow(missingPropertyISE(CLUSTER_NAME));
    return clusterName + new Random().nextInt(999);
  }

  private static String computeIp(Configuration configuration) {
    return "hardcoded IP";
    // return configuration.get("sonar.cluster.node.host")
    // .orElseThrow(missingPropertyISE("sonar.cluster.node.host"));
  }

  private static int computePort(Configuration configuration) {
    return configuration.getInt(CLUSTER_NODE_PORT)
      .orElseThrow(missingPropertyISE(CLUSTER_NODE_PORT));
  }

  private static Supplier<IllegalStateException> missingPropertyISE(String propertyName) {
    return () -> new IllegalStateException(format("Property %s is not defined", propertyName));
  }

  @Override
  public NodeHealth get() {
    Health nodeHealth = healthChecker.checkNode();
    long now = system2.now();
    this.nodeHealthBuilder
      .clearCauses()
      .setStatus(NodeHealth.Status.valueOf(nodeHealth.getStatus().name()));
    nodeHealth.getCauses().forEach(this.nodeHealthBuilder::addCause);

    return this.nodeHealthBuilder
      .setDate(now)
      .setDetails(nodeDetails)
      .build();
  }
}
