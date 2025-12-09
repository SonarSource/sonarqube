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
package org.sonar.server.telemetry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

public class TelemetryBadgeProvider extends AbstractTelemetryDataProvider<Integer> {
  private final Map<String, AtomicInteger> badgesCounters = new ConcurrentHashMap<>();

  public TelemetryBadgeProvider() {
    super("project_badges_count", Dimension.INSTALLATION, Granularity.ADHOC, TelemetryDataType.INTEGER);
  }

  @Override
  public Map<String, Integer> getValues() {
    return badgesCounters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
  }

  @Override
  public void after() {
    badgesCounters.clear();
  }

  public void incrementForMetric(String metricKey) {
    badgesCounters.merge(metricKey, new AtomicInteger(1), (oldValue, newValue) -> {
      oldValue.addAndGet(newValue.get());
      return oldValue;
    });
  }
}
