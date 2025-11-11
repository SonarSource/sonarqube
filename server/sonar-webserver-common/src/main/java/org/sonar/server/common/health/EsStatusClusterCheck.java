/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.common.health;

import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import java.util.Set;
import org.sonar.process.cluster.health.NodeHealth;
import org.sonar.server.es.EsClient;
import org.sonar.server.health.Health;

public class EsStatusClusterCheck extends EsStatusCheck implements ClusterHealthCheck {
  private static final String MINIMUM_NODE_MESSAGE = "There should be at least three search nodes";
  private static final int RECOMMENDED_MIN_NUMBER_OF_ES_NODES = 3;

  public EsStatusClusterCheck(EsClient esClient) {
    super(esClient);
  }

  @Override
  public Health check(Set<NodeHealth> nodeHealths) {
    HealthResponse esClusterHealth = this.getEsClusterHealth();

    if (esClusterHealth == null) {
      return RED_HEALTH_UNAVAILABLE;
    }

    Health minimumNodes = checkMinimumNodes(esClusterHealth);
    Health clusterStatus = extractStatusHealth(esClusterHealth);
    return HealthReducer.merge(minimumNodes, clusterStatus);

  }

  private static Health checkMinimumNodes(HealthResponse esClusterHealth) {
    int nodeCount = esClusterHealth.numberOfNodes();
    if (nodeCount < RECOMMENDED_MIN_NUMBER_OF_ES_NODES) {
      return Health.builder()
        .setStatus(Health.Status.YELLOW)
        .addCause(MINIMUM_NODE_MESSAGE)
        .build();
    }
    return Health.GREEN;
  }

}
