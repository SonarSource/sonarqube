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
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class IssueBulkChangeServiceTest {

  private DefaultIssueFinder finder = mock(DefaultIssueFinder.class);
  private IssueUpdater issueUpdater = mock(IssueUpdater.class);
  private IssueStorage issueStorage = mock(IssueStorage.class);
  private IssueNotifications issueNotifications = mock(IssueNotifications.class);

  private IssueQueryResult issueQueryResult = mock(IssueQueryResult.class);
  private UserSession userSession = mock(UserSession.class);
  private DefaultIssue issue = new DefaultIssue().setKey("ABCD");

  private IssueBulkChangeService service;

  private Action action = mock(Action.class);

  @Before
  public void before() {
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.userId()).thenReturn(10);
    when(userSession.login()).thenReturn("fred");

    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);
    when(issueQueryResult.issues()).thenReturn(newArrayList((Issue) issue));

    when(action.key()).thenReturn("assign");

    List<Action> actions = newArrayList();
    actions.add(action);

    service = new IssueBulkChangeService(finder, issueStorage, issueNotifications, actions);
  }

  @Test
  public void should_execute_bulk_change() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");

    when(action.supports(any(Issue.class))).thenReturn(true);
    when(action.execute(anyMap(), any(IssueBulkChangeService.ActionContext.class))).thenReturn(true);
    when(action.execute(eq(properties), any(IssueBulkChangeService.ActionContext.class))).thenReturn(true);

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).isEmpty();

    verifyNoMoreInteractions(issueUpdater);
    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_not_execute_bulk_if_issue_does_not_support_action() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");

    when(action.supports(any(Issue.class))).thenReturn(false);

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).isEmpty();
    assertThat(result.issuesNotChanged()).hasSize(1);

    verifyZeroInteractions(issueUpdater);
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void should_not_execute_bulk_if_action_could_not_be_executed_on_issue() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");

    when(action.supports(any(Issue.class))).thenReturn(true);
    when(action.execute(anyMap(), any(IssueBulkChangeService.ActionContext.class))).thenReturn(false);

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).isEmpty();
    assertThat(result.issuesNotChanged()).hasSize(1);

    verifyZeroInteractions(issueUpdater);
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void should_not_execute_bulk_on_unexpected_error() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");

    when(action.supports(any(Issue.class))).thenReturn(true);
    doThrow(new RuntimeException("Error")).when(action).execute(anyMap(), any(IssueBulkChangeService.ActionContext.class));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).isEmpty();
    assertThat(result.issuesNotChanged()).hasSize(1);

    verifyZeroInteractions(issueUpdater);
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void should_fail_if_user_not_logged() {
    when(userSession.isLoggedIn()).thenReturn(false);

    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    try {
      service.execute(issueBulkChangeQuery, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("User is not logged in");
    }
    verifyZeroInteractions(issueUpdater);
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void should_fail_if_action_not_found() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "unknown");
    properties.put("unknown.unknown", "unknown");
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    try {
      service.execute(issueBulkChangeQuery, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The action : 'unknown' is unknown");
    }
    verifyZeroInteractions(issueUpdater);
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

}
