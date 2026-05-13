/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.IndexState;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.response.NodeStats;
import org.sonar.server.es.response.NodeStatsResponse;

public class ElasticSearchMetricTask implements MonitoringTask {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchMetricTask.class);

  private static final String DELAY_IN_MILLISECONDS_PROPERTY = "sonar.server.monitoring.es.initial.delay";
  private static final String PERIOD_IN_MILLISECONDS_PROPERTY = "sonar.server.monitoring.es.period";
  private static final String DISK_SPACE_THRESHOLD_PROPERTY = "sonar.server.monitoring.es.disk.threshold.percent";
  private static final double DEFAULT_DISK_SPACE_THRESHOLD_PERCENT = 15.0;

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
    NodeStatsResponse nodeStatsResponse = null;
    try {
      nodeStatsResponse = esClient.nodesStats();
    } catch (Exception e) {
      LOG.error("Failed to query ES node stats", e);
    }
    updateElasticSearchHealthStatus(nodeStatsResponse);
    updateFileSystemMetrics(nodeStatsResponse);
  }

  private void updateElasticSearchHealthStatus(@Nullable final NodeStatsResponse nodeStatsResponse) {
    try {
      HealthStatus esStatus = esClient.clusterHealthV2(req -> req).status();

      long readOnlyIndicesCount = updateAndGetReadOnlyIndicesCount();
      final double thresholdPercent = config.getDouble(DISK_SPACE_THRESHOLD_PROPERTY)
        .orElse(DEFAULT_DISK_SPACE_THRESHOLD_PERCENT);
      double maxDiskUsagePercent = updateAndGetMaxDiskUsagePercent(nodeStatsResponse, thresholdPercent);

      if (readOnlyIndicesCount > 0) {
        LOG.warn("Elasticsearch indices are in read-only mode, likely due to disk space issues");
        serverMonitoringMetrics.setElasticSearchStatusToRed();
        return;
      }

      final double freePercent = 100.0 - maxDiskUsagePercent;

      if (freePercent < thresholdPercent) {
        LOG.warn("Elasticsearch nodes have critically low disk space");
        serverMonitoringMetrics.setElasticSearchStatusToRed();
        return;
      }

      // Fall back to cluster health status
      if (esStatus == null) {
        serverMonitoringMetrics.setElasticSearchStatusToRed();
      } else {
        switch (esStatus) {
          case Green, Yellow:
            serverMonitoringMetrics.setElasticSearchStatusToGreen();
            break;
          case Red:
            serverMonitoringMetrics.setElasticSearchStatusToRed();
            break;
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to query ES status", e);
      serverMonitoringMetrics.setElasticSearchStatusToRed();
    }
  }

  private void updateFileSystemMetrics(@Nullable final NodeStatsResponse nodeStatsResponse) {
    try {
      if (nodeStatsResponse == null || nodeStatsResponse.getNodeStats().isEmpty()) {
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

  private long updateAndGetReadOnlyIndicesCount() {
    try {
      final GetIndicesSettingsResponse settingsResponse = esClient.getSettingsV2(req -> req.index("*"));
      final Map<String, IndexState> indices = settingsResponse.result();

      long readOnlyCount = 0;
      for (Map.Entry<String, IndexState> entry : indices.entrySet()) {
        final String indexName = entry.getKey();
        final IndexState indexState = entry.getValue();

        if (indexState.settings() != null && indexState.settings().index() != null) {
          final var blocks = indexState.settings().index().blocks();
          if (blocks != null && Boolean.TRUE.equals(blocks.readOnlyAllowDelete())) {
            LOG.warn("Index [{}] is in read-only mode (index.blocks.read_only_allow_delete=true)", indexName);
            readOnlyCount++;
          }
        }
      }

      // Update metric for observability
      serverMonitoringMetrics.setElasticSearchReadOnlyIndicesCount(readOnlyCount);

      return readOnlyCount;
    } catch (Exception e) {
      LOG.error("Failed to check for readonly indices", e);
      // Return 0 on error to avoid false positives - let cluster health handle it
      return 0;
    }
  }

  private double updateAndGetMaxDiskUsagePercent(@Nullable NodeStatsResponse nodeStatsResponse, double thresholdPercent) {
    try {
      if (nodeStatsResponse == null || nodeStatsResponse.getNodeStats().isEmpty()) {
        return 0.0;
      }

      double maxDiskUsagePercent = 0.0;

      for (NodeStats nodeStat : nodeStatsResponse.getNodeStats()) {
        final long availableBytes = nodeStat.getDiskAvailableBytes();
        final long totalBytes = nodeStat.getDiskTotalBytes();

        if (totalBytes > 0) {
          final double freePercent = (availableBytes * 100.0) / totalBytes;
          final double usedPercent = 100.0 - freePercent;
          maxDiskUsagePercent = Math.max(maxDiskUsagePercent, usedPercent);

          if (freePercent < thresholdPercent) {
            LOG.warn("Node [{}] has critically low disk space: {}% free ({} bytes available out of {} bytes total)",
              nodeStat.getName(), String.format(Locale.ROOT, "%.2f", freePercent), availableBytes, totalBytes);
          }
        }
      }

      // Update metric for observability
      serverMonitoringMetrics.setElasticSearchDiskUsagePercent(maxDiskUsagePercent);

      return maxDiskUsagePercent;
    } catch (Exception e) {
      LOG.error("Failed to check disk space", e);
      // Return 0.0 on error to avoid false positives
      return 0.0;
    }
  }

  @Override
  public long getDelay() {
    return config.getLong(DELAY_IN_MILLISECONDS_PROPERTY).orElse(10_000L);
  }

  @Override
  public long getPeriod() {
    return config.getLong(PERIOD_IN_MILLISECONDS_PROPERTY).orElse(10_000L);
  }
}
