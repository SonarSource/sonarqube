/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

package org.sonar.batch.issue;

import com.google.common.collect.Lists;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.test.IsRuleMeasure;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.core.issue.DefaultIssue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class IssuesDecoratorTest {

  private IssuesDecorator decorator;
  private TimeMachineConfiguration timeMachineConfiguration;
  private RuleFinder rulefinder;
  private Issuable issuable;
  private DecoratorContext context;
  private Resource resource;
  private Project project;
  private Rule ruleA1;
  private Rule ruleA2;
  private Rule ruleB1;
  private Date rightNow;
  private Date tenDaysAgo;
  private Date fiveDaysAgo;

  @Before
  public void before() {
    ruleA1 = Rule.create().setRepositoryKey("ruleA1").setKey("ruleA1").setName("nameA1");
    ruleA2 = Rule.create().setRepositoryKey("ruleA2").setKey("ruleA2").setName("nameA2");
    ruleB1 = Rule.create().setRepositoryKey("ruleB1").setKey("ruleB1").setName("nameB1");

    rulefinder = mock(RuleFinder.class);
    when(rulefinder.findByKey(ruleA1.getRepositoryKey(), ruleA1.getKey())).thenReturn(ruleA1);
    when(rulefinder.findByKey(ruleA2.getRepositoryKey(), ruleA2.getKey())).thenReturn(ruleA2);
    when(rulefinder.findByKey(ruleB1.getRepositoryKey(), ruleB1.getKey())).thenReturn(ruleB1);

    rightNow = new Date();
    tenDaysAgo = DateUtils.addDays(rightNow, -10);
    fiveDaysAgo = DateUtils.addDays(rightNow, -5);

    PastSnapshot pastSnapshot = mock(PastSnapshot.class);
    when(pastSnapshot.getIndex()).thenReturn(1);
    when(pastSnapshot.getTargetDate()).thenReturn(fiveDaysAgo);

    PastSnapshot pastSnapshot2 = mock(PastSnapshot.class);
    when(pastSnapshot2.getIndex()).thenReturn(2);
    when(pastSnapshot2.getTargetDate()).thenReturn(tenDaysAgo);

    timeMachineConfiguration = mock(TimeMachineConfiguration.class);
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Arrays.asList(pastSnapshot, pastSnapshot2));

    project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(true);

    resource = mock(Resource.class);
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(resource);
    when(context.getProject()).thenReturn(project);
    when(context.getMeasure(CoreMetrics.NEW_ISSUES)).thenReturn(null);

    issuable = mock(Issuable.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);
    decorator = new IssuesDecorator(perspectives, rulefinder, timeMachineConfiguration);
  }

  @Test
  public void should_be_depended_upon_metric() {
    assertThat(decorator.generatesIssuesMetrics()).hasSize(14);
  }

  @Test
  public void should_count_issues() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createIssues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.ISSUES, 4.0);
  }

  @Test
  public void should_do_nothing_when_issuable_is_null() {
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(null);
    IssuesDecorator decorator = new IssuesDecorator(perspectives, rulefinder, timeMachineConfiguration);

    decorator.decorate(resource, context);

    verifyZeroInteractions(context);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-1729
   */
  @Test
  public void should_not_count_issues_if_measure_already_exists() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createIssues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());
    when(context.getMeasure(CoreMetrics.ISSUES)).thenReturn(new Measure(CoreMetrics.ISSUES, 3000.0));
    when(context.getMeasure(CoreMetrics.MAJOR_ISSUES)).thenReturn(new Measure(CoreMetrics.MAJOR_ISSUES, 500.0));

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.ISSUES), anyDouble());// not changed
    verify(context, never()).saveMeasure(eq(CoreMetrics.MAJOR_ISSUES), anyDouble());// not changed
    verify(context, times(1)).saveMeasure(eq(CoreMetrics.CRITICAL_ISSUES), anyDouble());// did not exist
  }

  @Test
  public void should_save_zero_on_projects() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(Lists.<Issue>newArrayList());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.ISSUES, 0.0);
  }

  @Test
  public void should_save_zero_on_directories() {
    when(resource.getScope()).thenReturn(Scopes.DIRECTORY);
    when(issuable.issues()).thenReturn(Lists.<Issue>newArrayList());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.ISSUES, 0.0);
  }

  @Test
  public void should_count_issues_by_severity() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createIssues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.BLOCKER_ISSUES, 0.0);
    verify(context).saveMeasure(CoreMetrics.CRITICAL_ISSUES, 2.0);
    verify(context).saveMeasure(CoreMetrics.MAJOR_ISSUES, 1.0);
    verify(context).saveMeasure(CoreMetrics.MINOR_ISSUES, 1.0);
    verify(context).saveMeasure(CoreMetrics.INFO_ISSUES, 0.0);
  }

  @Test
  public void should_count_issues_per_rule() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setSeverity(RulePriority.MAJOR.name()));
    when(issuable.issues()).thenReturn(issues);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.CRITICAL_ISSUES, ruleA1, 2.0)));
    verify(context, never()).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MAJOR_ISSUES, ruleA1, 0.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MAJOR_ISSUES, ruleA2, 1.0)));
  }

  @Test
  public void should_save_unassigned_issues() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setStatus(Issue.STATUS_OPEN).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setStatus(Issue.STATUS_REOPENED).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setStatus(Issue.STATUS_OPEN).setAssigneeLogin("arthur").setSeverity(RulePriority.CRITICAL.name()));
    when(issuable.issues()).thenReturn(issues);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.UNASSIGNED_ISSUES, 2.0);
  }

  @Test
  public void should_save_false_positive_issues() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setResolution(Issue.RESOLUTION_FALSE_POSITIVE).setStatus(Issue.STATUS_OPEN).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setResolution(Issue.RESOLUTION_FIXED).setStatus(Issue.STATUS_OPEN).setSeverity(RulePriority.CRITICAL.name()));
    when(issuable.issues()).thenReturn(issues);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.FALSE_POSITIVE_ISSUES, 1.0);
  }

  @Test
  public void same_rule_should_have_different_severities() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.MINOR.name()));
    when(issuable.issues()).thenReturn(issues);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.CRITICAL_ISSUES, ruleA1, 2.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MINOR_ISSUES, ruleA1, 1.0)));
  }

  @Test
  public void should_count_issues_after_date() {
    List<Issue> issues = createIssuesForNewMetrics();

    assertThat(decorator.countIssuesAfterDate(null, fiveDaysAgo)).isEqualTo(0);
    assertThat(decorator.countIssuesAfterDate(issues, fiveDaysAgo)).isEqualTo(1); // 1 rightNow
    assertThat(decorator.countIssuesAfterDate(issues, tenDaysAgo)).isEqualTo(3); // 1 rightNow + 2 fiveDaysAgo
  }

  @Test
  public void should_clear_cache_after_execution() {
    Issue issue1 = new DefaultIssue().setRuleKey(RuleKey.of(ruleA1.getRepositoryKey(), ruleA1.getKey())).setSeverity(RulePriority.CRITICAL.name()).setCreatedAt(rightNow);
    Issue issue2 = new DefaultIssue().setRuleKey(RuleKey.of(ruleA2.getRepositoryKey(), ruleA2.getKey())).setSeverity(RulePriority.CRITICAL.name()).setCreatedAt(rightNow);
    when(issuable.issues()).thenReturn(newArrayList(issue1)).thenReturn(newArrayList(issue2));

    decorator.decorate(resource, context);
    decorator.decorate(resource, context);

    verify(context, times(2)).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_ISSUES, 1.0, 1.0)));
    verify(context, never()).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_ISSUES, 2.0, 2.0)));
  }

  @Test
  public void should_save_severity_new_issues() {
    when(issuable.issues()).thenReturn(createIssuesForNewMetrics());

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_BLOCKER_ISSUES, 0.0, 0.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_ISSUES, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_MAJOR_ISSUES, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_MINOR_ISSUES, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_INFO_ISSUES, 0.0, 0.0)));
  }

  @Test
  public void should_save_rule_new_issues() {
    when(issuable.issues()).thenReturn(createIssuesForNewMetrics());

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_CRITICAL_ISSUES, ruleA1, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_MAJOR_ISSUES, ruleA2, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_MINOR_ISSUES, ruleB1, 0.0, 1.0)));
  }

  @Test
  public void should_not_save_new_issues_if_not_last_analysis() {
    when(project.isLatestAnalysis()).thenReturn(false);
    when(issuable.issues()).thenReturn(createIssuesForNewMetrics());

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_BLOCKER_ISSUES)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_CRITICAL_ISSUES)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_MAJOR_ISSUES)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_MINOR_ISSUES)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_INFO_ISSUES)));
    verify(context, never()).saveMeasure(argThat(new IsMetricMeasure(CoreMetrics.NEW_CRITICAL_ISSUES)));
  }

  private List<Issue> createIssues() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()).setStatus(Issue.STATUS_OPEN));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()).setStatus(Issue.STATUS_REOPENED));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setSeverity(RulePriority.MAJOR.name()).setStatus(Issue.STATUS_REOPENED));
    issues.add(new DefaultIssue().setRuleKey(ruleB1.ruleKey()).setSeverity(RulePriority.MINOR.name()).setStatus(Issue.STATUS_OPEN));
    return issues;
  }

  private List<Issue> createIssuesForNewMetrics() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()).setCreatedAt(rightNow).setStatus(Issue.STATUS_OPEN));
    issues.add(new DefaultIssue().setRuleKey(ruleA1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()).setCreatedAt(tenDaysAgo).setStatus(Issue.STATUS_OPEN));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setSeverity(RulePriority.MAJOR.name()).setCreatedAt(fiveDaysAgo).setStatus(Issue.STATUS_REOPENED));
    issues.add(new DefaultIssue().setRuleKey(ruleA2.ruleKey()).setSeverity(RulePriority.MAJOR.name()).setCreatedAt(tenDaysAgo).setStatus(Issue.STATUS_REOPENED));
    issues.add(new DefaultIssue().setRuleKey(ruleB1.ruleKey()).setSeverity(RulePriority.MINOR.name()).setCreatedAt(fiveDaysAgo).setStatus(Issue.STATUS_OPEN));
    issues.add(new DefaultIssue().setRuleKey(ruleB1.ruleKey()).setSeverity(RulePriority.MINOR.name()).setCreatedAt(tenDaysAgo).setStatus(Issue.STATUS_OPEN));
    return issues;
  }

  private class IsVariationRuleMeasure extends BaseMatcher<Measure> {
    private Metric metric = null;
    private Rule rule = null;
    private Double var1 = null;
    private Double var2 = null;

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
        ObjectUtils.equals(var2, m.getVariation2()) &&
        !(m instanceof RuleMeasure);
    }

    public void describeTo(Description o) {
    }
  }

  private class IsMetricMeasure extends BaseMatcher<Measure> {
    private Metric metric = null;

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

    public void describeTo(Description o) {
    }
  }
}
