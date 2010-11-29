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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RulesCategory;
import org.sonar.api.rules.Violation;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsRuleMeasure;

public class ViolationsDecoratorTest {
  private RulesCategory categA;
  private RulesCategory categB;
  private Rule ruleA1;
  private Rule ruleA2;
  private Rule ruleB1;
  private ViolationsDecorator decorator;
  private Resource resource;
  private DecoratorContext context;

  @Before
  public void before() {
    categA = new RulesCategory("Maintainability");
    categA.setId(1);

    categB = new RulesCategory("Usability");
    categB.setId(2);

    ruleA1 = Rule.create().setPluginName("ruleA1").setKey("ruleA1").setName("nameA1").setRulesCategory(categA);
    ruleA2 = Rule.create().setPluginName("ruleA2").setKey("ruleA2").setName("nameA2").setRulesCategory(categA);
    ruleB1 = Rule.create().setPluginName("ruleB1").setKey("ruleB1").setName("nameB1").setRulesCategory(categB);

    decorator = new ViolationsDecorator();
    resource = mock(Resource.class);
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(resource);
  }

  @Test
  public void countViolations() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure> emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 4.0)));
  }

  @Test
  public void resetCountersAfterExecution() {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure> emptyList());

    decorator.decorate(resource, context);
    decorator.decorate(resource, context);

    // we must not have 8 violations !
    verify(context, times(2)).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 4.0)));
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 8.0)));
  }

  @Test
  public void saveZeroOnProjects() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(Collections.<Violation> emptyList());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure> emptyList());

    decorator.decorate(resource, context);

    verify(context, atLeast(1)).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 0.0)));
  }

  @Test
  public void saveZeroOnDirectories() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SPACE);
    when(context.getViolations()).thenReturn(Collections.<Violation> emptyList());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure> emptyList());

    decorator.decorate(resource, context);

    verify(context, atLeast(1)).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 0.0)));
  }

  @Test
  public void priorityViolations() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure> emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, null, RulePriority.BLOCKER, 0.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, null, RulePriority.CRITICAL, 2.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, null, RulePriority.MAJOR, 1.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, null, RulePriority.MINOR, 1.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, null, RulePriority.INFO, 0.0)));

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.BLOCKER_VIOLATIONS, 0.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.CRITICAL_VIOLATIONS, 2.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.MAJOR_VIOLATIONS, 1.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.MINOR_VIOLATIONS, 1.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.INFO_VIOLATIONS, 0.0)));
  }

  @Test
  public void categoryViolations() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure> emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, categA.getId(), null, 3.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, categB.getId(), null, 1.0)));
  }

  private List<Violation> createViolations() {
    List<Violation> violations = new ArrayList<Violation>();
    violations.add(Violation.create(ruleA1, resource).setPriority(RulePriority.CRITICAL));
    violations.add(Violation.create(ruleA1, resource).setPriority(RulePriority.CRITICAL));
    violations.add(Violation.create(ruleA2, resource).setPriority(RulePriority.MAJOR));
    violations.add(Violation.create(ruleB1, resource).setPriority(RulePriority.MINOR));
    return violations;
  }
}
