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
package org.sonar.wsclient.issue;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;


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
      .onComponentOnly(true)
      .resolutions("FIXED", "FALSE-POSITIVE")
      .resolved(true)
      .hideRules(true)
      .rules("squid:AvoidCycle")
      .actionPlans("ABC")
      .statuses("OPEN", "CLOSED")
      .severities("BLOCKER", "INFO")
      .reporters("login1", "login2")
      .languages("xoo", "java")
      .createdAt(df.parse("2015-01-02T05:59:50:50"))
      .createdBefore(df.parse("2015-12-13T05:59:50"))
      .createdAfter(df.parse("2012-01-23T13:40:50"))
      .sort("ASSIGNEE")
      .asc(false)
      .pageSize(5)
      .pageIndex(4);

    assertThat(query.urlParams()).hasSize(22);
    assertThat(query.urlParams()).containsEntry("issues", "ABCDE,FGHIJ");
    assertThat(query.urlParams()).containsEntry("assignees", "arthur,perceval");
    assertThat(query.urlParams()).containsEntry("assigned", true);
    assertThat(query.urlParams()).containsEntry("planned", true);
    assertThat(query.urlParams()).containsEntry("components", "Action.java,Filter.java");
    assertThat(query.urlParams()).containsEntry("onComponentOnly", true);
    assertThat(query.urlParams()).containsEntry("rules", "squid:AvoidCycle");
    assertThat(query.urlParams()).containsEntry("actionPlans", "ABC");
    assertThat(query.urlParams()).containsEntry("resolutions", "FIXED,FALSE-POSITIVE");
    assertThat(query.urlParams()).containsEntry("resolved", true);
    assertThat(query.urlParams()).containsEntry("hideRules", true);
    assertThat(query.urlParams()).containsEntry("statuses", "OPEN,CLOSED");
    assertThat(query.urlParams()).containsEntry("severities", "BLOCKER,INFO");
    assertThat(query.urlParams()).containsEntry("reporters", "login1,login2");
    assertThat(query.urlParams()).containsEntry("languages", "xoo,java");
    assertThat((String) query.urlParams().get("createdBefore")).startsWith("2015-12-13T05:59:50");
    assertThat((String) query.urlParams().get("createdAfter")).startsWith("2012-01-23T13:40:50");
    assertThat((String) query.urlParams().get("createdAt")).startsWith("2015-01-02T05:59:50");
    assertThat(query.urlParams()).containsEntry("sort", "ASSIGNEE");
    assertThat(query.urlParams()).containsEntry("asc", false);
    assertThat(query.urlParams()).containsEntry("pageSize", 5);
    assertThat(query.urlParams()).containsEntry("pageIndex", 4);
  }

  @Test
  public void should_ignore_null_values() {
    IssueQuery query = IssueQuery.create().severities(null);
    assertThat(query.urlParams()).isEmpty();
  }
}
