/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.api.measures;

import org.sonar.api.resources.ResourceUtils;

import java.util.List;
import java.util.Arrays;

/**
 * @since 2.1
 */
public class AverageComplexityFormula implements Formula {

  private Metric byMetric;

  /**
   * @param byMetric The metric on which average complexity should be calculated : complexity by file, by method...
   */
  public AverageComplexityFormula(Metric byMetric) {
    this.byMetric = byMetric;
  }

  public List<Metric> dependsUponMetrics() {
    return Arrays.asList(CoreMetrics.COMPLEXITY, byMetric);
  }

  public Measure calculate(FormulaData data, FormulaContext context) {
    if (!shouldDecorateResource(data, context)) {
      return null;
    }
    if (ResourceUtils.isFile(context.getResource())) {
      Double byMeasure = MeasureUtils.getValue(data.getMeasure(byMetric), null);
      Double complexity = MeasureUtils.getValue(data.getMeasure(CoreMetrics.COMPLEXITY), null);
      if (complexity != null && byMeasure != null && byMeasure > 0.0) {
        return new Measure(context.getTargetMetric(), (complexity / byMeasure));
      }
    } else {
      double totalByMeasure = 0;
      double totalComplexity = 0;
      boolean hasApplicableChildren = false;

      for (FormulaData childrenData : data.getChildren()) {
        Double childrenByMeasure = MeasureUtils.getValue(childrenData.getMeasure(byMetric), null);
        Double childrenComplexity = MeasureUtils.getValue(childrenData.getMeasure(CoreMetrics.COMPLEXITY), null);
        if (childrenComplexity != null && childrenByMeasure != null && childrenByMeasure > 0.0) {
          totalByMeasure += childrenByMeasure;
          totalComplexity += childrenComplexity;
          hasApplicableChildren = true;
        }
      }
      if (hasApplicableChildren) {
        return new Measure(context.getTargetMetric(), (totalComplexity / totalByMeasure));
      }
    }
    return null;
  }

  private boolean shouldDecorateResource(FormulaData data, FormulaContext context) {
    return !MeasureUtils.hasValue(data.getMeasure(context.getTargetMetric()));
  }
}