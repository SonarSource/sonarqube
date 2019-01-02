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

import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;

import static java.util.Objects.requireNonNull;
import static org.sonar.ce.task.projectanalysis.formula.coverage.CoverageUtils.getMeasureVariations;

public final class SingleWithUncoveredVariationCounter extends ElementsAndCoveredElementsVariationCounter {
  private final SingleWithUncoveredMetricKeys metricKeys;

  public SingleWithUncoveredVariationCounter(SingleWithUncoveredMetricKeys metricKeys) {
    this.metricKeys = requireNonNull(metricKeys);
  }

  @Override
  protected void initializeForSupportedLeaf(CounterInitializationContext counterContext) {
    long newConditions = (long) getMeasureVariations(counterContext, metricKeys.getCovered());
    long uncoveredConditions = (long) getMeasureVariations(counterContext, metricKeys.getUncovered());
    this.elements.increment(newConditions);
    coveredElements.increment(newConditions - uncoveredConditions);
  }
}
