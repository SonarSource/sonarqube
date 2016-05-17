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

import com.google.common.base.Predicate;
import com.google.common.collect.Table;
import javax.annotation.Nonnull;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;

class HasMeasure implements Predicate<ComponentDtoWithSnapshotId> {
  private final Predicate<ComponentDtoWithSnapshotId> predicate;

  HasMeasure(Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric, MetricDto metric, ComponentTreeWsRequest request) {
    Integer periodIndex = request.getMetricPeriodSort();
    this.predicate = periodIndex == null
      ? new HasAbsoluteValue(measuresByComponentUuidAndMetric, metric)
      : new HasValueOnPeriod(periodIndex, measuresByComponentUuidAndMetric, metric);
  }

  @Override
  public boolean apply(@Nonnull ComponentDtoWithSnapshotId input) {
    return predicate.apply(input);
  }

  private static class HasAbsoluteValue implements Predicate<ComponentDtoWithSnapshotId> {
    private final Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;
    private final MetricDto metric;

    private HasAbsoluteValue(Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric, MetricDto metric) {
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
      this.metric = metric;
    }

    @Override
    public boolean apply(@Nonnull ComponentDtoWithSnapshotId input) {
      MeasureDto measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      return measure != null && (measure.getValue() != null || measure.getData() != null);
    }
  }

  private static class HasValueOnPeriod implements Predicate<ComponentDtoWithSnapshotId> {
    private final int periodIndex;
    private final Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;
    private final MetricDto metric;

    private HasValueOnPeriod(int periodIndex, Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric, MetricDto metric) {
      this.periodIndex = periodIndex;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
      this.metric = metric;
    }

    @Override
    public boolean apply(@Nonnull ComponentDtoWithSnapshotId input) {
      MeasureDto measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      return measure != null && measure.getVariation(periodIndex) != null;
    }
  }

}
