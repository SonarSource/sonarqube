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
package org.sonar.api.measures;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 2.0
 */
public class MeanAggregationFormula implements Formula {

  private boolean forceZeroIfMissingData=false;

  public MeanAggregationFormula(boolean forceZeroIfMissingData) {
    this.forceZeroIfMissingData = forceZeroIfMissingData;
  }

  public MeanAggregationFormula() {
    this(false);
  }

  @Override
  public List<Metric> dependsUponMetrics() {
    return Collections.emptyList();
  }

  @Override
  public Measure calculate(FormulaData data, FormulaContext context) {
    double sum=0.0;
    int count=0;
    boolean hasValue=false;
    Collection<Measure> measures = data.getChildrenMeasures(context.getTargetMetric());
    for (Measure measure : measures) {
      if (MeasureUtils.hasValue(measure)) {
        sum+=measure.getValue();
        count++;
        hasValue=true;
      }
    }

    if (!hasValue && !forceZeroIfMissingData) {
      return null;
    }
    return new Measure(context.getTargetMetric(), count==0 ? 0.0 : sum/count);
  }
}
