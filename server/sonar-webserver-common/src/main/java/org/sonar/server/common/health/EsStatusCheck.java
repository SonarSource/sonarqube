/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.es.EsClient;
import org.sonar.server.health.Health;

abstract class EsStatusCheck {
  private static final Logger LOG = LoggerFactory.getLogger(EsStatusCheck.class);

  private static final Health YELLOW_HEALTH = Health.builder()
    .setStatus(Health.Status.YELLOW)
    .addCause("Elasticsearch status is YELLOW")
    .build();
  private static final Health RED_HEALTH = Health.builder()
    .setStatus(Health.Status.RED)
    .addCause("Elasticsearch status is RED")
    .build();
  protected static final Health RED_HEALTH_UNAVAILABLE = Health.builder()
    .setStatus(Health.Status.RED)
    .addCause("Elasticsearch status is RED (unavailable)")
    .build();

  private final EsClient esClient;

  EsStatusCheck(EsClient esClient) {
    this.esClient = esClient;
  }

  protected ClusterHealthResponse getEsClusterHealth() {
    try {
      return esClient.clusterHealth(new ClusterHealthRequest());
    } catch (Exception e) {
      LOG.error("Failed to query ES status", e);
      return null;
    }
  }

  protected static Health extractStatusHealth(ClusterHealthResponse healthResponse) {
    ClusterHealthStatus esStatus = healthResponse.getStatus();
    if (esStatus == null) {
      return RED_HEALTH_UNAVAILABLE;
    }
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
