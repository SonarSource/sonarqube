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

public final class LinesAndConditionsWithUncoveredVariationCounter extends ElementsAndCoveredElementsVariationCounter {
  private final LinesAndConditionsWithUncoveredMetricKeys metricKeys;

  public LinesAndConditionsWithUncoveredVariationCounter(LinesAndConditionsWithUncoveredMetricKeys metricKeys) {
    this.metricKeys = metricKeys;
  }

  @Override
  public void initializeForSupportedLeaf(CounterInitializationContext counterContext) {
    Optional<Measure> newLinesMeasure = counterContext.getMeasure(metricKeys.getLines());
    if (!newLinesMeasure.isPresent() || !newLinesMeasure.get().hasVariation()) {
      return;
    }
    double newLines = newLinesMeasure.get().getVariation();
    long newConditions = (long) CoverageUtils.getMeasureVariations(counterContext, metricKeys.getConditions());
    long uncoveredLines = (long) CoverageUtils.getMeasureVariations(counterContext, metricKeys.getUncoveredLines());
    long uncoveredConditions = (long) CoverageUtils.getMeasureVariations(counterContext, metricKeys.getUncoveredConditions());

    long elements = (long) newLines + newConditions;
    this.elements.increment(elements);
    coveredElements.increment(elements - uncoveredConditions - uncoveredLines);
  }
}
