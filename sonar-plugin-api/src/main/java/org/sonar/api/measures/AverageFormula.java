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
package org.sonar.api.measures;

import java.util.List;

/**
 * @since 3.0
 * @deprecated since 5.2. Aggregation of measures is provided by {@link org.sonar.api.ce.measure.MeasureComputer}. {@link org.sonar.api.batch.Decorator}
 * and {@link Formula} are no more supported.
 */
@Deprecated
public class AverageFormula implements Formula {

  /**
   * Creates a new {@link AverageFormula} class.
   *
   * @param main The metric on which average should be calculated (ex.: "complexity")
   * @param by   The metric used to divide the main metric to compute average (ex.: "file" for "complexity by file")
   */
  public static AverageFormula create(Metric main, Metric by) {
    return new AverageFormula();
  }

  /**
   * Set a fallback metric if no measures found for the main metric.
   *
   * @param fallbackMetric The fallback metric
   * @since 3.6
   */
  public AverageFormula setFallbackForMainMetric(Metric fallbackMetric) {
    throw fail();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Metric> dependsUponMetrics() {
    throw fail();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Measure calculate(FormulaData data, FormulaContext context) {
    throw fail();
  }

  private static RuntimeException fail() {
    throw new UnsupportedOperationException("Unsupported since version 5.2. Decorators and formulas are not used anymore for aggregation measures. " +
      "Please use org.sonar.api.ce.measure.MeasureComputer.");
  }
}
