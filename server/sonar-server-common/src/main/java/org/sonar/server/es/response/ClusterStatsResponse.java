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
package org.sonar.server.es.response;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import com.google.gson.JsonObject;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ClusterStatsResponse {

  private final HealthStatus healthStatus;
  private final int nodeCount;

  private ClusterStatsResponse(JsonObject clusterStatsJson) {
    this.healthStatus = HealthStatus._DESERIALIZER.parse(clusterStatsJson.get("status").getAsString());
    this.nodeCount = clusterStatsJson.getAsJsonObject("nodes").getAsJsonObject("count").get("total").getAsInt();
  }

  public static ClusterStatsResponse toClusterStatsResponse(JsonObject jsonObject) {
    return new ClusterStatsResponse(jsonObject);
  }

  public HealthStatus getHealthStatus() {
    return healthStatus;
  }

  public int getNodeCount() {
    return nodeCount;
  }
}
