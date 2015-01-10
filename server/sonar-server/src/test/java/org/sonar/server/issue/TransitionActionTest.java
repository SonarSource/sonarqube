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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.user.MockUserSession;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TransitionActionTest {

  private TransitionAction action;

  private IssueWorkflow workflow = mock(IssueWorkflow.class);

  @Before
  public void before() {
    action = new TransitionAction(workflow);
  }

  @Test
  public void should_execute() {
    String transition = "reopen";
    Map<String, Object> properties = newHashMap();
    properties.put("transition", transition);
    DefaultIssue issue = mock(DefaultIssue.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issue()).thenReturn(issue);

    when(workflow.outTransitions(context.issue())).thenReturn(newArrayList(Transition.create(transition, "REOPEN", "CONFIRMED")));

    action.execute(properties, context);
    verify(workflow).doTransition(eq(issue), eq(transition), any(IssueChangeContext.class));
  }

  @Test
  public void should_not_execute_if_transition_is_not_available() {
    String transition = "reopen";
    Map<String, Object> properties = newHashMap();
    properties.put("transition", transition);
    DefaultIssue issue = mock(DefaultIssue.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issue()).thenReturn(issue);

    // Do not contain reopen, transition is not possible
    when(workflow.outTransitions(context.issue())).thenReturn(newArrayList(Transition.create("resolve", "OPEN", "RESOLVED")));

    assertThat(action.execute(properties, context)).isFalse();
    verify(workflow, never()).doTransition(eq(issue), eq(transition), any(IssueChangeContext.class));
  }

  @Test
  public void should_verify_fail_if_parameter_not_found() {
    String transition = "reopen";
    Map<String, Object> properties = newHashMap();
    properties.put("unknwown", transition);
    try {
      action.verify(properties, Lists.<Issue>newArrayList(), MockUserSession.create());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing parameter : 'transition'");
    }
    verifyZeroInteractions(workflow);
  }

  @Test
  public void should_support_all_issues() {
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isTrue();
  }

}
