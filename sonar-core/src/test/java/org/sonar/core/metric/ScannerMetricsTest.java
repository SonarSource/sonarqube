/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class ScannerMetricsTest {

  static final ScannerMetrics SENSOR_METRICS_WITHOUT_METRIC_PLUGIN = new ScannerMetrics(new Metrics[] {});
  static final ScannerMetrics SENSOR_METRICS_WITH_PLUGIN = new ScannerMetrics(new Metrics[] {new FakeMetrics()});

  @Test
  public void check_number_of_allowed_core_metrics() throws Exception {
    assertThat(SENSOR_METRICS_WITHOUT_METRIC_PLUGIN.getMetrics()).hasSize(34);
  }

  @Test
  public void check_metrics_from_plugin() throws Exception {
    List<Metric> metrics = newArrayList(SENSOR_METRICS_WITH_PLUGIN.getMetrics());
    Iterables.removeAll(metrics, SENSOR_METRICS_WITHOUT_METRIC_PLUGIN.getMetrics());
    assertThat(metrics).hasSize(2);
  }

  private static class FakeMetrics implements Metrics {

    @Override
    public List<Metric> getMetrics() {
      return ImmutableList.<Metric>of(
        new Metric.Builder("key1", "name1", Metric.ValueType.INT).create(),
        new Metric.Builder("key2", "name2", Metric.ValueType.FLOAT).create());
    }
  }
}
