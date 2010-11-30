package org.sonar.plugins.core.timemachine;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Violation;

import java.util.Arrays;
import java.util.Date;

public class NewViolationsDecoratorTest {

  private NewViolationsDecorator decorator;

  @Before
  public void setUp() {
    decorator = new NewViolationsDecorator(null);
  }

  @Test
  public void test() {
    DecoratorContext context = mock(DecoratorContext.class);
    Date date1 = new Date();
    Date date2 = DateUtils.addDays(date1, -20);
    Project project = new Project("project");
    project.setAnalysisDate(date1);
    Violation violation1 = new Violation(null).setCreatedAt(date1);
    Violation violation2 = new Violation(null).setCreatedAt(date2);
    when(context.getViolations()).thenReturn(Arrays.asList(violation1, violation2));
    when(context.getProject()).thenReturn(project);

    assertThat(decorator.calculate(context, 10), is(1));
    assertThat(decorator.calculate(context, 30), is(2));
  }
}
