/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.formula.coverage;

import java.util.Optional;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public final class CoverageUtils {
  private static final Measure DEFAULT_MEASURE = newMeasureBuilder().create(0L);

  private CoverageUtils() {
    // prevents instantiation
  }

  static double calculateCoverage(long coveredLines, long lines) {
    return (100.0 * coveredLines) / lines;
  }

  static long getLongMeasureValue(CounterInitializationContext counterContext, String metricKey) {
    Measure measure = counterContext.getMeasure(metricKey).orElse(DEFAULT_MEASURE);
    if (measure.getValueType() == Measure.ValueType.NO_VALUE) {
      return 0L;
    }
    if (measure.getValueType() == Measure.ValueType.INT) {
      return measure.getIntValue();
    }
    return measure.getLongValue();
  }

  static double getMeasureVariations(CounterInitializationContext counterContext, String metricKey) {
    Optional<Measure> measure = counterContext.getMeasure(metricKey);
    if (!measure.isPresent() || !measure.get().hasVariation()) {
      return 0d;
    }
    return measure.get().getVariation();
  }

}
