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
package org.sonar.server.es.response;

import com.google.gson.JsonObject;
import javax.annotation.concurrent.Immutable;

@Immutable
public class NodeStats {
  private static final String PROCESS_ATTRIBUTE_NAME = "process";
  private static final String BREAKERS_ATTRIBUTE_NAME = "breakers";

  private final String name;
  private final String host;

  private final long cpuUsage;
  private final long openFileDescriptors;
  private final long maxFileDescriptors;
  private final long diskAvailableBytes;
  private final long diskTotalBytes;

  private final long fieldDataCircuitBreakerLimit;
  private final long fieldDataCircuitBreakerEstimation;
  private final long requestCircuitBreakerLimit;
  private final long requestCircuitBreakerEstimation;

  private final JvmStats jvmStats;
  private final IndicesStats indicesStats;

  private NodeStats(JsonObject nodeStatsJson) {
    this.name = nodeStatsJson.get("name").getAsString();
    this.host = nodeStatsJson.get("host").getAsString();

    this.cpuUsage = nodeStatsJson.getAsJsonObject(PROCESS_ATTRIBUTE_NAME).getAsJsonObject("cpu").get("percent").getAsLong();
    this.openFileDescriptors = nodeStatsJson.getAsJsonObject(PROCESS_ATTRIBUTE_NAME).get("open_file_descriptors").getAsLong();
    this.maxFileDescriptors = nodeStatsJson.getAsJsonObject(PROCESS_ATTRIBUTE_NAME).get("max_file_descriptors").getAsLong();
    this.diskAvailableBytes = nodeStatsJson.getAsJsonObject("fs").getAsJsonObject("total").get("available_in_bytes").getAsLong();
    this.diskTotalBytes = nodeStatsJson.getAsJsonObject("fs").getAsJsonObject("total").get("total_in_bytes").getAsLong();

    this.fieldDataCircuitBreakerLimit = nodeStatsJson.getAsJsonObject(BREAKERS_ATTRIBUTE_NAME).getAsJsonObject("fielddata").get("limit_size_in_bytes").getAsLong();
    this.fieldDataCircuitBreakerEstimation = nodeStatsJson.getAsJsonObject(BREAKERS_ATTRIBUTE_NAME).getAsJsonObject("fielddata").get("estimated_size_in_bytes").getAsLong();
    this.requestCircuitBreakerLimit = nodeStatsJson.getAsJsonObject(BREAKERS_ATTRIBUTE_NAME).getAsJsonObject("request").get("limit_size_in_bytes").getAsLong();
    this.requestCircuitBreakerEstimation = nodeStatsJson.getAsJsonObject(BREAKERS_ATTRIBUTE_NAME).getAsJsonObject("request").get("estimated_size_in_bytes").getAsLong();

    this.jvmStats = JvmStats.toJvmStats(nodeStatsJson.getAsJsonObject("jvm"));
    this.indicesStats = IndicesStats.toIndicesStats(nodeStatsJson.getAsJsonObject("indices"));
  }

  public static NodeStats toNodeStats(JsonObject jsonObject) {
    return new NodeStats(jsonObject);
  }

  public String getName() {
    return name;
  }

  public String getHost() {
    return host;
  }

  public long getCpuUsage() {
    return cpuUsage;
  }

  public long getOpenFileDescriptors() {
    return openFileDescriptors;
  }

  public long getMaxFileDescriptors() {
    return maxFileDescriptors;
  }

  public long getDiskAvailableBytes() {
    return diskAvailableBytes;
  }

  public long getDiskTotalBytes() {
    return diskTotalBytes;
  }

  public long getFieldDataCircuitBreakerLimit() {
    return fieldDataCircuitBreakerLimit;
  }

  public long getFieldDataCircuitBreakerEstimation() {
    return fieldDataCircuitBreakerEstimation;
  }

  public long getRequestCircuitBreakerLimit() {
    return requestCircuitBreakerLimit;
  }

  public long getRequestCircuitBreakerEstimation() {
    return requestCircuitBreakerEstimation;
  }

  public JvmStats getJvmStats() {
    return jvmStats;
  }

  public IndicesStats getIndicesStats() {
    return indicesStats;
  }
}
