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

public class IssueQueryTest {
  @Test
  public void get_all_issues() {
    IssueQuery query = IssueQuery.create();
    assertThat(query.urlParams()).isEmpty();
  }

  @Test
  public void get_all_issues_by_parameter() {
    IssueQuery query = IssueQuery.create()
      .issues("ABCDE", "FGHIJ")
      .assignees("arthur", "perceval")
      .assigned(true)
      .components("Action.java", "Filter.java")
      .componentRoots("struts")
      .resolutions("FIXED", "FALSE-POSITIVE")
      .rules("squid:AvoidCycle")
      .statuses("OPEN", "CLOSED")
      .severities("BLOCKER", "INFO")
      .userLogins("login1", "login2")
      .sort("assignee")
      .asc(false)
      .pageSize(5)
      .pageIndex(4);

    assertThat(query.urlParams()).hasSize(14);
    assertThat(query.urlParams()).includes(entry("issues", "ABCDE,FGHIJ"));
    assertThat(query.urlParams()).includes(entry("assignees", "arthur,perceval"));
    assertThat(query.urlParams()).includes(entry("assigned", true));
    assertThat(query.urlParams()).includes(entry("components", "Action.java,Filter.java"));
    assertThat(query.urlParams()).includes(entry("componentRoots", "struts"));
    assertThat(query.urlParams()).includes(entry("resolutions", "FIXED,FALSE-POSITIVE"));
    assertThat(query.urlParams()).includes(entry("statuses", "OPEN,CLOSED"));
    assertThat(query.urlParams()).includes(entry("severities", "BLOCKER,INFO"));
    assertThat(query.urlParams()).includes(entry("userLogins", "login1,login2"));
    assertThat(query.urlParams()).includes(entry("sort", "assignee"));
    assertThat(query.urlParams()).includes(entry("asc", false));
    assertThat(query.urlParams()).includes(entry("pageSize", 5));
    assertThat(query.urlParams()).includes(entry("pageIndex", 4));
  }

  @Test
  public void should_ignore_null_values() {
    IssueQuery query = IssueQuery.create().severities(null);
    assertThat(query.urlParams()).isEmpty();
  }
}
