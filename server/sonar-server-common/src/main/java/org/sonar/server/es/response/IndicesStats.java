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
public class IndicesStats {
  private static final String MEMORY_SIZE_IN_BYTES_ATTRIBUTE_NAME = "memory_size_in_bytes";
  private final long storeSizeInBytes;
  private final long translogSizeInBytes;
  private final long requestCacheMemorySizeInBytes;
  private final long fieldDataMemorySizeInBytes;
  private final long queryCacheMemorySizeInBytes;

  private IndicesStats(JsonObject indicesStatsJson) {
    this.storeSizeInBytes = indicesStatsJson.getAsJsonObject("store").get("size_in_bytes").getAsLong();
    this.translogSizeInBytes = indicesStatsJson.getAsJsonObject("translog").get("size_in_bytes").getAsLong();
    this.requestCacheMemorySizeInBytes = indicesStatsJson.getAsJsonObject("request_cache").get(MEMORY_SIZE_IN_BYTES_ATTRIBUTE_NAME).getAsLong();
    this.fieldDataMemorySizeInBytes = indicesStatsJson.getAsJsonObject("fielddata").get(MEMORY_SIZE_IN_BYTES_ATTRIBUTE_NAME).getAsLong();
    this.queryCacheMemorySizeInBytes = indicesStatsJson.getAsJsonObject("query_cache").get(MEMORY_SIZE_IN_BYTES_ATTRIBUTE_NAME).getAsLong();
  }

  public static IndicesStats toIndicesStats(JsonObject jsonObject) {
    return new IndicesStats(jsonObject);
  }

  public long getStoreSizeInBytes() {
    return storeSizeInBytes;
  }

  public long getTranslogSizeInBytes() {
    return translogSizeInBytes;
  }

  public long getRequestCacheMemorySizeInBytes() {
    return requestCacheMemorySizeInBytes;
  }

  public long getFieldDataMemorySizeInBytes() {
    return fieldDataMemorySizeInBytes;
  }

  public long getQueryCacheMemorySizeInBytes() {
    return queryCacheMemorySizeInBytes;
  }
}
