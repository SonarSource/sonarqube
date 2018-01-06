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

import java.util.Arrays;
import java.util.Set;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;

import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.process.cluster.health.NodeHealth.Status.GREEN;
import static org.sonar.process.cluster.health.NodeHealth.Status.RED;
import static org.sonar.process.cluster.health.NodeHealth.Status.YELLOW;
import static org.sonar.server.health.Health.newHealthCheckBuilder;

public class SearchNodeClusterCheck implements ClusterHealthCheck {

  @Override
  public Health check(Set<NodeHealth> nodeHealths) {
    Set<NodeHealth> searchNOdes = nodeHealths.stream()
      .filter(s -> s.getDetails().getType() == NodeDetails.Type.SEARCH)
      .collect(toSet());

    return Arrays.stream(SearchNodeClusterSubChecks.values())
      .map(s -> s.check(searchNOdes))
      .reduce(Health.GREEN, HealthReducer.INSTANCE);
  }

  private enum SearchNodeClusterSubChecks implements ClusterHealthSubCheck {
    NO_SEARCH_NODE() {
      @Override
      public Health check(Set<NodeHealth> searchNodes) {
        int searchNodeCount = searchNodes.size();
        if (searchNodeCount == 0) {
          return newHealthCheckBuilder()
            .setStatus(Health.Status.RED)
            .addCause("No search node")
            .build();
        }
        return Health.GREEN;
      }
    },
    NUMBER_OF_NODES() {
      @Override
      public Health check(Set<NodeHealth> searchNodes) {
        int searchNodeCount = searchNodes.size();
        if (searchNodeCount == 0) {
          // skipping this check
          return Health.GREEN;
        }

        if (searchNodeCount < 3) {
          long yellowGreenNodesCount = withStatus(searchNodes, GREEN, YELLOW).count();
          return newHealthCheckBuilder()
            .setStatus(yellowGreenNodesCount > 1 ? Health.Status.YELLOW : Health.Status.RED)
            .addCause("There should be at least three search nodes")
            .build();
        }
        if (searchNodeCount > 3 && isEven(searchNodeCount)) {
          return newHealthCheckBuilder()
            .setStatus(Health.Status.YELLOW)
            .addCause("There should be an odd number of search nodes")
            .build();
        }
        return Health.GREEN;
      }

      private boolean isEven(int searchNodeCount) {
        return searchNodeCount % 2 == 0;
      }
    },
    RED_OR_YELLOW_NODES() {
      @Override
      public Health check(Set<NodeHealth> searchNodes) {
        int searchNodeCount = searchNodes.size();
        if (searchNodeCount == 0) {
          // skipping this check
          return Health.GREEN;
        }

        long redNodesCount = withStatus(searchNodes, RED).count();
        long yellowNodesCount = withStatus(searchNodes, YELLOW).count();
        if (redNodesCount == 0 && yellowNodesCount == 0) {
          return Health.GREEN;
        }

        Health.Builder builder = newHealthCheckBuilder();
        if (redNodesCount == searchNodeCount) {
          return builder
            .setStatus(Health.Status.RED)
            .addCause("Status of all search nodes is RED")
            .build();
        } else if (redNodesCount > 0) {
          builder.addCause("At least one search node is RED");
        }
        if (yellowNodesCount == searchNodeCount) {
          return builder
            .setStatus(Health.Status.YELLOW)
            .addCause("Status of all search nodes is YELLOW")
            .build();
        } else if (yellowNodesCount > 0) {
          builder.addCause("At least one search node is YELLOW");
        }

        long greenNodesCount = withStatus(searchNodes, GREEN).count();
        builder.setStatus(greenNodesCount + yellowNodesCount > 1 ? Health.Status.YELLOW : Health.Status.RED);

        return builder.build();
      }
    }
  }
}
