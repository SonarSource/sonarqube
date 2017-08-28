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

import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.sonar.server.es.EsClient;

import static org.sonar.server.health.Health.newHealthCheckBuilder;

/**
 * Checks the ElasticSearch cluster status.
 */
public class EsStatusNodeCheck implements NodeHealthCheck {
  private static final Health YELLOW_HEALTH = newHealthCheckBuilder()
    .setStatus(Health.Status.YELLOW)
    .addCause("Elasticsearch status is YELLOW")
    .build();
  private static final Health RED_HEALTH = newHealthCheckBuilder()
    .setStatus(Health.Status.RED)
    .addCause("Elasticsearch status is RED")
    .build();

  private final EsClient esClient;

  public EsStatusNodeCheck(EsClient esClient) {
    this.esClient = esClient;
  }

  @Override
  public Health check() {
    ClusterHealthStatus esStatus = esClient.prepareClusterStats().get().getStatus();
    switch (esStatus) {
      case GREEN:
        return Health.GREEN;
      case YELLOW:
        return YELLOW_HEALTH;
      case RED:
        return RED_HEALTH;
      default:
        throw new IllegalArgumentException("Unsupported Elasticsearch status " + esStatus);
    }
  }
}
