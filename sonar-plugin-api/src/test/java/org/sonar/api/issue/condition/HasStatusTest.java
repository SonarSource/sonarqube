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

import org.junit.Test;
import org.sonar.api.issue.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HasStatusTest {

  Issue issue = mock(Issue.class);

  @Test
  public void should_match() {
    HasStatus condition = new HasStatus("OPEN", "REOPENED", "CONFIRMED");

    when(issue.status()).thenReturn("OPEN");
    assertThat(condition.matches(issue)).isTrue();

    when(issue.status()).thenReturn("REOPENED");
    assertThat(condition.matches(issue)).isTrue();

    when(issue.status()).thenReturn("CONFIRMED");
    assertThat(condition.matches(issue)).isTrue();

    when(issue.status()).thenReturn("open");
    assertThat(condition.matches(issue)).isFalse();

    when(issue.status()).thenReturn("CLOSED");
    assertThat(condition.matches(issue)).isFalse();
  }

}
