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
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AssignActionTest {

  private AssignAction action;

  private final UserFinder userFinder = mock(UserFinder.class);
  private IssueUpdater issueUpdater = mock(IssueUpdater.class);

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void before() {
    action = new AssignAction(userFinder, issueUpdater);
  }

  @Test
  public void should_execute() {
    User assignee = new DefaultUser();

    Map<String, Object> properties = newHashMap();
    properties.put(AssignAction.VERIFIED_ASSIGNEE, assignee);
    DefaultIssue issue = mock(DefaultIssue.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issue()).thenReturn(issue);

    action.execute(properties, context);
    verify(issueUpdater).assign(eq(issue), eq(assignee), any(IssueChangeContext.class));
  }

  @Test
  public void should_fail_if_assignee_is_not_verified() throws Exception {
    throwable.expect(IllegalArgumentException.class);
    throwable.expectMessage("Assignee is missing from the execution parameters");

    Map<String, Object> properties = newHashMap();

    Action.Context context = mock(Action.Context.class);

    action.execute(properties, context);
  }

  @Test
  public void should_verify_assignee_exists() {
    String assignee = "arthur";
    Map<String, Object> properties = newHashMap();
    properties.put("assignee", assignee);

    User user = new DefaultUser().setLogin(assignee);

    List<Issue> issues = newArrayList((Issue) new DefaultIssue().setKey("ABC"));
    when(userFinder.findByLogin(assignee)).thenReturn(user);
    assertThat(action.verify(properties, issues, mock(UserSession.class))).isTrue();
    assertThat(properties.get(AssignAction.VERIFIED_ASSIGNEE)).isEqualTo(user);
  }

  @Test
  public void should_fail_if_assignee_does_not_exists() {
    throwable.expect(IllegalArgumentException.class);
    throwable.expectMessage("Unknown user: arthur");

    String assignee = "arthur";
    Map<String, Object> properties = newHashMap();
    properties.put("assignee", assignee);

    List<Issue> issues = newArrayList((Issue) new DefaultIssue().setKey("ABC"));
    when(userFinder.findByLogin(assignee)).thenReturn(null);
    action.verify(properties, issues, mock(UserSession.class));
  }

  @Test
  public void should_unassign_if_assignee_is_empty() {
    String assignee = "";
    Map<String, Object> properties = newHashMap();
    properties.put("assignee", assignee);

    List<Issue> issues = newArrayList((Issue) new DefaultIssue().setKey("ABC"));
    action.verify(properties, issues, mock(UserSession.class));
    assertThat(action.verify(properties, issues, mock(UserSession.class))).isTrue();
    assertThat(properties.containsKey(AssignAction.VERIFIED_ASSIGNEE)).isTrue();
    assertThat(properties.get(AssignAction.VERIFIED_ASSIGNEE)).isNull();
  }

  @Test
  public void should_support_only_unresolved_issues() {
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isFalse();
  }
}
