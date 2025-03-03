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
package org.sonar.ce.task.telemetry;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class StepsTelemetryHolderImpl implements MutableStepsTelemetryHolder {

  private final Map<String, Object> telemetryMetrics = new LinkedHashMap<>();

  @Override
  public Map<String, Object> getTelemetryMetrics() {
    return telemetryMetrics;
  }

  @Override
  public StepsTelemetryHolderImpl add(String key, Object value) {
    requireNonNull(key, "Metric has null key");
    requireNonNull(value, () -> String.format("Metric with key [%s] has null value", key));
    checkArgument(!telemetryMetrics.containsKey(key), "Metric with key [%s] is already present", key);
    telemetryMetrics.put(key, value);
    return this;
  }
}
