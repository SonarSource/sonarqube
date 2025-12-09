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
public class JvmStats {
  private final long heapUsedPercent;
  private final long heapUsedInBytes;
  private final long heapMaxInBytes;
  private final long nonHeapUsedInBytes;
  private final long threadCount;

  private JvmStats(JsonObject jvmStatsJson) {
    this.heapUsedPercent = jvmStatsJson.getAsJsonObject("mem").get("heap_used_percent").getAsLong();
    this.heapUsedInBytes = jvmStatsJson.getAsJsonObject("mem").get("heap_used_in_bytes").getAsLong();
    this.heapMaxInBytes = jvmStatsJson.getAsJsonObject("mem").get("heap_max_in_bytes").getAsLong();
    this.nonHeapUsedInBytes = jvmStatsJson.getAsJsonObject("mem").get("non_heap_used_in_bytes").getAsLong();
    this.threadCount = jvmStatsJson.getAsJsonObject("threads").get("count").getAsLong();
  }

  public static JvmStats toJvmStats(JsonObject jvmStatsJson) {
    return new JvmStats(jvmStatsJson);
  }

  public long getHeapUsedPercent() {
    return heapUsedPercent;
  }

  public long getHeapUsedInBytes() {
    return heapUsedInBytes;
  }

  public long getHeapMaxInBytes() {
    return heapMaxInBytes;
  }

  public long getNonHeapUsedInBytes() {
    return nonHeapUsedInBytes;
  }

  public long getThreadCount() {
    return threadCount;
  }
}
