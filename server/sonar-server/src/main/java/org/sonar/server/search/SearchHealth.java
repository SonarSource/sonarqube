/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;

import java.util.Map;
import java.util.Map.Entry;

public class SearchHealth {

  private SearchClient searchClient;
  private IndexClient indexClient;

  public SearchHealth(SearchClient searchClient, IndexClient indexClient) {
    this.searchClient = searchClient;
    this.indexClient = indexClient;
  }

  public ClusterHealth getClusterHealth() {
    return searchClient.getClusterHealth();
  }

  public Map<String, IndexHealth> getIndexHealth() {
    Builder<String, IndexHealth> builder = ImmutableMap.builder();
    for (Index index: indexClient.allIndices()) {
      IndexStat indexStat = index.getIndexStat();
      IndexHealth newIndexHealth = new IndexHealth();
      newIndexHealth.name = index.getIndexName() + "/" + index.getIndexType();
      newIndexHealth.documentCount = indexStat.getDocumentCount();
      newIndexHealth.lastSync = indexStat.getLastUpdate();

      IndicesStatsRequestBuilder statRequest = searchClient.admin().indices().prepareStats(index.getIndexName())
        .setTypes(index.getIndexType());
      IndicesStatsResponse indicesStatsResponse = searchClient.execute(statRequest);
      newIndexHealth.segmentCount = indicesStatsResponse.getTotal().getSegments().getCount();
      newIndexHealth.pendingDeletion = indicesStatsResponse.getTotal().getDocs().getDeleted();

      builder.put(newIndexHealth.name, newIndexHealth);
    }
    return builder.build();
  }

  public Map<String, NodeHealth > getNodesHealth() {
    NodesStatsRequestBuilder nodesStatsRequest = searchClient.admin().cluster().prepareNodesStats().all();
    NodesStatsResponse nodesStats = searchClient.execute(nodesStatsRequest);

    Map<String, NodeHealth> health = Maps.newHashMap();
    for (Entry<String, NodeStats> nodeEntry: nodesStats.getNodesMap().entrySet()) {
      health.put(nodeEntry.getKey(), new NodeHealth(nodeEntry.getValue()));
    }
    return ImmutableMap.copyOf(health);
  }
}
