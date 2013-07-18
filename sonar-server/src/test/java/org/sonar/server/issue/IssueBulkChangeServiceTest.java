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
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.condition.Condition;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.MockUserSession;
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
  private IssueStorage issueStorage = mock(IssueStorage.class);
  private IssueNotifications issueNotifications = mock(IssueNotifications.class);

  private IssueQueryResult issueQueryResult = mock(IssueQueryResult.class);
  private UserSession userSession = MockUserSession.create().setLogin("john").setUserId(10);
  private DefaultIssue issue = new DefaultIssue().setKey("ABCD");

  private IssueBulkChangeService service;

  private List<Action> actions;

  @Before
  public void before() {
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);
    when(issueQueryResult.issues()).thenReturn(newArrayList((Issue) issue));

    actions = newArrayList();

    service = new IssueBulkChangeService(finder, issueStorage, issueNotifications, actions);
  }

  @Test
  public void should_execute_bulk_change() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    actions.add(new MockAction("assign"));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).isEmpty();

    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_execute_bulk_change_with_comment() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");

    Action commentAction = mock(Action.class);
    when(commentAction.key()).thenReturn("comment");
    when(commentAction.supports(any(Issue.class))).thenReturn(true);
    when(commentAction.verify(anyMap(), anyListOf(Issue.class), any(UserSession.class))).thenReturn(true);
    when(commentAction.execute(anyMap(), any(IssueBulkChangeService.ActionContext.class))).thenReturn(true);
    actions.add(commentAction);
    actions.add(new MockAction("assign"));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, "my comment");
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).isEmpty();

    verify(commentAction).execute(anyMap(), any(IssueBulkChangeService.ActionContext.class));
    verify(issueStorage).save(eq(issue));
  }

  @Test
  public void should_execute_bulk_change_with_comment_only_on_changed_issues() {
    when(issueQueryResult.issues()).thenReturn(newArrayList((Issue) new DefaultIssue().setKey("ABCD"), new DefaultIssue().setKey("EFGH")));

    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD,EFGH");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");

    Action commentAction = mock(Action.class);
    when(commentAction.key()).thenReturn("comment");
    when(commentAction.supports(any(Issue.class))).thenReturn(true);
    when(commentAction.verify(anyMap(), anyListOf(Issue.class), any(UserSession.class))).thenReturn(true);
    when(commentAction.execute(anyMap(), any(IssueBulkChangeService.ActionContext.class))).thenReturn(true);
    actions.add(commentAction);

    // This action will only be executed on the first issue, not the second
    Action assignAction = mock(Action.class);
    when(assignAction.key()).thenReturn("assign");
    when(assignAction.supports(any(Issue.class))).thenReturn(true).thenReturn(false);
    when(assignAction.verify(anyMap(), anyListOf(Issue.class), any(UserSession.class))).thenReturn(true);
    when(assignAction.execute(anyMap(), any(IssueBulkChangeService.ActionContext.class))).thenReturn(true).thenReturn(false);
    actions.add(assignAction);

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, "my comment");
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).hasSize(1);

    // Only one issue will receive the comment
    verify(assignAction, times(1)).execute(anyMap(), any(IssueBulkChangeService.ActionContext.class));
    verify(issueStorage).save(eq(issue));
  }

  @Test
  public void should_save_once_per_issue() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign,set_severity");
    properties.put("assign.assignee", "fred");
    properties.put("set_severity.severity", "MINOR");

    actions.add(new MockAction("set_severity"));
    actions.add(new MockAction("assign"));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).isEmpty();

    verify(issueStorage, times(1)).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications, times(1)).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_load_issues_from_issue_keys_with_maximum_page_size() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD,DEFG");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    actions.add(new MockAction("assign"));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    service.execute(issueBulkChangeQuery, userSession);

    ArgumentCaptor<IssueQuery> captor = ArgumentCaptor.forClass(IssueQuery.class);
    verify(finder).find(captor.capture());
    IssueQuery query = captor.getValue();
    assertThat(query.issueKeys()).containsOnly("ABCD", "DEFG");
    assertThat(query.pageSize()).isEqualTo(IssueQuery.MAX_PAGE_SIZE);
    assertThat(query.requiredRole()).isEqualTo(UserRole.USER);
  }

  @Test
  public void should_not_execute_bulk_if_issue_does_not_support_action() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    actions.add(new MockAction("assign", true, true, false));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).isEmpty();
    assertThat(result.issuesNotChanged()).hasSize(1);

    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void should_not_execute_bulk_if_action_could_not_be_executed_on_issue() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    actions.add(new MockAction("assign", true, false, true));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).isEmpty();
    assertThat(result.issuesNotChanged()).hasSize(1);

    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void should_not_execute_bulk_on_unexpected_error() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");

    Action action = mock(Action.class);
    when(action.key()).thenReturn("assign");
    when(action.supports(any(Issue.class))).thenReturn(true);
    when(action.verify(anyMap(), anyListOf(Issue.class), any(UserSession.class))).thenReturn(true);
    doThrow(new RuntimeException("Error")).when(action).execute(anyMap(), any(IssueBulkChangeService.ActionContext.class));
    actions.add(action);

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).isEmpty();
    assertThat(result.issuesNotChanged()).hasSize(1);

    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void should_fail_if_user_not_logged() {
    userSession = MockUserSession.create().setLogin(null);

    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties);
    try {
      service.execute(issueBulkChangeQuery, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnauthorizedException.class);
    }
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
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("The action : 'unknown' is unknown");
    }
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }


  class MockAction extends Action {

    private boolean verify;
    private boolean execute;

    public MockAction(String key, boolean verify, boolean execute, final boolean support) {
      super(key);
      this.verify = verify;
      this.execute = execute;
      setConditions(new Condition() {
        @Override
        public boolean matches(Issue issue) {
          return support;
        }
      });
    }

    public MockAction(String key) {
      this(key, true, true, true);
    }

    @Override
    boolean verify(Map<String, Object> properties, List<Issue> issues, UserSession userSession) {
      return verify;
    }

    @Override
    boolean execute(Map<String, Object> properties, Context context) {
      return execute;
    }
  }
}
