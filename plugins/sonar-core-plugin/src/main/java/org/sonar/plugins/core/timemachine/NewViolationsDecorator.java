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
package org.sonar.plugins.core.timemachine;

import org.sonar.api.batch.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

import java.util.Date;
import java.util.List;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATIONS_GENERATION)
public class NewViolationsDecorator implements Decorator {

  private TimeMachineConfiguration timeMachineConfiguration;

  public NewViolationsDecorator(TimeMachineConfiguration timeMachineConfiguration) {
    this.timeMachineConfiguration = timeMachineConfiguration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  @DependedUpon
  public Metric generatesMetric() {
    return CoreMetrics.NEW_VIOLATIONS;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Measure measure = new Measure(CoreMetrics.NEW_VIOLATIONS);
    for (VariationTarget variationTarget : timeMachineConfiguration.getVariationTargets()) {
      Date date = variationTarget.getDate();
      double value = countViolationsAfterDate(context.getViolations(), date) + sumChildren(context, variationTarget.getIndex());
      measure.setVariation(variationTarget.getIndex(), value);
    }
    context.saveMeasure(measure);
  }

  int countViolationsAfterDate(List<Violation> violations, Date targetDate) {
    int newViolations = 0;
    for (Violation violation : violations) {
      if (!violation.getCreatedAt().before(targetDate)) {
        newViolations++;
      }
    }
    return newViolations;
  }

  int sumChildren(DecoratorContext context, int index) {
    int sum = 0;
    for (Measure measure : context.getChildrenMeasures(CoreMetrics.NEW_VIOLATIONS)) {
      Double var = measure.getVariation(index);
      if (var != null) {
        sum = sum + var.intValue();
      }
    }
    return sum;
  }


  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
