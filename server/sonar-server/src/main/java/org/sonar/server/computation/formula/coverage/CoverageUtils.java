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
package org.sonar.server.computation.formula.coverage;

import com.google.common.base.Optional;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.formula.CounterInitializationContext;
import org.sonar.server.computation.formula.CreateMeasureContext;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.period.Period;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.period.PeriodPredicates.viewsRestrictedPeriods;

public final class CoverageUtils {
  private static final Measure DEFAULT_MEASURE = newMeasureBuilder().create(0L);
  private static final MeasureVariations DEFAULT_VARIATIONS = new MeasureVariations(0d, 0d, 0d, 0d, 0d);

  private CoverageUtils() {
    // prevents instantiation
  }

  static double calculateCoverage(long coveredLines, long lines) {
    return (100.0 * coveredLines) / lines;
  }

  static long getLongMeasureValue(CounterInitializationContext counterContext, String metricKey) {
    Measure measure = counterContext.getMeasure(metricKey).or(DEFAULT_MEASURE);
    if (measure.getValueType() == Measure.ValueType.NO_VALUE) {
      return 0L;
    }
    if (measure.getValueType() == Measure.ValueType.INT) {
      return measure.getIntValue();
    }
    return measure.getLongValue();
  }

  static MeasureVariations getMeasureVariations(CounterInitializationContext counterContext, String metricKey) {
    Optional<Measure> measure = counterContext.getMeasure(metricKey);
    if (!measure.isPresent() || !measure.get().hasVariations()) {
      return DEFAULT_VARIATIONS;
    }
    return measure.get().getVariations();
  }

  static long getLongVariation(MeasureVariations variations, Period period) {
    if (variations.hasVariation(period.getIndex())) {
      return (long) variations.getVariation(period.getIndex());
    }
    return 0L;
  }

  /**
   * Since Periods 4 and 5 can be customized per project and/or per view/subview, aggregating values on this period
   * will only generate garbage data which will make no sense. These Periods should be ignored when processing views/subviews.
   */
  static Iterable<Period> supportedPeriods(CreateMeasureContext context) {
    return supportedPeriods(context.getComponent().getType(), context.getPeriods());
  }

  /**
   * Since Periods 4 and 5 can be customized per project and/or per view/subview, aggregating values on this period
   * will only generate garbage data which will make no sense. These Periods should be ignored when processing views/subviews.
   */
  public static Iterable<Period> supportedPeriods(CounterInitializationContext context) {
    return supportedPeriods(context.getLeaf().getType(), context.getPeriods());
  }

  private static Iterable<Period> supportedPeriods(Component.Type type, Iterable<Period> periods) {
    if (type.isReportType()) {
      return periods;
    }
    return from(periods).filter(viewsRestrictedPeriods());
  }

}
