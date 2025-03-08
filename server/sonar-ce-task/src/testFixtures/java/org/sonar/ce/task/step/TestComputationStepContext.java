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
package org.sonar.ce.task.step;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.ce.task.telemetry.MutableStepsTelemetryHolder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Implementation of {@link ComputationStep.Context} for unit tests.
 */
public class TestComputationStepContext implements ComputationStep.Context {

  private final TestStatistics statistics = new TestStatistics();
  private final TestTelemetryMetrics metrics = new TestTelemetryMetrics();

  private static String prependPrefix(@Nullable String prefix, String key) {
    if (prefix == null) {
      return key;
    } else {
      return prefix + "." + key;
    }
  }

  @Override
  public TestStatistics getStatistics() {
    return statistics;
  }

  @Override
  public void addTelemetryMetricOnly(String telemetryPrefix, String key, Object value) {
    metrics.add(prependPrefix(telemetryPrefix, key), value);
  }

  @Override
  public void addTelemetryWithStatistic(String telemetryPrefix, String key, Object value) {
    Objects.requireNonNull(telemetryPrefix);
    metrics.add(prependPrefix(telemetryPrefix, key), value);
    statistics.add(key, value);
  }

  public Map<String, Object> getTelemetryMetrics() {
    return metrics.getTelemetryMetrics();
  }

  public static class TestStatistics implements ComputationStep.Statistics {
    private final Map<String, Object> map = new HashMap<>();

    @Override
    public ComputationStep.Statistics add(String key, Object value) {
      requireNonNull(key, "Statistic has null key");
      requireNonNull(value, () -> String.format("Statistic with key [%s] has null value", key));
      checkArgument(!key.equalsIgnoreCase("time"), "Statistic with key [time] is not accepted");
      checkArgument(!map.containsKey(key), "Statistic with key [%s] is already present", key);
      map.put(key, value);
      return this;
    }

    public Map<String, Object> getAll() {
      return map;
    }

    public Object get(String key) {
      return requireNonNull(map.get(key));
    }

    public TestStatistics assertValue(String key, @Nullable Object expectedValue) {
      if (expectedValue == null) {
        assertThat(map.get(key)).as(key).isNull();
      } else {
        assertThat(map.get(key)).as(key).isEqualTo(expectedValue);
      }
      return this;
    }
  }

  public static class TestTelemetryMetrics implements MutableStepsTelemetryHolder {
    private final Map<String, Object> map = new LinkedHashMap<>();

    public TestTelemetryMetrics add(String key, Object value) {
      requireNonNull(key, "Metric has null key");
      requireNonNull(value, () -> String.format("Metric with key [%s] has null value", key));
      checkArgument(!map.containsKey(key), "Metric with key [%s] is already present", key);
      map.put(key, value);
      return this;
    }

    @Override
    public Map<String, Object> getTelemetryMetrics() {
      return map;
    }

    public Object get(String key) {
      return requireNonNull(map.get(key));
    }

    public TestTelemetryMetrics assertValue(String key, @Nullable Object expectedValue) {
      if (expectedValue == null) {
        assertThat(map.get(key)).as(key).isNull();
      } else {
        assertThat(map.get(key)).as(key).isEqualTo(expectedValue);
      }
      return this;
    }
  }
}
