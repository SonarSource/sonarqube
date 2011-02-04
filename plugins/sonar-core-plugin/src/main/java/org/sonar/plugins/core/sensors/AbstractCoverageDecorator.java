/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

public abstract class AbstractCoverageDecorator implements Decorator {

  public boolean shouldExecuteOnProject(Project project) {
    return project.getAnalysisType().isDynamic(true);
  }

  @DependedUpon
  public Metric generatesCoverage() {
    return getTargetMetric();
  }

  public void decorate(final Resource resource, final DecoratorContext context) {
    if (shouldDecorate(resource, context)) {
      saveLineCoverage(context);
    }
  }

  protected boolean shouldDecorate(final Resource resource, final DecoratorContext context) {
    return context.getMeasure(getTargetMetric()) == null && !ResourceUtils.isUnitTestClass(resource);
  }

  private void saveLineCoverage(DecoratorContext context) {
    Double elements = countElements(context);
    Double coveredElements = countCoveredElements(context);

    if (elements != null && elements > 0.0 && coveredElements != null) {
      context.saveMeasure(getTargetMetric(), calculateCoverage(coveredElements, elements));
    }
  }

  private double calculateCoverage(final Double coveredLines, final Double lines) {
    return (100.0 * coveredLines) / lines;
  }

  protected abstract Metric getTargetMetric();

  protected abstract Double countCoveredElements(DecoratorContext context);

  protected abstract Double countElements(DecoratorContext context);
}
