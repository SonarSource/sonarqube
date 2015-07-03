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

package org.sonar.core.issue;

import java.util.Date;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.sonar.api.issue.ActionPlan;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionPlanStatsTest {

  @Test
  public void test_over_due() throws Exception {
    Date yesterday = DateUtils.addDays(new Date(), -1);
    Date tomorrow = DateUtils.addDays(new Date(), 1);

    assertThat(((ActionPlanStats) ActionPlanStats.create("Short term").setStatus(ActionPlan.STATUS_OPEN).setDeadLine(tomorrow)).overDue()).isFalse();
    assertThat(((ActionPlanStats) ActionPlanStats.create("Short term").setStatus(ActionPlan.STATUS_OPEN).setDeadLine(yesterday)).overDue()).isTrue();
    assertThat(((ActionPlanStats) ActionPlanStats.create("Short term").setStatus(ActionPlan.STATUS_CLOSED).setDeadLine(tomorrow)).overDue()).isFalse();
    assertThat(((ActionPlanStats) ActionPlanStats.create("Short term").setStatus(ActionPlan.STATUS_CLOSED).setDeadLine(yesterday)).overDue()).isFalse();
    assertThat(((ActionPlanStats) ActionPlanStats.create("Short term").setStatus(ActionPlan.STATUS_CLOSED)).overDue()).isFalse();
  }
}
