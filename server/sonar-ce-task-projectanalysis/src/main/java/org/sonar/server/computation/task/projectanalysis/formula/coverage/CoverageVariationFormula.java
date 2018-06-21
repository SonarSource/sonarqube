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
package org.sonar.server.computation.task.projectanalysis.formula.coverage;

import com.google.common.base.Optional;
import org.sonar.server.computation.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.server.computation.task.projectanalysis.formula.Formula;
import org.sonar.server.computation.task.projectanalysis.formula.counter.LongValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

import static org.sonar.server.computation.task.projectanalysis.formula.coverage.CoverageUtils.calculateCoverage;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * An abstract Formula which implements the aggregation of a Counter of type ElementsAndCoveredElementsVariationCounter
 * with another counter.
 */
public abstract class CoverageVariationFormula<T extends ElementsAndCoveredElementsVariationCounter> implements Formula<T> {

  @Override
  public Optional<Measure> createMeasure(T counter, CreateMeasureContext context) {
    LongValue elements = counter.elements;
    if (elements.isSet() && elements.getValue() > 0d) {
      LongValue coveredElements = counter.coveredElements;
      double variation = calculateCoverage(coveredElements.getValue(), elements.getValue());
      return Optional.of(newMeasureBuilder().setVariation(variation).createNoValue());
    }
    return Optional.absent();
  }

}
