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
package org.sonar.plugins.core.sensors;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsRuleMeasure;

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class ViolationsDecoratorTest {
  private Rule ruleA1;
  private Rule ruleA2;
  private Rule ruleB1;
  private ViolationsDecorator decorator;
  private Resource resource;
  private DecoratorContext context;

  @Before
  public void before() {
    ruleA1 = Rule.create().setRepositoryKey("ruleA1").setKey("ruleA1").setName("nameA1");
    ruleA2 = Rule.create().setRepositoryKey("ruleA2").setKey("ruleA2").setName("nameA2");
    ruleB1 = Rule.create().setRepositoryKey("ruleB1").setKey("ruleB1").setName("nameB1");

    decorator = new ViolationsDecorator();
    resource = mock(Resource.class);
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(resource);
  }

  @Test
  public void countViolations() {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 4.0)));
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-1729
   */
  @Test
  public void shouldNotCountViolationsIfMeasureAlreadyExists() {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());
    when(context.getMeasure(CoreMetrics.VIOLATIONS)).thenReturn(new Measure(CoreMetrics.VIOLATIONS, 3000.0));
    when(context.getMeasure(CoreMetrics.MAJOR_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.MAJOR_VIOLATIONS, 500.0));

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 4.0)));// not changed
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.MAJOR_VIOLATIONS)));// not changed
    verify(context, times(1)).saveMeasure(argThat(new IsMeasure(CoreMetrics.CRITICAL_VIOLATIONS)));// did not exist
  }

  @Test
  public void resetCountersAfterExecution() {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);
    decorator.decorate(resource, context);

    // we must not have 8 violations !
    verify(context, times(2)).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 4.0)));
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 8.0)));
  }

  @Test
  public void saveZeroOnProjects() {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(Collections.<Violation>emptyList());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context, atLeast(1)).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 0.0)));
  }

  @Test
  public void saveZeroOnDirectories() {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SPACE);
    when(context.getViolations()).thenReturn(Collections.<Violation>emptyList());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context, atLeast(1)).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 0.0)));
  }

  @Test
  public void shouldCountViolationsBySeverity() {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.BLOCKER_VIOLATIONS, 0.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.CRITICAL_VIOLATIONS, 2.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.MAJOR_VIOLATIONS, 1.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.MINOR_VIOLATIONS, 1.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.INFO_VIOLATIONS, 0.0)));
  }

  @Test
  public void ruleViolations() {
    when(context.getViolations()).thenReturn(createViolations());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, ruleA1, RulePriority.CRITICAL, 2.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, ruleA2, RulePriority.MAJOR, 1.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, ruleB1, RulePriority.MINOR, 1.0)));
  }

  private List<Violation> createViolations() {
    List<Violation> violations = Lists.newArrayList();
    violations.add(Violation.create(ruleA1, resource).setSeverity(RulePriority.CRITICAL));
    violations.add(Violation.create(ruleA1, resource).setSeverity(RulePriority.CRITICAL));
    violations.add(Violation.create(ruleA2, resource).setSeverity(RulePriority.MAJOR));
    violations.add(Violation.create(ruleB1, resource).setSeverity(RulePriority.MINOR));
    return violations;
  }
}
