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
import org.sonar.server.computation.formula.CounterInitializationContext;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.period.Period;

import static org.sonar.server.computation.formula.coverage.CoverageUtils.getLongVariation;
import static org.sonar.server.computation.formula.coverage.CoverageUtils.supportedPeriods;

public final class LinesAndConditionsWithUncoveredVariationCounter extends ElementsAndCoveredElementsVariationCounter {
  private final LinesAndConditionsWithUncoveredMetricKeys metricKeys;

  public LinesAndConditionsWithUncoveredVariationCounter(LinesAndConditionsWithUncoveredMetricKeys metricKeys) {
    this.metricKeys = metricKeys;
  }

  @Override
  public void initializeForSupportedLeaf(CounterInitializationContext counterContext) {
    Optional<Measure> newLinesMeasure = counterContext.getMeasure(metricKeys.getLines());
    if (!newLinesMeasure.isPresent() || !newLinesMeasure.get().hasVariations()) {
      return;
    }

    MeasureVariations newLines = newLinesMeasure.get().getVariations();
    MeasureVariations newConditions = CoverageUtils.getMeasureVariations(counterContext, metricKeys.getConditions());
    MeasureVariations uncoveredLines = CoverageUtils.getMeasureVariations(counterContext, metricKeys.getUncoveredLines());
    MeasureVariations uncoveredConditions = CoverageUtils.getMeasureVariations(counterContext, metricKeys.getUncoveredConditions());
    for (Period period : supportedPeriods(counterContext)) {
      if (!newLines.hasVariation(period.getIndex())) {
        continue;
      }
      long elements = (long) newLines.getVariation(period.getIndex()) + getLongVariation(newConditions, period);
      this.elements.increment(period, elements);
      coveredElements.increment(
        period,
        elements - getLongVariation(uncoveredConditions, period) - getLongVariation(uncoveredLines, period));
    }
  }
}
