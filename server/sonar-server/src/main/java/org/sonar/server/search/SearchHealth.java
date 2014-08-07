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
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;

import java.util.Map;

public class SearchHealth {

  private ESNode node;
  private IndexClient indexClient;

  public SearchHealth(ESNode node, IndexClient indexClient) {
    this.node = node;
    this.indexClient = indexClient;
  }

  public NodeHealth getNodeHealth() {
    return node.getNodeHealth();
  }

  public Map<String, IndexHealth> getIndexHealth() {
    Builder<String, IndexHealth> builder = ImmutableMap.builder();
    for (Index index: indexClient.allIndices()) {
      IndexStat indexStat = index.getIndexStat();
      IndexHealth newIndexHealth = new IndexHealth();
      newIndexHealth.name = index.getIndexName() + "/" + index.getIndexType();
      newIndexHealth.documentCount = indexStat.getDocumentCount();
      newIndexHealth.lastSync = indexStat.getLastUpdate();

      IndicesStatsRequestBuilder statRequest = node.client().admin().indices().prepareStats(index.getIndexName())
        .setTypes(index.getIndexType());
      IndicesStatsResponse indicesStatsResponse = node.execute(statRequest);
      newIndexHealth.segmentCount = indicesStatsResponse.getTotal().getSegments().getCount();
      newIndexHealth.pendingDeletion = indicesStatsResponse.getTotal().getDocs().getDeleted();

      builder.put(newIndexHealth.name, newIndexHealth);
    }
    return builder.build();
  }

}
