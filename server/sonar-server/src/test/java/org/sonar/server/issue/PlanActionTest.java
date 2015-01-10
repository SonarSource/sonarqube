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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PlanActionTest {

  private PlanAction action;

  private ActionPlanService actionPlanService = mock(ActionPlanService.class);
  private IssueUpdater issueUpdater = mock(IssueUpdater.class);

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void before(){
    action = new PlanAction(actionPlanService, issueUpdater);
  }

  @Test
  public void should_execute(){
    ActionPlan actionPlan = new DefaultActionPlan();
    Map<String, Object> properties = newHashMap();
    properties.put(PlanAction.VERIFIED_ACTION_PLAN, actionPlan);
    DefaultIssue issue = mock(DefaultIssue.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issue()).thenReturn(issue);

    action.execute(properties, context);
    verify(issueUpdater).plan(eq(issue), eq(actionPlan), any(IssueChangeContext.class));
  }

  @Test
  public void should_fail_on_unverified_action_plan() throws Exception {
    throwable.expect(IllegalArgumentException.class);
    throwable.expectMessage("Action plan is missing from the execution parameters");

    Map<String, Object> properties = newHashMap();
    Action.Context context = mock(Action.Context.class);

    action.execute(properties, context);
  }

  @Test
  public void should_execute_on_null_action_plan(){
    Map<String, Object> properties = newHashMap();
    properties.put(PlanAction.VERIFIED_ACTION_PLAN, null);
    DefaultIssue issue = mock(DefaultIssue.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issue()).thenReturn(issue);

    action.execute(properties, context);
    verify(issueUpdater).plan(eq(issue), eq((ActionPlan) null), any(IssueChangeContext.class));
  }

  @Test
  public void should_verify(){
    String planKey = "ABCD";
    Map<String, Object> properties = newHashMap();
    properties.put("plan", planKey);
    ActionPlan actionPlan = new DefaultActionPlan().setProjectKey("struts");

    List<Issue> issues = newArrayList((Issue) new DefaultIssue().setKey("ABC").setProjectKey("struts"));
    when(actionPlanService.findByKey(eq(planKey), any(UserSession.class))).thenReturn(actionPlan);
    assertThat(action.verify(properties, issues, mock(UserSession.class))).isTrue();
    assertThat(properties.get(PlanAction.VERIFIED_ACTION_PLAN)).isEqualTo(actionPlan);
  }

  @Test
  public void should_fail_if_action_plan_does_not_exist(){
    throwable.expect(IllegalArgumentException.class);
    throwable.expectMessage("Unknown action plan: ABCD");

    String planKey = "ABCD";
    Map<String, Object> properties = newHashMap();
    properties.put("plan", planKey);

    List<Issue> issues = newArrayList((Issue) new DefaultIssue().setKey("ABC").setProjectKey("struts"));
    when(actionPlanService.findByKey(eq(planKey), any(UserSession.class))).thenReturn(null);
    action.verify(properties, issues, mock(UserSession.class));
  }

  @Test
  public void should_unplan_if_action_plan_is_empty() throws Exception {
    String planKey = "";
    Map<String, Object> properties = newHashMap();
    properties.put("plan", planKey);

    List<Issue> issues = newArrayList((Issue) new DefaultIssue().setKey("ABC").setProjectKey("struts"));
    action.verify(properties, issues, mock(UserSession.class));
    assertThat(properties.containsKey(PlanAction.VERIFIED_ACTION_PLAN)).isTrue();
    assertThat(properties.get(PlanAction.VERIFIED_ACTION_PLAN)).isNull();
  }

  @Test
  public void should_verify_issues_are_on_the_same_project(){
    throwable.expect(IllegalArgumentException.class);
    throwable.expectMessage("Issues are not all related to the action plan project: struts");

    String planKey = "ABCD";
    Map<String, Object> properties = newHashMap();
    properties.put("plan", planKey);

    when(actionPlanService.findByKey(eq(planKey), any(UserSession.class))).thenReturn(new DefaultActionPlan().setProjectKey("struts"));
    List<Issue> issues = newArrayList(new DefaultIssue().setKey("ABC").setProjectKey("struts"), (Issue) new DefaultIssue().setKey("ABE").setProjectKey("mybatis"));
    action.verify(properties, issues, mock(UserSession.class));
  }

  @Test
  public void should_support_only_unresolved_issues(){
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isFalse();
  }
}
