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

package org.sonar.server.issue.actionplan;

import java.util.Collection;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ResourceQuery;
import org.sonar.db.issue.ActionPlanDao;
import org.sonar.db.issue.ActionPlanDto;
import org.sonar.db.issue.ActionPlanStatsDao;
import org.sonar.db.issue.ActionPlanStatsDto;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueStorage;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActionPlanServiceTest {

  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

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

  UserSession projectAdministratorUserSession = new MockUserSession("nicolas").setName("Nicolas").addProjectPermissions(UserRole.ADMIN, projectKey);
  UserSession projectUserSession = new MockUserSession("nicolas").setName("Nicolas").addProjectPermissions(UserRole.USER, projectKey);
  UserSession unauthorizedUserSession = new MockUserSession("nicolas").setName("Nicolas");

  private ActionPlanService actionPlanService;

  @Before
  public void before() {
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.issueDao()).thenReturn(issueDao);
    actionPlanService = new ActionPlanService(dbClient, actionPlanDao, actionPlanStatsDao, resourceDao, issueUpdater, issueStorage);
  }

  @Test
  public void create() {
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    ActionPlan actionPlan = DefaultActionPlan.create("Long term");

    actionPlanService.create(actionPlan, projectAdministratorUserSession);
    verify(actionPlanDao).save(any(ActionPlanDto.class));
  }

  @Test
  public void create_required_admin_role() {
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
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
    when(actionPlanDao.selectByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD").setProjectKey_unit_test_only(projectKey));
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));

    ActionPlan result = actionPlanService.setStatus("ABCD", "CLOSED", projectAdministratorUserSession);
    verify(actionPlanDao).update(any(ActionPlanDto.class));

    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo("CLOSED");
  }

  @Test
  public void update() {
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    ActionPlan actionPlan = DefaultActionPlan.create("Long term");

    actionPlanService.update(actionPlan, projectAdministratorUserSession);
    verify(actionPlanDao).update(any(ActionPlanDto.class));
  }

  @Test
  public void delete() {
    when(actionPlanDao.selectByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD").setProjectKey_unit_test_only(projectKey));
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    actionPlanService.delete("ABCD", projectAdministratorUserSession);
    verify(actionPlanDao).delete("ABCD");
  }

  @Test
  public void unplan_all_linked_issues_when_deleting_an_action_plan() {
    when(actionPlanDao.selectByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD").setProjectKey_unit_test_only(projectKey));
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));

    IssueDto issueDto = new IssueDto().setId(100L).setStatus(Issue.STATUS_OPEN).setRuleKey("squid", "s100").setIssueCreationDate(new Date());
    when(issueDao.selectByActionPlan(session, "ABCD")).thenReturn(newArrayList(issueDto));
    when(issueUpdater.plan(any(DefaultIssue.class), eq((ActionPlan) null), any(IssueChangeContext.class))).thenReturn(true);

    ArgumentCaptor<DefaultIssue> captor = ArgumentCaptor.forClass(DefaultIssue.class);
    actionPlanService.delete("ABCD", projectAdministratorUserSession);
    verify(actionPlanDao).delete("ABCD");
    verify(issueUpdater).plan(captor.capture(), eq((ActionPlan) null), any(IssueChangeContext.class));
    verify(issueStorage).save(newArrayList(captor.getAllValues()));
  }

  @Test
  public void find_by_key() {
    when(actionPlanDao.selectByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD").setProjectKey_unit_test_only(projectKey));
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));

    ActionPlan result = actionPlanService.findByKey("ABCD", projectUserSession);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("ABCD");
  }

  @Test
  public void return_null_if_no_action_plan_when_find_by_key() {
    when(actionPlanDao.selectByKey("ABCD")).thenReturn(null);
    assertThat(actionPlanService.findByKey("ABCD", projectUserSession)).isNull();
  }

  @Test
  public void find_by_keys() {
    when(actionPlanDao.selectByKeys(newArrayList("ABCD"))).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    Collection<ActionPlan> results = actionPlanService.findByKeys(newArrayList("ABCD"));
    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().key()).isEqualTo("ABCD");
  }

  @Test
  public void find_open_by_project_key() {
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    when(actionPlanDao.selectOpenByProjectId(1l)).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    Collection<ActionPlan> results = actionPlanService.findOpenByProjectKey(projectKey, projectUserSession);
    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().key()).isEqualTo("ABCD");
  }

  @Test
  public void find_open_by_project_key_required_user_role() {
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey(projectKey).setId(1l));
    when(actionPlanDao.selectOpenByProjectId(1l)).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));

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
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(null);
    actionPlanService.findOpenByProjectKey("<Unkown>", projectUserSession);
  }

  @Test
  public void find_action_plan_stats() {
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setId(1L).setKey(projectKey));
    when(actionPlanStatsDao.selectByProjectId(1L)).thenReturn(newArrayList(new ActionPlanStatsDto()));

    Collection<ActionPlanStats> results = actionPlanService.findActionPlanStats(projectKey, projectUserSession);
    assertThat(results).hasSize(1);
  }

  @Test(expected = NotFoundException.class)
  public void throw_exception_if_project_not_found_when_find_open_action_plan_stats() {
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(null);

    actionPlanService.findActionPlanStats(projectKey, projectUserSession);
  }

}
