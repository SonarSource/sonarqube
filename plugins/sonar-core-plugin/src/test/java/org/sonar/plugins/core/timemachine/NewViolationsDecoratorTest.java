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
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Violation;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NewViolationsDecoratorTest {

  private NewViolationsDecorator decorator;
  private DecoratorContext context;

  @Before
  public void setUp() {
    context = mock(DecoratorContext.class);
    decorator = new NewViolationsDecorator(null);
  }

  @Test
  public void shouldExecuteIfLastAnalysis() {
    Project project = mock(Project.class);

    when(project.isLatestAnalysis()).thenReturn(false);
    assertThat(decorator.shouldExecuteOnProject(project), is(false));

    when(project.isLatestAnalysis()).thenReturn(true);
    assertThat(decorator.shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void shouldBeDependedUponMetric() {
    assertThat(decorator.generatesMetric(), is(CoreMetrics.NEW_VIOLATIONS));
  }

  @Test
  public void shouldCountViolationsAfterDate() {
    Date date1 = new Date();
    Date date2 = DateUtils.addDays(date1, -20);
    Project project = new Project("project");
    project.setAnalysisDate(date1);
    Violation violation1 = new Violation(null).setCreatedAt(date1);
    Violation violation2 = new Violation(null).setCreatedAt(date2);
    List<Violation> violations = Arrays.asList(violation1, violation2);

    assertThat(decorator.countViolationsAfterDate(violations, DateUtils.addDays(date1, -10)), is(1));
    assertThat(decorator.countViolationsAfterDate(violations, DateUtils.addDays(date1, -30)), is(2));
  }

  @Test
  public void shouldSumChildren() {
    Measure measure1 = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(1.0).setVariation2(1.0).setVariation3(3.0);
    Measure measure2 = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(1.0).setVariation2(2.0).setVariation3(3.0);
    when(context.getChildrenMeasures(CoreMetrics.NEW_VIOLATIONS)).thenReturn(Arrays.asList(measure1, measure2));

    assertThat(decorator.sumChildren(context, 1), is(2));
    assertThat(decorator.sumChildren(context, 2), is(3));
    assertThat(decorator.sumChildren(context, 3), is(6));
  }
}
