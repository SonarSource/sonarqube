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

import com.google.common.collect.Lists;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class NewViolationsDecoratorTest {
  private Rule rule1;
  private Rule rule2;
  private Rule rule3;

  private NewViolationsDecorator decorator;
  private DecoratorContext context;
  private Resource resource;

  private Date rightNow;
  private Date tenDaysAgo;
  private Date fiveDaysAgo;

  @Before
  public void setUp() {
    rightNow = new Date();
    tenDaysAgo = DateUtils.addDays(rightNow, -10);
    fiveDaysAgo = DateUtils.addDays(rightNow, -5);

    PastSnapshot pastSnapshot = mock(PastSnapshot.class);
    when(pastSnapshot.getIndex()).thenReturn(1);
    when(pastSnapshot.getTargetDate()).thenReturn(fiveDaysAgo);

    PastSnapshot pastSnapshot2 = mock(PastSnapshot.class);
    when(pastSnapshot2.getIndex()).thenReturn(2);
    when(pastSnapshot2.getTargetDate()).thenReturn(tenDaysAgo);

    TimeMachineConfiguration timeMachineConfiguration = mock(TimeMachineConfiguration.class);
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Arrays.asList(pastSnapshot, pastSnapshot2));

    context = mock(DecoratorContext.class);
    resource = mock(Resource.class);
    when(context.getResource()).thenReturn(resource);

    decorator = new NewViolationsDecorator(timeMachineConfiguration);

    rule1 = Rule.create().setRepositoryKey("rule1").setKey("rule1").setName("name1");
    rule2 = Rule.create().setRepositoryKey("rule2").setKey("rule2").setName("name2");
    rule3 = Rule.create().setRepositoryKey("rule3").setKey("rule3").setName("name3");
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
    assertThat(decorator.generatesMetric().size(), is(6));
  }

  @Test
  public void shouldCountViolationsAfterDate() {
    List<Violation> violations = createViolations();

    assertThat(decorator.countViolations(null, fiveDaysAgo), is(0));
    assertThat(decorator.countViolations(violations, fiveDaysAgo), is(1)); // 1 rightNow
    assertThat(decorator.countViolations(violations, tenDaysAgo), is(3)); // 1 rightNow + 2 fiveDaysAgo
  }

  @Test
  public void shouldSumChildren() {
    Measure measure1 = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(1.0).setVariation2(1.0).setVariation3(3.0);
    Measure measure2 = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(1.0).setVariation2(2.0).setVariation3(3.0);
    List<Measure> children = Arrays.asList(measure1, measure2);

    assertThat(decorator.sumChildren(1, children), is(2));
    assertThat(decorator.sumChildren(2, children), is(3));
    assertThat(decorator.sumChildren(3, children), is(6));
  }

  @Test
  public void shouldClearCacheAfterExecution() {
    Violation violation1 = Violation.create(rule1, resource).setSeverity(RulePriority.CRITICAL).setCreatedAt(rightNow);
    Violation violation2 = Violation.create(rule2, resource).setSeverity(RulePriority.CRITICAL).setCreatedAt(rightNow);
    when(context.getViolations()).thenReturn(Arrays.asList(violation1)).thenReturn(Arrays.asList(violation2));

    decorator.decorate(resource, context);
    decorator.decorate(resource, context);

    verify(context, times(2)).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS, 1.0, 1.0)));
    verify(context, never()).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS, 2.0, 2.0)));
  }

  @Test
  public void priorityViolations() {
    when(context.getViolations()).thenReturn(createViolations());

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_BLOCKER_VIOLATIONS, 0.0, 0.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_MAJOR_VIOLATIONS, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_MINOR_VIOLATIONS, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_INFO_VIOLATIONS, 0.0, 0.0)));
  }

  @Test
  public void ruleViolations() {
    when(context.getViolations()).thenReturn(createViolations());

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_VIOLATIONS, rule1, RulePriority.CRITICAL, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_VIOLATIONS, rule2, RulePriority.MAJOR, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_VIOLATIONS, rule3, RulePriority.MINOR, 0.0, 1.0)));
  }

  private List<Violation> createViolations() {
    List<Violation> violations = Lists.newLinkedList();
    violations.add(Violation.create(rule1, resource).setSeverity(RulePriority.CRITICAL).setCreatedAt(rightNow));
    violations.add(Violation.create(rule1, resource).setSeverity(RulePriority.CRITICAL).setCreatedAt(tenDaysAgo));
    violations.add(Violation.create(rule2, resource).setSeverity(RulePriority.MAJOR).setCreatedAt(fiveDaysAgo));
    violations.add(Violation.create(rule2, resource).setSeverity(RulePriority.MAJOR).setCreatedAt(tenDaysAgo));
    violations.add(Violation.create(rule3, resource).setSeverity(RulePriority.MINOR).setCreatedAt(fiveDaysAgo));
    violations.add(Violation.create(rule3, resource).setSeverity(RulePriority.MINOR).setCreatedAt(tenDaysAgo));
    return violations;
  }

  private class IsVariationRuleMeasure extends BaseMatcher<Measure> {
    private Metric metric = null;
    private Rule rule = null;
    private RulePriority priority = null;
    private Double var1 = null;
    private Double var2 = null;

    public IsVariationRuleMeasure(Metric metric, Rule rule, RulePriority priority, Double var1, Double var2) {
      this.metric = metric;
      this.rule = rule;
      this.priority = priority;
      this.var1 = var1;
      this.var2 = var2;
    }

    public boolean matches(Object o) {
      if (!(o instanceof RuleMeasure)) {
        return false;
      }
      RuleMeasure m = (RuleMeasure) o;
      return ObjectUtils.equals(metric, m.getMetric()) &&
          ObjectUtils.equals(rule, m.getRule()) &&
          ObjectUtils.equals(priority, m.getRulePriority()) &&
          ObjectUtils.equals(var1, m.getVariation1()) &&
          ObjectUtils.equals(var2, m.getVariation2());
    }

    public void describeTo(Description arg0) {
    }
  }

  private class IsVariationMeasure extends BaseMatcher<Measure> {
    private Metric metric = null;
    private Double var1 = null;
    private Double var2 = null;

    public IsVariationMeasure(Metric metric, Double var1, Double var2) {
      this.metric = metric;
      this.var1 = var1;
      this.var2 = var2;
    }

    public boolean matches(Object o) {
      if (!(o instanceof Measure)) {
        return false;
      }
      Measure m = (Measure) o;
      return ObjectUtils.equals(metric, m.getMetric()) &&
          ObjectUtils.equals(var1, m.getVariation1()) &&
          ObjectUtils.equals(var2, m.getVariation2());
    }

    public void describeTo(Description o) {
    }
  }
}
