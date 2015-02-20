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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.search.NodeHealth;
import org.sonar.server.search.NodeHealth.Performance;
import org.sonar.server.search.SearchHealth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.sonar.api.utils.DateUtils.formatDateTime;

public class ElasticSearchNodesMonitoring extends MonitoringMBean implements ElasticSearchNodesMonitoringMBean {
  private final SearchHealth searchHealth;

  public ElasticSearchNodesMonitoring(SearchHealth searchHealth) {
    this.searchHealth = searchHealth;
  }

  @Override
  public String getNodes() {
    List<String> formattedNodes = new ArrayList<>();
    Table<String, String, String> nodes = nodes();

    for (String nodeName : nodes.rowKeySet()) {
      StringBuilder formattedNode = new StringBuilder();
      formattedNode.append(nodeName)
        .append(" - ")
        .append(Joiner.on(", ").withKeyValueSeparator(": ").join(nodes.row(nodeName)));
      formattedNodes.add(formattedNode.toString());
    }

    return Joiner.on(" | ").join(formattedNodes);
  }

  @Override
  public String name() {
    return "ElasticSearchNodes";
  }

  @Override
  public void toJson(JsonWriter json) {
    Table<String, String, String> nodes = nodes();
    json.beginObject();
    for (String nodeName : nodes.rowKeySet()) {
      json.name(nodeName);
      json.beginObject();
      for (Map.Entry<String, String> nodeProperty : nodes.row(nodeName).entrySet()) {
        json.prop(nodeProperty.getKey(), nodeProperty.getValue());
      }
      json.endObject();
    }

    json.endObject();
  }

  private Table<String, String, String> nodes() {
    Table<String, String, String> nodes = HashBasedTable.create();
    for (Map.Entry<String, NodeHealth> nodeEntry : searchHealth.getNodesHealth().entrySet()) {
      String name = nodeEntry.getKey();
      nodes.put(name, "Name", nodeEntry.getKey());
      NodeHealth nodeHealth = nodeEntry.getValue();
      nodes.put(name, "Type", nodeHealth.isMaster() ? "Master" : "Slave");
      nodes.put(name, "Address", nodeHealth.getAddress());
      nodes.put(name, "JVM Heap Usage", nodeHealth.getJvmHeapUsedPercent());
      nodes.put(name, "JVM Threads", String.valueOf(nodeHealth.getJvmThreads()));
      nodes.put(name, "JVM Started Since", formatDateTime(nodeHealth.getJvmUpSince()));
      nodes.put(name, "Disk Usage", nodeHealth.getFsUsedPercent());
      nodes.put(name, "Open Files", String.valueOf(nodeHealth.getOpenFiles()));
      nodes.put(name, "CPU Load Average", nodeHealth.getProcessCpuPercent());
      nodes.put(name, "Field Cache Size", byteCountToDisplaySize(nodeHealth.getFieldCacheMemory()));
      nodes.put(name, "Filter Cache Size", byteCountToDisplaySize(nodeHealth.getFilterCacheMemory()));

      for (Performance performance : nodeHealth.getPerformanceStats()) {
        String message = "";
        if (Performance.Status.ERROR.equals(performance.getStatus()) || Performance.Status.WARN.equals(performance.getStatus())) {
          message = String.format("- %s: %s", performance.getStatus(), performance.getMessage());
        }
        if (performance.getName().contains("Eviction")) {
          nodes.put(name, performance.getName(), String.format("%f %s", performance.getValue(), message));
        } else {
          nodes.put(name, performance.getName(), String.format("%.1f ms %s", performance.getValue(), message));
        }
      }
    }

    return nodes;
  }
}
