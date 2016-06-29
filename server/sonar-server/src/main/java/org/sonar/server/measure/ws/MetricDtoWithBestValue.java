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
package org.sonar.server.measure.ws;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.WsMeasures;

class MetricDtoWithBestValue {
  private static final String LOWER_CASE_NEW_METRIC_PREFIX = "new_";

  private final MetricDto metric;
  private final MeasureDto bestValue;

  MetricDtoWithBestValue(MetricDto metric, List<Integer> periodIndexes) {
    this.metric = metric;
    MeasureDto measure = new MeasureDto().setMetricId(metric.getId());
    boolean isNewTypeMetric = metric.getKey().toLowerCase(Locale.ENGLISH).startsWith(LOWER_CASE_NEW_METRIC_PREFIX);
    if (isNewTypeMetric) {
      for (Integer periodIndex : periodIndexes) {
        measure.setVariation(periodIndex, 0.0d);
      }
    } else {
      measure.setValue(metric.getBestValue());
    }

    this.bestValue = measure;
  }

  MetricDto getMetric() {
    return metric;
  }

  MeasureDto getBestValue() {
    return bestValue;
  }

  static class MetricDtoToMetricDtoWithBestValueFunction implements Function<MetricDto, MetricDtoWithBestValue> {
    private final List<Integer> periodIndexes;

    MetricDtoToMetricDtoWithBestValueFunction(List<WsMeasures.Period> periods) {
      this.periodIndexes = Lists.transform(periods, WsPeriodToIndex.INSTANCE);
    }

    @Override
    public MetricDtoWithBestValue apply(@Nonnull MetricDto input) {
      return new MetricDtoWithBestValue(input, periodIndexes);
    }
  }

  private enum WsPeriodToIndex implements Function<WsMeasures.Period, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull WsMeasures.Period input) {
      return input.getIndex();
    }
  }
}
