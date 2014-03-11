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
import org.sonar.api.issue.internal.DefaultIssue;

import static org.fest.assertions.Assertions.assertThat;

public class IssuesBySeverityTest {

  @Test
  public void add_issue(){
    IssuesBySeverity issuesBySeverity = new IssuesBySeverity();
    issuesBySeverity.add(new DefaultIssue().setSeverity("MINOR"));
    assertThat(issuesBySeverity.size()).isEqualTo(1);
  }

  @Test
  public void get_issues_by_severity(){
    IssuesBySeverity issuesBySeverity = new IssuesBySeverity();
    issuesBySeverity.add(new DefaultIssue().setSeverity("MINOR"));
    issuesBySeverity.add(new DefaultIssue().setSeverity("MINOR"));
    issuesBySeverity.add(new DefaultIssue().setSeverity("MAJOR"));
    assertThat(issuesBySeverity.issues("MINOR")).isEqualTo(2);
    assertThat(issuesBySeverity.issues("MAJOR")).isEqualTo(1);
  }

  @Test
  public void get_zero_issues_on_empty_severity(){
    IssuesBySeverity issuesBySeverity = new IssuesBySeverity();
    issuesBySeverity.add(new DefaultIssue().setSeverity("MAJOR"));
    assertThat(issuesBySeverity.issues("MINOR")).isEqualTo(0);
  }
}
