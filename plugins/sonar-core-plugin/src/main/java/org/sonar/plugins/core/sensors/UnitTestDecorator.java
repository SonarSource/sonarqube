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
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class UnitTestDecorator implements Decorator {

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return Arrays.<Metric>asList(CoreMetrics.TEST_EXECUTION_TIME, CoreMetrics.TESTS, CoreMetrics.TEST_ERRORS, CoreMetrics.TEST_FAILURES, CoreMetrics.TEST_SUCCESS_DENSITY);
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return !Project.AnalysisType.STATIC.equals(project.getAnalysisType());
  }

  public boolean shouldDecorateResource(Resource resource) {
    return ResourceUtils.isUnitTestFile(resource) || !ResourceUtils.isEntity(resource);
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource)) {
      sumChildren(context, CoreMetrics.TEST_EXECUTION_TIME);
      sumChildren(context, CoreMetrics.SKIPPED_TESTS);
      Double tests = sumChildren(context, CoreMetrics.TESTS);
      Double errors = sumChildren(context, CoreMetrics.TEST_ERRORS);
      Double failures = sumChildren(context, CoreMetrics.TEST_FAILURES);

      if (isPositive(tests, true) && isPositive(errors, false) && isPositive(failures, false)) {
        Double errorsAndFailuresRatio = (errors + failures) * 100.0 / tests;
        context.saveMeasure(CoreMetrics.TEST_SUCCESS_DENSITY, 100.0 - errorsAndFailuresRatio);
      }
    }
  }

  private boolean isPositive(Double d, boolean strict) {
    return d != null && (strict ? d > 0.0 : d >= 0.0);
  }

  private Double sumChildren(DecoratorContext jobContext, Metric metric) {
    Collection<Measure> childrenMeasures = jobContext.getChildrenMeasures(metric);
    if (childrenMeasures != null && !childrenMeasures.isEmpty()) {
      Double sum = 0.0;
      boolean hasChildrenMeasures = false;
      for (Measure measure : childrenMeasures) {
        if (MeasureUtils.hasValue(measure)) {
          sum += measure.getValue();
          hasChildrenMeasures = true;
        }
      }
      if (hasChildrenMeasures) {
        jobContext.saveMeasure(metric, sum);
        return sum;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
