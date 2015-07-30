/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.api.measures;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricDefinitionTest {

  MetricDefinition.NewMetricContext newMetricContext = new MetricDefinition.NewMetricContext();

  @Test
  public void define_metric() throws Exception {
    assertThat(newMetricContext.getMetrics()).isEmpty();

    newMetricContext.createNewMetric()
      .setKey("key")
      .setName("name")
      .createMeasureComputer()
      .setInputMetricKeys("key1", "key2")
      .setMeasureComputer(new MetricDefinition.MeasureComputer() {
        @Override
        public void compute(MetricDefinition.MeasureComputerContext context) {

        }
      })
      .done()
      .done();

    assertThat(newMetricContext.getMetrics()).hasSize(1);

    MetricDefinition.Metric metric = newMetricContext.getMetric("key");
    assertThat(metric).isNotNull();
    assertThat(metric.getKey()).isEqualTo("key");
    assertThat(metric.getName()).isEqualTo("name");
    assertThat(metric.getComputer()).isNotNull();
  }
}
