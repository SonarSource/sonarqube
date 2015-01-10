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
import org.sonar.api.issue.internal.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;

public class IsManualTest {
  DefaultIssue issue = new DefaultIssue();

  @Test
  public void should_match() throws Exception {
    IsManual condition = new IsManual(true);
    assertThat(condition.matches(issue.setReporter("you"))).isTrue();
    assertThat(condition.matches(issue.setReporter(null))).isFalse();
  }

  @Test
  public void should_match_dead() throws Exception {
    IsManual condition = new IsManual(false);
    assertThat(condition.matches(issue.setReporter("you"))).isFalse();
    assertThat(condition.matches(issue.setReporter(null))).isTrue();
  }
}
