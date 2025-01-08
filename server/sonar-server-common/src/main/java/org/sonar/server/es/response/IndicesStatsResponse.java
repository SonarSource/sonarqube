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

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

@Immutable
public class IndicesStatsResponse {

  private final ImmutableMap<String, IndexStats> indexStatsMap;

  private IndicesStatsResponse(JsonObject indicesStats) {
    ImmutableMap.Builder<String, IndexStats> builder = ImmutableMap.builder();
    for (Map.Entry<String, JsonElement> indexStats : indicesStats.getAsJsonObject("indices").entrySet()) {
      builder.put(indexStats.getKey(), IndexStats.toIndexStats(indexStats.getKey(), indexStats.getValue().getAsJsonObject()));
    }
    this.indexStatsMap = builder.build();
  }

  public static IndicesStatsResponse toIndicesStatsResponse(JsonObject jsonObject) {
    return new IndicesStatsResponse(jsonObject);
  }

  public Collection<IndexStats> getAllIndexStats() {
    return indexStatsMap.values();
  }

}
