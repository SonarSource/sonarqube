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

@Immutable
public class IndexStats {
  private final String name;
  private final long docCount;
  private final long shardsCount;
  private final long storeSizeBytes;

  private IndexStats(String name, JsonObject indexStatsJson) {
    this.name = name;
    this.docCount = indexStatsJson.getAsJsonObject().getAsJsonObject("primaries").getAsJsonObject("docs").get("count").getAsLong();
    this.shardsCount = indexStatsJson.getAsJsonObject().getAsJsonObject("shards").size();
    this.storeSizeBytes = indexStatsJson.getAsJsonObject().getAsJsonObject("primaries").getAsJsonObject("store").get("size_in_bytes").getAsLong();
  }

  static IndexStats toIndexStats(String name, JsonObject indexStatsJson) {
    return new IndexStats(name, indexStatsJson);
  }

  public String getName() {
    return name;
  }

  public long getDocCount() {
    return docCount;
  }

  public long getShardsCount() {
    return shardsCount;
  }

  public long getStoreSizeBytes() {
    return storeSizeBytes;
  }
}
