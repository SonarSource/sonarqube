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
package org.sonar.server.measure.ws;

import com.google.common.collect.Table;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;

import static org.sonar.server.measure.ws.ComponentTreeData.Measure;

class HasMeasure implements Predicate<ComponentDto> {
  private final Predicate<ComponentDto> predicate;

  HasMeasure(Table<String, MetricDto, ComponentTreeData.Measure> table, MetricDto metric, @Nullable Integer metricPeriodSort) {
    this.predicate = metricPeriodSort == null
      ? new HasAbsoluteValue(table, metric)
      : new HasValueOnPeriod(table, metric);
  }

  @Override
  public boolean test(@Nonnull ComponentDto input) {
    return predicate.test(input);
  }

  private static class HasAbsoluteValue implements Predicate<ComponentDto> {
    private final Table<String, MetricDto, ComponentTreeData.Measure> table;
    private final MetricDto metric;

    private HasAbsoluteValue(Table<String, MetricDto, ComponentTreeData.Measure> table, MetricDto metric) {
      this.table = table;
      this.metric = metric;
    }

    @Override
    public boolean test(@Nonnull ComponentDto input) {
      Measure measure = table.get(input.uuid(), metric);
      return measure != null && (measure.isValueSet() || measure.getData() != null);
    }
  }

  private static class HasValueOnPeriod implements Predicate<ComponentDto> {
    private final Table<String, MetricDto, ComponentTreeData.Measure> table;
    private final MetricDto metric;

    private HasValueOnPeriod(Table<String, MetricDto, ComponentTreeData.Measure> table, MetricDto metric) {
      this.table = table;
      this.metric = metric;
    }

    @Override
    public boolean test(@Nonnull ComponentDto input) {
      Measure measure = table.get(input.uuid(), metric);
      return measure != null && measure.isVariationSet();
    }
  }

}
