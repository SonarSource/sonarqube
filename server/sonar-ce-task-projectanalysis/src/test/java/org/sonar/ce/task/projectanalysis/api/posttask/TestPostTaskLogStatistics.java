/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.api.posttask;

import java.util.HashMap;
import java.util.Map;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

class TestPostTaskLogStatistics implements PostProjectAnalysisTask.LogStatistics {
  private final Map<String, Object> stats = new HashMap<>();

  @Override
  public PostProjectAnalysisTask.LogStatistics add(String key, Object value) {
    requireNonNull(key, "Statistic has null key");
    requireNonNull(value, () -> format("Statistic with key [%s] has null value", key));
    checkArgument(!key.equalsIgnoreCase("time"), "Statistic with key [time] is not accepted");
    checkArgument(!stats.containsKey(key), "Statistic with key [%s] is already present", key);
    stats.put(key, value);
    return this;
  }

  public Map<String, Object> getStats() {
    return stats;
  }
}
