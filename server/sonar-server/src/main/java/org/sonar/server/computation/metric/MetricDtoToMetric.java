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
package org.sonar.server.computation.metric;

import com.google.common.base.Function;
import javax.annotation.Nonnull;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.computation.measure.Measure;

import static com.google.common.base.Objects.firstNonNull;

enum MetricDtoToMetric implements Function<MetricDto, Metric> {
  INSTANCE;

  private static final int DEFAULT_DECIMAL_SCALE = 1;

  @Override
  @Nonnull
  public Metric apply(@Nonnull MetricDto metricDto) {
    Metric.MetricType metricType = Metric.MetricType.valueOf(metricDto.getValueType());
    Integer decimalScale = null;
    if (metricType.getValueType() == Measure.ValueType.DOUBLE) {
      decimalScale = firstNonNull(metricDto.getDecimalScale(), DEFAULT_DECIMAL_SCALE);
    }

    return new MetricImpl(
      metricDto.getId(), metricDto.getKey(), metricDto.getShortName(), metricType,
      decimalScale,
      metricDto.getBestValue(), metricDto.isOptimizedBestValue());
  }
}
