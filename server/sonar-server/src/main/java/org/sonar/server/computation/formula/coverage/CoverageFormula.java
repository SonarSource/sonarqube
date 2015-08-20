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
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.formula.CreateMeasureContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.measure.Measure;

import static org.sonar.server.computation.formula.coverage.CoverageUtils.calculateCoverage;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

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
      return Optional.of(newMeasureBuilder().create(calculateCoverage(coveredElements, elements)));
    }
    return Optional.absent();
  }

}
