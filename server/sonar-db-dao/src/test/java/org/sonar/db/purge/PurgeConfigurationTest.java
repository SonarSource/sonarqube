/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.purge;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class PurgeConfigurationTest {
  @Test
  void should_delete_all_closed_issues() {
    PurgeConfiguration conf = new PurgeConfiguration("root", "project", 0, Optional.empty(), System2.INSTANCE, emptySet(), 0);
    assertThat(conf.maxLiveDateOfClosedIssues()).isNull();

    conf = new PurgeConfiguration("root", "project", -1, Optional.empty(), System2.INSTANCE, emptySet(), 0);
    assertThat(conf.maxLiveDateOfClosedIssues()).isNull();
  }

  @Test
  void should_delete_only_old_closed_issues() {
    Date now = DateUtils.parseDate("2013-05-18");

    PurgeConfiguration conf = new PurgeConfiguration("root", "project", 30, Optional.empty(), System2.INSTANCE, emptySet(), 0);
    Date toDate = conf.maxLiveDateOfClosedIssues(now);

    assertThat(toDate.getYear()).isEqualTo(113);// =2013
    assertThat(toDate.getMonth()).isEqualTo(3); // means April
    assertThat(toDate.getDate()).isEqualTo(18);
  }

  @Test
  void should_have_empty_branch_purge_date() {
    PurgeConfiguration conf = new PurgeConfiguration("root", "project", 30, Optional.of(10), System2.INSTANCE, emptySet(), 0);
    assertThat(conf.maxLiveDateOfInactiveBranches()).isNotEmpty();
    long tenDaysAgo = DateUtils.addDays(new Date(System2.INSTANCE.now()), -10).getTime();
    assertThat(conf.maxLiveDateOfInactiveBranches().get().getTime()).isBetween(tenDaysAgo - 5000, tenDaysAgo + 5000);
  }

  @Test
  void should_calculate_branch_purge_date() {
    PurgeConfiguration conf = new PurgeConfiguration("root", "project", 30, Optional.empty(), System2.INSTANCE, emptySet(), 0);
    assertThat(conf.maxLiveDateOfInactiveBranches()).isEmpty();
  }

  @Test
  void should_delete_only_old_anticipated_transitions() {
    int anticipatedTransitionMaxAge = 30;
    TestSystem2 system2 = new TestSystem2();
    system2.setNow(Instant.now().toEpochMilli());
    PurgeConfiguration conf = new PurgeConfiguration("root", "project", 30, Optional.empty(), system2, emptySet(),
      anticipatedTransitionMaxAge);

    Instant toDate = conf.maxLiveDateOfAnticipatedTransitions();

    assertThat(toDate)
      .isBeforeOrEqualTo(Instant.now().minus(30, ChronoUnit.DAYS))
      .isAfter(Instant.now().minus(31, ChronoUnit.DAYS));
  }

}
