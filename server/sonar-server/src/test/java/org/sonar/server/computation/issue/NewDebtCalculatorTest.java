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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.server.computation.period.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_PREVIOUS_VERSION;

public class NewDebtCalculatorTest {

  private static final int HOURS_IN_DAY = 8;
  private static final Duration ONE_DAY = Duration.create(HOURS_IN_DAY * 60 * 60L);
  private static final Duration TWO_DAYS = Duration.create(2 * HOURS_IN_DAY * 60 * 60L);
  private static final Duration FOUR_DAYS = Duration.create(4 * HOURS_IN_DAY * 60 * 60L);
  private static final Duration FIVE_DAYS = Duration.create(5 * HOURS_IN_DAY * 60 * 60L);
  private static final Duration TEN_DAYS = Duration.create(10 * HOURS_IN_DAY * 60 * 60L);
  private static final long PERIOD_DATE = 150000000L;
  private static final long SNAPSHOT_ID = 1000L;
  private static final Period PERIOD = new Period(1, TIMEMACHINE_MODE_PREVIOUS_VERSION, null, PERIOD_DATE, SNAPSHOT_ID);

  DefaultIssue issue = new DefaultIssue();
  NewDebtCalculator underTest = new NewDebtCalculator();

  /**
   * New debt is the value of the debt when issue is created during the period
   */
  @Test
  public void total_debt_if_issue_created_during_period() {
    issue.setDebt(TWO_DAYS).setCreationDate(new Date(PERIOD_DATE + 10000));

    long newDebt = underTest.calculate(issue, Collections.<IssueChangeDto>emptyList(), PERIOD);

    assertThat(newDebt).isEqualTo(TWO_DAYS.toMinutes());
  }

  @Test
  public void new_debt_if_issue_created_before_period() {
    // creation: 1d
    // before period: increased to 2d
    // after period: increased to 5d, decreased to 4d then increased to 10d
    // -> new debt is 10d - 2d = 8d
    issue.setDebt(TEN_DAYS).setCreationDate(new Date(PERIOD_DATE - 10000));
    List<IssueChangeDto> changelog = Arrays.asList(
      newDebtChangelog(ONE_DAY.toMinutes(), TWO_DAYS.toMinutes(), PERIOD_DATE - 9000),
      newDebtChangelog(TWO_DAYS.toMinutes(), FIVE_DAYS.toMinutes(), PERIOD_DATE + 10000),
      newDebtChangelog(FIVE_DAYS.toMinutes(), FOUR_DAYS.toMinutes(), PERIOD_DATE + 20000),
      newDebtChangelog(FOUR_DAYS.toMinutes(), TEN_DAYS.toMinutes(), PERIOD_DATE + 30000)
    );

    long newDebt = underTest.calculate(issue, changelog, PERIOD);

    assertThat(newDebt).isEqualTo(TEN_DAYS.toMinutes() - TWO_DAYS.toMinutes());
  }

  @Test
  public void new_debt_is_positive() {
    // creation: 1d
    // before period: increased to 10d
    // after period: decreased to 2d
    // -> new debt is 2d - 10d = -8d -> 0d
    issue.setDebt(TWO_DAYS).setCreationDate(new Date(PERIOD_DATE - 10000));
    List<IssueChangeDto> changelog = Arrays.asList(
      newDebtChangelog(ONE_DAY.toMinutes(), TEN_DAYS.toMinutes(), PERIOD_DATE - 9000),
      newDebtChangelog(TEN_DAYS.toMinutes(), TWO_DAYS.toMinutes(), PERIOD_DATE + 30000)
    );

    long newDebt = underTest.calculate(issue, changelog, PERIOD);

    assertThat(newDebt).isEqualTo(0L);
  }

  @Test
  public void guess_initial_debt_when_first_change_is_after_period() {
    // creation: 1d
    // after period: increased to 2d, then to 5d
    // -> new debt is 5d - 1d = 4d
    issue.setDebt(FIVE_DAYS).setCreationDate(new Date(PERIOD_DATE - 10000));
    List<IssueChangeDto> changelog = Arrays.asList(
      newDebtChangelog(ONE_DAY.toMinutes(), TWO_DAYS.toMinutes(), PERIOD_DATE + 20000),
      newDebtChangelog(TWO_DAYS.toMinutes(), FIVE_DAYS.toMinutes(), PERIOD_DATE + 30000)
    );

    long newDebt = underTest.calculate(issue, changelog, PERIOD);

    assertThat(newDebt).isEqualTo(FIVE_DAYS.toMinutes() - ONE_DAY.toMinutes());
  }


  private static IssueChangeDto newDebtChangelog(long previousValue, long value, @Nullable Long date) {
    FieldDiffs diffs = new FieldDiffs().setDiff("technicalDebt", previousValue, value);
    if (date != null) {
      diffs.setCreationDate(new Date(date));
    }
    return new IssueChangeDto().setIssueChangeCreationDate(date).setChangeData(diffs.toString());
  }

}
