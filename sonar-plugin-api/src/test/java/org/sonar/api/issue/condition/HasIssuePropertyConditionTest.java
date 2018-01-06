/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.issue.condition;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HasIssuePropertyConditionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  Issue issue = mock(Issue.class);

  @Test
  public void should_match() {
    HasIssuePropertyCondition condition = new HasIssuePropertyCondition("foo");

    assertThat(condition.matches(issue)).isFalse();

    when(issue.attribute("foo")).thenReturn("");
    assertThat(condition.matches(issue)).isFalse();

    when(issue.attribute("foo")).thenReturn("bar");
    assertThat(condition.matches(issue)).isTrue();
  }

  @Test
  public void should_get_property_key() {
    HasIssuePropertyCondition condition = new HasIssuePropertyCondition("foo");
    assertThat(condition.getPropertyKey()).isEqualTo("foo");
  }

  @Test
  public void shoul_fail_if_null_property() {
    thrown.expect(IllegalArgumentException.class);
    new HasIssuePropertyCondition(null);
  }

  @Test
  public void should_fail_if_empty_property() {
    thrown.expect(IllegalArgumentException.class);
    new HasIssuePropertyCondition("");
  }

}
