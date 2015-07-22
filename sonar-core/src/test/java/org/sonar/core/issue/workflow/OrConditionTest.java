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
package org.sonar.core.issue.workflow;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.Condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OrConditionTest {

  private static final Condition TRUE_CONDITION = new BooleanCondition(true);
  private static final Condition FALSE_CONDITION = new BooleanCondition(false);
  Issue issue = mock(Issue.class);

  @Test
  public void match() {
    assertThat(new OrCondition(TRUE_CONDITION).matches(issue)).isTrue();
    assertThat(new OrCondition(FALSE_CONDITION).matches(issue)).isFalse();
    assertThat(new OrCondition(FALSE_CONDITION, TRUE_CONDITION).matches(issue)).isTrue();
    assertThat(new OrCondition(FALSE_CONDITION, FALSE_CONDITION).matches(issue)).isFalse();
  }

  private static class BooleanCondition implements Condition {
    private final boolean b;

    public BooleanCondition(boolean b) {
      this.b = b;
    }

    @Override
    public boolean matches(Issue issue) {
      return b;
    }
  }
}
