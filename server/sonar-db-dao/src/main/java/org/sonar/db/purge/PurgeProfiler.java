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
package org.sonar.db.purge;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sonar.api.utils.TimeUtils;

public class PurgeProfiler {

  private final Map<String, Long> durations = new HashMap<>();
  private long startTime;
  private String currentTable;
  private final Clock clock;

  public PurgeProfiler() {
    this(new Clock());
  }

  @VisibleForTesting
  PurgeProfiler(Clock clock) {
    this.clock = clock;
  }

  public void reset() {
    durations.clear();
  }

  void start(String table) {
    this.startTime = clock.now();
    this.currentTable = table;
  }

  void stop() {
    Long cumulatedDuration = durations.getOrDefault(currentTable, 0L);
    durations.put(currentTable, cumulatedDuration + (clock.now() - startTime));
  }

  public List<String> getProfilingResult(long totalTime) {
    List<Entry<String, Long>> data = new ArrayList<>(durations.entrySet());
    data.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
    double percent = totalTime / 100.0;
    return truncateList(data).stream().map( entry ->
      "   o " + entry.getKey() + ": " + TimeUtils.formatDuration(entry.getValue()) +
        " (" + (int) (entry.getValue() / percent) + "%)"
    ).toList();
  }

  private static List<Entry<String, Long>> truncateList(List<Entry<String, Long>> sortedFullList) {
    int maxSize = 10;
    List<Entry<String, Long>> result = new ArrayList<>(maxSize);
    int i = 0;
    for (Entry<String, Long> item : sortedFullList) {
      if (i++ >= maxSize || item.getValue() == 0) {
        return result;
      }
      result.add(item);
    }
    return result;
  }

  static class Clock {
    public long now() {
      return System.currentTimeMillis();
    }
  }

}
