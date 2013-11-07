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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
import org.sonar.core.issue.IssueChangelogFinder;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Decorator that computes the technical debt metric
 */
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public final class NewTechnicalDebtDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final TimeMachineConfiguration timeMachineConfiguration;
  private final IssueChangelogFinder changelogFinder;
  private final TechnicalDebtConverter technicalDebtConverter;

  public NewTechnicalDebtDecorator(ResourcePerspectives perspectives, TimeMachineConfiguration timeMachineConfiguration, IssueChangelogFinder changelogFinder,
                                   TechnicalDebtConverter technicalDebtConverter) {
    this.perspectives = perspectives;
    this.timeMachineConfiguration = timeMachineConfiguration;
    this.changelogFinder = changelogFinder;
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
      Multimap<Issue, FieldDiffs> changelogList = changelogFinder.findByIssues(issues);
      saveMeasures(context, issues, changelogList);
    }
  }

  private void saveMeasures(DecoratorContext context, Collection<Issue> issues, Multimap<Issue, FieldDiffs> changelogList) {
    Measure measure = new Measure(CoreMetrics.NEW_TECHNICAL_DEBT);
    for (Period period : timeMachineConfiguration.periods()) {
      Date periodDate = period.getDate() != null ? DateUtils.addSeconds(period.getDate(), 1) : null;
      double value = calculateNewTechnicalDebtValue(issues, changelogList, periodDate);
      Collection<Measure> children = context.getChildrenMeasures(measure.getMetric());
      double sum = MeasureUtils.sumOnVariation(true, period.getIndex(), children) + value;
      measure.setVariation(period.getIndex(), sum);
    }
    context.saveMeasure(measure);
  }

  private Double calculateNewTechnicalDebtValue(Collection<Issue> issues, Multimap<Issue, FieldDiffs> changelogList, @Nullable Date periodDate){
    double value = 0;
    for (Issue issue : issues) {
      WorkDayDuration currentTechnicalDebt = ((DefaultIssue) issue).technicalDebt();
      List<FieldDiffs> technicalDebtChangelog = changesOnField(IssueUpdater.TECHNICAL_DEBT, changelogList.get(issue));
      if (technicalDebtChangelog.isEmpty()) {
        if (isAfter(issue.creationDate(), periodDate)) {
          value += technicalDebtConverter.toDays(currentTechnicalDebt);
        }
      } else {
        value += calculateNewTechnicalDebtValueFromChangelog(currentTechnicalDebt, technicalDebtChangelog, periodDate);
      }
    }
    return value;
  }

  private Double calculateNewTechnicalDebtValueFromChangelog(WorkDayDuration currentTechnicalDebt, List<FieldDiffs> technicalDebtChangelog, Date periodDate) {
    double currentTechnicalDebtValue = technicalDebtConverter.toDays(currentTechnicalDebt);

    // Changelog have to be sorted from oldest to newest to catch oldest value just before the period date -> By default it's sorted from newest to oldest
    for (FieldDiffs fieldDiffs : Lists.reverse(technicalDebtChangelog)) {
      if (isAfter(fieldDiffs.createdAt(), periodDate)) {
        WorkDayDuration pastTechnicalDebt = newValue(IssueUpdater.TECHNICAL_DEBT, fieldDiffs);
        double pastTechnicalDebtValue = technicalDebtConverter.toDays(pastTechnicalDebt);
        return currentTechnicalDebtValue - pastTechnicalDebtValue;
      }
    }
    return null;
  }

  private List<FieldDiffs> changesOnField(final String field, Collection<FieldDiffs> fieldDiffs) {
    List<FieldDiffs> diffs = newArrayList();
    for (FieldDiffs fieldDiff : fieldDiffs) {
      if (fieldDiff.diffs().containsKey(field)) {
        diffs.add(fieldDiff);
      }
    }
    return diffs;
  }

  private WorkDayDuration newValue(final String field, FieldDiffs fieldDiffs) {
    for (Map.Entry<String, FieldDiffs.Diff> entry : fieldDiffs.diffs().entrySet()) {
      if (entry.getKey().equals(field)) {
        Serializable newValue = entry.getValue().newValue();
        return (WorkDayDuration) newValue;
      }
    }
    return null;
  }

  private boolean isAfter(Date currentDate, @Nullable Date pastDate) {
    return pastDate == null || (currentDate != null && DateUtils.truncatedCompareTo(currentDate, pastDate, Calendar.SECOND) > 0);
  }

  private boolean shouldSaveNewMetrics(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.NEW_TECHNICAL_DEBT) == null;
  }

}
