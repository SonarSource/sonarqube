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
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.user.UserSession;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class IssueBulkChangeServiceTest {

  private DefaultIssueFinder finder = mock(DefaultIssueFinder.class);
  private IssueWorkflow workflow = mock(IssueWorkflow.class);
  private IssueUpdater issueUpdater = mock(IssueUpdater.class);
  private IssueStorage issueStorage = mock(IssueStorage.class);
  private IssueNotifications issueNotifications = mock(IssueNotifications.class);
  private ActionPlanService actionPlanService = mock(ActionPlanService.class);
  private UserFinder userFinder = mock(UserFinder.class);

  private IssueQueryResult issueQueryResult = mock(IssueQueryResult.class);
  private UserSession userSession = mock(UserSession.class);
  private DefaultIssue issue = new DefaultIssue().setKey("ABCD");

  private IssueBulkChangeService service;

  @Before
  public void before(){
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.userId()).thenReturn(10);
    when(userSession.login()).thenReturn("fred");

    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);
    when(issueQueryResult.issues()).thenReturn(newArrayList((Issue) issue));

    service = new IssueBulkChangeService(finder, workflow, actionPlanService, userFinder, issueUpdater, issueStorage, issueNotifications);
  }

  @Test
  public void should_do_bulk_assign(){
    String assignee = "perceval";
    when(userFinder.findByLogin(assignee)).thenReturn(new DefaultUser());

    IssueBulkChangeQuery issueBulkChangeQuery = IssueBulkChangeQuery.builder().issueKeys(newArrayList(issue.key())).assignee(assignee).build();
    Result result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.ok()).isTrue();
    assertThat((List)result.get()).hasSize(1);

    verify(issueUpdater).assign(eq(issue), eq(assignee), any(IssueChangeContext.class));
    verifyNoMoreInteractions(issueUpdater);
    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_do_bulk_plan(){
    String actionPlanKey = "EFGH";
    when(actionPlanService.findByKey(actionPlanKey, userSession)).thenReturn(new DefaultActionPlan());

    IssueBulkChangeQuery issueBulkChangeQuery = IssueBulkChangeQuery.builder().issueKeys(newArrayList(issue.key())).plan(actionPlanKey).build();
    Result result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.ok()).isTrue();
    assertThat((List)result.get()).hasSize(1);

    verify(issueUpdater).plan(eq(issue), eq(actionPlanKey), any(IssueChangeContext.class));
    verifyNoMoreInteractions(issueUpdater);
    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_do_bulk_change_severity(){
    String severity = "MINOR";

    IssueBulkChangeQuery issueBulkChangeQuery = IssueBulkChangeQuery.builder().issueKeys(newArrayList(issue.key())).severity(severity).build();
    Result result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.ok()).isTrue();
    assertThat((List)result.get()).hasSize(1);

    verify(issueUpdater).setManualSeverity(eq(issue), eq(severity), any(IssueChangeContext.class));
    verifyNoMoreInteractions(issueUpdater);
    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_do_bulk_transition(){
    String transition = "reopen";

    when(workflow.doTransition(eq(issue), eq(transition), any(IssueChangeContext.class))).thenReturn(true);

    IssueBulkChangeQuery issueBulkChangeQuery = IssueBulkChangeQuery.builder().issueKeys(newArrayList(issue.key())).transition(transition).build();
    Result result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.ok()).isTrue();
    assertThat((List)result.get()).hasSize(1);

    verify(workflow).doTransition(eq(issue), eq(transition), any(IssueChangeContext.class));
    verifyNoMoreInteractions(issueUpdater);
    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_do_bulk_comment(){
    String comment = "Bulk change comment";

    IssueBulkChangeQuery issueBulkChangeQuery = IssueBulkChangeQuery.builder().issueKeys(newArrayList(issue.key())).comment(comment).build();
    Result result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.ok()).isTrue();
    assertThat((List)result.get()).hasSize(1);

    verify(issueUpdater).addComment(eq(issue), eq(comment), any(IssueChangeContext.class));
    verifyNoMoreInteractions(issueUpdater);
    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

  @Test
  public void should_ignore_an_issue_if_an_action_fail(){
    when(issueQueryResult.issues()).thenReturn(newArrayList((Issue) issue, new DefaultIssue().setKey("EFGH")));

    // Bulk change with 2 actions : severity and comment
    IssueBulkChangeQuery issueBulkChangeQuery = IssueBulkChangeQuery.builder().issueKeys(newArrayList("ABCD", "EFGH")).severity("MAJOR").comment("Bulk change comment").build();

    // The first call the change severity is ok, the second will fail
    when(issueUpdater.setManualSeverity(any(DefaultIssue.class), eq("MAJOR"),any(IssueChangeContext.class))).thenReturn(true).thenThrow(new RuntimeException("Cant change severity"));

    Result result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.ok()).isFalse();
    assertThat(((Result.Message) result.errors().get(0)).text()).isEqualTo("Cant change severity");

    List<Issue> issues = (List) result.get();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).key()).isEqualTo("ABCD");

    verify(issueStorage).save(eq(issue));
    verifyNoMoreInteractions(issueStorage);
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult));
    verifyNoMoreInteractions(issueNotifications);
  }

}
