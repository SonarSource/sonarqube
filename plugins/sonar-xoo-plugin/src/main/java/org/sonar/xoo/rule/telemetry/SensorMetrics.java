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
package org.sonar.xoo.rule.telemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.scanner.ScannerSide;

@ScannerSide
public class SensorMetrics {

  private final SensorTelemetry sensorTelemetry;

  // Metrics
  private final AtomicInteger uninitializedVariableRuleIssues = new AtomicInteger(0);
  private final AtomicLong uninitializedVariableRuleEffortInMinutes = new AtomicLong(0L);

  SensorMetrics(SensorContext context) {
    this.sensorTelemetry = new SensorTelemetry(context);
  }

  protected void incrementUninitializedVariableRuleIssueCounter() {
    uninitializedVariableRuleIssues.incrementAndGet();
  }

  protected void addUninitializedVariableRuleEffortInMinutes(Long value) {
    uninitializedVariableRuleEffortInMinutes.addAndGet(value);
  }

  protected void finalizeAndReportTelemetry() {
    sensorTelemetry
      .addTelemetry("uninitializedVariableRuleIssues", String.valueOf(uninitializedVariableRuleIssues.get()))
      .addTelemetry("uninitializedVariableRuleEffortInMinutes", String.valueOf(uninitializedVariableRuleEffortInMinutes.get()))
      .reportTelemetry();
  }

  private static class SensorTelemetry {
    private final SensorContext context;
    private static final String KEY_PREFIX = "xoo.";
    private final Map<String, String> telemetry = new HashMap<>();

    SensorTelemetry(SensorContext context) {
      this.context = context;
    }

    SensorTelemetry addTelemetry(String key, String value) {
      key = KEY_PREFIX + key;
      if (telemetry.containsKey(key)) {
        throw new IllegalArgumentException("Telemetry key is reported more than once: " + key);
      }
      telemetry.put(key, value);
      return this;
    }

    void reportTelemetry() {
      telemetry.forEach(context::addTelemetryProperty);
    }
  }

}
