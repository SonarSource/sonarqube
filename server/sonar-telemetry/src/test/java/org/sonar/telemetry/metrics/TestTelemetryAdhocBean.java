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
package org.sonar.telemetry.metrics;

import java.util.Optional;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.core.TelemetryDataType;

public class TestTelemetryAdhocBean implements TelemetryDataProvider<Boolean> {

  private static final String METRIC_KEY = "telemetry-adhoc-bean";
  private static final Granularity METRIC_GRANULARITY = Granularity.ADHOC;
  private static final TelemetryDataType METRIC_TYPE = TelemetryDataType.BOOLEAN;

  private final Dimension dimension;
  private final boolean value;

  public TestTelemetryAdhocBean(Dimension dimension, boolean value) {
    this.dimension = dimension;
    this.value = value;
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
  public Optional<Boolean> getValue() {
    return value ? Optional.of(true) : Optional.empty();
  }

}
