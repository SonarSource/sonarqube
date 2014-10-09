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
package org.sonar.plugins.core.timemachine;

import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

import java.util.Arrays;
import java.util.List;

@DependedUpon(DecoratorBarriers.END_OF_TIME_MACHINE)
public final class NewCoverageAggregator implements Decorator {

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesNewCoverageMetrics() {
    return Arrays.<Metric>asList(
      CoreMetrics.NEW_LINES_TO_COVER, CoreMetrics.NEW_UNCOVERED_LINES, CoreMetrics.NEW_CONDITIONS_TO_COVER, CoreMetrics.NEW_UNCOVERED_CONDITIONS,
      CoreMetrics.NEW_IT_LINES_TO_COVER, CoreMetrics.NEW_IT_UNCOVERED_LINES, CoreMetrics.NEW_IT_CONDITIONS_TO_COVER, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS,
      CoreMetrics.NEW_OVERALL_LINES_TO_COVER, CoreMetrics.NEW_OVERALL_UNCOVERED_LINES, CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER, CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS);
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorate(resource)) {
      int maxPeriods = Qualifiers.isView(resource, true) ? 3 : 5;
      aggregate(context, CoreMetrics.NEW_LINES_TO_COVER, maxPeriods);
      aggregate(context, CoreMetrics.NEW_UNCOVERED_LINES, maxPeriods);
      aggregate(context, CoreMetrics.NEW_CONDITIONS_TO_COVER, maxPeriods);
      aggregate(context, CoreMetrics.NEW_UNCOVERED_CONDITIONS, maxPeriods);
      aggregate(context, CoreMetrics.NEW_IT_LINES_TO_COVER, maxPeriods);
      aggregate(context, CoreMetrics.NEW_IT_UNCOVERED_LINES, maxPeriods);
      aggregate(context, CoreMetrics.NEW_IT_CONDITIONS_TO_COVER, maxPeriods);
      aggregate(context, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS, maxPeriods);
      aggregate(context, CoreMetrics.NEW_OVERALL_LINES_TO_COVER, maxPeriods);
      aggregate(context, CoreMetrics.NEW_OVERALL_UNCOVERED_LINES, maxPeriods);
      aggregate(context, CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER, maxPeriods);
      aggregate(context, CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS, maxPeriods);
    }
  }

  void aggregate(DecoratorContext context, Metric metric, int maxPeriods) {
    int[] variations = {0, 0, 0, 0, 0};
    boolean[] hasValues = {false, false, false, false, false};
    for (Measure child : context.getChildrenMeasures(metric)) {
      for (int indexPeriod = 1; indexPeriod <= maxPeriods; indexPeriod++) {
        Double variation = child.getVariation(indexPeriod);
        if (variation != null) {
          variations[indexPeriod - 1] = variations[indexPeriod - 1] + variation.intValue();
          hasValues[indexPeriod - 1] = true;
        }
      }
    }

    if (ArrayUtils.contains(hasValues, true)) {
      Measure measure = new Measure(metric);
      for (int index = 0; index < 5; index++) {
        if (hasValues[index]) {
          measure.setVariation(index + 1, (double) variations[index]);
        }
      }
      context.saveMeasure(measure);
    }
  }

  boolean shouldDecorate(Resource resource) {
    return Scopes.isHigherThan(resource, Scopes.FILE);
  }
}
