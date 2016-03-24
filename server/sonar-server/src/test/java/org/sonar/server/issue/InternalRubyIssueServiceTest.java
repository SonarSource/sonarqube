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
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.Issue;
import org.sonar.api.user.User;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ResourceQuery;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Message;
import org.sonar.server.issue.filter.IssueFilterService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ThreadLocalUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InternalRubyIssueServiceTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  IssueService issueService;

  IssueQueryService issueQueryService;

  IssueCommentService commentService;

  IssueChangelogService changelogService;

  ResourceDao resourceDao;

  IssueFilterService issueFilterService;

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
    issueFilterService = mock(IssueFilterService.class);
    issueBulkChangeService = mock(IssueBulkChangeService.class);
    actionService = mock(ActionService.class);

    ResourceDto project = new ResourceDto().setKey("org.sonar.Sample");
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(project);

    service = new InternalRubyIssueService(issueService, issueQueryService, commentService, changelogService,
      issueFilterService, issueBulkChangeService, actionService, userSessionRule);
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
  public void create_issue_filter() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");

    service.createIssueFilter(parameters);

    ArgumentCaptor<IssueFilterDto> issueFilterCaptor = ArgumentCaptor.forClass(IssueFilterDto.class);
    verify(issueFilterService).save(issueFilterCaptor.capture(), any(ThreadLocalUserSession.class));
    IssueFilterDto issueFilter = issueFilterCaptor.getValue();
    assertThat(issueFilter.getName()).isEqualTo("Long term");
    assertThat(issueFilter.getDescription()).isEqualTo("Long term issues");
  }

  @Test
  public void update_issue_filter() {
    Map<String, String> parameters = newHashMap();
    parameters.put("id", "10");
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("user", "John");

    service.updateIssueFilter(parameters);

    ArgumentCaptor<IssueFilterDto> issueFilterCaptor = ArgumentCaptor.forClass(IssueFilterDto.class);
    verify(issueFilterService).update(issueFilterCaptor.capture(), any(ThreadLocalUserSession.class));
    IssueFilterDto issueFilter = issueFilterCaptor.getValue();
    assertThat(issueFilter.getId()).isEqualTo(10L);
    assertThat(issueFilter.getName()).isEqualTo("Long term");
    assertThat(issueFilter.getDescription()).isEqualTo("Long term issues");
  }

  @Test
  public void update_data() {
    Map<String, Object> data = newHashMap();
    service.updateIssueFilterQuery(10L, data);
    verify(issueFilterService).updateFilterQuery(eq(10L), eq(data), any(ThreadLocalUserSession.class));
  }

  @Test
  public void delete_issue_filter() {
    service.deleteIssueFilter(1L);
    verify(issueFilterService).delete(eq(1L), any(ThreadLocalUserSession.class));
  }

  @Test
  public void copy_issue_filter() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Copy of Long term");
    parameters.put("description", "Copy of Long term issues");

    service.copyIssueFilter(1L, parameters);

    ArgumentCaptor<IssueFilterDto> issueFilterCaptor = ArgumentCaptor.forClass(IssueFilterDto.class);
    verify(issueFilterService).copy(eq(1L), issueFilterCaptor.capture(), any(ThreadLocalUserSession.class));
    IssueFilterDto issueFilter = issueFilterCaptor.getValue();
    assertThat(issueFilter.getName()).isEqualTo("Copy of Long term");
    assertThat(issueFilter.getDescription()).isEqualTo("Copy of Long term issues");
  }

  @Test
  public void get_error_on_create_issue_filter_result_when_no_name() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "");
    parameters.put("description", "Long term issues");
    parameters.put("user", "John");

    try {
      service.createIssueFilterResultForNew(parameters);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "errors.cant_be_empty", "name");
    }
  }

  @Test
  public void get_error_on_create_issue_filter_result_when_name_is_too_long() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", createLongString(101));
    parameters.put("description", "Long term issues");
    parameters.put("user", "John");

    try {
      service.createIssueFilterResultForNew(parameters);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "errors.is_too_long", "name", 100);
    }
  }

  @Test
  public void get_error_on_create_issue_filter_result_when_description_is_too_long() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", createLongString(4001));
    parameters.put("user", "John");

    try {
      service.createIssueFilterResultForNew(parameters);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "errors.is_too_long", "description", 4000);
    }
  }

  @Test
  public void get_error_on_create_issue_filter_result_when_id_is_null_on_update() {
    Map<String, String> parameters = newHashMap();
    parameters.put("id", null);
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("user", "John");

    try {
      service.createIssueFilterResultForUpdate(parameters);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "errors.cant_be_empty", "id");
    }
  }

  @Test
  public void get_error_on_create_issue_filter_result_when_user_is_null_on_update() {
    Map<String, String> parameters = newHashMap();
    parameters.put("id", "10");
    parameters.put("name", "All Open Issues");
    parameters.put("description", "Long term issues");
    parameters.put("user", null);

    try {
      service.createIssueFilterResultForUpdate(parameters);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "errors.cant_be_empty", "user");
    }
  }

  @Test
  public void get_no_error_on_issue_filter_result_when_id_and_user_are_null_on_copy() {
    Map<String, String> parameters = newHashMap();
    parameters.put("id", null);
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("user", null);

    IssueFilterDto result = service.createIssueFilterResultForCopy(parameters);
    assertThat(result).isNotNull();
  }

  @Test
  public void execute_issue_filter_from_issue_query() {
    service.execute(Maps.<String, Object>newHashMap());
    verify(issueFilterService).execute(any(IssueQuery.class), any(SearchOptions.class));
  }

  @Test
  public void execute_issue_filter_from_existing_filter() {
    Map<String, Object> props = newHashMap();
    props.put("componentRoots", "struts");
    props.put("statuses", "OPEN");
    when(issueFilterService.deserializeIssueFilterQuery(any(IssueFilterDto.class))).thenReturn(props);

    Map<String, Object> overrideProps = newHashMap();
    overrideProps.put("statuses", "CLOSED");
    overrideProps.put("resolved", true);
    overrideProps.put("pageSize", 20);
    overrideProps.put("pageIndex", 2);

    IssueQuery query = IssueQuery.builder(userSessionRule).build();
    when(issueQueryService.createFromMap(eq(overrideProps))).thenReturn(query);

    service.execute(10L, overrideProps);

    ArgumentCaptor<IssueQuery> issueQueryArgumentCaptor = ArgumentCaptor.forClass(IssueQuery.class);
    ArgumentCaptor<SearchOptions> contextArgumentCaptor = ArgumentCaptor.forClass(SearchOptions.class);

    verify(issueFilterService).execute(issueQueryArgumentCaptor.capture(), contextArgumentCaptor.capture());
    verify(issueFilterService).find(eq(10L), any(ThreadLocalUserSession.class));

    SearchOptions searchOptions = contextArgumentCaptor.getValue();
    assertThat(searchOptions.getLimit()).isEqualTo(20);
    assertThat(searchOptions.getPage()).isEqualTo(2);
  }

  @Test
  public void serialize_filter_query() {
    Map<String, Object> props = newHashMap();
    props.put("componentRoots", "struts");
    service.serializeFilterQuery(props);
    verify(issueFilterService).serializeFilterQuery(props);
  }

  @Test
  public void deserialize_filter_query() {
    IssueFilterDto issueFilter = new IssueFilterDto();
    service.deserializeFilterQuery(issueFilter);
    verify(issueFilterService).deserializeIssueFilterQuery(issueFilter);
  }

  @Test
  public void sanitize_filter_query() {
    Map<String, Object> query = newHashMap();
    query.put("statuses", "CLOSED");
    query.put("resolved", true);
    query.put("unknown", "john");
    Map<String, Object> result = service.sanitizeFilterQuery(query);
    assertThat(result.keySet()).containsOnly("statuses", "resolved");
  }

  @Test
  public void find_user_issue_filters() {
    service.findIssueFiltersForCurrentUser();
    verify(issueFilterService).findByUser(any(ThreadLocalUserSession.class));
  }

  @Test
  public void find_shared_issue_filters() {
    service.findSharedFiltersForCurrentUser();
    verify(issueFilterService).findSharedFiltersWithoutUserFilters(any(ThreadLocalUserSession.class));
  }

  @Test
  public void find_favourite_issue_filters() {
    service.findFavouriteIssueFiltersForCurrentUser();
    verify(issueFilterService).findFavoriteFilters(any(ThreadLocalUserSession.class));
  }

  @Test
  public void toggle_favourite_issue_filter() {
    service.toggleFavouriteIssueFilter(10L);
    verify(issueFilterService).toggleFavouriteIssueFilter(eq(10L), any(ThreadLocalUserSession.class));
  }

  @Test
  public void check_if_user_is_authorized_to_see_issue_filter() {
    IssueFilterDto issueFilter = new IssueFilterDto();
    service.isUserAuthorized(issueFilter);
    verify(issueFilterService).getLoggedLogin(any(ThreadLocalUserSession.class));
    verify(issueFilterService).verifyCurrentUserCanReadFilter(eq(issueFilter), anyString());
  }

  @Test
  public void check_if_user_can_share_issue_filter() {
    service.canUserShareIssueFilter();
    verify(issueFilterService).canShareFilter(any(ThreadLocalUserSession.class));
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
