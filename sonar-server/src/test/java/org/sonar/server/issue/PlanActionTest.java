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
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PlanActionTest {

  private PlanAction action;

  private ActionPlanService actionPlanService = mock(ActionPlanService.class);

  @Before
  public void before(){
    action = new PlanAction(actionPlanService);
  }

  @Test
  public void should_execute(){
    String planKey = "ABCD";
    Map<String, Object> properties = newHashMap();
    properties.put("plan", planKey);
    DefaultIssue issue = mock(DefaultIssue.class);
    IssueUpdater issueUpdater = mock(IssueUpdater.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issueUpdater()).thenReturn(issueUpdater);
    when(context.issue()).thenReturn(issue);

    action.execute(properties, context);
    verify(issueUpdater).plan(eq(issue), eq(planKey), any(IssueChangeContext.class));
  }

  @Test
  public void should_verify(){
    String planKey = "ABCD";
    Map<String, Object> properties = newHashMap();
    properties.put("plan", planKey);

    List<Issue> issues = newArrayList((Issue) new DefaultIssue().setKey("ABC").setProjectKey("struts"));
    when(actionPlanService.findByKey(eq(planKey), any(UserSession.class))).thenReturn(new DefaultActionPlan().setProjectKey("struts"));
    assertThat(action.verify(properties, issues, mock(UserSession.class))).isTrue();
  }

  @Test
  public void should_fail_if_action_plan_does_not_exists(){
    String planKey = "ABCD";
    Map<String, Object> properties = newHashMap();
    properties.put("plan", planKey);

    List<Issue> issues = newArrayList((Issue) new DefaultIssue().setKey("ABC").setProjectKey("struts"));
    when(actionPlanService.findByKey(eq(planKey), any(UserSession.class))).thenReturn(null);
    try {
      action.verify(properties, issues, mock(UserSession.class));
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown action plan: ABCD");
    }
  }

  @Test
  public void should_verify_issues_are_on_the_same_project(){
    String planKey = "ABCD";
    Map<String, Object> properties = newHashMap();
    properties.put("plan", planKey);

    when(actionPlanService.findByKey(eq(planKey), any(UserSession.class))).thenReturn(new DefaultActionPlan().setProjectKey("struts"));
    List<Issue> issues = newArrayList(new DefaultIssue().setKey("ABC").setProjectKey("struts"), (Issue) new DefaultIssue().setKey("ABE").setProjectKey("mybatis"));
    try {
      action.verify(properties, issues, mock(UserSession.class));
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Issues are not all related to the action plan project: struts");
    }
  }

  @Test
  public void should_support_only_unresolved_issues(){
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isFalse();
  }
}