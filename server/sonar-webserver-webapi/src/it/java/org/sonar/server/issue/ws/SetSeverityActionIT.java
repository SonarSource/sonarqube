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

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.pushapi.issues.IssueChangeEventService;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;

public class SetSeverityActionIT {

  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = mock(System2.class);

  private DbClient dbClient = dbTester.getDbClient();
  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private ArgumentCaptor<SearchResponseData> preloadedSearchResponseDataCaptor = ArgumentCaptor.forClass(SearchResponseData.class);

  private IssueChangeEventService issueChangeEventService = mock(IssueChangeEventService.class);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), null);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private IssuesChangesNotificationSerializer issuesChangesSerializer = new IssuesChangesNotificationSerializer();
  private WsActionTester tester = new WsActionTester(new SetSeverityAction(userSession, dbClient, issueChangeEventService,
    new IssueFinder(dbClient, userSession), new IssueFieldsSetter(),
    new IssueUpdater(dbClient,
      new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, new SequenceUuidFactory()),
      mock(NotificationManager.class), issueChangePostProcessor, issuesChangesSerializer),
    responseWriter));

  @Test
  public void set_severity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), MINOR);

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class), eq(true));
    verifyContentOfPreloadedSearchResponseData(issueDto);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any(), any(), any(), any());

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(issueReloaded.isManualSeverity()).isTrue();
    assertThat(issueChangePostProcessor.calledComponents())
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(issueDto.getComponentUuid());
  }

  @Test
  public void set_severity_is_not_distributed_for_pull_request() {
    RuleDto rule = dbTester.rules().insertIssueRule();
    ComponentDto mainBranch = dbTester.components().insertPrivateProject().getMainBranchComponent();

    ComponentDto pullRequest = dbTester.components().insertProjectBranch(mainBranch, b -> b.setKey("myBranch1")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(mainBranch.uuid()));

    ComponentDto file = dbTester.components().insertComponent(newFileDto(pullRequest));
    IssueDto issue = newIssue(rule, pullRequest, file).setType(CODE_SMELL).setSeverity(MAJOR);
    issueDbTester.insertIssue(issue);

    setUserWithBrowseAndAdministerIssuePermission(issue);

    call(issue.getKey(), MINOR);

    verifyNoInteractions(issueChangeEventService);
  }

  @Test
  public void insert_entry_in_changelog_when_setting_severity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), MINOR);

    List<FieldDiffs> fieldDiffs = dbClient.issueChangeDao().selectChangelogByIssue(dbTester.getSession(), issueDto.getKey());
    assertThat(fieldDiffs).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs()).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs().get("severity").newValue()).isEqualTo(MINOR);
    assertThat(fieldDiffs.get(0).diffs().get("severity").oldValue()).isEqualTo(MAJOR);
  }

  @Test
  public void fail_if_bad_severity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity("unknown"));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    assertThatThrownBy(() -> call(issueDto.getKey(), "unknown"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'severity' (unknown) must be one of: [INFO, MINOR, MAJOR, CRITICAL, BLOCKER]");
  }

  @Test
  public void fail_NFE_if_hotspot() {
    IssueDto hotspot = issueDbTester.insertHotspot(h -> h.setSeverity("CRITICAL"));
    setUserWithBrowseAndAdministerIssuePermission(hotspot);

    String hotspotKey = hotspot.getKey();
    assertThatThrownBy(() -> call(hotspotKey, "MAJOR"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Issue with key '%s' does not exist", hotspotKey);
  }

  @Test
  public void fail_when_not_authenticated() {
    assertThatThrownBy(() -> call("ABCD", MAJOR))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_browse_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    logInAndAddProjectPermission(issueDto, ISSUE_ADMIN);

    assertThatThrownBy(() -> call(issueDto.getKey(), MAJOR))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_missing_administer_issue_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    logInAndAddProjectPermission(issueDto, USER);

    assertThatThrownBy(() -> call(issueDto.getKey(), MAJOR))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("set_severity");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);
    assertThat(action.responseExample()).isNotNull();
  }

  private TestResponse call(@Nullable String issueKey, @Nullable String severity) {
    TestRequest request = tester.newRequest();
    ofNullable(issueKey).ifPresent(issue -> request.setParam("issue", issue));
    ofNullable(severity).ifPresent(value -> request.setParam("severity", value));
    return request.execute();
  }

  private void logInAndAddProjectPermission(IssueDto issueDto, String permission) {
    BranchDto branchDto = dbClient.branchDao().selectByUuid(dbTester.getSession(), issueDto.getProjectUuid())
      .orElseThrow(() -> new IllegalStateException(format("Couldn't find branch with uuid : %s", issueDto.getProjectUuid())));
    UserDto user = dbTester.users().insertUser("john");
    userSession.logIn(user)
      .addProjectPermission(permission, dbClient.projectDao().selectByUuid(dbTester.getSession(), branchDto.getProjectUuid())
        .orElseThrow(() -> new IllegalStateException(format("Couldn't find project with uuid %s", branchDto.getProjectUuid()))));
  }

  private void setUserWithBrowseAndAdministerIssuePermission(IssueDto issueDto) {
    BranchDto branchDto = dbClient.branchDao().selectByUuid(dbTester.getSession(), issueDto.getProjectUuid())
      .orElseThrow(() -> new IllegalStateException(format("Couldn't find branch with uuid : %s", issueDto.getProjectUuid())));
    ProjectDto project = dbClient.projectDao().selectByUuid(dbTester.getSession(), branchDto.getProjectUuid())
      .orElseThrow(() -> new IllegalStateException(format("Couldn't find project with uuid : %s", branchDto.getProjectUuid())));
    UserDto user = dbTester.users().insertUser("john");
    userSession.logIn(user)
      .addProjectPermission(ISSUE_ADMIN, project)
      .addProjectPermission(USER, project)
      .registerBranches(branchDto);
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
