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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import javax.annotation.Nonnull;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.ValueType.NO_VALUE;

public class BestValueOptimization implements Predicate<Measure> {
  private final Metric metric;

  private BestValueOptimization(Metric metric) {
    this.metric = requireNonNull(metric);
  }

  public static Predicate<Measure> from(Metric metric, Component component) {
    if (isBestValueOptimized(metric) && isBestValueOptimized(component)) {
      return new BestValueOptimization(metric);
    }
    return Predicates.alwaysFalse();
  }

  private static boolean isBestValueOptimized(Metric metric) {
    return metric.isBestValueOptimized();
  }

  private static boolean isBestValueOptimized(Component component) {
    return component.getType() == Component.Type.FILE;
  }

  @Override
  public boolean apply(@Nonnull Measure measure) {
    return isBestValueOptimized(measure);
  }

  private boolean isBestValueOptimized(Measure measure) {
    return measure.getDescription() == null
      && measure.getData() == null
      && !measure.hasQualityGateStatus()
      && hasNoVariation(measure)
      && (measure.getValueType() == NO_VALUE || isBestValue(measure, metric.getBestValue()));
  }

  private static boolean hasNoVariation(Measure measure) {
    return !measure.hasVariations() || hasOnlyZeroVariations(measure.getVariations());
  }

  private static boolean hasOnlyZeroVariations(MeasureVariations variations) {
    return (!variations.hasVariation1() || NumberUtils.compare(variations.getVariation1(), 0.0) == 0)
      && (!variations.hasVariation2() || NumberUtils.compare(variations.getVariation2(), 0.0) == 0)
      && (!variations.hasVariation3() || NumberUtils.compare(variations.getVariation3(), 0.0) == 0)
      && (!variations.hasVariation4() || NumberUtils.compare(variations.getVariation4(), 0.0) == 0)
      && (!variations.hasVariation5() || NumberUtils.compare(variations.getVariation5(), 0.0) == 0);
  }

  private static boolean isBestValue(Measure measure, Double bestValue) {
    switch (measure.getValueType()) {
      case BOOLEAN:
        return bestValue.intValue() == 1 ? measure.getBooleanValue() : !measure.getBooleanValue();
      case INT:
        return bestValue.intValue() == measure.getIntValue();
      case LONG:
        return bestValue.longValue() == measure.getLongValue();
      case DOUBLE:
        return bestValue.compareTo(measure.getDoubleValue()) == 0;
      default:
        return false;
    }
  }
}
