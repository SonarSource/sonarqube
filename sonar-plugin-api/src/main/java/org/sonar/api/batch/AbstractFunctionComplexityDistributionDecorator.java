/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.api.batch;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.CountDistributionBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

/**
 * @deprecated since 2.1, a formula has been implemented on the metric, so no need to have decorator anymore
 * @since 2.0
 */
@Deprecated
public abstract class AbstractFunctionComplexityDistributionDecorator implements Decorator {

  private CountDistributionBuilder builder = new CountDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
  private Language language;

  public AbstractFunctionComplexityDistributionDecorator(Language language) {
    this.language = language;
  }

  @DependedUpon
  public Metric generatesMetrics() {
    return CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return language.equals(project.getLanguage());
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(context)) {
      reset();
      saveDistribution(context);
    }
  }

  private void saveDistribution(DecoratorContext context) {
    for (Measure childMeasure : context.getChildrenMeasures(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION)) {
      builder.add(childMeasure);
    }

    if (!builder.isEmpty()) {
      context.saveMeasure(builder.build());
    }
  }

  private void reset() {
    builder.clear();
  }

  private boolean shouldDecorateResource(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION) == null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
