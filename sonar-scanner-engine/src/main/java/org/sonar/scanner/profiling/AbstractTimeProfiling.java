/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.profiling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TimeUtils;

public abstract class AbstractTimeProfiling {

  private final long startTime;

  private long totalTime;

  private System2 system;

  public AbstractTimeProfiling(System2 system) {
    this.system = system;
    this.startTime = system.now();
  }

  protected System2 system() {
    return system;
  }

  public long startTime() {
    return startTime;
  }

  public void stop() {
    this.totalTime = system.now() - startTime;
  }

  public long totalTime() {
    return totalTime;
  }

  public String totalTimeAsString() {
    return TimeUtils.formatDuration(totalTime);
  }

  public void setTotalTime(long totalTime) {
    this.totalTime = totalTime;
  }

  protected void add(AbstractTimeProfiling other) {
    this.setTotalTime(this.totalTime() + other.totalTime());
  }

  static <G extends AbstractTimeProfiling> Map<Object, G> sortByDescendingTotalTime(Map<?, G> unsorted) {
    List<Map.Entry<?, G>> entries = new ArrayList<>(unsorted.entrySet());
    Collections.sort(entries, (o1, o2) -> Long.valueOf(o2.getValue().totalTime()).compareTo(o1.getValue().totalTime()));
    Map<Object, G> sortedMap = new LinkedHashMap<>();
    for (Map.Entry<?, G> entry : entries) {
      sortedMap.put(entry.getKey(), entry.getValue());
    }
    return sortedMap;
  }

  static <G extends AbstractTimeProfiling> List<G> truncate(Collection<G> sortedList) {
    int maxSize = 10;
    List<G> result = new ArrayList<>(maxSize);
    int i = 0;
    for (G item : sortedList) {
      if (i >= maxSize || item.totalTime() == 0) {
        return result;
      }
      i++;
      result.add(item);
    }
    return result;
  }

  protected void println(String msg) {
    PhasesSumUpTimeProfiler.println(msg);
  }

  protected void println(String text, @Nullable Double percent, AbstractTimeProfiling phaseProfiling) {
    PhasesSumUpTimeProfiler.println(text, percent, phaseProfiling);
  }

  protected void println(String text, AbstractTimeProfiling phaseProfiling) {
    println(text, null, phaseProfiling);
  }

}
