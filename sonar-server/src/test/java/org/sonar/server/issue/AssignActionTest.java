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
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.user.UserSession;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AssignActionTest {

  private AssignAction action;

  private final UserFinder userFinder = mock(UserFinder.class);

  @Before
  public void before(){
    action = new AssignAction(userFinder);
  }

  @Test
  public void should_execute(){
    String assignee = "arthur";

    Map<String, Object> properties = newHashMap();
    properties.put("assignee", assignee);
    DefaultIssue issue = mock(DefaultIssue.class);
    IssueUpdater issueUpdater = mock(IssueUpdater.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issueUpdater()).thenReturn(issueUpdater);
    when(context.issue()).thenReturn(issue);

    action.execute(properties, context);
    verify(issueUpdater).assign(eq(issue), eq(assignee), any(IssueChangeContext.class));
  }

  @Test
  public void should_verify_assignee_exists(){
    String assignee = "arthur";
    Map<String, Object> properties = newHashMap();
    properties.put("assignee", assignee);

    when(userFinder.findByLogin(assignee)).thenReturn(new DefaultUser());
    assertThat(action.verify(properties, mock(UserSession.class))).isTrue();

    when(userFinder.findByLogin(assignee)).thenReturn(null);
    try {
      action.verify(properties, mock(UserSession.class));
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown user: arthur");
    }
  }

  @Test
  public void should_support_only_unresolved_issues(){
    assertThat(action.supports(new DefaultIssue().setStatus(Issue.STATUS_OPEN))).isTrue();
    assertThat(action.supports(new DefaultIssue().setStatus(Issue.STATUS_CLOSED))).isTrue();
  }
}