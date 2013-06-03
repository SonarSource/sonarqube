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

package org.sonar.api.issue.action;

import org.junit.Test;
import org.sonar.api.issue.condition.Condition;
import org.sonar.api.issue.internal.DefaultIssue;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActionTest {

  Condition condition1 = mock(Condition.class);
  Condition condition2 = mock(Condition.class);
  Function function1 = mock(Function.class);
  Function function2 = mock(Function.class);

  @Test
  public void test_action() throws Exception {
    Action action = new Action("link-to-jira")
      .setConditions(condition1, condition2)
      .setFunctions(function1, function2);

    assertThat(action.key()).isEqualTo("link-to-jira");
    assertThat(action.conditions()).containsOnly(condition1, condition2);
    assertThat(action.functions()).containsOnly(function1, function2);
  }

  @Test
  public void key_should_be_set() throws Exception {
    try {
      new Action("");
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Action key must be set");
    }
  }

  @Test
  public void should_verify_conditions() throws Exception {
    DefaultIssue issue = new DefaultIssue();
    Action action = new Action("link-to-jira")
      .setConditions(condition1, condition2);

    when(condition1.matches(issue)).thenReturn(true);
    when(condition2.matches(issue)).thenReturn(false);
    assertThat(action.supports(issue)).isFalse();

    when(condition1.matches(issue)).thenReturn(true);
    when(condition2.matches(issue)).thenReturn(true);
    assertThat(action.supports(issue)).isTrue();
  }

  @Test
  public void test_equals_and_hashCode() throws Exception {
    Action t1 = new Action("link-to-jira");
    Action t2 = new Action("link-to-jira");
    Action t3 = new Action("comment");

    assertThat(t1).isEqualTo(t1);
    assertThat(t1).isEqualTo(t2);
    assertThat(t1).isNotEqualTo(t3);

    assertThat(t1.hashCode()).isEqualTo(t1.hashCode());
  }

  @Test
  public void test_toString() throws Exception {
    Action t1 = new Action("link-to-jira");
    assertThat(t1.toString()).isEqualTo("link-to-jira");
  }

}
