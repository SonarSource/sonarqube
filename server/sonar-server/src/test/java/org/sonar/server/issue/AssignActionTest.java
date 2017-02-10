/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.Issue;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.user.ThreadLocalUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssignActionTest {

  private AssignAction action;

  private final UserFinder userFinder = mock(UserFinder.class);
  private IssueFieldsSetter issueUpdater = mock(IssueFieldsSetter.class);

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
  public void should_fail_if_assignee_is_not_verified() {
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

    List<DefaultIssue> issues = newArrayList(new DefaultIssue().setKey("ABC"));
    when(userFinder.findByLogin(assignee)).thenReturn(user);
    assertThat(action.verify(properties, issues, mock(ThreadLocalUserSession.class))).isTrue();
    assertThat(properties.get(AssignAction.VERIFIED_ASSIGNEE)).isEqualTo(user);
  }

  @Test
  public void should_fail_if_assignee_does_not_exists() {
    throwable.expect(IllegalArgumentException.class);
    throwable.expectMessage("Unknown user: arthur");

    String assignee = "arthur";
    Map<String, Object> properties = newHashMap();
    properties.put("assignee", assignee);

    List<DefaultIssue> issues = newArrayList(new DefaultIssue().setKey("ABC"));
    when(userFinder.findByLogin(assignee)).thenReturn(null);
    action.verify(properties, issues, mock(ThreadLocalUserSession.class));
  }

  @Test
  public void should_unassign_if_assignee_is_empty() {
    String assignee = "";
    Map<String, Object> properties = newHashMap();
    properties.put("assignee", assignee);

    List<DefaultIssue> issues = newArrayList(new DefaultIssue().setKey("ABC"));
    action.verify(properties, issues, mock(ThreadLocalUserSession.class));
    assertThat(action.verify(properties, issues, mock(ThreadLocalUserSession.class))).isTrue();
    assertThat(properties.containsKey(AssignAction.VERIFIED_ASSIGNEE)).isTrue();
    assertThat(properties.get(AssignAction.VERIFIED_ASSIGNEE)).isNull();
  }

  @Test
  public void support_only_unresolved_issues() {
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isFalse();
  }

}
