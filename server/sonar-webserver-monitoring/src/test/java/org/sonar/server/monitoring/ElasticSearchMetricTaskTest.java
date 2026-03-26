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
package org.sonar.server.monitoring;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.IndexState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.apachecommons.io.IOUtils;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.response.NodeStats;
import org.sonar.server.es.response.NodeStatsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ElasticSearchMetricTaskTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final ServerMonitoringMetrics serverMonitoringMetrics = mock(ServerMonitoringMetrics.class);
  private final EsClient esClient = mock(EsClient.class);
  private final Configuration configuration = new MapSettings().asConfig();

  private final ElasticSearchMetricTask underTest = new ElasticSearchMetricTask(serverMonitoringMetrics, esClient, configuration);

  @BeforeEach
  void before() {
    CollectorRegistry.defaultRegistry.clear();
  }

  @Test
  void when_elasticsearch_up_status_is_updated_to_green() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  void elasticsearch_free_disk_space_is_updated() throws IOException {
    URL esNodeResponseUrl = getClass().getResource("es-node-response.json");
    String jsonPayload = StringUtils.trim(IOUtils.toString(esNodeResponseUrl, StandardCharsets.UTF_8));

    JsonObject jsonObject = new Gson().fromJson(jsonPayload, JsonObject.class);
    NodeStatsResponse nodeStats = NodeStatsResponse.toNodeStatsResponse(jsonObject);

    when(esClient.nodesStats()).thenReturn(nodeStats);

    underTest.run();

    String nodeName = nodeStats.getNodeStats().get(0).getName();
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes(nodeName, 136144027648L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes(nodeName, 250685575168L);

    // elasticsearch health status is not mocked in this test, so this part raise an exception
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(Level.ERROR)).containsOnly("Failed to query ES status");
  }

  @Test
  void when_elasticsearch_yellow_status_is_updated_to_green() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Yellow);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  void when_elasticsearch_down_status_is_updated_to_red() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Red);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  void when_no_es_status_null_status_is_updated_to_red() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(null);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  void when_es_status_throw_exception_status_is_updated_to_red() {
    when(esClient.clusterHealthV2(any())).thenThrow(new IllegalStateException("exception in cluster health"));
    NodeStatsResponse nodeStatsResponse = mock(NodeStatsResponse.class);
    NodeStats nodeStats = mock(NodeStats.class);
    when(nodeStats.getName()).thenReturn("test-node");
    when(nodeStatsResponse.getNodeStats()).thenReturn(List.of(nodeStats));
    when(esClient.nodesStats()).thenReturn(nodeStatsResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("test-node", 0L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("test-node", 0L);
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(Level.ERROR)).containsOnly("Failed to query ES status");
  }

  @Test
  void task_has_default_delay() {
    Assertions.assertThat(underTest.getDelay()).isPositive();
    Assertions.assertThat(underTest.getPeriod()).isPositive();
  }

  @Test
  void when_indices_are_readonly_status_is_updated_to_red() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    Map<String, IndexState> indices = new HashMap<>();

    IndexState indexState = mock(IndexState.class, Mockito.RETURNS_DEEP_STUBS);
    when(indexState.settings().index().blocks().readOnlyAllowDelete()).thenReturn(true);

    indices.put("test-index", indexState);
    when(settingsResponse.result()).thenReturn(indices);
    when(esClient.getSettingsV2(any())).thenReturn(settingsResponse);

    // Mock node stats so hasLowDiskSpace() can be called (disk metrics should be updated even when indices are read-only)
    NodeStatsResponse nodeStatsResponse = mock(NodeStatsResponse.class);
    NodeStats nodeStats = mock(NodeStats.class);
    when(nodeStats.getName()).thenReturn("test-node");
    when(nodeStats.getDiskAvailableBytes()).thenReturn(50_000_000_000L);
    when(nodeStats.getDiskTotalBytes()).thenReturn(100_000_000_000L);
    when(nodeStatsResponse.getNodeStats()).thenReturn(List.of(nodeStats));
    when(esClient.nodesStats()).thenReturn(nodeStatsResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchReadOnlyIndicesCount(1L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskUsagePercent(50.0);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("test-node", 50_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("test-node", 100_000_000_000L);
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs(Level.WARN)).contains(
      "Index [test-index] is in read-only mode (index.blocks.read_only_allow_delete=true)",
      "Elasticsearch indices are in read-only mode, likely due to disk space issues");
  }

  @Test
  void when_disk_space_below_threshold_status_fails() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    // Mock index settings with no readonly blocks
    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    when(settingsResponse.result()).thenReturn(new HashMap<>());
    when(esClient.getSettingsV2(any())).thenReturn(settingsResponse);

    // Mock node stats with low disk space (2% free)
    NodeStatsResponse nodeStatsResponse = mock(NodeStatsResponse.class);
    NodeStats nodeStats = mock(NodeStats.class);
    when(nodeStats.getName()).thenReturn("test-node");
    when(nodeStats.getDiskAvailableBytes()).thenReturn(2_000_000_000L); // 2 GB
    when(nodeStats.getDiskTotalBytes()).thenReturn(100_000_000_000L); // 100 GB (2% free)
    when(nodeStatsResponse.getNodeStats()).thenReturn(List.of(nodeStats));
    when(esClient.nodesStats()).thenReturn(nodeStatsResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchReadOnlyIndicesCount(0L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskUsagePercent(98.0);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("test-node", 2_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("test-node", 100_000_000_000L);
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs(Level.WARN)).anyMatch(log -> log.contains("Node [test-node] has critically low disk space") && log.contains("2.00%"));
  }

  @Test
  void when_disk_space_above_threshold_and_no_readonly_status_good() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    // Mock index settings with no readonly blocks
    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    when(settingsResponse.result()).thenReturn(new HashMap<>());
    when(esClient.getSettingsV2(any())).thenReturn(settingsResponse);

    // Mock node stats with sufficient disk space (50% free)
    NodeStatsResponse nodeStatsResponse = mock(NodeStatsResponse.class);
    NodeStats nodeStats = mock(NodeStats.class);
    when(nodeStats.getName()).thenReturn("test-node");
    when(nodeStats.getDiskAvailableBytes()).thenReturn(50_000_000_000L); // 50 GB
    when(nodeStats.getDiskTotalBytes()).thenReturn(100_000_000_000L); // 100 GB (50% free)
    when(nodeStatsResponse.getNodeStats()).thenReturn(List.of(nodeStats));
    when(esClient.nodesStats()).thenReturn(nodeStatsResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchReadOnlyIndicesCount(0L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskUsagePercent(50.0);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("test-node", 50_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("test-node", 100_000_000_000L);
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void when_readonly_check_fails_fallback_to_cluster_health() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    // Mock getSettingsV2 to throw exception
    when(esClient.getSettingsV2(any())).thenThrow(new RuntimeException("Failed to get settings"));

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs(Level.ERROR)).contains("Failed to check for readonly indices");
  }

  @Test
  void when_multiple_indices_are_readonly_all_are_counted() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    // Mock index settings with multiple readonly indices
    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    Map<String, IndexState> indices = new HashMap<>();

    // Create 3 read-only indices
    IndexState readOnlyIndex1 = mock(IndexState.class, Mockito.RETURNS_DEEP_STUBS);
    when(readOnlyIndex1.settings().index().blocks().readOnlyAllowDelete()).thenReturn(true);
    indices.put("issues-index", readOnlyIndex1);

    IndexState readOnlyIndex2 = mock(IndexState.class, Mockito.RETURNS_DEEP_STUBS);
    when(readOnlyIndex2.settings().index().blocks().readOnlyAllowDelete()).thenReturn(true);
    indices.put("components-index", readOnlyIndex2);

    IndexState readOnlyIndex3 = mock(IndexState.class, Mockito.RETURNS_DEEP_STUBS);
    when(readOnlyIndex3.settings().index().blocks().readOnlyAllowDelete()).thenReturn(true);
    indices.put("rules-index", readOnlyIndex3);

    // Create one normal index
    IndexState normalIndex = mock(IndexState.class, Mockito.RETURNS_DEEP_STUBS);
    when(normalIndex.settings().index().blocks().readOnlyAllowDelete()).thenReturn(false);
    indices.put("measures-index", normalIndex);

    when(settingsResponse.result()).thenReturn(indices);
    when(esClient.getSettingsV2(any())).thenReturn(settingsResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchReadOnlyIndicesCount(3L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs(Level.WARN)).contains(
      "Index [issues-index] is in read-only mode (index.blocks.read_only_allow_delete=true)",
      "Index [components-index] is in read-only mode (index.blocks.read_only_allow_delete=true)",
      "Index [rules-index] is in read-only mode (index.blocks.read_only_allow_delete=true)",
      "Elasticsearch indices are in read-only mode, likely due to disk space issues");
  }

  @Test
  void when_some_indices_have_null_blocks_they_are_skipped() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    // Mock index settings with various states
    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    Map<String, IndexState> indices = new HashMap<>();

    // Index with null blocks
    IndexState indexWithNullBlocks = mock(IndexState.class, Mockito.RETURNS_DEEP_STUBS);
    when(indexWithNullBlocks.settings().index().blocks()).thenReturn(null);
    indices.put("index-null-blocks", indexWithNullBlocks);

    // Index with blocks but null readOnlyAllowDelete
    IndexState indexWithNullReadOnly = mock(IndexState.class, Mockito.RETURNS_DEEP_STUBS);
    when(indexWithNullReadOnly.settings().index().blocks().readOnlyAllowDelete()).thenReturn(null);
    indices.put("index-null-readonly", indexWithNullReadOnly);

    // One actual read-only index
    IndexState readOnlyIndex = mock(IndexState.class, Mockito.RETURNS_DEEP_STUBS);
    when(readOnlyIndex.settings().index().blocks().readOnlyAllowDelete()).thenReturn(true);
    indices.put("readonly-index", readOnlyIndex);

    when(settingsResponse.result()).thenReturn(indices);
    when(esClient.getSettingsV2(any())).thenReturn(settingsResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchReadOnlyIndicesCount(1L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs(Level.WARN)).contains(
      "Index [readonly-index] is in read-only mode (index.blocks.read_only_allow_delete=true)");
  }

  @Test
  void when_multiple_nodes_have_different_disk_usage_max_is_tracked() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    // Mock index settings with no readonly blocks
    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    when(settingsResponse.result()).thenReturn(new HashMap<>());
    when(esClient.getSettingsV2(any())).thenReturn(settingsResponse);

    // Mock node stats with multiple nodes at different disk usage levels
    NodeStatsResponse nodeStatsResponse = mock(NodeStatsResponse.class);

    // Node 1: 30% used (70% free)
    NodeStats node1 = mock(NodeStats.class);
    when(node1.getName()).thenReturn("node-1");
    when(node1.getDiskAvailableBytes()).thenReturn(70_000_000_000L);
    when(node1.getDiskTotalBytes()).thenReturn(100_000_000_000L);

    // Node 2: 85% used (15% free) - highest usage
    NodeStats node2 = mock(NodeStats.class);
    when(node2.getName()).thenReturn("node-2");
    when(node2.getDiskAvailableBytes()).thenReturn(15_000_000_000L);
    when(node2.getDiskTotalBytes()).thenReturn(100_000_000_000L);

    // Node 3: 50% used (50% free)
    NodeStats node3 = mock(NodeStats.class);
    when(node3.getName()).thenReturn("node-3");
    when(node3.getDiskAvailableBytes()).thenReturn(50_000_000_000L);
    when(node3.getDiskTotalBytes()).thenReturn(100_000_000_000L);

    when(nodeStatsResponse.getNodeStats()).thenReturn(List.of(node1, node2, node3));
    when(esClient.nodesStats()).thenReturn(nodeStatsResponse);

    underTest.run();

    // Should track the maximum disk usage (85%)
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskUsagePercent(85.0);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchReadOnlyIndicesCount(0L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("node-1", 70_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("node-1", 100_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("node-2", 15_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("node-2", 100_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("node-3", 50_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("node-3", 100_000_000_000L);
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void when_multiple_nodes_with_low_disk_space_all_are_logged_and_max_is_tracked() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    // Mock index settings with no readonly blocks
    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    when(settingsResponse.result()).thenReturn(new HashMap<>());
    when(esClient.getSettingsV2(any())).thenReturn(settingsResponse);

    // Mock node stats with multiple nodes below threshold (default 5%)
    NodeStatsResponse nodeStatsResponse = mock(NodeStatsResponse.class);

    // Node 1: 97% used (3% free) - below threshold
    NodeStats node1 = mock(NodeStats.class);
    when(node1.getName()).thenReturn("critical-node-1");
    when(node1.getDiskAvailableBytes()).thenReturn(3_000_000_000L);
    when(node1.getDiskTotalBytes()).thenReturn(100_000_000_000L);

    // Node 2: 99% used (1% free) - highest usage, below threshold
    NodeStats node2 = mock(NodeStats.class);
    when(node2.getName()).thenReturn("critical-node-2");
    when(node2.getDiskAvailableBytes()).thenReturn(1_000_000_000L);
    when(node2.getDiskTotalBytes()).thenReturn(100_000_000_000L);

    // Node 3: 50% used (50% free) - healthy
    NodeStats node3 = mock(NodeStats.class);
    when(node3.getName()).thenReturn("healthy-node");
    when(node3.getDiskAvailableBytes()).thenReturn(50_000_000_000L);
    when(node3.getDiskTotalBytes()).thenReturn(100_000_000_000L);

    when(nodeStatsResponse.getNodeStats()).thenReturn(List.of(node1, node2, node3));
    when(esClient.nodesStats()).thenReturn(nodeStatsResponse);

    underTest.run();

    // Should track the maximum disk usage (99%) and set status to RED
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskUsagePercent(99.0);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchReadOnlyIndicesCount(0L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("critical-node-1", 3_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("critical-node-1", 100_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("critical-node-2", 1_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("critical-node-2", 100_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("healthy-node", 50_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("healthy-node", 100_000_000_000L);
    verifyNoMoreInteractions(serverMonitoringMetrics);

    // Both critical nodes should be logged
    assertThat(logTester.logs(Level.WARN)).anyMatch(log -> log.contains("Node [critical-node-1] has critically low disk space") && log.contains("3.00%"));
    assertThat(logTester.logs(Level.WARN)).anyMatch(log -> log.contains("Node [critical-node-2] has critically low disk space") && log.contains("1.00%"));
  }

  @Test
  void when_node_has_zero_total_bytes_it_is_skipped_from_percentage_calculation() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    // Mock index settings with no readonly blocks
    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    when(settingsResponse.result()).thenReturn(new HashMap<>());
    when(esClient.getSettingsV2(any())).thenReturn(settingsResponse);

    // Mock node stats with zero total bytes (edge case)
    NodeStatsResponse nodeStatsResponse = mock(NodeStatsResponse.class);

    NodeStats nodeWithZeroTotal = mock(NodeStats.class);
    when(nodeWithZeroTotal.getName()).thenReturn("invalid-node");
    when(nodeWithZeroTotal.getDiskAvailableBytes()).thenReturn(0L);
    when(nodeWithZeroTotal.getDiskTotalBytes()).thenReturn(0L);

    NodeStats validNode = mock(NodeStats.class);
    when(validNode.getName()).thenReturn("valid-node");
    when(validNode.getDiskAvailableBytes()).thenReturn(40_000_000_000L);
    when(validNode.getDiskTotalBytes()).thenReturn(100_000_000_000L);

    when(nodeStatsResponse.getNodeStats()).thenReturn(List.of(nodeWithZeroTotal, validNode));
    when(esClient.nodesStats()).thenReturn(nodeStatsResponse);

    underTest.run();

    // Node with zero total is recorded but skipped from percentage calculation (60% used for valid node only)
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskUsagePercent(60.0);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchReadOnlyIndicesCount(0L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    // Both nodes have their raw metrics recorded
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("invalid-node", 0L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("invalid-node", 0L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes("valid-node", 40_000_000_000L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes("valid-node", 100_000_000_000L);
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

}
