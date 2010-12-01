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

import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;

import java.util.Date;

@DependedUpon(DecoratorBarriers.END_OF_VIOLATIONS_GENERATION)
public class NewViolationsDecorator implements Decorator {

  private TimeMachineConfiguration timeMachineConfiguration;

  public NewViolationsDecorator(TimeMachineConfiguration timeMachineConfiguration) {
    this.timeMachineConfiguration = timeMachineConfiguration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public Metric generatesMetric() {
    return CoreMetrics.NEW_VIOLATIONS;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Measure measure = new Measure(CoreMetrics.NEW_VIOLATIONS);
    for (int index = 0; index < 3; index++) {
      Integer days = timeMachineConfiguration.getDiffPeriodInDays(index);
      if (days != null) {
        double value = calculate(context, days) + sumChildren(context, index);
        setDiffValue(measure, index, value);
      }
    }
    context.saveMeasure(measure);
  }

  int calculate(DecoratorContext context, int days) {
    Date targetDate = getTargetDate(context.getProject(), days);
    int newViolations = 0;
    for (Violation violation : context.getViolations()) {
      if (!violation.getCreatedAt().before(targetDate)) {
        newViolations++;
      }
    }
    return newViolations;
  }

  double sumChildren(DecoratorContext context, int index) {
    double sum = 0;
    for (Measure measure : context.getChildrenMeasures(CoreMetrics.NEW_VIOLATIONS)) {
      sum = sum + getDiffValue(measure, index);
    }
    return sum;
  }

  private Date getTargetDate(Project project, int distanceInDays) {
    return DateUtils.addDays(project.getAnalysisDate(), -distanceInDays);
  }

  private void setDiffValue(Measure measure, int index, double value) {
    switch (index) {
      case 0:
        measure.setDiffValue1(value);
        break;
      case 1:
        measure.setDiffValue2(value);
        break;
      case 2:
        measure.setDiffValue3(value);
        break;
      default:
        throw new SonarException("Should never happen");
    }
  }

  private double getDiffValue(Measure measure, int index) {
    switch (index) {
      case 0:
        return measure.getDiffValue1();
      case 1:
        return measure.getDiffValue2();
      case 2:
        return measure.getDiffValue3();
      default:
        throw new SonarException("Should never happen");
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
