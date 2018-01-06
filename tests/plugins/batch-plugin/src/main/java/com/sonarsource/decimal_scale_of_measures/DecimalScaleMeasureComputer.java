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
package com.sonarsource.decimal_scale_of_measures;

import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;

public class DecimalScaleMeasureComputer implements MeasureComputer {

  @Override
  public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
    return defContext.newDefinitionBuilder()
      // Output metrics must contains at least one metric
      .setOutputMetrics(DecimalScaleMetric.KEY)

      .build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
    if (context.getMeasure(DecimalScaleMetric.KEY) == null) {
      Iterable<Measure> childMeasures = context.getChildrenMeasures(DecimalScaleMetric.KEY);
      int count = 0;
      double total = 0.0;
      for (Measure childMeasure : childMeasures) {
        count++;
        total += childMeasure.getDoubleValue();
      }
      double value = 0.0;
      if (count > 0) {
        value = total / count;
      }
      context.addMeasure(DecimalScaleMetric.KEY, value);
    }
  }
}
