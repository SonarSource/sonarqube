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

package org.sonar.server.platform.monitoring;

import com.google.common.base.Joiner;
import org.sonar.server.search.ClusterHealth;
import org.sonar.server.search.IndexHealth;
import org.sonar.server.search.SearchHealth;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.sonar.api.utils.DateUtils.formatDateTimeNullSafe;

public class EsClusterMonitor extends BaseMonitorMBean
  implements EsClusterMonitorMBean {

  private final SearchHealth searchHealth;

  public EsClusterMonitor(SearchHealth searchHealth) {
    this.searchHealth = searchHealth;
  }

  @Override
  public String getClusterState() {
    return clusterHealth().isClusterAvailable() ? "Available" : "Unavailable";
  }

  @Override
  public int getNumberOfNodes() {
    return clusterHealth().getNumberOfNodes();
  }

  @Override
  public String getIndexesHealth() {
    Map<String, String> formattedIndexes = new HashMap<>();
    for (Map.Entry<String, IndexHealth> healthEntry : searchHealth.getIndexHealth().entrySet()) {
      formattedIndexes.put(healthEntry.getKey(), formatIndexHealth(healthEntry.getValue()));
    }

    return Joiner.on(" | ").withKeyValueSeparator(": ").join(formattedIndexes);
  }

  @Override
  public String name() {
    return "ElasticSearchCluster";
  }

  @Override
  public LinkedHashMap<String, Object> attributes() {
    LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("Cluster State", getClusterState());
    attributes.put("Number of Nodes", getNumberOfNodes());
    for (Map.Entry<String, IndexHealth> healthEntry : searchHealth.getIndexHealth().entrySet()) {
      attributes.put(healthEntry.getKey() + " - Document Count", healthEntry.getValue().getDocumentCount());
      attributes.put(healthEntry.getKey() + " - Last Sync", formatDateTimeNullSafe(healthEntry.getValue().getLastSynchronization()));
      attributes.put(healthEntry.getKey() + " - Optimization", formatIndexHealthOptimisation(healthEntry.getValue()));
    }
    return attributes;
  }

  private String formatIndexHealthOptimisation(IndexHealth indexHealth) {
    return indexHealth.isOptimized() ? "Optimized" : "Unoptimized " +
      "(Segments: " + indexHealth.getSegmentcount() + ", Pending Deletions: "
      + indexHealth.getPendingDeletion() + ")";
  }

  private ClusterHealth clusterHealth() {
    return searchHealth.getClusterHealth();
  }

  private String formatIndexHealth(IndexHealth indexHealth) {
    return new StringBuilder()
      .append(indexHealth.getDocumentCount())
      .append("/")
      .append(formatDateTimeNullSafe(indexHealth.getLastSynchronization()))
      .append("/")
      .append(formatIndexHealthOptimisation(indexHealth)).toString();
  }
}
