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

package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.user.User;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.server.user.UserSession;

import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class InternalRubyIssueServiceTest {

  InternalRubyIssueService service;
  IssueService issueService = mock(IssueService.class);
  IssueCommentService commentService = mock(IssueCommentService.class);
  IssueChangelogService changelogService = mock(IssueChangelogService.class);
  ActionPlanService actionPlanService = mock(ActionPlanService.class);
  ResourceDao resourceDao = mock(ResourceDao.class);
  IssueStatsFinder issueStatsFinder = mock(IssueStatsFinder.class);
  ActionService actionService = mock(ActionService.class);
  IssueFilterService issueFilterService = mock(IssueFilterService.class);

  @Before
  public void setUp() {
    ResourceDto project = new ResourceDto().setKey("org.sonar.Sample");
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(project);
    service = new InternalRubyIssueService(issueService, commentService, changelogService, actionPlanService, issueStatsFinder, resourceDao, actionService, issueFilterService);
  }

  @Test
  public void should_create_action_plan() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("project", "org.sonar.Sample");
    parameters.put("deadLine", "2113-05-13");

    Result result = service.createActionPlan(parameters);
    assertThat(result.ok()).isTrue();

    ArgumentCaptor<ActionPlan> actionPlanCaptor = ArgumentCaptor.forClass(ActionPlan.class);
    verify(actionPlanService).create(actionPlanCaptor.capture(), any(UserSession.class));
    ActionPlan actionPlan = actionPlanCaptor.getValue();

    assertThat(actionPlan).isNotNull();
    assertThat(actionPlan.key()).isNotNull();
    assertThat(actionPlan.name()).isEqualTo("Long term");
    assertThat(actionPlan.description()).isEqualTo("Long term issues");
    assertThat(actionPlan.deadLine()).isNotNull();
  }

  @Test
  public void should_update_action_plan() {
    when(actionPlanService.findByKey(eq("ABCD"), any(UserSession.class))).thenReturn(DefaultActionPlan.create("Long term"));

    Map<String, String> parameters = newHashMap();
    parameters.put("name", "New Long term");
    parameters.put("description", "New Long term issues");
    parameters.put("deadLine", "2113-05-13");
    parameters.put("project", "org.sonar.MultiSample");

    Result result = service.updateActionPlan("ABCD", parameters);
    assertThat(result.ok()).isTrue();

    ArgumentCaptor<ActionPlan> actionPlanCaptor = ArgumentCaptor.forClass(ActionPlan.class);
    verify(actionPlanService).update(actionPlanCaptor.capture(), any(UserSession.class));
    ActionPlan actionPlan = actionPlanCaptor.getValue();

    assertThat(actionPlan).isNotNull();
    assertThat(actionPlan.key()).isNotNull();
    assertThat(actionPlan.name()).isEqualTo("New Long term");
    assertThat(actionPlan.description()).isEqualTo("New Long term issues");
    assertThat(actionPlan.deadLine()).isNotNull();
  }

  @Test
  public void should_update_action_plan_with_new_project() {
    when(actionPlanService.findByKey(eq("ABCD"), any(UserSession.class))).thenReturn(DefaultActionPlan.create("Long term").setProjectKey("org.sonar.MultiSample"));

    Map<String, String> parameters = newHashMap();
    parameters.put("name", "New Long term");
    parameters.put("description", "New Long term issues");
    parameters.put("deadLine", "2113-05-13");

    ArgumentCaptor<ActionPlan> actionPlanCaptor = ArgumentCaptor.forClass(ActionPlan.class);
    Result result = service.updateActionPlan("ABCD", parameters);
    assertThat(result.ok()).isTrue();

    verify(actionPlanService).update(actionPlanCaptor.capture(), any(UserSession.class));
    ActionPlan actionPlan = actionPlanCaptor.getValue();

    assertThat(actionPlan).isNotNull();
    assertThat(actionPlan.key()).isNotNull();
    assertThat(actionPlan.name()).isEqualTo("New Long term");
    assertThat(actionPlan.description()).isEqualTo("New Long term issues");
    assertThat(actionPlan.deadLine()).isNotNull();
    assertThat(actionPlan.projectKey()).isEqualTo("org.sonar.MultiSample");
  }

  @Test
  public void should_not_update_action_plan_when_action_plan_is_not_found() {
    when(actionPlanService.findByKey(eq("ABCD"), any(UserSession.class))).thenReturn(null);

    Result result = service.updateActionPlan("ABCD", null);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("action_plans.errors.action_plan_does_not_exist", "ABCD"));
  }

  @Test
  public void should_delete_action_plan() {
    when(actionPlanService.findByKey(eq("ABCD"), any(UserSession.class))).thenReturn(DefaultActionPlan.create("Long term"));

    Result result = service.deleteActionPlan("ABCD");
    verify(actionPlanService).delete(eq("ABCD"), any(UserSession.class));
    assertThat(result.ok()).isTrue();
  }

  @Test
  public void should_not_delete_action_plan_if_action_plan_not_found() {
    when(actionPlanService.findByKey(eq("ABCD"), any(UserSession.class))).thenReturn(null);

    Result result = service.deleteActionPlan("ABCD");
    verify(actionPlanService, never()).delete(eq("ABCD"), any(UserSession.class));
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("action_plans.errors.action_plan_does_not_exist", "ABCD"));
  }

  @Test
  public void should_close_action_plan() {
    when(actionPlanService.findByKey(eq("ABCD"), any(UserSession.class))).thenReturn(DefaultActionPlan.create("Long term"));

    Result result = service.closeActionPlan("ABCD");
    verify(actionPlanService).setStatus(eq("ABCD"), eq("CLOSED"), any(UserSession.class));
    assertThat(result.ok()).isTrue();
  }

  @Test
  public void should_open_action_plan() {
    when(actionPlanService.findByKey(eq("ABCD"), any(UserSession.class))).thenReturn(DefaultActionPlan.create("Long term"));

    Result result = service.openActionPlan("ABCD");
    verify(actionPlanService).setStatus(eq("ABCD"), eq("OPEN"), any(UserSession.class));
    assertThat(result.ok()).isTrue();
  }

  @Test
  public void should_get_error_on_action_plan_result_when_no_project() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");

    Result result = service.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.cant_be_empty", "project"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_no_name() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", null);
    parameters.put("description", "Long term issues");
    parameters.put("project", "org.sonar.Sample");

    Result result = service.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.cant_be_empty", "name"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_name_is_too_long() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", createLongString(201));
    parameters.put("description", "Long term issues");
    parameters.put("project", "org.sonar.Sample");

    Result result = service.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.is_too_long", "name", 200));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_description_is_too_long() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", createLongString(1001));
    parameters.put("project", "org.sonar.Sample");

    Result result = service.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.is_too_long", "description", 1000));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_dead_line_use_wrong_format() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("project", "org.sonar.Sample");
    parameters.put("deadLine", "18/05/2013");

    Result result = service.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.is_not_valid", "date"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_dead_line_is_in_the_past() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("project", "org.sonar.Sample");
    parameters.put("deadLine", "2000-01-01");

    Result result = service.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("action_plans.date_cant_be_in_past"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_name_is_already_used_for_project() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("project", "org.sonar.Sample");

    when(actionPlanService.isNameAlreadyUsedForProject(anyString(), anyString())).thenReturn(true);

    Result result = service.createActionPlanResult(parameters, DefaultActionPlan.create("Short term"));
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("action_plans.same_name_in_same_project"));
  }

  @Test
  public void should_get_error_on_action_plan_result_when_project_not_found() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");
    parameters.put("project", "org.sonar.Sample");

    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);

    Result result = service.createActionPlanResult(parameters);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("action_plans.errors.project_does_not_exist", "org.sonar.Sample"));
  }

  @Test
  public void test_changelog() throws Exception {
    IssueChangelog changelog = new IssueChangelog(Collections.<FieldDiffs>emptyList(), Collections.<User>emptyList());
    when(changelogService.changelog(eq("ABCDE"), any(UserSession.class))).thenReturn(changelog);

    IssueChangelog result = service.changelog("ABCDE");

    assertThat(result).isSameAs(changelog);
  }

  @Test
  public void should_create_issue_filter() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");

    Result<DefaultIssueFilter> result = service.createIssueFilter(parameters);
    assertThat(result.ok()).isTrue();

    ArgumentCaptor<DefaultIssueFilter> issueFilterCaptor = ArgumentCaptor.forClass(DefaultIssueFilter.class);
    verify(issueFilterService).save(issueFilterCaptor.capture(), any(UserSession.class));
    DefaultIssueFilter issueFilter =  issueFilterCaptor.getValue();
    assertThat(issueFilter.name()).isEqualTo("Long term");
    assertThat(issueFilter.description()).isEqualTo("Long term issues");
  }

  @Test
  public void should_update_issue_filter() {
    Map<String, String> parameters = newHashMap();
    parameters.put("id", "10");
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");

    Result<DefaultIssueFilter> result = service.updateIssueFilter(parameters);
    assertThat(result.ok()).isTrue();

    ArgumentCaptor<DefaultIssueFilter> issueFilterCaptor = ArgumentCaptor.forClass(DefaultIssueFilter.class);
    verify(issueFilterService).update(issueFilterCaptor.capture(), any(UserSession.class));
    DefaultIssueFilter issueFilter =  issueFilterCaptor.getValue();
    assertThat(issueFilter.id()).isEqualTo(10L);
    assertThat(issueFilter.name()).isEqualTo("Long term");
    assertThat(issueFilter.description()).isEqualTo("Long term issues");
  }

  @Test
  public void should_update_data() {
    Map<String, Object> data = newHashMap();
    service.updateIssueFilterData(10L, data);
    verify(issueFilterService).updateData(eq(10L), eq(data), any(UserSession.class));
  }

  @Test
  public void should_delete_issue_filter() {
    Result<DefaultIssueFilter> result = service.deleteIssueFilter(1L);
    assertThat(result.ok()).isTrue();
  }

  @Test
  public void should_copy_issue_filter() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Copy of Long term");
    parameters.put("description", "Copy of Long term issues");

    Result<DefaultIssueFilter> result = service.copyIssueFilter(1L, parameters);
    assertThat(result.ok()).isTrue();

    ArgumentCaptor<DefaultIssueFilter> issueFilterCaptor = ArgumentCaptor.forClass(DefaultIssueFilter.class);
    verify(issueFilterService).copy(eq(1L), issueFilterCaptor.capture(), any(UserSession.class));
    DefaultIssueFilter issueFilter =  issueFilterCaptor.getValue();
    assertThat(issueFilter.name()).isEqualTo("Copy of Long term");
    assertThat(issueFilter.description()).isEqualTo("Copy of Long term issues");
  }

  @Test
  public void should_get_error_on_issue_filter_result_when_no_name() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", null);
    parameters.put("description", "Long term issues");

    Result result = service.createIssueFilterResult(parameters, false);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.cant_be_empty", "name"));
  }

  @Test
  public void should_get_error_on_issue_filter_result_when_name_is_too_long() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", createLongString(101));
    parameters.put("description", "Long term issues");

    Result result = service.createIssueFilterResult(parameters, false);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.is_too_long", "name", 100));
  }

  @Test
  public void should_get_error_on_issue_filter_result_when_description_is_too_long() {
    Map<String, String> parameters = newHashMap();
    parameters.put("name", "Long term");
    parameters.put("description", createLongString(4001));

    Result result = service.createIssueFilterResult(parameters, false);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.is_too_long", "description", 4000));
  }

  @Test
  public void should_get_error_on_issue_filter_result_when_id_is_null_on_update() {
    Map<String, String> parameters = newHashMap();
    parameters.put("id", null);
    parameters.put("name", "Long term");
    parameters.put("description", "Long term issues");

    Result result = service.createIssueFilterResult(parameters, true);
    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).contains(Result.Message.ofL10n("errors.cant_be_empty", "id"));
  }

  @Test
  public void should_execute_issue_filter_from_issue_query() {
    service.execute(IssueQuery.builder().build());
    verify(issueFilterService).execute(any(IssueQuery.class));
  }

  @Test
  public void should_execute_issue_filter_from_existing_filter() {
    service.execute(10L);
    verify(issueFilterService).execute(eq(10L), any(UserSession.class));
  }

  @Test
  public void should_find_user_issue_filters() {
    service.findIssueFiltersForCurrentUser();
    verify(issueFilterService).findByUser(any(UserSession.class));
  }

  @Test
  public void should_find_shared_issue_filters() {
    service.findSharedFiltersForCurrentUser();
    verify(issueFilterService).findSharedFilters(any(UserSession.class));
  }

  @Test
  public void should_find_favourite_issue_filters() {
    service.findFavouriteIssueFiltersForCurrentUser();
    verify(issueFilterService).findFavoriteFilters(any(UserSession.class));
  }

  @Test
  public void should_toggle_favourite_issue_filter() {
    service.toggleFavouriteIssueFilter(10L);
    verify(issueFilterService).toggleFavouriteIssueFilter(eq(10L), any(UserSession.class));
  }

  private String createLongString(int size) {
    String result = "";
    for (int i = 0; i < size; i++) {
      result += "c";
    }
    return result;
  }

}
