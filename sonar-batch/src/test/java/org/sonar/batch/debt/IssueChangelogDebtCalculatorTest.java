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

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.utils.Duration;

import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class IssueChangelogDebtCalculatorTest {

  private static final int HOURS_IN_DAY = 8;

  IssueChangelogDebtCalculator issueChangelogDebtCalculator;

  Date rightNow = new Date();
  Date elevenDaysAgo = DateUtils.addDays(rightNow, -11);
  Date tenDaysAgo = DateUtils.addDays(rightNow, -10);
  Date nineDaysAgo = DateUtils.addDays(rightNow, -9);
  Date fiveDaysAgo = DateUtils.addDays(rightNow, -5);
  Date fourDaysAgo = DateUtils.addDays(rightNow, -4);

  long oneDay = 1 * HOURS_IN_DAY * 60 * 60L;
  long twoDays = 2 * HOURS_IN_DAY * 60 * 60L;
  long fiveDays = 5 * HOURS_IN_DAY * 60 * 60L;

  Duration oneDayDebt = Duration.create(oneDay);
  Duration twoDaysDebt = Duration.create(twoDays);
  Duration fiveDaysDebt = Duration.create(fiveDays);

  @Before
  public void setUp() throws Exception {
    issueChangelogDebtCalculator = new IssueChangelogDebtCalculator();
  }

  @Test
  public void calculate_new_technical_debt_with_one_diff_in_changelog() throws Exception {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setDebt(twoDaysDebt).setChanges(
      newArrayList(
        // changelog created at is null because it has just been created on the current analysis
        new FieldDiffs().setDiff("technicalDebt", oneDay, twoDays).setCreationDate(null)
      )
    );

    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, rightNow)).isEqualTo(oneDay);
    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, fiveDaysAgo)).isEqualTo(oneDay);

    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, elevenDaysAgo)).isEqualTo(twoDays);
  }

  @Test
  public void calculate_new_technical_debt_with_many_diffs_in_changelog() throws Exception {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setDebt(fiveDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", twoDays, fiveDays).setCreationDate(null),
        new FieldDiffs().setDiff("technicalDebt", oneDay, twoDays).setCreationDate(fourDaysAgo)
      )
    );

    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, rightNow)).isEqualTo(3 * oneDay);
    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, fiveDaysAgo)).isEqualTo(4 * oneDay);
    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, elevenDaysAgo)).isEqualTo(5 * oneDay);
  }

  @Test
  public void changelog_can_be_in_wrong_order() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setDebt(fiveDaysDebt).setChanges(
      newArrayList(
        // 3rd
        new FieldDiffs().setDiff("technicalDebt", null, oneDay).setCreationDate(nineDaysAgo),
        // 1st
        new FieldDiffs().setDiff("technicalDebt", twoDays, fiveDays).setCreationDate(rightNow),
        // 2nd
        new FieldDiffs().setDiff("technicalDebt", oneDay, twoDays).setCreationDate(fourDaysAgo)
      )
    );

    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, fiveDaysAgo)).isEqualTo(4 * oneDay);
    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, elevenDaysAgo)).isEqualTo(5 * oneDay);
  }

  @Test
  public void calculate_new_technical_debt_with_null_date() throws Exception {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setDebt(twoDaysDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", oneDay, twoDays).setCreationDate(null)
      )
    );

    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, null)).isEqualTo(2 * oneDay);
  }

  @Test
  public void calculate_new_technical_debt_when_new_debt_is_null() throws Exception {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setDebt(null).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", oneDay, null).setCreationDate(null),
        new FieldDiffs().setDiff("technicalDebt", null, oneDay).setCreationDate(nineDaysAgo)
      )
    );

    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, rightNow)).isNull();
  }

  @Test
  public void calculate_new_technical_debt_on_issue_without_technical_debt_and_without_changelog() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo);

    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, rightNow)).isNull();
  }

  @Test
  public void not_return_negative_debt() {
    Issue issue = new DefaultIssue().setKey("A").setCreationDate(tenDaysAgo).setDebt(oneDayDebt).setChanges(
      newArrayList(
        new FieldDiffs().setDiff("technicalDebt", twoDays, oneDay).setCreationDate(null)
      )
    );

    assertThat(issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, rightNow)).isNull();
  }

}
