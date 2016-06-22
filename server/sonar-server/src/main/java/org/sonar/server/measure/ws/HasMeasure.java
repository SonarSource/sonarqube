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

import com.google.common.collect.Table;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;

class HasMeasure implements Predicate<ComponentDto> {
  private final Predicate<ComponentDto> predicate;

  HasMeasure(Table<String, MetricDto, MeasureDto> table, MetricDto metric, ComponentTreeWsRequest request) {
    Integer periodIndex = request.getMetricPeriodSort();
    this.predicate = periodIndex == null
      ? new HasAbsoluteValue(table, metric)
      : new HasValueOnPeriod(periodIndex, table, metric);
  }

  @Override
  public boolean test(@Nonnull ComponentDto input) {
    return predicate.test(input);
  }

  private static class HasAbsoluteValue implements Predicate<ComponentDto> {
    private final Table<String, MetricDto, MeasureDto> table;
    private final MetricDto metric;

    private HasAbsoluteValue(Table<String, MetricDto, MeasureDto> table, MetricDto metric) {
      this.table = table;
      this.metric = metric;
    }

    @Override
    public boolean test(@Nonnull ComponentDto input) {
      MeasureDto measure = table.get(input.uuid(), metric);
      return measure != null && (measure.getValue() != null || measure.getData() != null);
    }
  }

  private static class HasValueOnPeriod implements Predicate<ComponentDto> {
    private final int periodIndex;
    private final Table<String, MetricDto, MeasureDto> table;
    private final MetricDto metric;

    private HasValueOnPeriod(int periodIndex, Table<String, MetricDto, MeasureDto> table, MetricDto metric) {
      this.periodIndex = periodIndex;
      this.table = table;
      this.metric = metric;
    }

    @Override
    public boolean test(@Nonnull ComponentDto input) {
      MeasureDto measure = table.get(input.uuid(), metric);
      return measure != null && measure.getVariation(periodIndex) != null;
    }
  }

}
