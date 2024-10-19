/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.ce.task.projectanalysis.formula.Counter;
import org.sonar.ce.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static org.sonar.ce.task.projectanalysis.formula.coverage.CoverageUtils.calculateCoverage;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * An abstract {@link Formula} which implements the aggregation of a {@link Counter} of 
 * type {@link ElementsAndCoveredElementsCounter} with another counter.
 */
public abstract class CoverageFormula<T extends ElementsAndCoveredElementsCounter> implements Formula<T> {

  @Override
  public Optional<Measure> createMeasure(T counter, CreateMeasureContext context) {
    long elements = counter.elements;
    long coveredElements = counter.coveredElements;
    if (elements > 0L) {
      return Optional.of(newMeasureBuilder().create(calculateCoverage(coveredElements, elements), context.getMetric().getDecimalScale()));
    }
    return Optional.empty();
  }

}
