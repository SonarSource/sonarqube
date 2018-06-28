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
package org.sonar.server.issue.notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DistributedMetricStatsInt {
  private MetricStatsInt globalStats = new MetricStatsInt();
  private Map<String, MetricStatsInt> statsPerLabel = new HashMap<>();

  DistributedMetricStatsInt increment(String label, boolean onLeak) {
    this.globalStats.increment(onLeak);
    statsPerLabel.computeIfAbsent(label, l -> new MetricStatsInt()).increment(onLeak);
    return this;
  }

  Map<String, MetricStatsInt> getForLabels() {
    return Collections.unmodifiableMap(statsPerLabel);
  }

  public Optional<MetricStatsInt> getForLabel(String label) {
    return Optional.ofNullable(statsPerLabel.get(label));
  }

  public int getOnLeak() {
    return globalStats.getOnLeak();
  }

  public int getOffLeak() {
    return globalStats.getOffLeak();
  }

  public int getTotal() {
    return globalStats.getTotal();
  }

  @Override
  public String toString() {
    return "DistributedMetricStatsInt{" +
      "globalStats=" + globalStats +
      ", statsPerLabel=" + statsPerLabel +
      '}';
  }
}
