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
package org.sonar.server.telemetry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

public class TelemetryPortfolioActivityRequestedMetricProvider extends AbstractTelemetryDataProvider<Boolean> {
  private static final String METRIC_KEY = "portfolio_activity_requested_metric";
  private final Map<String, AtomicBoolean> metricsRequested = new ConcurrentHashMap<>();

  public TelemetryPortfolioActivityRequestedMetricProvider() {
    super(METRIC_KEY, Dimension.INSTALLATION, Granularity.ADHOC, TelemetryDataType.BOOLEAN);
  }

  @Override
  public Map<String, Boolean> getValues() {
    return metricsRequested.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
  }

  @Override
  public void after() {
    metricsRequested.clear();
  }

  public void metricRequested(String metric) {
    metricsRequested.put(metric, new AtomicBoolean(true));
  }
}
