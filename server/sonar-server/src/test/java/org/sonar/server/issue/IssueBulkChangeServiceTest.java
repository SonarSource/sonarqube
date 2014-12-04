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
import org.sonar.api.issue.condition.Condition;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.Rule;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.search.QueryContext;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class IssueBulkChangeServiceTest {

  DbClient dbClient = mock(DbClient.class);
  DbSession dbSession = mock(DbSession.class);

  IssueDao issueDao = mock(IssueDao.class);
  IssueService issueService = mock(IssueService.class);
  IssueStorage issueStorage = mock(IssueStorage.class);
  DefaultRuleFinder ruleFinder = mock(DefaultRuleFinder.class);
  ComponentDao componentDao = mock(ComponentDao.class);
  IssueNotifications issueNotifications = mock(IssueNotifications.class);

  IssueBulkChangeService service;

  UserSession userSession = MockUserSession.create().setLogin("john").setUserId(10);

  DefaultIssue issue;
  Rule rule;
  ComponentDto project;
  ComponentDto file;

  List<Action> actions;

  @Before
  public void before() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.issueDao()).thenReturn(issueDao);

    rule = Rule.create("repo", "key");
    when(ruleFinder.findByKeys(newHashSet(rule.ruleKey()))).thenReturn(newArrayList(rule));

    project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setLongName("My Project")
      .setQualifier(Qualifiers.PROJECT)
      .setScope(Scopes.PROJECT);
    when(componentDao.getByKeys(dbSession, newHashSet(project.key()))).thenReturn(newArrayList(project));

    file = new ComponentDto()
      .setId(2L)
      .setParentProjectId(project.getId())
      .setKey("MyComponent")
      .setLongName("My Component");
    when(componentDao.getByKeys(dbSession, newHashSet(file.key()))).thenReturn(newArrayList(file));

    IssueDto issueDto = IssueTesting.newDto(RuleTesting.newDto(rule.ruleKey()).setId(50), file, project).setKee("ABCD");
    issue = issueDto.toDefaultIssue();

    org.sonar.server.search.Result<Issue> result = mock(org.sonar.server.search.Result.class);
    when(result.getHits()).thenReturn(newArrayList((Issue) issue));
    when(issueService.search(any(IssueQuery.class), any(QueryContext.class))).thenReturn(result);
    when(issueDao.selectByKeys(dbSession, newArrayList(issue.key()))).thenReturn(newArrayList(issueDto));

    actions = newArrayList();
    service = new IssueBulkChangeService(dbClient, issueService, issueStorage, ruleFinder, issueNotifications, actions, mock(PreviewCache.class));
  }

  @Test
  public void should_execute_bulk_change() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    actions.add(new MockAction("assign"));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).isEmpty();

    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(rule), eq(project), eq(file));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_skip_send_notifications() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    actions.add(new MockAction("assign"));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, false);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).isEmpty();

    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
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

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, "my comment", true);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).isEmpty();

    verify(commentAction).execute(anyMap(), any(IssueBulkChangeService.ActionContext.class));
    verify(issueStorage).save(eq(issue));
  }

  @Test
  public void should_execute_bulk_change_with_comment_only_on_changed_issues() {
    IssueDto issueDto1 = IssueTesting.newDto(RuleTesting.newDto(rule.ruleKey()).setId(50), file, project).setKee("ABCD");
    IssueDto issueDto2 = IssueTesting.newDto(RuleTesting.newDto(rule.ruleKey()).setId(50), file, project).setKee("EFGH");

    org.sonar.server.search.Result<Issue> resultIssues = mock(org.sonar.server.search.Result.class);
    when(resultIssues.getHits()).thenReturn(Lists.<Issue>newArrayList(issueDto1.toDefaultIssue(), issueDto2.toDefaultIssue()));
    when(issueService.search(any(IssueQuery.class), any(QueryContext.class))).thenReturn(resultIssues);
    when(issueDao.selectByKeys(dbSession, newArrayList("ABCD", "EFGH"))).thenReturn(newArrayList(issueDto1, issueDto2));

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

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, "my comment", true);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).hasSize(1);

    // Only one issue will receive the comment
    verify(assignAction, times(1)).execute(anyMap(), any(IssueBulkChangeService.ActionContext.class));
    verify(issueStorage).save(eq(issueDto1.toDefaultIssue()));
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

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(1);
    assertThat(result.issuesNotChanged()).isEmpty();

    verify(issueStorage, times(1)).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(rule), eq(project), eq(file));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_not_execute_bulk_if_issue_does_not_support_action() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    actions.add(new MockAction("assign", true, true, false));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).isEmpty();
    assertThat(result.issuesNotChanged()).hasSize(1);

    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void should_not_execute_bulk_if_action_is_not_verified() {
    Map<String, Object> properties = newHashMap();
    properties.put("issues", "ABCD");
    properties.put("actions", "assign");
    properties.put("assign.assignee", "fred");
    actions.add(new MockAction("assign", false, true, true));

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).isEmpty();
    assertThat(result.issuesNotChanged()).isEmpty();

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

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
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

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
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
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
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
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
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
