/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.function.Supplier;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.process.NetworkUtils;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;
import org.sonar.process.cluster.health.NodeHealthProvider;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_PORT;
import static org.sonar.process.cluster.health.NodeDetails.newNodeDetailsBuilder;
import static org.sonar.process.cluster.health.NodeHealth.newNodeHealthBuilder;

public class NodeHealthProviderImpl implements NodeHealthProvider {
  private final HealthChecker healthChecker;
  private final NodeHealth.Builder nodeHealthBuilder;
  private final NodeDetails nodeDetails;

  public NodeHealthProviderImpl(Configuration configuration, HealthChecker healthChecker, Server server, NetworkUtils networkUtils) {
    this.healthChecker = healthChecker;
    this.nodeHealthBuilder = newNodeHealthBuilder();
    this.nodeDetails = newNodeDetailsBuilder()
      .setName(computeName(configuration))
      .setType(NodeDetails.Type.APPLICATION)
      .setHost(computeHost(configuration, networkUtils))
      .setPort(computePort(configuration))
      .setStartedAt(server.getStartedAt().getTime())
      .build();
  }

  private static String computeName(Configuration configuration) {
    return configuration.get(CLUSTER_NODE_NAME.getKey())
      .orElseThrow(missingPropertyISE(CLUSTER_NODE_NAME.getKey()));
  }

  private static String computeHost(Configuration configuration, NetworkUtils networkUtils) {
    return configuration.get(CLUSTER_NODE_HOST.getKey())
      .filter(s -> !s.isEmpty())
      .orElseGet(networkUtils::getHostname);
  }

  private static int computePort(Configuration configuration) {
    return configuration.getInt(CLUSTER_NODE_PORT.getKey())
      .orElseThrow(missingPropertyISE(CLUSTER_NODE_PORT.getKey()));
  }

  private static Supplier<IllegalStateException> missingPropertyISE(String propertyName) {
    return () -> new IllegalStateException(format("Property %s is not defined", propertyName));
  }

  @Override
  public NodeHealth get() {
    Health nodeHealth = healthChecker.checkNode();
    this.nodeHealthBuilder
      .clearCauses()
      .setStatus(NodeHealth.Status.valueOf(nodeHealth.getStatus().name()));
    nodeHealth.getCauses().forEach(this.nodeHealthBuilder::addCause);

    return this.nodeHealthBuilder
      .setDetails(nodeDetails)
      .build();
  }
}
