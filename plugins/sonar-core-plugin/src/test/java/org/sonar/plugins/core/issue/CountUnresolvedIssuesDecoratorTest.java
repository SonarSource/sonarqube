/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.plugins.core.issue;

import com.google.common.collect.Lists;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.test.IsRuleMeasure;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CountUnresolvedIssuesDecoratorTest {

  CountUnresolvedIssuesDecorator decorator;
  TimeMachineConfiguration timeMachineConfiguration;
  RuleFinder ruleFinder;
  Issuable issuable;
  DecoratorContext context;
  Resource resource;
  Project project;
  Rule ruleA1;
  Rule ruleA2;
  Rule ruleB1;
  Date rightNow;
  Date tenDaysAgo;
  Date fiveDaysAgo;
  Date sameSecond;

  @Before
  public void before() {
    ruleA1 = Rule.create().setRepositoryKey("ruleA1").setKey("ruleA1").setName("nameA1");
    ruleA2 = Rule.create().setRepositoryKey("ruleA2").setKey("ruleA2").setName("nameA2");
    ruleB1 = Rule.create().setRepositoryKey("ruleB1").setKey("ruleB1").setName("nameB1");

    ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(ruleA1.getRepositoryKey(), ruleA1.getKey())).thenReturn(ruleA1);
    when(ruleFinder.findByKey(ruleA2.getRepositoryKey(), ruleA2.getKey())).thenReturn(ruleA2);
    when(ruleFinder.findByKey(ruleB1.getRepositoryKey(), ruleB1.getKey())).thenReturn(ruleB1);

    rightNow = new Date();
    tenDaysAgo = DateUtils.addDays(rightNow, -10);
    fiveDaysAgo = DateUtils.addDays(rightNow, -5);
    sameSecond = DateUtils.truncate(rightNow, Calendar.SECOND);

    PastSnapshot pastSnapshot = mock(PastSnapshot.class);
    when(pastSnapshot.getIndex()).thenReturn(1);
    when(pastSnapshot.getTargetDate()).thenReturn(fiveDaysAgo);

    PastSnapshot pastSnapshot2 = mock(PastSnapshot.class);
    when(pastSnapshot2.getIndex()).thenReturn(2);
    when(pastSnapshot2.getTargetDate()).thenReturn(tenDaysAgo);

    timeMachineConfiguration = mock(TimeMachineConfiguration.class);
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Arrays.asList(pastSnapshot, pastSnapshot2));

    project = mock(Project.class);
    resource = mock(Resource.class);
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(resource);
    when(context.getProject()).thenReturn(project);
    when(context.getMeasure(CoreMetrics.NEW_VIOLATIONS)).thenReturn(null);

    issuable = mock(Issuable.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);
    decorator = new CountUnresolvedIssuesDecorator(perspectives, ruleFinder, timeMachineConfiguration);
  }

  @Test
  public void should_be_depended_upon_metric() {
    assertThat(decorator.generatesIssuesMetrics()).hasSize(15);
  }

  @Test
  public void should_count_issues() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createissues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS, 4.0);
  }

  @Test
  public void should_do_nothing_when_issuable_is_null() {
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(null);
    CountUnresolvedIssuesDecorator decorator = new CountUnresolvedIssuesDecorator(perspectives, ruleFinder, timeMachineConfiguration);

    decorator.decorate(resource, context);

    verifyZeroInteractions(context);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-1729
   */
  @Test
  public void should_not_count_issues_if_measure_already_exists() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createissues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());
    when(context.getMeasure(CoreMetrics.VIOLATIONS)).thenReturn(new Measure(CoreMetrics.VIOLATIONS, 3000.0));
    when(context.getMeasure(CoreMetrics.MAJOR_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.MAJOR_VIOLATIONS, 500.0));

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.VIOLATIONS), anyDouble());// not changed
    verify(context, never()).saveMeasure(eq(CoreMetrics.MAJOR_VIOLATIONS), anyDouble());// not changed
    verify(context, times(1)).saveMeasure(eq(CoreMetrics.CRITICAL_VIOLATIONS), anyDouble());// did not exist
  }

  @Test
  public void should_save_zero_on_projects() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(Lists.<Issue>newArrayList());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS, 0.0);
  }

  @Test
  public void should_save_zero_on_directories() {
    when(resource.getScope()).thenReturn(Scopes.DIRECTORY);
    when(issuable.issues()).thenReturn(Lists.<Issue>newArrayList());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS, 0.0);
  }

  @Test
  public void should_count_issues_by_severity() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createissues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.BLOCKER_VIOLATIONS, 0.0);
    verify(context).saveMeasure(CoreMetrics.CRITICAL_VIOLATIONS, 2.0);
    verify(context).saveMeasure(CoreMetrics.MAJOR_VIOLATIONS, 1.0);
    verify(context).saveMeasure(CoreMetrics.MINOR_VIOLATIONS, 1.0);
    verify(context).saveMeasure(CoreMetrics.INFO_VIOLATIONS, 0.0);
  }

  @Test
  public void should_count_issues_per_rule() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setSeverity(RulePriority.MAJOR.name()));
    when(issuable.issues()).thenReturn(issues);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.CRITICAL_VIOLATIONS, ruleA1, 2.0)));
    verify(context, never()).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MAJOR_VIOLATIONS, ruleA1, 0.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MAJOR_VIOLATIONS, ruleA2, 1.0)));
  }

  @Test
  public void same_rule_should_have_different_severities() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.MINOR.name()));
    when(issuable.issues()).thenReturn(issues);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.CRITICAL_VIOLATIONS, ruleA1, 2.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MINOR_VIOLATIONS, ruleA1, 1.0)));
  }

  @Test
  public void should_count_issues_after_date() {
    List<Issue> issues = createIssuesForNewMetrics();

    assertThat(decorator.countIssuesAfterDate(null, fiveDaysAgo)).isEqualTo(0);
    assertThat(decorator.countIssuesAfterDate(issues, fiveDaysAgo)).isEqualTo(1); // 1 rightNow
    assertThat(decorator.countIssuesAfterDate(issues, tenDaysAgo)).isEqualTo(3); // 1 rightNow + 2 fiveDaysAgo
    assertThat(decorator.countIssuesAfterDate(issues, sameSecond)).isEqualTo(0); // 0
  }

  @Test
  public void should_clear_cache_after_execution() {
    Issue issue1 = new DefaultIssue().setRuleKey(RuleKey.of(ruleA1.getRepositoryKey(), ruleA1.getKey())).setSeverity(RulePriority.CRITICAL.name()).setCreationDate(rightNow);
    Issue issue2 = new DefaultIssue().setRuleKey(RuleKey.of(ruleA2.getRepositoryKey(), ruleA2.getKey())).setSeverity(RulePriority.CRITICAL.name()).setCreationDate(rightNow);
    when(issuable.issues()).thenReturn(newArrayList(issue1)).thenReturn(newArrayList(issue2));

    decorator.decorate(resource, context);
    decorator.decorate(resource, context);

    verify(context, times(2)).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS, 1.0, 1.0)));
    verify(context, never()).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS, 2.0, 2.0)));
  }

  @Test
  public void should_save_severity_new_issues() {
    when(issuable.issues()).thenReturn(createIssuesForNewMetrics());

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_BLOCKER_VIOLATIONS, 0.0, 0.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_MAJOR_VIOLATIONS, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_MINOR_VIOLATIONS, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_INFO_VIOLATIONS, 0.0, 0.0)));
  }

  @Test
  public void should_save_rule_new_issues() {
    when(issuable.issues()).thenReturn(createIssuesForNewMetrics());

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS, ruleA1, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_MAJOR_VIOLATIONS, ruleA2, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_MINOR_VIOLATIONS, ruleB1, 0.0, 1.0)));
  }

  @Test
  public void should_not_save_new_issues_if_measure_already_computed() {
    when(context.getMeasure(CoreMetrics.NEW_VIOLATIONS)).thenReturn(new Measure());
    when(issuable.issues()).thenReturn(createIssuesForNewMetrics());

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_BLOCKER_VIOLATIONS)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_MAJOR_VIOLATIONS)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_MINOR_VIOLATIONS)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_INFO_VIOLATIONS)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS)));
  }

  List<Issue> createissues() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(Severity.CRITICAL).setStatus(Issue.STATUS_OPEN));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(Severity.CRITICAL).setStatus(Issue.STATUS_REOPENED));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setSeverity(Severity.MAJOR).setStatus(Issue.STATUS_REOPENED));
    issues.add(new DefaultIssue().setRuleKey(ruleB1.ruleKey()).setSeverity(Severity.MINOR).setStatus(Issue.STATUS_OPEN));
    return issues;
  }

  List<Issue> createIssuesForNewMetrics() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()).setCreationDate(rightNow).setStatus(Issue.STATUS_OPEN));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()).setCreationDate(tenDaysAgo).setStatus(Issue.STATUS_OPEN));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setSeverity(RulePriority.MAJOR.name()).setCreationDate(fiveDaysAgo).setStatus(Issue.STATUS_REOPENED));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setSeverity(RulePriority.MAJOR.name()).setCreationDate(tenDaysAgo).setStatus(Issue.STATUS_REOPENED));
    issues.add(new DefaultIssue().setRuleKey(ruleB1.ruleKey()).setSeverity(RulePriority.MINOR.name()).setCreationDate(fiveDaysAgo).setStatus(Issue.STATUS_OPEN));
    issues.add(new DefaultIssue().setRuleKey(ruleB1.ruleKey()).setSeverity(RulePriority.MINOR.name()).setCreationDate(tenDaysAgo).setStatus(Issue.STATUS_OPEN));
    return issues;
  }

  class IsVariationRuleMeasure extends ArgumentMatcher<Measure> {
    Metric metric = null;
    Rule rule = null;
    Double var1 = null;
    Double var2 = null;

    public IsVariationRuleMeasure(Metric metric, Rule rule, Double var1, Double var2) {
      this.metric = metric;
      this.rule = rule;
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
        ObjectUtils.equals(var1, m.getVariation1()) &&
        ObjectUtils.equals(var2, m.getVariation2());
    }
  }

  class IsVariationMeasure extends ArgumentMatcher<Measure> {
    Metric metric = null;
    Double var1 = null;
    Double var2 = null;

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
        ObjectUtils.equals(var2, m.getVariation2()) &&
        !(m instanceof RuleMeasure);
    }
  }

  class IsMetricMeasure extends ArgumentMatcher<Measure> {
    Metric metric = null;

    public IsMetricMeasure(Metric metric) {
      this.metric = metric;
    }

    public boolean matches(Object o) {
      if (!(o instanceof Measure)) {
        return false;
      }
      Measure m = (Measure) o;
      return ObjectUtils.equals(metric, m.getMetric());
    }
  }
}
