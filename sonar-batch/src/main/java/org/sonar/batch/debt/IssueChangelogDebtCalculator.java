/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
import org.sonar.api.BatchSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.core.issue.IssueUpdater;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Warning, before modifying this class, please do not forget that it's used by the Dev Cockpit plugin
 */
@BatchSide
public class IssueChangelogDebtCalculator {

  @CheckForNull
  public Long calculateNewTechnicalDebt(Issue issue, @Nullable Date periodDate) {
    Long debt = ((DefaultIssue) issue).debtInMinutes();
    Date periodDatePlusOneSecond = periodDate != null ? DateUtils.addSeconds(periodDate, 1) : null;
    if (isAfter(issue.creationDate(), periodDatePlusOneSecond)) {
      return debt;
    } else {
      return calculateNewTechnicalDebtValueFromChangelog(debt, issue, periodDate);
    }
  }

  @CheckForNull
  private Long calculateNewTechnicalDebtValueFromChangelog(@Nullable Long currentTechnicalDebtValue, Issue issue, Date periodDate) {
    List<FieldDiffs> changelog = technicalDebtHistory(issue);
    for (Iterator<FieldDiffs> iterator = changelog.iterator(); iterator.hasNext();) {
      FieldDiffs diff = iterator.next();
      Date date = diff.creationDate();
      if (isLesserOrEqual(date, periodDate)) {
        // return new value from the change that is just before the period date
        return subtractNeverNegative(currentTechnicalDebtValue, newValue(diff));
      }
      if (!iterator.hasNext()) {
        // return old value from the change that is just after the period date when there's no more element in changelog
        return subtractNeverNegative(currentTechnicalDebtValue, oldValue(diff));
      }
    }
    // Return null when no changelog
    return null;
  }

  /**
   * SONAR-5059
   */
  @CheckForNull
  private Long subtractNeverNegative(@Nullable Long value, Long with) {
    Long result = (value != null ? value : 0) - (with != null ? with : 0);
    return result > 0 ? result : null;
  }

  private List<FieldDiffs> technicalDebtHistory(Issue issue) {
    List<FieldDiffs> technicalDebtChangelog = changesOnField(((DefaultIssue) issue).changes());
    if (!technicalDebtChangelog.isEmpty()) {
      // Changelog have to be sorted from newest to oldest.
      // Null date should be the first as this happen when technical debt has changed since previous analysis.
      Ordering<FieldDiffs> ordering = Ordering.natural().reverse().nullsFirst().onResultOf(new Function<FieldDiffs, Date>() {
        @Override
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
  private Long newValue(FieldDiffs fieldDiffs) {
    for (Map.Entry<String, FieldDiffs.Diff> entry : fieldDiffs.diffs().entrySet()) {
      if (entry.getKey().equals(IssueUpdater.TECHNICAL_DEBT)) {
        return entry.getValue().newValueLong();
      }
    }
    return null;
  }

  @CheckForNull
  private Long oldValue(FieldDiffs fieldDiffs) {
    for (Map.Entry<String, FieldDiffs.Diff> entry : fieldDiffs.diffs().entrySet()) {
      if (entry.getKey().equals(IssueUpdater.TECHNICAL_DEBT)) {
        return entry.getValue().oldValueLong();
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
