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
package org.sonar.server.computation.issue;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.server.computation.period.Period;

import static com.google.common.collect.FluentIterable.from;

/**
 * Gets the issue debt that was introduced on a period. The algorithm
 * is based on the issue changelog.
 */
public class NewDebtCalculator {

  public long calculate(DefaultIssue issue, Collection<IssueChangeDto> debtChangelog, Period period) {
    if (issue.creationDate().getTime() > period.getSnapshotDate() + 1000L) {
      return Objects.firstNonNull(issue.debtInMinutes(), 0L);
    }
    return calculateFromChangelog(issue, debtChangelog, period.getSnapshotDate());
  }

  private long calculateFromChangelog(DefaultIssue issue, Collection<IssueChangeDto> debtChangelog, long periodDate) {
    List<FieldDiffs> debtDiffs = from(debtChangelog).transform(ToFieldDiffs.INSTANCE).filter(HasDebtChange.INSTANCE).toSortedList(CHANGE_ORDERING);
    FieldDiffs currentChange = issue.currentChange();
    if (currentChange != null && HasDebtChange.INSTANCE.apply(currentChange)) {
      debtDiffs = Lists.newArrayList(debtDiffs);
      debtDiffs.add(currentChange);
    }
    long newDebt = issue.debtInMinutes();

    for (Iterator<FieldDiffs> it = debtDiffs.iterator(); it.hasNext();) {
      FieldDiffs diffs = it.next();
      Date date = diffs.creationDate();
      // TODO use longs
      if (isBeforeOrEqual(date, new Date(periodDate))) {
        // return new value from the change that is just before the period date
        return subtract(newDebt, debtDiff(diffs).newValueLong());
      }
      if (!it.hasNext()) {
        // return old value from the change that is just after the period date when there's no more element in changelog
        return subtract(newDebt, debtDiff(diffs).oldValueLong());
      }
    }
    // no changelog
    return 0L;
  }

  /**
   * SONAR-5059
   */
  @CheckForNull
  private static long subtract(long newDebt, @Nullable Long with) {
    if (with != null) {
      return Math.max(0L, newDebt - with);
    }
    return newDebt;
  }

  private static boolean isBeforeOrEqual(@Nullable Date changeDate, Date periodDate) {
    return (changeDate != null) && (DateUtils.truncatedCompareTo(changeDate, periodDate, Calendar.SECOND) <= 0);
  }

  private static FieldDiffs.Diff debtDiff(FieldDiffs diffs) {
    return diffs.diffs().get(IssueUpdater.TECHNICAL_DEBT);
  }

  /**
   * Changelog have to be sorted from newest to oldest.
   * Null date should be the first as this happen when technical debt has changed since previous analysis.
   */
  private static final Comparator<FieldDiffs> CHANGE_ORDERING = Ordering.natural().reverse().nullsFirst().onResultOf(new Function<FieldDiffs, Date>() {
    @Override
    public Date apply(@Nonnull FieldDiffs dto) {
      return dto.creationDate();
    }
  });

  private enum ToFieldDiffs implements Function<IssueChangeDto, FieldDiffs> {
    INSTANCE;
    @Override
    public FieldDiffs apply(@Nonnull IssueChangeDto dto) {
      return dto.toFieldDiffs();
    }
  }

  private enum HasDebtChange implements Predicate<FieldDiffs> {
    INSTANCE;
    @Override
    public boolean apply(@Nonnull FieldDiffs diffs) {
      return diffs.diffs().containsKey(IssueUpdater.TECHNICAL_DEBT);
    }
  }
}
