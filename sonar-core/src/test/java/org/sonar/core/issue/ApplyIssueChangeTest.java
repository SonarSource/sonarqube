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
package org.sonar.core.issue;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.api.rule.Severity;

import static org.fest.assertions.Assertions.assertThat;

public class ApplyIssueChangeTest {

  @Test
  public void should_change_fields() throws Exception {
    DefaultIssue issue = new DefaultIssue().setComponentKey("org/struts/Action.java").setKey("ABCDE");
    ApplyIssueChange.apply(issue, IssueChange.create()
      .setLine(200)
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setAttribute("JIRA", "FOO-123")
      .setManualSeverity(true)
      .setSeverity(Severity.CRITICAL)
      .setAssignee("arthur")
      .setTitle("new title")
      .setDescription("new desc")
      .setCost(4.2)
    );
    assertThat(issue.line()).isEqualTo(200);
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(issue.title()).isEqualTo("new title");
    assertThat(issue.description()).isEqualTo("new desc");
    assertThat(issue.attribute("JIRA")).isEqualTo("FOO-123");
    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issue.assignee()).isEqualTo("arthur");
    assertThat(issue.cost()).isEqualTo(4.2);
  }

  @Test
  public void should_not_touch_fields() throws Exception {
    DefaultIssue issue = new DefaultIssue()
      .setComponentKey("org/struts/Action.java")
      .setKey("ABCDE")
      .setLine(123)
      .setTitle("the title")
      .setDescription("the desc")
      .setAssignee("karadoc")
      .setCost(4.2)
      .setAttribute("JIRA", "FOO-123")
      .setManualSeverity(true)
      .setSeverity("BLOCKER")
      .setStatus("CLOSED")
      .setResolution("FIXED");
    ApplyIssueChange.apply(issue, IssueChange.create());

    assertThat(issue.componentKey()).isEqualTo("org/struts/Action.java");
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.line()).isEqualTo(123);
    assertThat(issue.resolution()).isEqualTo("FIXED");
    assertThat(issue.attribute("JIRA")).isEqualTo("FOO-123");
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.assignee()).isEqualTo("karadoc");
    assertThat(issue.cost()).isEqualTo(4.2);
    assertThat(issue.isManualSeverity()).isTrue();
    assertThat(issue.description()).isEqualTo("the desc");
    assertThat(issue.title()).isEqualTo("the title");
  }
}
