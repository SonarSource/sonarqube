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
package org.sonar.server.computation.formula.coverage;

import com.google.common.base.Optional;
import org.sonar.server.computation.formula.FileAggregateContext;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.period.Period;

import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public final class CoverageUtils {
  private static final Measure DEFAULT_MEASURE = newMeasureBuilder().create(0L);
  private static final MeasureVariations DEFAULT_VARIATIONS = new MeasureVariations(0d, 0d, 0d, 0d, 0d);

  private CoverageUtils() {
    // prevents instantiation
  }

  static double calculateCoverage(long coveredLines, long lines) {
    return (100.0 * coveredLines) / lines;
  }

  static long getLongMeasureValue(FileAggregateContext counterContext, String metricKey) {
    Measure measure = counterContext.getMeasure(metricKey).or(DEFAULT_MEASURE);
    if (measure.getValueType() == Measure.ValueType.NO_VALUE) {
      return 0L;
    }
    if (measure.getValueType() == Measure.ValueType.INT) {
      return measure.getIntValue();
    }
    return measure.getLongValue();
  }

  static MeasureVariations getMeasureVariations(FileAggregateContext counterContext, String metricKey) {
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
}
