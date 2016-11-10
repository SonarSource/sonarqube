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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.user.User;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ResourceQuery;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Message;
import org.sonar.server.issue.index.IssueIndex;
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

  IssueIndex issueIndex = mock(IssueIndex.class);

  IssueService issueService;

  IssueQueryService issueQueryService;

  IssueCommentService commentService;

  IssueChangelogService changelogService;

  ResourceDao resourceDao;

  IssueBulkChangeService issueBulkChangeService;

  ActionService actionService;

  InternalRubyIssueService service;

  @Before
  public void setUp() {
    issueService = mock(IssueService.class);
    issueQueryService = mock(IssueQueryService.class);
    commentService = mock(IssueCommentService.class);
    changelogService = mock(IssueChangelogService.class);
    resourceDao = mock(ResourceDao.class);
    issueBulkChangeService = mock(IssueBulkChangeService.class);
    actionService = mock(ActionService.class);

    ResourceDto project = new ResourceDto().setKey("org.sonar.Sample");
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(project);

    service = new InternalRubyIssueService(issueIndex, issueService, issueQueryService, commentService, changelogService,
      issueBulkChangeService, actionService, userSessionRule);
  }

  @Test
  public void list_transitions_by_issue_key() {
    service.listTransitions("ABCD");
    verify(issueService).listTransitions(eq("ABCD"));
  }

  @Test
  public void list_transitions_by_issue() {
    Issue issue = new DefaultIssue().setKey("ABCD");
    service.listTransitions(issue);
    verify(issueService).listTransitions(eq(issue));
  }

  @Test
  public void list_status() {
    service.listStatus();
    verify(issueService).listStatus();
  }

  @Test
  public void list_resolutions() {
    assertThat(service.listResolutions()).isEqualTo(Issue.RESOLUTIONS);
  }

  @Test
  public void find_comments_by_issue_key() {
    service.findComments("ABCD");
    verify(commentService).findComments("ABCD");
  }

  @Test
  public void find_comments_by_issue_keys() {
    service.findCommentsByIssueKeys(newArrayList("ABCD"));
    verify(commentService).findComments(newArrayList("ABCD"));
  }

  @Test
  public void test_changelog_from_issue_key() throws Exception {
    IssueChangelog changelog = new IssueChangelog(Collections.<FieldDiffs>emptyList(), Collections.<User>emptyList());
    when(changelogService.changelog(eq("ABCDE"))).thenReturn(changelog);

    IssueChangelog result = service.changelog("ABCDE");

    assertThat(result).isSameAs(changelog);
  }

  @Test
  public void test_changelog_from_issue() throws Exception {
    Issue issue = new DefaultIssue().setKey("ABCDE");

    IssueChangelog changelog = new IssueChangelog(Collections.<FieldDiffs>emptyList(), Collections.<User>emptyList());
    when(changelogService.changelog(eq(issue))).thenReturn(changelog);

    IssueChangelog result = service.changelog(issue);

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
    service.bulkChange(params, "My comment", true);
    verify(issueBulkChangeService).execute(any(IssueBulkChangeQuery.class), any(ThreadLocalUserSession.class));
  }

  @Test
  public void max_query_size() {
    assertThat(service.maxPageSize()).isEqualTo(500);
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

  @Test
  public void list_tags() {
    List<String> tags = Arrays.asList("tag1", "tag2", "tag3");
    when(issueService.listTags(null, 0)).thenReturn(tags);
    assertThat(service.listTags()).isEqualTo(tags);
  }

  @Test
  public void list_tags_for_component() {
    Map<String, Long> tags = ImmutableMap.of("tag1", 1L, "tag2", 2L, "tag3", 3L);
    int pageSize = 42;
    IssueQuery query = IssueQuery.builder(userSessionRule).build();
    String componentUuid = "polop";
    Map<String, Object> params = ImmutableMap.<String, Object>of("componentUuids", componentUuid, "resolved", false);
    when(issueQueryService.createFromMap(params)).thenReturn(query);
    when(issueService.listTagsForComponent(query, pageSize)).thenReturn(tags);
    assertThat(service.listTagsForComponent(componentUuid, pageSize)).isEqualTo(tags);
  }

  @Test
  public void is_user_issue_admin() {
    userSessionRule.addProjectUuidPermissions(UserRole.ISSUE_ADMIN, "bcde");
    assertThat(service.isUserIssueAdmin("abcd")).isFalse();
    assertThat(service.isUserIssueAdmin("bcde")).isTrue();
  }

  private void checkBadRequestException(Exception e, String key, Object... params) {
    BadRequestException exception = (BadRequestException) e;
    Message msg = exception.errors().messages().get(0);
    assertThat(msg.getKey()).isEqualTo(key);
    assertThat(msg.getParams()).containsOnly(params);
  }

  private String createLongString(int size) {
    String result = "";
    for (int i = 0; i < size; i++) {
      result += "c";
    }
    return result;
  }

}
