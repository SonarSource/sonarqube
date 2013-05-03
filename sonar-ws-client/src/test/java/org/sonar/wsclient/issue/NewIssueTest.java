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
package org.sonar.wsclient.issue;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class NewIssueTest {
  @Test
  public void test_create_empty() {
    assertThat(NewIssue.create().urlParams()).isEmpty();
  }

  @Test
  public void test_create() {
    NewIssue newIssue = NewIssue.create()
      .component("Action.java")
      .effortToFix(4.2)
      .description("the desc")
      .line(123)
      .rule("squid:AvoidCycle")
      .severity("BLOCKER")
      .attribute("JIRA", "FOO-123");

    assertThat(newIssue.urlParams()).hasSize(7).includes(
      entry("component", "Action.java"),
      entry("cost", 4.2),
      entry("desc", "the desc"),
      entry("line", 123),
      entry("rule", "squid:AvoidCycle"),
      entry("severity", "BLOCKER"),
      entry("attr[JIRA]", "FOO-123")
    );
  }
}
