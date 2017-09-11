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
package org.sonar.server.platform.monitoring;

import java.util.Map;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsClient;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

public class EsSection extends BaseSectionMBean implements EsSectionMBean {

  private final EsClient esClient;

  public EsSection(EsClient esClient) {
    this.esClient = esClient;
  }

  @Override
  public String name() {
    return "Elasticsearch";
  }

  /**
   * MXBean does not allow to return enum {@link ClusterHealthStatus}, so
   * returning String.
   */
  @Override
  public String getState() {
    return getStateAsEnum().name();
  }

  private ClusterHealthStatus getStateAsEnum() {
    return clusterStats().getStatus();
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName(name());
    try {
      setAttribute(protobuf, "State", getStateAsEnum().name());
      completeNodeAttributes(protobuf);
      completeIndexAttributes(protobuf);

    } catch (Exception es) {
      Loggers.get(EsSection.class).warn("Failed to retrieve ES attributes. There will be only a single \"state\" attribute.", es);
      setAttribute(protobuf, "State", es.getCause() instanceof ElasticsearchException ? es.getCause().getMessage() : es.getMessage());
    }
    return protobuf.build();
  }

  private void completeIndexAttributes(ProtobufSystemInfo.Section.Builder protobuf) {
    IndicesStatsResponse indicesStats = esClient.prepareStats().all().get();
    for (Map.Entry<String, IndexStats> indexStats : indicesStats.getIndices().entrySet()) {
      String prefix = "Index " + indexStats.getKey() + " - ";
      setAttribute(protobuf, prefix + "Docs", indexStats.getValue().getPrimaries().getDocs().getCount());
      setAttribute(protobuf, prefix + "Shards", indexStats.getValue().getShards().length);
      setAttribute(protobuf, prefix + "Store Size", byteCountToDisplaySize(indexStats.getValue().getPrimaries().getStore().getSizeInBytes()));
    }
  }

  private void completeNodeAttributes(ProtobufSystemInfo.Section.Builder protobuf) {
    NodesStatsResponse nodesStats = esClient.prepareNodesStats().all().get();
    if (!nodesStats.getNodes().isEmpty()) {
      NodeStats stats = nodesStats.getNodes().get(0);
      setAttribute(protobuf, "Disk Available", byteCountToDisplaySize(stats.getFs().getTotal().getAvailable().getBytes()));
      setAttribute(protobuf, "Store Size", byteCountToDisplaySize(stats.getIndices().getStore().getSizeInBytes()));
      setAttribute(protobuf, "Open Files", stats.getProcess().getOpenFileDescriptors());
      setAttribute(protobuf, "JVM Heap Usage", formatPercent(stats.getJvm().getMem().getHeapUsedPercent()));
      setAttribute(protobuf, "JVM Heap Used", byteCountToDisplaySize(stats.getJvm().getMem().getHeapUsed().getBytes()));
      setAttribute(protobuf, "JVM Heap Max", byteCountToDisplaySize(stats.getJvm().getMem().getHeapMax().getBytes()));
      setAttribute(protobuf, "JVM Non Heap Used", byteCountToDisplaySize(stats.getJvm().getMem().getNonHeapUsed().getBytes()));
      setAttribute(protobuf, "JVM Threads", stats.getJvm().getThreads().getCount());
      setAttribute(protobuf, "Field Data Memory", byteCountToDisplaySize(stats.getIndices().getFieldData().getMemorySizeInBytes()));
      setAttribute(protobuf, "Field Data Circuit Breaker Limit", byteCountToDisplaySize(stats.getBreaker().getStats(CircuitBreaker.FIELDDATA).getLimit()));
      setAttribute(protobuf, "Field Data Circuit Breaker Estimation", byteCountToDisplaySize(stats.getBreaker().getStats(CircuitBreaker.FIELDDATA).getEstimated()));
      setAttribute(protobuf, "Request Circuit Breaker Limit", byteCountToDisplaySize(stats.getBreaker().getStats(CircuitBreaker.REQUEST).getLimit()));
      setAttribute(protobuf, "Request Circuit Breaker Estimation", byteCountToDisplaySize(stats.getBreaker().getStats(CircuitBreaker.REQUEST).getEstimated()));
      setAttribute(protobuf, "Query Cache Memory", byteCountToDisplaySize(stats.getIndices().getQueryCache().getMemorySizeInBytes()));
      setAttribute(protobuf, "Request Cache Memory", byteCountToDisplaySize(stats.getIndices().getRequestCache().getMemorySizeInBytes()));
    }
  }

  private ClusterStatsResponse clusterStats() {
    return esClient.prepareClusterStats().get();
  }

  private static String formatPercent(long amount) {
    return String.format("%.1f%%", 100 * amount * 1.0D / 100L);
  }
}
