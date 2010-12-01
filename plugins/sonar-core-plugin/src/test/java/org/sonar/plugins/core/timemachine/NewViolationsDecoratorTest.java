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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Violation;

import java.util.Arrays;
import java.util.Date;

public class NewViolationsDecoratorTest {

  private NewViolationsDecorator decorator;
  private DecoratorContext context;

  @Before
  public void setUp() {
    context = mock(DecoratorContext.class);
    decorator = new NewViolationsDecorator(null);
  }

  @Test
  public void decoratorDefinition() {
    assertThat(decorator.shouldExecuteOnProject(new Project("project")), is(true));
    assertThat(decorator.generatesMetric(), is(CoreMetrics.NEW_VIOLATIONS));
    assertThat(decorator.toString(), is(NewViolationsDecorator.class.getSimpleName()));
  }

  @Test
  public void shouldCalculate() {
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

  @Test
  public void shouldSumChildren() {
    Measure measure1 = new Measure(CoreMetrics.NEW_VIOLATIONS).setDiffValue1(1.0).setDiffValue2(1.0).setDiffValue3(3.0);
    Measure measure2 = new Measure(CoreMetrics.NEW_VIOLATIONS).setDiffValue1(1.0).setDiffValue2(2.0).setDiffValue3(3.0);
    when(context.getChildrenMeasures(CoreMetrics.NEW_VIOLATIONS)).thenReturn(Arrays.asList(measure1, measure2));

    assertThat(decorator.sumChildren(context, 0), is(2.0));
    assertThat(decorator.sumChildren(context, 1), is(3.0));
    assertThat(decorator.sumChildren(context, 2), is(6.0));
  }
}
