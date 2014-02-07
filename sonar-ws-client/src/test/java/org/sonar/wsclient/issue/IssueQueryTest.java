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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class IssueQueryTest {
  @Test
  public void get_all_issues() {
    IssueQuery query = IssueQuery.create();
    assertThat(query.urlParams()).isEmpty();
  }

  @Test
  public void get_all_issues_by_parameter() throws ParseException {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    IssueQuery query = IssueQuery.create()
      .issues("ABCDE", "FGHIJ")
      .assignees("arthur", "perceval")
      .assigned(true)
      .planned(true)
      .components("Action.java", "Filter.java")
      .componentRoots("struts")
      .resolutions("FIXED", "FALSE-POSITIVE")
      .resolved(true)
      .hideRules(true)
      .rules("squid:AvoidCycle")
      .actionPlans("ABC")
      .statuses("OPEN", "CLOSED")
      .severities("BLOCKER", "INFO")
      .reporters("login1", "login2")
      .createdAt(df.parse("2015-01-02T05:59:50:50"))
      .createdBefore(df.parse("2015-12-13T05:59:50"))
      .createdAfter(df.parse("2012-01-23T13:40:50"))
      .sort("ASSIGNEE")
      .asc(false)
      .pageSize(5)
      .pageIndex(4);

    assertThat(query.urlParams()).hasSize(21);
    assertThat(query.urlParams()).includes(entry("issues", "ABCDE,FGHIJ"));
    assertThat(query.urlParams()).includes(entry("assignees", "arthur,perceval"));
    assertThat(query.urlParams()).includes(entry("assigned", true));
    assertThat(query.urlParams()).includes(entry("planned", true));
    assertThat(query.urlParams()).includes(entry("components", "Action.java,Filter.java"));
    assertThat(query.urlParams()).includes(entry("componentRoots", "struts"));
    assertThat(query.urlParams()).includes(entry("rules", "squid:AvoidCycle"));
    assertThat(query.urlParams()).includes(entry("actionPlans", "ABC"));
    assertThat(query.urlParams()).includes(entry("resolutions", "FIXED,FALSE-POSITIVE"));
    assertThat(query.urlParams()).includes(entry("resolved", true));
    assertThat(query.urlParams()).includes(entry("hideRules", true));
    assertThat(query.urlParams()).includes(entry("statuses", "OPEN,CLOSED"));
    assertThat(query.urlParams()).includes(entry("severities", "BLOCKER,INFO"));
    assertThat(query.urlParams()).includes(entry("reporters", "login1,login2"));
    assertThat((String)query.urlParams().get("createdBefore")).startsWith("2015-12-13T05:59:50");
    assertThat((String)query.urlParams().get("createdAfter")).startsWith("2012-01-23T13:40:50");
    assertThat((String)query.urlParams().get("createdAt")).startsWith("2015-01-02T05:59:50");
    assertThat(query.urlParams()).includes(entry("sort", "ASSIGNEE"));
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
