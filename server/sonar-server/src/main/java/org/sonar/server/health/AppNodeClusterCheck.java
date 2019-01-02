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
package org.sonar.server.health;

import java.util.Arrays;
import java.util.Set;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;

import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.process.cluster.health.NodeHealth.Status.GREEN;
import static org.sonar.process.cluster.health.NodeHealth.Status.RED;
import static org.sonar.process.cluster.health.NodeHealth.Status.YELLOW;
import static org.sonar.server.health.Health.newHealthCheckBuilder;

public class AppNodeClusterCheck implements ClusterHealthCheck {

  @Override
  public Health check(Set<NodeHealth> nodeHealths) {
    Set<NodeHealth> appNodes = nodeHealths.stream()
      .filter(s -> s.getDetails().getType() == NodeDetails.Type.APPLICATION)
      .collect(toSet());

    return Arrays.stream(AppNodeClusterHealthSubChecks.values())
      .map(s -> s.check(appNodes))
      .reduce(Health.GREEN, HealthReducer.INSTANCE);
  }

  private enum AppNodeClusterHealthSubChecks implements ClusterHealthSubCheck {
    NO_APPLICATION_NODE() {
      @Override
      public Health check(Set<NodeHealth> appNodes) {
        int appNodeCount = appNodes.size();
        if (appNodeCount == 0) {
          return newHealthCheckBuilder()
            .setStatus(Health.Status.RED)
            .addCause("No application node")
            .build();
        }
        return Health.GREEN;
      }
    },
    MIN_APPLICATION_NODE_COUNT() {
      @Override
      public Health check(Set<NodeHealth> appNodes) {
        int appNodeCount = appNodes.size();
        if (appNodeCount == 1) {
          return newHealthCheckBuilder()
            .setStatus(Health.Status.YELLOW)
            .addCause("There should be at least two application nodes")
            .build();
        }
        return Health.GREEN;
      }
    },
    REPORT_RED_OR_YELLOW_NODES() {
      @Override
      public Health check(Set<NodeHealth> appNodes) {
        int appNodeCount = appNodes.size();
        if (appNodeCount == 0) {
          // skipping this check
          return Health.GREEN;
        }

        long redNodesCount = withStatus(appNodes, RED).count();
        long yellowNodesCount = withStatus(appNodes, YELLOW).count();
        if (redNodesCount == 0 && yellowNodesCount == 0) {
          return Health.GREEN;
        }

        Health.Builder builder = newHealthCheckBuilder();
        if (redNodesCount == appNodeCount) {
          return builder
            .setStatus(Health.Status.RED)
            .addCause("Status of all application nodes is RED")
            .build();
        } else if (redNodesCount > 0) {
          builder.addCause("At least one application node is RED");
        }
        if (yellowNodesCount == appNodeCount) {
          return builder
            .setStatus(Health.Status.YELLOW)
            .addCause("Status of all application nodes is YELLOW")
            .build();
        } else if (yellowNodesCount > 0) {
          builder.addCause("At least one application node is YELLOW");
        }
        long greenNodesCount = withStatus(appNodes, GREEN).count();
        builder.setStatus(greenNodesCount > 0 || yellowNodesCount > 0 ? Health.Status.YELLOW : Health.Status.RED);

        return builder.build();
      }
    }
  }
}
