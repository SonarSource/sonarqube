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

public class MetricTest {

  @Test
  public void shouldCreateMetric() {
    Metric metric = new Metric.Builder("foo", "Foo", Metric.ValueType.INT)
      .setDomain("my domain")
      .create();

    assertThat(metric.getKey()).isEqualTo("foo");
    assertThat(metric.getName()).isEqualTo("Foo");
    assertThat(metric.getDomain()).isEqualTo("my domain");
  }

  @Test
  public void shouldCreateMetricWithDefaultValues() {
    Metric metric = new Metric.Builder("foo", "Foo", Metric.ValueType.INT)
      .create();

    assertThat(metric.getBestValue()).isNull();
    assertThat(metric.getDescription()).isNull();
    assertThat(metric.getWorstValue()).isNull();
    assertThat(metric.getDirection()).isEqualTo(Metric.DIRECTION_NONE);
    assertThat(metric.getEnabled()).isTrue();
    assertThat(metric.getFormula()).isNull();
    assertThat(metric.getId()).isNull();
    assertThat(metric.getUserManaged()).isFalse();
    assertThat(metric.isHidden()).isFalse();
    assertThat(metric.isOptimizedBestValue()).isFalse();
  }

  @Test
  public void shouldCreatePercentMetricWithDefaultValues() {
    Metric better = new Metric.Builder("foo", "Foo", Metric.ValueType.PERCENT)
      .setDirection(Metric.DIRECTION_BETTER)
      .create();
    Metric worst = new Metric.Builder("foo", "Foo", Metric.ValueType.PERCENT)
      .setDirection(Metric.DIRECTION_WORST)
      .create();

    assertThat(better.getBestValue()).isEqualTo(100.0);
    assertThat(better.getWorstValue()).isEqualTo(0.0);
    assertThat(worst.getBestValue()).isEqualTo(0.0);
    assertThat(worst.getWorstValue()).isEqualTo(100.0);
  }

  @Test
  public void override_decimal_scale() {
    Metric metric = new Metric.Builder("foo", "Foo", Metric.ValueType.FLOAT)
      .setDecimalScale(3)
      .create();
    assertThat(metric.getDecimalScale()).isEqualTo(3);
  }

  @Test
  public void default_decimal_scale_is_1() {
    Metric metric = new Metric.Builder("foo", "Foo", Metric.ValueType.FLOAT)
      .create();
    assertThat(metric.getDecimalScale()).isEqualTo(1);
  }
}
