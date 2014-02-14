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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.utils.WorkDuration;
import org.sonar.api.utils.WorkDurationFactory;
import org.sonar.batch.components.Period;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.debt.IssueChangelogDebtCalculator;

import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
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

  Date rightNow;
  Date elevenDaysAgo;
  Date tenDaysAgo;
  Date nineDaysAgo;
  Date fiveDaysAgo;
  Date fourDaysAgo;

  WorkDuration oneDaysDebt = WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.DAYS, 8);
  WorkDuration twoDaysDebt = WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.DAYS, 8);
  WorkDuration fiveDaysDebt = WorkDuration.createFromValueAndUnit(5, WorkDuration.UNIT.DAYS, 8);

  @Before
  public void setup() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, "8");

    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);

    rightNow = new Date();
    elevenDaysAgo = DateUtils.addDays(rightNow, -11);
    tenDaysAgo = DateUtils.addDays(rightNow, -10);
    nineDaysAgo = DateUtils.addDays(rightNow, -9);
    fiveDaysAgo = DateUtils.addDays(rightNow, -5);
    fourDaysAgo = DateUtils.addDays(rightNow, -4);

    when(timeMachineConfiguration.periods()).thenReturn(newArrayList(new Period(1, fiveDaysAgo), new Period(2, tenDaysAgo)));

    WorkDurationFactory workDurationFactory = new WorkDurationFactory(settings);
    decorator = new NewTechnicalDebtDecorator(perspectives, timeMachineConfiguration, new IssueChangelogDebtCalculator(workDurationFactory));
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
  public void save_on_one_issue_with_one_new_changelog() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(twoDaysDebt).setChanges(
      newArrayList(
        // changelog created at is null because it has just been created on the current analysis
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), fromWorkDayDuration(twoDaysDebt)).setCreationDate(null)
      )
    );
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 1.0, 1.0)));
  }

  @Test
  public void save_on_one_issue_with_changelog() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(fiveDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(twoDaysDebt), fromWorkDayDuration(fiveDaysDebt)).setCreationDate(null),
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), fromWorkDayDuration(twoDaysDebt)).setCreationDate(fourDaysAgo)
      )
    );
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 4.0, 4.0)));
  }

  @Test
  public void save_on_one_issue_with_changelog_only_in_the_past() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(oneDaysDebt).setChanges(
      newArrayList(
        // Change before all periods
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(oneDaysDebt)).setCreationDate(elevenDaysAgo)
      )
    );
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 0.0, 0.0)));
  }

  @Test
  public void save_on_one_issue_with_changelog_having_null_value() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(fiveDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(fiveDaysDebt)).setCreationDate(null),
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), null).setCreationDate(fourDaysAgo),
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(oneDaysDebt)).setCreationDate(nineDaysAgo)
      )
    );
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 4.0, 5.0)));
  }

  @Test
  public void save_on_one_issue_with_changelog_and_periods_have_no_dates() {
    when(timeMachineConfiguration.periods()).thenReturn(newArrayList(new Period(1, null), new Period(2, null)));

    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(fiveDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(fiveDaysDebt)).setCreationDate(null),
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), null).setCreationDate(fourDaysAgo),
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(oneDaysDebt)).setCreationDate(nineDaysAgo)
      )
    );
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 5.0, 5.0)));
  }

  @Test
  public void save_on_one_issue_with_changelog_having_not_only_technical_debt_changes() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(fiveDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs()
          .setDiff("actionPlan", "1.0", "1.1").setCreationDate(fourDaysAgo)
          .setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), fromWorkDayDuration(twoDaysDebt)).setCreationDate(fourDaysAgo)
      )
    );
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 4.0, 4.0)));
  }

  @Test
  public void save_on_issues_with_changelog() {
    Issue issue1 = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(fiveDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(twoDaysDebt), fromWorkDayDuration(fiveDaysDebt)).setCreationDate(rightNow),
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), fromWorkDayDuration(twoDaysDebt)).setCreationDate(fourDaysAgo),
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(oneDaysDebt)).setCreationDate(nineDaysAgo)
      )
    );
    Issue issue2 = new DefaultIssue().setKey("B").setCreationDate(tenDaysAgo).setTechnicalDebt(twoDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), fromWorkDayDuration(twoDaysDebt)).setCreationDate(rightNow),
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(oneDaysDebt)).setCreationDate(nineDaysAgo)
      )
    );
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 5.0, 7.0)));
  }

  @Test
  public void save_on_one_issue_without_changelog() {
    when(issuable.issues()).thenReturn(newArrayList(
      (Issue) new DefaultIssue().setKey("A").setCreationDate(nineDaysAgo).setTechnicalDebt(fiveDaysDebt))
    );

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 0.0, 5.0)));
  }

  @Test
  public void save_on_one_issue_without_technical_debt_and_without_changelog() {
    when(issuable.issues()).thenReturn(newArrayList(
      (Issue) new DefaultIssue().setKey("A").setCreationDate(nineDaysAgo).setTechnicalDebt(null))
    );

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 0.0, 0.0)));
  }

  @Test
  public void save_on_one_issue_without_changelog_and_periods_have_no_dates() {
    when(timeMachineConfiguration.periods()).thenReturn(newArrayList(new Period(1, null), new Period(2, null)));

    when(issuable.issues()).thenReturn(newArrayList(
      (Issue) new DefaultIssue().setKey("A").setCreationDate(nineDaysAgo).setTechnicalDebt(fiveDaysDebt))
    );

    decorator.decorate(resource, context);

    // remember : period1 is null, period2 is null
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 5.0, 5.0)));
  }

  @Test
  public void save_on_issues_without_changelog() {
    when(issuable.issues()).thenReturn(newArrayList(
      (Issue) new DefaultIssue().setKey("A").setCreationDate(nineDaysAgo).setTechnicalDebt(fiveDaysDebt),
      new DefaultIssue().setKey("B").setCreationDate(fiveDaysAgo).setTechnicalDebt(twoDaysDebt)
    ));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 0.0, 7.0)));
  }

  @Test
  public void save_on_issues_with_changelog_and_issues_without_changelog() {
    // issue1 and issue2 have changelog
    Issue issue1 = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(fiveDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(twoDaysDebt), fromWorkDayDuration(fiveDaysDebt)).setCreationDate(rightNow),
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), fromWorkDayDuration(twoDaysDebt)).setCreationDate(fourDaysAgo),
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(oneDaysDebt)).setCreationDate(nineDaysAgo)
      )
    );
    Issue issue2 = new DefaultIssue().setKey("B").setCreationDate(tenDaysAgo).setTechnicalDebt(twoDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(oneDaysDebt), fromWorkDayDuration(twoDaysDebt)).setCreationDate(rightNow),
        new FieldDiffs().setDiff("technicalDebt", null, fromWorkDayDuration(oneDaysDebt)).setCreationDate(nineDaysAgo)
      )
    );

    // issue3 and issue4 have no changelog
    Issue issue3 = new DefaultIssue().setKey("C").setCreationDate(nineDaysAgo).setTechnicalDebt(fiveDaysDebt);
    Issue issue4 = new DefaultIssue().setKey("D").setCreationDate(fiveDaysAgo).setTechnicalDebt(twoDaysDebt);
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2, issue3, issue4));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 5.0, 14.0)));
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

  /**
   * SONAR-5059
   */
  @Test
  public void not_return_negative_debt() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setTechnicalDebt(oneDaysDebt).setChanges(
      newArrayList(
        // changelog created at is null because it has just been created on the current analysis
        new FieldDiffs().setDiff("technicalDebt", fromWorkDayDuration(twoDaysDebt), fromWorkDayDuration(oneDaysDebt)).setCreationDate(null)
      )
    );
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    // remember : period1 is 5daysAgo, period2 is 10daysAgo
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_TECHNICAL_DEBT, 0.0, 0.0)));
  }

  private Long fromWorkDayDuration(WorkDuration workDayDuration) {
    return workDayDuration.toLong();
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
