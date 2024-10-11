/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.issue.ws;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.pushapi.issues.IssueChangeEventService;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;

public class DoTransitionActionTest {

  private static final long NOW = 999_776_888L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();

  private IssueChangeEventService issueChangeEventService = mock(IssueChangeEventService.class);
  private IssueFieldsSetter updater = new IssueFieldsSetter();
  private IssueWorkflow workflow = new IssueWorkflow(new FunctionExecutor(updater), updater);
  private TransitionService transitionService = new TransitionService(userSession, workflow);
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), null);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private IssuesChangesNotificationSerializer issuesChangesSerializer = new IssuesChangesNotificationSerializer();
  private IssueUpdater issueUpdater = new IssueUpdater(dbClient,
    new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, new SequenceUuidFactory()),
    mock(NotificationManager.class), issueChangePostProcessor, issuesChangesSerializer);
  private ArgumentCaptor<SearchResponseData> preloadedSearchResponseDataCaptor = ArgumentCaptor.forClass(SearchResponseData.class);

  private WsAction underTest = new DoTransitionAction(dbClient, userSession, issueChangeEventService,
    new IssueFinder(dbClient, userSession), issueUpdater, transitionService, responseWriter, system2);
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    workflow.start();
  }

  @Test
  public void do_transition() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn(db.users().insertUser()).addProjectPermission(USER, project, file);

    call(issue.getKey(), "confirm");

    verify(responseWriter).write(eq(issue.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class), eq(true));
    verifyContentOfPreloadedSearchResponseData(issue);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any(), any(), any(), any());
    IssueDto issueReloaded = db.getDbClient().issueDao().selectByKey(db.getSession(), issue.getKey()).get();
    assertThat(issueReloaded.getStatus()).isEqualTo(STATUS_CONFIRMED);
    assertThat(issueChangePostProcessor.calledComponents()).containsExactlyInAnyOrder(file);
  }

  @Test
  public void do_transition_is_not_distributed_for_pull_request() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();

    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setKey("myBranch1")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.uuid()));

    ComponentDto file = db.components().insertComponent(newFileDto(pullRequest));
    IssueDto issue = newIssue(rule, pullRequest, file).setType(CODE_SMELL).setSeverity(MAJOR);
    db.issues().insertIssue(issue);
    userSession.logIn(db.users().insertUser()).addProjectPermission(USER, pullRequest, file);

    call(issue.getKey(), "confirm");

    verifyNoInteractions(issueChangeEventService);
  }

  @Test
  public void fail_if_external_issue() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto externalRule = db.rules().insertIssueRule(r -> r.setIsExternal(true));
    IssueDto externalIssue = db.issues().insertIssue(externalRule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn().addProjectPermission(USER, project, file);

    assertThatThrownBy(() -> call(externalIssue.getKey(), "confirm"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Transition is not allowed on issues imported from external rule engines");
  }

  @Test
  public void fail_if_hotspot() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertHotspotRule();
    IssueDto hotspot = db.issues().insertHotspot(rule, project, file, i -> i.setType(RuleType.SECURITY_HOTSPOT));
    userSession.logIn().addProjectPermission(USER, project, file);

    String hotspotKey = hotspot.getKey();
    assertThatThrownBy(() -> call(hotspotKey, "confirm"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Issue with key '%s' does not exist", hotspotKey);
  }

  @Test
  public void fail_if_issue_does_not_exist() {
    userSession.logIn();

    assertThatThrownBy(() -> call("UNKNOWN", "confirm"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_no_issue_param() {
    userSession.logIn();

    assertThatThrownBy(() -> call(null, "confirm"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_no_transition_param() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn().addProjectPermission(USER, project, file);

    assertThatThrownBy(() -> call(issue.getKey(), null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_not_enough_permission_to_access_issue() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn().addProjectPermission(CODEVIEWER, project, file);

    assertThatThrownBy(() -> call(issue.getKey(), "confirm"))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_not_enough_permission_to_apply_transition() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn().addProjectPermission(USER, project, file);

    // False-positive transition is requiring issue admin permission
    assertThatThrownBy(() -> call(issue.getKey(), "falsepositive"))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_not_authenticated() {
    assertThatThrownBy(() -> call("ISSUE_KEY", "confirm"))
      .isInstanceOf(UnauthorizedException.class);
  }

  private TestResponse call(@Nullable String issueKey, @Nullable String transition) {
    TestRequest request = tester.newRequest();
    if (issueKey != null) {
      request.setParam("issue", issueKey);
    }
    if (transition != null) {
      request.setParam("transition", transition);
    }
    return request.execute();
  }

  private void verifyContentOfPreloadedSearchResponseData(IssueDto issue) {
    SearchResponseData preloadedSearchResponseData = preloadedSearchResponseDataCaptor.getValue();
    assertThat(preloadedSearchResponseData.getIssues())
      .extracting(IssueDto::getKey)
      .containsOnly(issue.getKey());
    assertThat(preloadedSearchResponseData.getRules())
      .extracting(RuleDto::getKey)
      .containsOnly(issue.getRuleKey());
    assertThat(preloadedSearchResponseData.getComponents())
      .extracting(ComponentDto::uuid)
      .containsOnly(issue.getComponentUuid(), issue.getProjectUuid());
  }

}
