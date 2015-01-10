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
package org.sonar.api.issue.condition;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class NotConditionTest {

  Condition target = Mockito.mock(Condition.class);

  @Test
  public void should_match_opposite() throws Exception {
    NotCondition condition = new NotCondition(target);

    when(target.matches(any(Issue.class))).thenReturn(true);
    assertThat(condition.matches(new DefaultIssue())).isFalse();

    when(target.matches(any(Issue.class))).thenReturn(false);
    assertThat(condition.matches(new DefaultIssue())).isTrue();
  }
}
