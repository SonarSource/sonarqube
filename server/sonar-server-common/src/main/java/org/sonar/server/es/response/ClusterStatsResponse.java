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
package org.sonar.server.es.response;

import com.google.gson.JsonObject;
import javax.annotation.concurrent.Immutable;
import org.elasticsearch.cluster.health.ClusterHealthStatus;

@Immutable
public class ClusterStatsResponse {

  private final ClusterHealthStatus healthStatus;
  private final int nodeCount;

  private ClusterStatsResponse(JsonObject clusterStatsJson) {
    this.healthStatus = ClusterHealthStatus.fromString(clusterStatsJson.get("status").getAsString());
    this.nodeCount = clusterStatsJson.getAsJsonObject("nodes").getAsJsonObject("count").get("total").getAsInt();
  }

  public static ClusterStatsResponse toClusterStatsResponse(JsonObject jsonObject) {
    return new ClusterStatsResponse(jsonObject);
  }

  public ClusterHealthStatus getHealthStatus() {
    return healthStatus;
  }

  public int getNodeCount() {
    return nodeCount;
  }
}
