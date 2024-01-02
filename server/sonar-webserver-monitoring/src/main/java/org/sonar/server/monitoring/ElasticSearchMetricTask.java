/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.monitoring;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.sonar.api.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.response.NodeStats;
import org.sonar.server.es.response.NodeStatsResponse;

public class ElasticSearchMetricTask implements MonitoringTask {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchMetricTask.class);

  private static final String DELAY_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.es.initial.delay";
  private static final String PERIOD_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.es.period";

  private final ServerMonitoringMetrics serverMonitoringMetrics;
  private final EsClient esClient;
  private final Configuration config;

  public ElasticSearchMetricTask(ServerMonitoringMetrics serverMonitoringMetrics, EsClient esClient, Configuration configuration) {
    this.serverMonitoringMetrics = serverMonitoringMetrics;
    this.esClient = esClient;
    config = configuration;
  }

  @Override
  public void run() {
    updateElasticSearchHealthStatus();
    updateFileSystemMetrics();
  }

  private void updateElasticSearchHealthStatus() {
    try {
      ClusterHealthStatus esStatus = esClient.clusterHealth(new ClusterHealthRequest()).getStatus();
      if (esStatus == null) {
        serverMonitoringMetrics.setElasticSearchStatusToRed();
      } else {
        switch (esStatus) {
          case GREEN, YELLOW:
            serverMonitoringMetrics.setElasticSearchStatusToGreen();
            break;
          case RED:
            serverMonitoringMetrics.setElasticSearchStatusToRed();
            break;
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to query ES status", e);
      serverMonitoringMetrics.setElasticSearchStatusToRed();
    }
  }

  private void updateFileSystemMetrics() {
    try {
      NodeStatsResponse nodeStatsResponse = esClient.nodesStats();
      if (nodeStatsResponse.getNodeStats().isEmpty()) {
        LOG.error("Failed to query ES status, no nodes stats returned by elasticsearch API");
      } else {
        for (NodeStats nodeStat : nodeStatsResponse.getNodeStats()) {
          serverMonitoringMetrics.setElasticSearchDiskSpaceFreeBytes(nodeStat.getName(), nodeStat.getDiskAvailableBytes());
          serverMonitoringMetrics.setElasticSearchDiskSpaceTotalBytes(nodeStat.getName(), nodeStat.getDiskTotalBytes());
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to query ES status", e);
    }
  }

  @Override
  public long getDelay() {
    return config.getLong(DELAY_IN_MILISECONDS_PROPERTY).orElse(10_000L);
  }

  @Override
  public long getPeriod() {
    return config.getLong(PERIOD_IN_MILISECONDS_PROPERTY).orElse(10_000L);
  }
}
