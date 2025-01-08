/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

import static com.google.common.collect.ImmutableList.toImmutableList;

@Immutable
public class NodeStatsResponse {

  private final ImmutableList<NodeStats> nodeStats;

  private NodeStatsResponse(JsonObject nodeStatsJson) {
    this.nodeStats = nodeStatsJson.getAsJsonObject("nodes").entrySet().stream()
      .map(Map.Entry::getValue)
      .map(JsonElement::getAsJsonObject)
      .map(NodeStats::toNodeStats)
      .collect(toImmutableList());
  }

  public static NodeStatsResponse toNodeStatsResponse(JsonObject jsonObject) {
    return new NodeStatsResponse(jsonObject);
  }

  public List<NodeStats> getNodeStats() {
    return this.nodeStats;
  }

}
