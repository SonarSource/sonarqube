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
import org.sonar.server.computation.formula.CreateMeasureContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.counter.LongVariationValue;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.period.Period;

import static org.sonar.server.computation.formula.coverage.CoverageUtils.calculateCoverage;
import static org.sonar.server.computation.formula.coverage.CoverageUtils.supportedPeriods;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

/**
 * An abstract Formula which implements the aggregation of a Counter of type ElementsAndCoveredElementsVariationCounter
 * with another counter.
 */
public abstract class CoverageVariationFormula<T extends ElementsAndCoveredElementsVariationCounter> implements Formula<T> {

  @Override
  public Optional<Measure> createMeasure(T counter, CreateMeasureContext context) {
    MeasureVariations.Builder builder = createAndPopulateBuilder(counter, context);
    if (!builder.isEmpty()) {
      return Optional.of(newMeasureBuilder().setVariations(builder.build()).createNoValue());
    }

    return Optional.absent();
  }

  private MeasureVariations.Builder createAndPopulateBuilder(T counter, CreateMeasureContext context) {
    MeasureVariations.Builder builder = MeasureVariations.newMeasureVariationsBuilder();
    for (Period period : supportedPeriods(context)) {
      LongVariationValue elements = counter.elements.get(period);
      if (elements.isSet() && elements.getValue() > 0d) {
        LongVariationValue coveredElements = counter.coveredElements.get(period);
        builder.setVariation(period, calculateCoverage(coveredElements.getValue(), elements.getValue()));
      }
    }
    return builder;
  }
}
