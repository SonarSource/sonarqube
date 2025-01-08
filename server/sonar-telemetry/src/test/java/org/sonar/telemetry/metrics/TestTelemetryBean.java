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
package org.sonar.telemetry.metrics;

import java.util.Map;
import java.util.Optional;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.core.TelemetryDataType;

public class TestTelemetryBean implements TelemetryDataProvider<String> {

  private static final String METRIC_KEY = "telemetry-bean-a";
  private static final Granularity METRIC_GRANULARITY = Granularity.DAILY;
  private static final TelemetryDataType METRIC_TYPE = TelemetryDataType.STRING;
  private static final String METRIC_VALUE = "value";
  private static final Map<String, String> METRIC_UUID_VALUES = Map.of("key-1", "value-1", "key-2", "value-2");

  private final Dimension dimension;

  public TestTelemetryBean(Dimension dimension) {
    this.dimension = dimension;
  }

  @Override
  public Dimension getDimension() {
    return dimension;
  }

  @Override
  public String getMetricKey() {
    return METRIC_KEY;
  }

  @Override
  public Granularity getGranularity() {
    return METRIC_GRANULARITY;
  }

  @Override
  public TelemetryDataType getType() {
    return METRIC_TYPE;
  }

  @Override
  public Optional<String> getValue() {
    return Optional.of(METRIC_VALUE);
  }

  @Override
  public Map<String, String> getValues() {
    return METRIC_UUID_VALUES;
  }

}
