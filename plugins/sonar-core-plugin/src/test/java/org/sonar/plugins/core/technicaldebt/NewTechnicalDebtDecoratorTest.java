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

package org.sonar.plugins.core.technicaldebt;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.ObjectUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.batch.components.Period;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.core.issue.IssueChangelogFinder;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;

import java.util.Calendar;
import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NewTechnicalDebtDecoratorTest {

  NewTechnicalDebtDecorator decorator;

  @Mock
  TimeMachineConfiguration timeMachineConfiguration;

  @Mock
  Resource resource;

  @Mock
  Issuable issuable;

  @Mock
  DecoratorContext context;

  @Mock
  IssueChangelogFinder changelogFinder;

  @Mock
  TechnicalDebtConverter technicalDebtConverter;

  Date rightNow;
  Date elevenDaysAgo;
  Date tenDaysAgo;
  Date nineDaysAgo;
  Date fiveDaysAgo;
  Date fourDaysAgo;
  Date sameSecond;

  WorkDayDuration oneDaysDebt = WorkDayDuration.of(0, 0, 1);
  WorkDayDuration twoDaysDebt = WorkDayDuration.of(0, 0, 2);
  WorkDayDuration fiveDaysDebt = WorkDayDuration.of(0, 0, 5);

  @Before
  public void setup() {
    when(technicalDebtConverter.toDays(oneDaysDebt)).thenReturn(1d);
    when(technicalDebtConverter.toDays(twoDaysDebt)).thenReturn(2d);
    when(technicalDebtConverter.toDays(fiveDaysDebt)).thenReturn(5d);

    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);

    rightNow = new Date();
    elevenDaysAgo = org.apache.commons.lang.time.DateUtils.addDays(rightNow, -11);
    tenDaysAgo = org.apache.commons.lang.time.DateUtils.addDays(rightNow, -10);
    nineDaysAgo = org.apache.commons.lang.time.DateUtils.addDays(rightNow, -9);
    fiveDaysAgo = org.apache.commons.lang.time.DateUtils.addDays(rightNow, -5);
    fourDaysAgo = org.apache.commons.lang.time.DateUtils.addDays(rightNow, -4);
    sameSecond = org.apache.commons.lang.time.DateUtils.truncate(rightNow, Calendar.SECOND);

    when(timeMachineConfiguration.periods()).thenReturn(newArrayList(new Period(1, fiveDaysAgo, fiveDaysAgo), new Period(2, tenDaysAgo, tenDaysAgo)));

    decorator = new NewTechnicalDebtDecorator(perspectives, timeMachineConfiguration, changelogFinder, technicalDebtConverter);
  }

  @Test
  public void generates_metrics() throws Exception {
    assertThat(decorator.generatesMetrics()).hasSize(1);
  }

  @Test
  public void execute_on_project() throws Exception {
    assertThat(decorator.shouldExecuteOnProject(null)).isTrue();
  }

  @Test
  public void save_on_one_issue_with_changelog() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(fiveDaysAgo).setTechnicalDebt(fiveDaysDebt);
    when(issuable.issues()).thenReturn(newArrayList(issue));

    Multimap<Issue, FieldDiffs> changelogList = ArrayListMultimap.create();
    // Changes should be sorted from newest to oldest
    changelogList.putAll(issue, newArrayList(
      new FieldDiffs().setDiff("technicalDebt", twoDaysDebt, fiveDaysDebt).setCreatedAt(rightNow),
      new FieldDiffs().setDiff("technicalDebt", oneDaysDebt, twoDaysDebt).setCreatedAt(fourDaysAgo),
      new FieldDiffs().setDiff("technicalDebt", null, oneDaysDebt).setCreatedAt(nineDaysAgo)
    ));
    when(changelogFinder.findByIssues(anyCollection())).thenReturn(changelogList);

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 3.0, 4.0)));
  }

  @Test
  public void save_on_issues_with_changelog() {
    Issue issue1 = new DefaultIssue().setKey("A").setCreationDate(fiveDaysAgo).setTechnicalDebt(fiveDaysDebt);
    Issue issue2 = new DefaultIssue().setKey("B").setCreationDate(fiveDaysAgo).setTechnicalDebt(twoDaysDebt);
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2));

    Multimap<Issue, FieldDiffs> changelogList = ArrayListMultimap.create();
    changelogList.putAll(issue1, newArrayList(
      new FieldDiffs().setDiff("technicalDebt", twoDaysDebt, fiveDaysDebt).setCreatedAt(rightNow),
      new FieldDiffs().setDiff("technicalDebt", oneDaysDebt, twoDaysDebt).setCreatedAt(fourDaysAgo),
      new FieldDiffs().setDiff("technicalDebt", null, oneDaysDebt).setCreatedAt(nineDaysAgo)
    ));
    changelogList.putAll(issue2, newArrayList(
      new FieldDiffs().setDiff("technicalDebt", oneDaysDebt, twoDaysDebt).setCreatedAt(rightNow),
      new FieldDiffs().setDiff("technicalDebt", null, oneDaysDebt).setCreatedAt(nineDaysAgo)
    ));

    when(changelogFinder.findByIssues(anyCollection())).thenReturn(changelogList);

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 3.0, 5.0)));
  }

  @Test
  public void save_on_one_issue_without_changelog() {
    when(issuable.issues()).thenReturn(newArrayList(
      (Issue) new DefaultIssue().setKey("A").setCreationDate(nineDaysAgo).setTechnicalDebt(fiveDaysDebt))
    );

    Multimap<Issue, FieldDiffs> changelogList = ArrayListMultimap.create();
    when(changelogFinder.findByIssues(anyCollection())).thenReturn(changelogList);

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 0.0, 5.0)));
  }

  @Test
  public void save_on_issues_without_changelog() {
    when(issuable.issues()).thenReturn(newArrayList(
      (Issue) new DefaultIssue().setKey("A").setCreationDate(nineDaysAgo).setTechnicalDebt(fiveDaysDebt),
      new DefaultIssue().setKey("B").setCreationDate(fiveDaysAgo).setTechnicalDebt(twoDaysDebt)
    ));

    Multimap<Issue, FieldDiffs> changelogList = ArrayListMultimap.create();
    when(changelogFinder.findByIssues(anyCollection())).thenReturn(changelogList);

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 0.0, 7.0)));
  }

  @Test
  public void save_on_issues_with_changelog_and_issues_without_changelog() {
    Issue issue1 = new DefaultIssue().setKey("A").setCreationDate(fiveDaysAgo).setTechnicalDebt(fiveDaysDebt);
    Issue issue2 = new DefaultIssue().setKey("B").setCreationDate(fiveDaysAgo).setTechnicalDebt(twoDaysDebt);
    Issue issue3 = new DefaultIssue().setKey("C").setCreationDate(nineDaysAgo).setTechnicalDebt(fiveDaysDebt);
    Issue issue4 = new DefaultIssue().setKey("D").setCreationDate(fiveDaysAgo).setTechnicalDebt(twoDaysDebt);
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2, issue3, issue4));

    Multimap<Issue, FieldDiffs> changelogList = ArrayListMultimap.create();
    changelogList.putAll(issue1, newArrayList(
      new FieldDiffs().setDiff("technicalDebt", twoDaysDebt, fiveDaysDebt).setCreatedAt(rightNow),
      new FieldDiffs().setDiff("technicalDebt", oneDaysDebt, twoDaysDebt).setCreatedAt(fourDaysAgo),
      new FieldDiffs().setDiff("technicalDebt", null, oneDaysDebt).setCreatedAt(nineDaysAgo)
    ));
    changelogList.putAll(issue2, newArrayList(
      new FieldDiffs().setDiff("technicalDebt", oneDaysDebt, twoDaysDebt).setCreatedAt(rightNow),
      new FieldDiffs().setDiff("technicalDebt", null, oneDaysDebt).setCreatedAt(nineDaysAgo)
    ));

    when(changelogFinder.findByIssues(anyCollection())).thenReturn(changelogList);

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 3.0, 12.0)));
  }

  @Test
  public void not_save_if_measure_already_computed() {
    when(context.getMeasure(CoreMetrics.NEW_TECHNICAL_DEBT)).thenReturn(new Measure());
    when(issuable.issues()).thenReturn(newArrayList(
      (Issue) new DefaultIssue().setKey("A").setCreationDate(nineDaysAgo).setTechnicalDebt(fiveDaysDebt),
      new DefaultIssue().setKey("B").setCreationDate(fiveDaysAgo).setTechnicalDebt(twoDaysDebt)
    ));

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.NEW_TECHNICAL_DEBT)));
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
        ObjectUtils.equals(var2, m.getVariation2());
    }
  }

}
