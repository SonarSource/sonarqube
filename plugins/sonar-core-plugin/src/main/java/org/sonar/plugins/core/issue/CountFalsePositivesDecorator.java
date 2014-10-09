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
package org.sonar.plugins.core.issue;

import org.sonar.api.batch.*;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

/**
 * Computes the number of false-positives
 *
 * @since 3.6
 */
@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class CountFalsePositivesDecorator implements Decorator {

  private final ResourcePerspectives perspectives;

  public CountFalsePositivesDecorator(ResourcePerspectives perspectives) {
    this.perspectives = perspectives;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public Metric generatesFalsePositiveMeasure() {
    return CoreMetrics.FALSE_POSITIVE_ISSUES;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null) {
      int falsePositives = 0;
      for (Issue issue : issuable.resolvedIssues()) {
        if (Issue.RESOLUTION_FALSE_POSITIVE.equals(issue.resolution())) {
          falsePositives++;
        }
      }
      saveMeasure(context, CoreMetrics.FALSE_POSITIVE_ISSUES, falsePositives);
    }
  }

  private void saveMeasure(DecoratorContext context, Metric metric, int value) {
    context.saveMeasure(metric, (double) (value + sumChildren(context, metric)));
  }

  private int sumChildren(DecoratorContext context, Metric metric) {
    return MeasureUtils.sum(true, context.getChildrenMeasures(metric)).intValue();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
