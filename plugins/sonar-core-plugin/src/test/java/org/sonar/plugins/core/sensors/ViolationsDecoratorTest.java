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

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    ruleA1 = new Rule("ruleA1", "ruleA1", "nameA1", categA, null);
    ruleA1.setId(1);

    ruleA2 = new Rule("ruleA2", "ruleA2", "nameA2", categA, null);
    ruleA2.setId(2);

    ruleB1 = new Rule("ruleB1", "ruleB1", "nameB1", categB, null);
    ruleB1.setId(3);

    decorator = new ViolationsDecorator();
    resource = mock(Resource.class);
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(resource);
  }

  @Test
  public void countViolations() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 4.0)));
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
  public void saveZeroOnProjects() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(Collections.<Violation>emptyList());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context, atLeast(1)).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 0.0)));
  }

  @Test
  public void saveZeroOnDirectories() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SPACE);
    when(context.getViolations()).thenReturn(Collections.<Violation>emptyList());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context, atLeast(1)).saveMeasure(argThat(new IsMeasure(CoreMetrics.VIOLATIONS, 0.0)));
  }

  @Test
  public void priorityViolations() throws Exception {
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);
    when(context.getViolations()).thenReturn(createViolations());
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

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
    when(context.getChildrenMeasures((MeasuresFilter) anyObject())).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, categA.getId(), null, 3.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.VIOLATIONS, null, categB.getId(), null, 1.0)));
  }


  private List<Violation> createViolations() {
    List<Violation> violations = new ArrayList<Violation>();
    violations.add(new Violation(ruleA1).setPriority(RulePriority.CRITICAL));
    violations.add(new Violation(ruleA1).setPriority(RulePriority.CRITICAL));
    violations.add(new Violation(ruleA2).setPriority(RulePriority.MAJOR));
    violations.add(new Violation(ruleB1).setPriority(RulePriority.MINOR));
    return violations;
  }
}
