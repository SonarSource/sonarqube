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
package org.sonar.ce.task.projectanalysis.metric;

import org.junit.Test;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetricDtoToMetricTest {

  private static final double SOME_BEST_VALUE = 951;

  private MetricDtoToMetric underTest = MetricDtoToMetric.INSTANCE;

  @Test
  public void apply_throws_NPE_if_arg_is_null() {
    assertThatThrownBy(() -> underTest.apply(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void verify_mapping_from_dto() {
    for (Metric.MetricType metricType : Metric.MetricType.values()) {
      MetricDto metricDto = createMetricDto(metricType);
      Metric metric = underTest.apply(metricDto);

      assertThat(metric.getUuid()).isEqualTo(metricDto.getUuid());
      assertThat(metric.getKey()).isEqualTo(metricDto.getKey());
      assertThat(metric.getName()).isEqualTo(metricDto.getShortName());
      assertThat(metric.getType()).isEqualTo(metricType);
      assertThat(metric.isBestValueOptimized()).isFalse();
      assertThat(metric.getBestValue()).isEqualTo(SOME_BEST_VALUE);
      assertThat(metric.isDeleteHistoricalData()).isTrue();

    }
  }

  @Test
  public void verify_mapping_of_isBestValueOptimized() {
    assertThat(underTest.apply(createMetricDto(Metric.MetricType.INT).setOptimizedBestValue(true)).isBestValueOptimized()).isTrue();
  }

  @Test
  public void apply_throws_IAE_if_valueType_can_not_be_parsed() {
    assertThatThrownBy(() -> underTest.apply(new MetricDto().setUuid("1").setKey("key").setValueType("trololo")))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private static MetricDto createMetricDto(Metric.MetricType metricType) {
    return new MetricDto()
      .setUuid(metricType.name())
      .setKey(metricType.name() + "_key")
      .setShortName(metricType.name() + "_name")
      .setValueType(metricType.name())
      .setBestValue(SOME_BEST_VALUE)
      .setDeleteHistoricalData(true)
      .setEnabled(true);
  }
}
