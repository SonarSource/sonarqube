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

package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ActionPlanServiceTest {

  @Mock
  ActionPlanDao actionPlanDao;

  @Mock
  ActionPlanStatsDao actionPlanStatsDao;

  @Mock
  ResourceDao resourceDao;

  @Mock
  IssueDao issueDao;

  @Mock
  IssueUpdater issueUpdater;

  @Mock
  IssueStorage issueStorage;

  String projectKey = "org.sonar.Sample";

  UserSession projectAdministratorUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").addProjectPermissions(UserRole.ADMIN, projectKey);
  UserSession projectUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").addProjectPermissions(UserRole.USER, projectKey);
  UserSession unauthorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas");

  private ActionPlanService actionPlanService;

  @Before
  public void before() {
    actionPlanService = new ActionPlanService(actionPlanDao, actionPlanStatsDao, resourceDao, issueDao, issueUpdater, issueStorage);
  }

  @Test
  public void create() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    ActionPlan actionPlan = DefaultActionPlan.create("Long term");

    actionPlanService.create(actionPlan, projectAdministratorUserSession);
    verify(actionPlanDao).save(any(ActionPlanDto.class));
  }

  @Test
  public void create_required_admin_role() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    ActionPlan actionPlan = DefaultActionPlan.create("Long term");

    try {
      actionPlanService.create(actionPlan, unauthorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyZeroInteractions(actionPlanDao);
  }

  @Test
  public void set_status() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD").setProjectKey_unit_test_only(projectKey));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));

    ActionPlan result = actionPlanService.setStatus("ABCD", "CLOSED", projectAdministratorUserSession);
    verify(actionPlanDao).update(any(ActionPlanDto.class));

    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo("CLOSED");
  }

  @Test
  public void update() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    ActionPlan actionPlan = DefaultActionPlan.create("Long term");

    actionPlanService.update(actionPlan, projectAdministratorUserSession);
    verify(actionPlanDao).update(any(ActionPlanDto.class));
  }

  @Test
  public void delete() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD").setProjectKey_unit_test_only(projectKey));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    actionPlanService.delete("ABCD", projectAdministratorUserSession);
    verify(actionPlanDao).delete("ABCD");
  }

  @Test
  public void unplan_all_linked_issues_when_deleting_an_action_plan() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD").setProjectKey_unit_test_only(projectKey));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));

    IssueDto issueDto = new IssueDto().setId(100L).setStatus(Issue.STATUS_OPEN).setRuleKey_unit_test_only("squid", "s100");
    when(issueDao.selectIssues(any(IssueQuery.class))).thenReturn(newArrayList(issueDto));
    when(issueUpdater.plan(any(DefaultIssue.class), eq((ActionPlan) null), any(IssueChangeContext.class))).thenReturn(true);

    ArgumentCaptor<DefaultIssue> captor = ArgumentCaptor.forClass(DefaultIssue.class);
    actionPlanService.delete("ABCD", projectAdministratorUserSession);
    verify(actionPlanDao).delete("ABCD");
    verify(issueUpdater).plan(captor.capture(), eq((ActionPlan) null), any(IssueChangeContext.class));
    verify(issueStorage).save(newArrayList(captor.getAllValues()));
  }

  @Test
  public void find_by_key() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD").setProjectKey_unit_test_only(projectKey));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));

    ActionPlan result = actionPlanService.findByKey("ABCD", projectUserSession);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("ABCD");
  }

  @Test
  public void return_null_if_no_action_plan_when_find_by_key() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(null);
    assertThat(actionPlanService.findByKey("ABCD", projectUserSession)).isNull();
  }

  @Test
  public void find_by_keys() {
    when(actionPlanDao.findByKeys(newArrayList("ABCD"))).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    Collection<ActionPlan> results = actionPlanService.findByKeys(newArrayList("ABCD"));
    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().key()).isEqualTo("ABCD");
  }

  @Test
  public void find_open_by_project_key() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    when(actionPlanDao.findOpenByProjectId(1l)).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    Collection<ActionPlan> results = actionPlanService.findOpenByProjectKey(projectKey, projectUserSession);
    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().key()).isEqualTo("ABCD");
  }

  @Test
  public void find_open_by_project_key_required_user_role() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    when(actionPlanDao.findOpenByProjectId(1l)).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));

    try {
      actionPlanService.findOpenByProjectKey(projectKey, unauthorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyZeroInteractions(actionPlanDao);
  }

  @Test(expected = NotFoundException.class)
  public void throw_exception_if_project_not_found_when_find_open_by_project_key() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);
    actionPlanService.findOpenByProjectKey("<Unkown>", projectUserSession);
  }

  @Test
  public void find_action_plan_stats() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setId(1L).setKey(projectKey));
    when(actionPlanStatsDao.findByProjectId(1L)).thenReturn(newArrayList(new ActionPlanStatsDto()));

    Collection<ActionPlanStats> results = actionPlanService.findActionPlanStats(projectKey, projectUserSession);
    assertThat(results).hasSize(1);
  }

  @Test(expected = NotFoundException.class)
  public void throw_exception_if_project_not_found_when_find_open_action_plan_stats() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);

    actionPlanService.findActionPlanStats(projectKey, projectUserSession);
  }

}
