/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.issue;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.user.User;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ThreadLocalUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InternalRubyIssueServiceTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  IssueCommentService commentService = mock(IssueCommentService.class);
  IssueChangelogService changelogService = mock(IssueChangelogService.class);
  IssueBulkChangeService issueBulkChangeService = mock(IssueBulkChangeService.class);

  InternalRubyIssueService underTest = new InternalRubyIssueService(commentService, changelogService, issueBulkChangeService, userSessionRule);

  @Test
  public void test_changelog_from_issue_key() throws Exception {
    IssueChangelog changelog = new IssueChangelog(Collections.<FieldDiffs>emptyList(), Collections.<User>emptyList());
    when(changelogService.changelog(eq("ABCDE"))).thenReturn(changelog);

    IssueChangelog result = underTest.changelog("ABCDE");

    assertThat(result).isSameAs(changelog);
  }

  @Test
  public void execute_bulk_change() {
    Map<String, Object> params = newHashMap();
    params.put("issues", newArrayList("ABCD", "EFGH"));
    params.put("actions", newArrayList("do_transition", "assign", "set_severity", "plan"));
    params.put("do_transition.transition", "confirm");
    params.put("assign.assignee", "arthur");
    params.put("set_severity.severity", "MINOR");
    params.put("plan.plan", "3.7");

    underTest.bulkChange(params, "My comment", true);

    verify(issueBulkChangeService).execute(any(IssueBulkChangeQuery.class), any(ThreadLocalUserSession.class));
  }

  @Test
  public void create_context_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("pageSize", 10l);
    map.put("pageIndex", 50);
    SearchOptions searchOptions = InternalRubyIssueService.toSearchOptions(map);
    assertThat(searchOptions.getLimit()).isEqualTo(10);
    assertThat(searchOptions.getPage()).isEqualTo(50);

    map = newHashMap();
    map.put("pageSize", -1);
    map.put("pageIndex", 50);
    searchOptions = InternalRubyIssueService.toSearchOptions(map);
    assertThat(searchOptions.getLimit()).isEqualTo(500);
    assertThat(searchOptions.getPage()).isEqualTo(1);

    searchOptions = InternalRubyIssueService.toSearchOptions(Maps.<String, Object>newHashMap());
    assertThat(searchOptions.getLimit()).isEqualTo(100);
    assertThat(searchOptions.getPage()).isEqualTo(1);
  }
}
