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
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NewViolationsDecoratorTest {
  private Rule rule1;
  private Rule rule2;
  private Rule rule3;

  private NewViolationsDecorator decorator;
  private DecoratorContext context;
  private Resource<?> resource;
  private NotificationManager notificationManager;

  private Date rightNow;
  private Date tenDaysAgo;
  private Date fiveDaysAgo;
  private TimeMachineConfiguration timeMachineConfiguration;

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

    timeMachineConfiguration = mock(TimeMachineConfiguration.class);
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Arrays.asList(pastSnapshot, pastSnapshot2));

    context = mock(DecoratorContext.class);
    resource = new File("com/foo/bar");
    when(context.getResource()).thenReturn(resource);

    notificationManager = mock(NotificationManager.class);
    decorator = new NewViolationsDecorator(timeMachineConfiguration, notificationManager);

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
  public void severityViolations() {
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
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_CRITICAL_VIOLATIONS, rule1, 1.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_MAJOR_VIOLATIONS, rule2, 0.0, 1.0)));
    verify(context).saveMeasure(argThat(new IsVariationRuleMeasure(CoreMetrics.NEW_MINOR_VIOLATIONS, rule3, 0.0, 1.0)));
  }

  @Test
  public void shouldNotNotifyIfNotLastestAnalysis() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(false);
    assertThat(decorator.shouldExecuteOnProject(project), is(false));
  }

  @Test
  public void shouldNotNotifyIfNotRootProject() throws Exception {
    Project project = mock(Project.class);
    when(project.getQualifier()).thenReturn(Qualifiers.MODULE);

    decorator.decorate(project, context);

    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void shouldNotNotifyIfNoNotEnoughPastSnapshots() throws Exception {
    Project project = new Project("key");
    // the #setUp method adds 2 snapshots: if last period analysis is 3, then it's not enough
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(new ArrayList<PastSnapshot>());

    decorator.notifyNewViolations(project, context);
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void shouldNotNotifyIfNoNewViolations() throws Exception {
    Project project = new Project("key");
    Measure m = new Measure(CoreMetrics.NEW_VIOLATIONS);
    when(context.getMeasure(CoreMetrics.NEW_VIOLATIONS)).thenReturn(m);

    // NULL is returned here
    decorator.notifyNewViolations(project, context);
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));

    // 0 will be returned now
    m.setVariation1(0.0);
    decorator.notifyNewViolations(project, context);
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void shouldNotNotifyUserIfFirstAnalysis() throws Exception {
    Project project = new Project("key").setName("LongName");
    project.setId(45);
    // PastSnapshot with targetDate==null means first analysis
    PastSnapshot pastSnapshot = new PastSnapshot("", null);
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Lists.newArrayList(pastSnapshot));
    Measure m = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(0.0);
    when(context.getMeasure(CoreMetrics.NEW_VIOLATIONS)).thenReturn(m);

    decorator.decorate(project, context);
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void shouldNotifyUserAboutNewViolations() throws Exception {
    Project project = new Project("key").setName("LongName");
    project.setId(45);
    Calendar pastDate = new GregorianCalendar(2011, 10, 25);
    PastSnapshot pastSnapshot = new PastSnapshot("", pastDate.getTime());
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Lists.newArrayList(pastSnapshot, pastSnapshot));
    Measure m = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(32.0);
    when(context.getMeasure(CoreMetrics.NEW_VIOLATIONS)).thenReturn(m);

    decorator.decorate(project, context);

    DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
    Notification notification = new Notification("new-violations")
        .setDefaultMessage("32 new violations on LongName.")
        .setFieldValue("count", "32")
        .setFieldValue("projectName", "LongName")
        .setFieldValue("projectKey", "key")
        .setFieldValue("projectId", "45")
        .setFieldValue("fromDate", dateformat.format(pastDate.getTime()));
    verify(notificationManager, times(1)).scheduleForSending(eq(notification));
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
