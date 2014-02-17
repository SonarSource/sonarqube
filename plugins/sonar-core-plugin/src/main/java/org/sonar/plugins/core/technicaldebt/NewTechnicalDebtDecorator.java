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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.batch.*;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.components.Period;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;

import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Decorator that computes the technical debt metric
 */
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public final class NewTechnicalDebtDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final TimeMachineConfiguration timeMachineConfiguration;
  private final TechnicalDebtConverter technicalDebtConverter;

  public NewTechnicalDebtDecorator(ResourcePerspectives perspectives, TimeMachineConfiguration timeMachineConfiguration, TechnicalDebtConverter technicalDebtConverter) {
    this.perspectives = perspectives;
    this.timeMachineConfiguration = timeMachineConfiguration;
    this.technicalDebtConverter = technicalDebtConverter;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return ImmutableList.of(
      CoreMetrics.NEW_TECHNICAL_DEBT
    );
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null && shouldSaveNewMetrics(context)) {
      List<Issue> issues = newArrayList(issuable.issues());
      saveMeasures(context, issues);
    }
  }

  private void saveMeasures(DecoratorContext context, Collection<Issue> issues) {
    Measure measure = new Measure(CoreMetrics.NEW_TECHNICAL_DEBT);
    for (Period period : timeMachineConfiguration.periods()) {
      Date periodDate = period.getDate();
      double value = calculateNewTechnicalDebtValue(issues, periodDate);
      Collection<Measure> children = context.getChildrenMeasures(measure.getMetric());
      double sum = MeasureUtils.sumOnVariation(true, period.getIndex(), children) + value;
      measure.setVariation(period.getIndex(), sum);
    }
    context.saveMeasure(measure);
  }

  private Double calculateNewTechnicalDebtValue(Collection<Issue> issues, @Nullable Date periodDate) {
    double value = 0;
    for (Issue issue : issues) {
      WorkDayDuration currentTechnicalDebt = ((DefaultIssue) issue).technicalDebt();

      Date periodDatePlusOneSecond = periodDate != null ? DateUtils.addSeconds(periodDate, 1) : null;
      if (isAfter(issue.creationDate(), periodDatePlusOneSecond)) {
        value += technicalDebtConverter.toDays(currentTechnicalDebt);
      } else {
        double debt = calculateNewTechnicalDebtValueFromChangelog(currentTechnicalDebt, issue, periodDate);
        if (debt > 0) {
          value += debt;
        }
      }
    }
    return value;
  }

  private double calculateNewTechnicalDebtValueFromChangelog(WorkDayDuration currentTechnicalDebt, Issue issue, Date periodDate) {
    double currentTechnicalDebtValue = technicalDebtConverter.toDays(currentTechnicalDebt);

    List<FieldDiffs> changelog = technicalDebtHistory(issue);
    for (Iterator<FieldDiffs> iterator = changelog.iterator(); iterator.hasNext(); ) {
      FieldDiffs diff = iterator.next();
      Date date = diff.creationDate();
      if (isLesserOrEqual(date, periodDate)) {
        // return new value from the change that is just before the period date
        return currentTechnicalDebtValue - technicalDebtConverter.toDays(newValue(diff));
      }
      if (!iterator.hasNext()) {
        // return old value from the change that is just after the period date when there's no more element in changelog
        return currentTechnicalDebtValue - technicalDebtConverter.toDays(oldValue(diff));
      }
    }
    // Return 0 when no changelog
    return 0d;
  }

  private List<FieldDiffs> technicalDebtHistory(Issue issue) {
    List<FieldDiffs> technicalDebtChangelog = changesOnField(((DefaultIssue) issue).changes());
    if (!technicalDebtChangelog.isEmpty()) {
      // Changelog have to be sorted from newest to oldest.
      // Null date should be the latest as this happen when technical debt has changed since previous analysis.
      Ordering<FieldDiffs> ordering = Ordering.natural().reverse().nullsFirst().onResultOf(new Function<FieldDiffs, Date>() {
        public Date apply(FieldDiffs diff) {
          return diff.creationDate();
        }
      });
      return ordering.immutableSortedCopy(technicalDebtChangelog);
    }
    return Collections.emptyList();
  }

  private List<FieldDiffs> changesOnField(Collection<FieldDiffs> fieldDiffs) {
    List<FieldDiffs> diffs = newArrayList();
    for (FieldDiffs fieldDiff : fieldDiffs) {
      if (fieldDiff.diffs().containsKey(IssueUpdater.TECHNICAL_DEBT)) {
        diffs.add(fieldDiff);
      }
    }
    return diffs;
  }

  private WorkDayDuration newValue(FieldDiffs fieldDiffs) {
    for (Map.Entry<String, FieldDiffs.Diff> entry : fieldDiffs.diffs().entrySet()) {
      if (entry.getKey().equals(IssueUpdater.TECHNICAL_DEBT)) {
        Long newValue = entry.getValue().newValueLong();
        return newValue != null ? WorkDayDuration.fromLong(newValue) : null;
      }
    }
    return null;
  }

  private WorkDayDuration oldValue(FieldDiffs fieldDiffs) {
    for (Map.Entry<String, FieldDiffs.Diff> entry : fieldDiffs.diffs().entrySet()) {
      if (entry.getKey().equals(IssueUpdater.TECHNICAL_DEBT)) {
        Long value = entry.getValue().oldValueLong();
        return value != null ? WorkDayDuration.fromLong(value) : null;
      }
    }
    return null;
  }

  private boolean isAfter(@Nullable Date currentDate, @Nullable Date pastDate) {
    return pastDate == null || (currentDate != null && DateUtils.truncatedCompareTo(currentDate, pastDate, Calendar.SECOND) > 0);
  }

  private boolean isLesserOrEqual(@Nullable Date currentDate, @Nullable Date pastDate) {
    return (currentDate != null) && (pastDate == null || (DateUtils.truncatedCompareTo(currentDate, pastDate, Calendar.SECOND) <= 0));
  }

  private boolean shouldSaveNewMetrics(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.NEW_TECHNICAL_DEBT) == null;
  }

}
