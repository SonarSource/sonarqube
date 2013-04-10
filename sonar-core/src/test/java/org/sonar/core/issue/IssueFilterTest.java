/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class IssueFilterTest {

  private IssueFilter issueFilter;
  private IssueFinder issueFinder;

  @Before
  public void before() {
    issueFinder = mock(IssueFinder.class);
    issueFilter = new IssueFilter(issueFinder);
  }

  @Test
  public void should_call_find() {
    Map<String, String> map = newHashMap();
    issueFilter.execute(map);
    verify(issueFinder).find(any(IssueQuery.class));
  }

  @Test
  public void should_call_find_by_key() {
    issueFilter.execute("key");
    verify(issueFinder).findByKey("key");
  }

  @Test
  public void should_not_call_find_by_key_with_empty_key() {
    Issue issue = issueFilter.execute("");
    assertThat(issue).isNull();
    verify(issueFinder, never()).findByKey(anyString());
  }

  @Test
  public void should_create_empty_issue_query() {
    Map<String, String> map = newHashMap();
    IssueQuery issueQuery = issueFilter.createIssueQuery(map);
    assertThat(issueQuery.componentKeys()).isEmpty();
  }

  @Test
  public void should_create_empty_issue_query_if_value_is_null() {
    Map<String, String> map = newHashMap();
    map.put("components", null);
    IssueQuery issueQuery = issueFilter.createIssueQuery(map);
    assertThat(issueQuery.componentKeys()).isEmpty();
  }

  @Test
  public void should_create_issue_query() {
    Map<String, String> map = newHashMap();
    map.put("keys", "keys");
    map.put("severities", "severities");
    map.put("minSeverity", "MINOR");
    map.put("status", "status");
    map.put("resolutions", "resolutions");
    map.put("components", "key");
    map.put("rules", "rules");
    map.put("userLogins", "userLogins");
    map.put("assigneeLogins", "assigneeLogins");
    map.put("limit", "1");

    IssueQuery issueQuery = issueFilter.createIssueQuery(map);
    assertThat(issueQuery.keys()).isNotEmpty();
    assertThat(issueQuery.severities()).isNotEmpty();
    assertThat(issueQuery.minSeverity()).isEqualTo("MINOR");
    assertThat(issueQuery.status()).isNotEmpty();
    assertThat(issueQuery.resolutions()).isNotEmpty();
    assertThat(issueQuery.rules()).isNotEmpty();
    assertThat(issueQuery.userLogins()).isNotEmpty();
    assertThat(issueQuery.assigneeLogins()).isNotEmpty();
    assertThat(issueQuery.limit()).isEqualTo(1);
  }

  @Test
  public void should_split_property_list() {
    Map<String, String> map = newHashMap();
    map.put("components", "key1,key2");
    IssueQuery issueQuery = issueFilter.createIssueQuery(map);
    List<String> components = issueQuery.componentKeys();
    assertThat(components).hasSize(2);
    assertThat(components).containsOnly("key1", "key2");
  }

}
