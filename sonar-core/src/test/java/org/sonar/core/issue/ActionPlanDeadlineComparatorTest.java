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

import org.junit.Test;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.utils.DateUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionPlanDeadlineComparatorTest {

  ActionPlan shortTerm = new DefaultActionPlan().setDeadLine(DateUtils.parseDate("2013-12-01"));
  ActionPlan longTerm = new DefaultActionPlan().setDeadLine(DateUtils.parseDate("2018-05-05"));
  ActionPlan noDeadline = new DefaultActionPlan().setDeadLine(null);

  @Test
  public void compare_plans_with_deadlines() throws Exception {
    List<ActionPlan> plans = Arrays.asList(shortTerm, longTerm);
    Collections.sort(plans, new ActionPlanDeadlineComparator());
    assertThat(plans).containsSequence(shortTerm, longTerm);

    plans = Arrays.asList(longTerm, shortTerm);
    Collections.sort(plans, new ActionPlanDeadlineComparator());
    assertThat(plans).containsSequence(shortTerm, longTerm);
  }

  @Test
  public void end_with_plans_without_deadline() throws Exception {
    List<ActionPlan> plans = Arrays.asList(noDeadline, longTerm, shortTerm);
    Collections.sort(plans, new ActionPlanDeadlineComparator());
    assertThat(plans).containsSequence(shortTerm, longTerm, noDeadline);

    plans = Arrays.asList(longTerm, noDeadline, shortTerm);
    Collections.sort(plans, new ActionPlanDeadlineComparator());
    assertThat(plans).containsSequence(shortTerm, longTerm, noDeadline);
  }
}
