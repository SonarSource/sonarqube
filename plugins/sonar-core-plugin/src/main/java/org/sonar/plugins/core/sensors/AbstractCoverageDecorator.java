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
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.Arrays;
import java.util.Collection;

public abstract class AbstractCoverageDecorator implements Decorator {

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public Collection<Metric> generatedMetrics() {
    return Arrays.asList(getGeneratedMetric(), getGeneratedMetricForNewCode());
  }

  @Override
  public void decorate(final Resource resource, final DecoratorContext context) {
    if (shouldDecorate(resource)) {
      computeMeasure(context);
      computeMeasureForNewCode(context);
    }
  }

  protected boolean shouldDecorate(final Resource resource) {
    return !ResourceUtils.isUnitTestFile(resource);
  }

  private void computeMeasure(DecoratorContext context) {
    if (context.getMeasure(getGeneratedMetric()) == null) {
      Long elements = countElements(context);
      if (elements != null && elements > 0L) {
        Long coveredElements = countCoveredElements(context);
        context.saveMeasure(getGeneratedMetric(), calculateCoverage(coveredElements, elements));
      }
    }
  }

  private void computeMeasureForNewCode(DecoratorContext context) {
    if (context.getMeasure(getGeneratedMetricForNewCode()) == null) {
      Measure measure = new Measure(getGeneratedMetricForNewCode());
      boolean hasValue = false;
      /* TODO remove this magic number */
      for (int periodIndex = 1; periodIndex <= 5; periodIndex++) {
        Long elements = countElementsForNewCode(context, periodIndex);

        if (elements != null && elements > 0L) {
          long coveredElements = countCoveredElementsForNewCode(context, periodIndex);
          measure.setVariation(periodIndex, calculateCoverage(coveredElements, elements));
          hasValue = true;
        }
      }
      if (hasValue) {
        context.saveMeasure(measure);
      }
    }
  }

  private double calculateCoverage(final long coveredLines, final long lines) {
    return (100.0 * coveredLines) / lines;
  }

  protected abstract Metric getGeneratedMetric();

  protected abstract Long countElements(DecoratorContext context);

  protected abstract long countCoveredElements(DecoratorContext context);

  protected abstract Metric getGeneratedMetricForNewCode();

  protected abstract Long countElementsForNewCode(DecoratorContext context, int periodIndex);

  protected abstract long countCoveredElementsForNewCode(DecoratorContext context, int periodIndex);
}
