package org.sonar.plugins.core.timemachine;

import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

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

  public void decorate(Resource resource, DecoratorContext context) {
    Measure measure = new Measure(CoreMetrics.NEW_VIOLATIONS);
    for (int index = 0; index < 3; index++) {
      int days = timeMachineConfiguration.getDiffPeriodInDays(index);
      setDiffValue(measure, index, calculate(context, days));
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
        measure.setDiffValue2(value);
        break;
      default:
        break;
    }
  }

  @Override
  public String toString() {
    return getClass().toString();
  }
}
