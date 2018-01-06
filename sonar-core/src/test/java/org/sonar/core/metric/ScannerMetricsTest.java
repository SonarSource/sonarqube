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
package org.sonar.core.metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static org.assertj.core.api.Assertions.assertThat;

public class ScannerMetricsTest {

  // metrics that are always added, regardless of plugins
  private static final List<Metric> SENSOR_METRICS_WITHOUT_METRIC_PLUGIN = metrics();

  @Test
  public void check_number_of_allowed_core_metrics() throws Exception {
    assertThat(SENSOR_METRICS_WITHOUT_METRIC_PLUGIN).hasSize(34);
  }

  @Test
  public void check_metrics_from_plugin() throws Exception {
    List<Metric> metrics = metrics(new FakeMetrics());
    metrics.removeAll(SENSOR_METRICS_WITHOUT_METRIC_PLUGIN);
    assertThat(metrics).hasSize(2);
  }

  @Test
  public void should_not_crash_on_null_metrics_from_faulty_plugins() {
    Metrics faultyMetrics = () -> null;
    Metrics okMetrics = new FakeMetrics();

    List<Metric> metrics = metrics(okMetrics, faultyMetrics);
    metrics.removeAll(SENSOR_METRICS_WITHOUT_METRIC_PLUGIN);

    assertThat(metrics).isEqualTo(okMetrics.getMetrics());
  }

  private static List<Metric> metrics(Metrics... metrics) {
    return new ArrayList<>(new ScannerMetrics(metrics).getMetrics());
  }

  private static class FakeMetrics implements Metrics {
    @Override
    public List<Metric> getMetrics() {
      return Arrays.asList(
        new Metric.Builder("key1", "name1", Metric.ValueType.INT).create(),
        new Metric.Builder("key2", "name2", Metric.ValueType.FLOAT).create());
    }
  }
}
