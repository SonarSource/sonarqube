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
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.utils.WorkDurationFactory;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.user.UserSession;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ActionPlanServiceTest {

  private ActionPlanDao actionPlanDao = mock(ActionPlanDao.class);
  private ActionPlanStatsDao actionPlanStatsDao = mock(ActionPlanStatsDao.class);
  private ResourceDao resourceDao = mock(ResourceDao.class);
  private AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
  private UserSession userSession = mock(UserSession.class);
  private IssueDao issueDao = mock(IssueDao.class);
  private IssueUpdater issueUpdater = mock(IssueUpdater.class);
  private IssueStorage issueStorage = mock(IssueStorage.class);

  private ActionPlanService actionPlanService;

  @Before
  public void before() {
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.userId()).thenReturn(10);
    when(authorizationDao.isAuthorizedComponentKey(anyString(), eq(10), anyString())).thenReturn(true);

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, 8);
    actionPlanService = new ActionPlanService(actionPlanDao, actionPlanStatsDao, resourceDao, authorizationDao, issueDao, issueUpdater, issueStorage,
      new WorkDurationFactory(settings));
  }

  @Test
  public void should_create() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));
    ActionPlan actionPlan = DefaultActionPlan.create("Long term");

    actionPlanService.create(actionPlan, userSession);
    verify(actionPlanDao).save(any(ActionPlanDto.class));
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.ADMIN));
  }

  @Test
  public void should_create_required_admin_role() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));
    ActionPlan actionPlan = DefaultActionPlan.create("Long term");
    when(authorizationDao.isAuthorizedComponentKey(anyString(), eq(10), anyString())).thenReturn(false);

    try {
      actionPlanService.create(actionPlan, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("User does not have the required role on the project: org.sonar.Sample");
    }
    verify(authorizationDao).isAuthorizedComponentKey(eq("org.sonar.Sample"), eq(10), eq(UserRole.ADMIN));
    verifyZeroInteractions(actionPlanDao);
  }

  @Test
  public void should_set_status() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD"));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));

    ActionPlan result = actionPlanService.setStatus("ABCD", "CLOSED", userSession);
    verify(actionPlanDao).update(any(ActionPlanDto.class));

    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo("CLOSED");
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.ADMIN));
  }

  @Test
  public void should_update() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));
    ActionPlan actionPlan = DefaultActionPlan.create("Long term");

    actionPlanService.update(actionPlan, userSession);
    verify(actionPlanDao).update(any(ActionPlanDto.class));
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.ADMIN));
  }

  @Test
  public void should_delete() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD"));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));
    actionPlanService.delete("ABCD", userSession);
    verify(actionPlanDao).delete("ABCD");
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.ADMIN));
  }

  @Test
  public void should_unplan_all_linked_issues_when_deleting_an_action_plan() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD"));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));

    IssueDto issueDto = new IssueDto().setId(100L).setStatus(Issue.STATUS_OPEN).setRuleKey_unit_test_only("squid", "s100");
    when(issueDao.selectIssues(any(IssueQuery.class))).thenReturn(newArrayList(issueDto));
    when(issueUpdater.plan(any(DefaultIssue.class), eq((ActionPlan) null), any(IssueChangeContext.class))).thenReturn(true);

    ArgumentCaptor<DefaultIssue> captor = ArgumentCaptor.forClass(DefaultIssue.class);
    actionPlanService.delete("ABCD", userSession);
    verify(actionPlanDao).delete("ABCD");
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.ADMIN));
    verify(issueUpdater).plan(captor.capture(), eq((ActionPlan) null), any(IssueChangeContext.class));
    verify(issueStorage).save(newArrayList(captor.getAllValues()));
  }

  @Test
  public void should_find_by_key() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(new ActionPlanDto().setKey("ABCD"));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));

    ActionPlan result = actionPlanService.findByKey("ABCD", userSession);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("ABCD");
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.USER));
  }

  @Test
  public void should_return_null_if_no_action_plan_when_find_by_key() {
    when(actionPlanDao.findByKey("ABCD")).thenReturn(null);
    assertThat(actionPlanService.findByKey("ABCD", userSession)).isNull();
  }

  @Test
  public void should_find_by_keys() {
    when(actionPlanDao.findByKeys(newArrayList("ABCD"))).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    Collection<ActionPlan> results = actionPlanService.findByKeys(newArrayList("ABCD"));
    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().key()).isEqualTo("ABCD");
  }

  @Test
  public void should_find_open_by_project_key() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));
    when(actionPlanDao.findOpenByProjectId(1l)).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    Collection<ActionPlan> results = actionPlanService.findOpenByProjectKey("org.sonar.Sample", userSession);
    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().key()).isEqualTo("ABCD");
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.USER));
  }

  @Test
  public void should_find_open_by_project_key_required_user_role() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));
    when(actionPlanDao.findOpenByProjectId(1l)).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    when(authorizationDao.isAuthorizedComponentKey(anyString(), eq(10), anyString())).thenReturn(false);

    try {
      actionPlanService.findOpenByProjectKey("org.sonar.Sample", userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("User does not have the required role on the project: org.sonar.Sample");
    }
    verify(authorizationDao).isAuthorizedComponentKey(eq("org.sonar.Sample"), eq(10), eq(UserRole.USER));
    verifyZeroInteractions(actionPlanDao);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_exception_if_project_not_found_when_find_open_by_project_key() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);
    actionPlanService.findOpenByProjectKey("<Unkown>", userSession);
  }

  @Test
  public void should_find_action_plan_stats() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setId(1L).setKey("org.sonar.Sample"));
    when(actionPlanStatsDao.findByProjectId(1L)).thenReturn(newArrayList(new ActionPlanStatsDto()));

    Collection<ActionPlanStats> results = actionPlanService.findActionPlanStats("org.sonar.Sample", userSession);
    assertThat(results).hasSize(1);
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.USER));
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_exception_if_project_not_found_when_find_open_action_plan_stats() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);

    actionPlanService.findActionPlanStats("org.sonar.Sample", userSession);
  }

}
