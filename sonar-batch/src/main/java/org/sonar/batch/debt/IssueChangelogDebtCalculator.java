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

package org.sonar.batch.debt;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.utils.WorkDuration;
import org.sonar.api.utils.WorkDurationFactory;
import org.sonar.core.issue.IssueUpdater;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class IssueChangelogDebtCalculator implements BatchComponent {

  private final WorkDurationFactory workDurationFactory;

  public IssueChangelogDebtCalculator(WorkDurationFactory workDurationFactory) {
    this.workDurationFactory = workDurationFactory;
  }

  @CheckForNull
  public WorkDuration calculateNewTechnicalDebt(Issue issue, @Nullable Date periodDate) {
    WorkDuration currentTechnicalDebt = ((DefaultIssue) issue).technicalDebt();
    Date periodDatePlusOneSecond = periodDate != null ? DateUtils.addSeconds(periodDate, 1) : null;
    if (isAfter(issue.creationDate(), periodDatePlusOneSecond)) {
      return currentTechnicalDebt;
    } else {
      return calculateNewTechnicalDebtValueFromChangelog(currentTechnicalDebt, issue, periodDate);
    }
  }

  @CheckForNull
  private WorkDuration calculateNewTechnicalDebtValueFromChangelog(WorkDuration currentTechnicalDebtValue, Issue issue, Date periodDate) {
    List<FieldDiffs> changelog = technicalDebtHistory(issue);
    for (Iterator<FieldDiffs> iterator = changelog.iterator(); iterator.hasNext(); ) {
      FieldDiffs diff = iterator.next();
      Date date = diff.creationDate();
      WorkDuration newValue = newValue(diff);
      WorkDuration oldValue = oldValue(diff);
      if (isLesserOrEqual(date, periodDate)) {
        // return new value from the change that is just before the period date
        return subtractNeverNegative(currentTechnicalDebtValue, newValue);
      }
      if (!iterator.hasNext()) {
        // return old value from the change that is just after the period date when there's no more element in changelog
        return subtractNeverNegative(currentTechnicalDebtValue, oldValue);
      }
    }
    // Return null when no changelog
    return null;
  }

  private WorkDuration subtractNeverNegative(WorkDuration workDuration, WorkDuration toSubtractWith){
    WorkDuration result = workDuration.subtract(toSubtractWith);
    if (result.toSeconds() > 0) {
      return result;
    }
    return workDurationFactory.createFromWorkingLong(0L);
  }

  private List<FieldDiffs> technicalDebtHistory(Issue issue) {
    List<FieldDiffs> technicalDebtChangelog = changesOnField(((DefaultIssue) issue).changes());
    if (!technicalDebtChangelog.isEmpty()) {
      // Changelog have to be sorted from newest to oldest.
      // Null date should be the first as this happen when technical debt has changed since previous analysis.
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

  @CheckForNull
  private WorkDuration newValue(FieldDiffs fieldDiffs) {
    for (Map.Entry<String, FieldDiffs.Diff> entry : fieldDiffs.diffs().entrySet()) {
      if (entry.getKey().equals(IssueUpdater.TECHNICAL_DEBT)) {
        Long newValue = entry.getValue().newValueLong();
        return workDurationFactory.createFromWorkingLong(newValue);
      }
    }
    return null;
  }

  @CheckForNull
  private WorkDuration oldValue(FieldDiffs fieldDiffs) {
    for (Map.Entry<String, FieldDiffs.Diff> entry : fieldDiffs.diffs().entrySet()) {
      if (entry.getKey().equals(IssueUpdater.TECHNICAL_DEBT)) {
        Long value = entry.getValue().oldValueLong();
        return workDurationFactory.createFromWorkingLong(value);
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


}
