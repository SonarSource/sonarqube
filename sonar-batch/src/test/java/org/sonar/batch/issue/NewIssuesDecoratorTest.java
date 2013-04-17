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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.core.issue.DefaultIssue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class NewIssuesDecoratorTest {
  private Rule rule1;
  private Rule rule2;
  private Rule rule3;
  private NewIssuesDecorator decorator;
  private Issuable issuable;
  private RuleFinder rulefinder;
  private DecoratorContext context;
  private Resource<?> resource;
  private NotificationManager notificationManager;
  private Date rightNow;
  private Date tenDaysAgo;
  private Date fiveDaysAgo;
  private TimeMachineConfiguration timeMachineConfiguration;

  @Before
  public void before() {
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

    context = mock(DecoratorContext.class);
    resource = new File("com/foo/bar");
    when(context.getResource()).thenReturn(resource);

    notificationManager = mock(NotificationManager.class);

    rule1 = Rule.create().setRepositoryKey("rule1").setKey("rule1").setName("name1");
    rule2 = Rule.create().setRepositoryKey("rule2").setKey("rule2").setName("name2");
    rule3 = Rule.create().setRepositoryKey("rule3").setKey("rule3").setName("name3");

    rulefinder = mock(RuleFinder.class);
    when(rulefinder.findByKey(rule1.getRepositoryKey(), rule1.getKey())).thenReturn(rule1);
    when(rulefinder.findByKey(rule2.getRepositoryKey(), rule2.getKey())).thenReturn(rule2);
    when(rulefinder.findByKey(rule3.getRepositoryKey(), rule3.getKey())).thenReturn(rule3);

    issuable = mock(Issuable.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);
    decorator = new NewIssuesDecorator(timeMachineConfiguration, notificationManager, perspectives, rulefinder);
  }

  @Test
  public void should_execute_if_last_analysis() {
    Project project = mock(Project.class);

    when(project.isLatestAnalysis()).thenReturn(false);
    assertThat(decorator.shouldExecuteOnProject(project)).isFalse();

    when(project.isLatestAnalysis()).thenReturn(true);
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_be_depended_upon_metric() {
    assertThat(decorator.generatesMetric()).hasSize(6);
  }

  @Test
  public void should_count_issues_after_date() {
    List<Issue> issues = createIssues();

    assertThat(decorator.countIssues(null, fiveDaysAgo)).isEqualTo(0);
    assertThat(decorator.countIssues(issues, fiveDaysAgo)).isEqualTo(1); // 1 rightNow
    assertThat(decorator.countIssues(issues, tenDaysAgo)).isEqualTo(3); // 1 rightNow + 2 fiveDaysAgo
  }

  @Test
  public void should_clear_cache_after_execution() {
    Issue issue1 = new DefaultIssue().setRuleKey(RuleKey.of(rule1.getRepositoryKey(), rule1.getKey())).setSeverity(RulePriority.CRITICAL.name()).setCreatedAt(rightNow);
    Issue issue2 = new DefaultIssue().setRuleKey(RuleKey.of(rule2.getRepositoryKey(), rule2.getKey())).setSeverity(RulePriority.CRITICAL.name()).setCreatedAt(rightNow);
    when(issuable.issues()).thenReturn(newArrayList(issue1)).thenReturn(newArrayList(issue2));

    decorator.decorate(resource, context);
    decorator.decorate(resource, context);

    verify(context, times(2)).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_ISSUES, 1.0, 1.0)));
    verify(context, never()).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_ISSUES, 2.0, 2.0)));
  }

  @Test
  public void severity_issues() {
    when(issuable.issues()).thenReturn(createIssues());

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_BLOCKER_ISSUES, 0.0, 0.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_CRITICAL_ISSUES, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_MAJOR_ISSUES, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_MINOR_ISSUES, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_INFO_ISSUES, 0.0, 0.0)));
  }

  @Test
  public void rule_issues() {
    when(issuable.issues()).thenReturn(createIssues());

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_CRITICAL_ISSUES, rule1, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_MAJOR_ISSUES, rule2, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_MINOR_ISSUES, rule3, 0.0, 1.0)));
  }

  @Test
  public void should_not_notify_if_not_lastest_analysis() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(false);
    assertThat(decorator.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_notify_if_not_root_project() throws Exception {
    Project project = mock(Project.class);
    when(project.getQualifier()).thenReturn(Qualifiers.MODULE);

    decorator.decorate(project, context);

    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void should_not_notify_if_no_not_enough_past_snapshots() throws Exception {
    Project project = new Project("key");
    // the #setUp method adds 2 snapshots: if last period analysis is 3, then it's not enough
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(new ArrayList<PastSnapshot>());

    decorator.notifyNewIssues(project, context);
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void should_not_notify_if_no_new_issues() throws Exception {
    Project project = new Project("key");
    Measure m = new Measure(CoreMetrics.NEW_ISSUES);
    when(context.getMeasure(CoreMetrics.NEW_ISSUES)).thenReturn(m);

    // NULL is returned here
    decorator.notifyNewIssues(project, context);
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));

    // 0 will be returned now
    m.setVariation1(0.0);
    decorator.notifyNewIssues(project, context);
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void should_not_notify_user_if_first_analysis() throws Exception {
    Project project = new Project("key").setName("LongName");
    project.setId(45);
    // PastSnapshot with targetDate==null means first analysis
    PastSnapshot pastSnapshot = new PastSnapshot("", null);
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Lists.newArrayList(pastSnapshot));
    Measure m = new Measure(CoreMetrics.NEW_ISSUES).setVariation1(0.0);
    when(context.getMeasure(CoreMetrics.NEW_ISSUES)).thenReturn(m);

    decorator.decorate(project, context);
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void should_notify_user_about_new_issues() throws Exception {
    Project project = new Project("key").setName("LongName");
    project.setId(45);
    Calendar pastDate = new GregorianCalendar(2011, 10, 25);
    PastSnapshot pastSnapshot = new PastSnapshot("", pastDate.getTime());
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Lists.newArrayList(pastSnapshot, pastSnapshot));
    Measure m = new Measure(CoreMetrics.NEW_ISSUES).setVariation1(32.0);
    when(context.getMeasure(CoreMetrics.NEW_ISSUES)).thenReturn(m);

    decorator.decorate(project, context);

    DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
    Notification notification = new Notification("new-issues")
        .setDefaultMessage("32 new issues on LongName.")
        .setFieldValue("count", "32")
        .setFieldValue("projectName", "LongName")
        .setFieldValue("projectKey", "key")
        .setFieldValue("projectId", "45")
        .setFieldValue("fromDate", dateformat.format(pastDate.getTime()));
    verify(notificationManager, times(1)).scheduleForSending(eq(notification));
  }

  private List<Issue> createIssues() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleKey(rule1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()).setCreatedAt(rightNow));
    issues.add(new DefaultIssue().setRuleKey(rule1.ruleKey()).setSeverity(RulePriority.CRITICAL.name()).setCreatedAt(tenDaysAgo));
    issues.add(new DefaultIssue().setRuleKey(rule2.ruleKey()).setSeverity(RulePriority.MAJOR.name()).setCreatedAt(fiveDaysAgo));
    issues.add(new DefaultIssue().setRuleKey(rule2.ruleKey()).setSeverity(RulePriority.MAJOR.name()).setCreatedAt(tenDaysAgo));
    issues.add(new DefaultIssue().setRuleKey(rule3.ruleKey()).setSeverity(RulePriority.MINOR.name()).setCreatedAt(fiveDaysAgo));
    issues.add(new DefaultIssue().setRuleKey(rule3.ruleKey()).setSeverity(RulePriority.MINOR.name()).setCreatedAt(tenDaysAgo));
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
}
