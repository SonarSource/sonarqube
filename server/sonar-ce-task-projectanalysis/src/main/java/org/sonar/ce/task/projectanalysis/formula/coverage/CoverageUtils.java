/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.ValueType;

import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public final class CoverageUtils {
  private static final Measure DEFAULT_MEASURE_LONG = newMeasureBuilder().create(0L);
  private static final Measure DEFAULT_MEASURE_INT = newMeasureBuilder().create(0);

  private CoverageUtils() {
    // prevents instantiation
  }

  static double calculateCoverage(long coveredLines, long lines) {
    return (100.0 * coveredLines) / lines;
  }

  static long getLongMeasureValue(CounterInitializationContext counterContext, String metricKey) {
    Measure measure = counterContext.getMeasure(metricKey).orElse(DEFAULT_MEASURE_LONG);
    if (measure.getValueType() == ValueType.NO_VALUE) {
      return 0L;
    }
    if (measure.getValueType() == ValueType.INT) {
      return measure.getIntValue();
    }
    return measure.getLongValue();
  }

  static int getIntMeasureValue(CounterInitializationContext counterContext, String metricKey) {
    Measure measure = counterContext.getMeasure(metricKey).orElse(DEFAULT_MEASURE_INT);
    if (measure.getValueType() == ValueType.NO_VALUE) {
      return 0;
    }
    return measure.getIntValue();
  }
}
