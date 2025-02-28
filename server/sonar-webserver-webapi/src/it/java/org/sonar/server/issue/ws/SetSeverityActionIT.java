/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
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
import org.sonar.db.issue.ImpactDto;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.core.rule.RuleType.BUG;
import static org.sonar.core.rule.RuleType.CODE_SMELL;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;

class SetSeverityActionIT {

  @RegisterExtension
  public DbTester dbTester = DbTester.create();
  @RegisterExtension
  public EsTester es = EsTester.create();
  @RegisterExtension
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
  void set_severity_whenSetSeverity_shouldAlsoUpdateImpactSeverity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR).setType(CODE_SMELL)
      .replaceAllImpacts(List.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, false))));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), MINOR, null);

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class), eq(true));
    verifyContentOfPreloadedSearchResponseData(issueDto);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), eq(MINOR), eq(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.LOW)), any(), any(), any(), any());

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(issueReloaded.isManualSeverity()).isTrue();
    Set<ImpactDto> impacts = issueReloaded.getImpacts();
    assertThat(impacts).hasSize(1);
    ImpactDto impact = impacts.iterator().next();
    assertThat(impact.getSoftwareQuality()).isEqualTo(SoftwareQuality.MAINTAINABILITY);
    assertThat(impact.getSeverity()).isEqualTo(Severity.LOW);
    assertThat(impact.isManualSeverity()).isTrue();
    assertThat(issueChangePostProcessor.calledComponents())
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(issueDto.getComponentUuid());
  }

  @Test
  void set_severity_whenSetSeverityAndTypeNotMatch_shouldNotUpdateImpactSeverity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR).setType(BUG));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), MINOR, null);

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class),
      any(Response.class), eq(true));
    verifyContentOfPreloadedSearchResponseData(issueDto);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any(), any(), any(), any(), any());

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(issueReloaded.isManualSeverity()).isTrue();

    Set<ImpactDto> impacts = issueReloaded.getImpacts();
    assertThat(impacts).hasSize(1);
    ImpactDto impact = impacts.iterator().next();
    assertThat(impact.getSoftwareQuality()).isEqualTo(SoftwareQuality.MAINTAINABILITY);
    assertThat(impact.getSeverity()).isEqualTo(Severity.HIGH);
    assertThat(impact.isManualSeverity()).isFalse();
  }

  @Test
  void set_severity_whenIssueHasNuImpacts_shouldProperlyUpdate() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR).setType(CODE_SMELL).replaceAllImpacts(List.of()));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), null, "MAINTAINABILITY=LOW");

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class),
      any(Response.class), eq(true));
    verifyContentOfPreloadedSearchResponseData(issueDto);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any(), any(), any(), any(), any());

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(issueReloaded.isManualSeverity()).isTrue();
    Set<ImpactDto> impacts = issueReloaded.getImpacts();
    assertThat(impacts).hasSize(1);
    ImpactDto impact = impacts.iterator().next();
    assertThat(impact.getSoftwareQuality()).isEqualTo(SoftwareQuality.MAINTAINABILITY);
    assertThat(impact.getSeverity()).isEqualTo(Severity.LOW);
    assertThat(impact.isManualSeverity()).isTrue();
    assertThat(issueChangePostProcessor.calledComponents())
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(issueDto.getComponentUuid());
  }

  @Test
  void set_severity_whenSetImpactSeverity_shouldAlsoUpdateSeverity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR).setType(CODE_SMELL));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), null, "MAINTAINABILITY=LOW");

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class),
      any(Response.class), eq(true));
    verifyContentOfPreloadedSearchResponseData(issueDto);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), eq(MINOR), eq(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.LOW)), any(), any(), any(), any());

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(issueReloaded.isManualSeverity()).isTrue();
    Set<ImpactDto> impacts = issueReloaded.getImpacts();
    assertThat(impacts).hasSize(1);
    ImpactDto impact = impacts.iterator().next();
    assertThat(impact.getSoftwareQuality()).isEqualTo(SoftwareQuality.MAINTAINABILITY);
    assertThat(impact.getSeverity()).isEqualTo(Severity.LOW);
    assertThat(impact.isManualSeverity()).isTrue();
    assertThat(issueChangePostProcessor.calledComponents())
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(issueDto.getComponentUuid());
  }

  @Test
  void set_severity_whenIssueTypeNotMatchSoftwareQuality_shouldNotUpdateImpactSeverity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR).setType(BUG));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), null, "MAINTAINABILITY=LOW");

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class),
      any(Response.class), eq(true));
    verifyContentOfPreloadedSearchResponseData(issueDto);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any(), any(), any(), any(), any());

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(MAJOR);
    assertThat(issueReloaded.isManualSeverity()).isFalse();
    Set<ImpactDto> impacts = issueReloaded.getImpacts();
    assertThat(impacts).hasSize(1);
    ImpactDto impact = impacts.iterator().next();
    assertThat(impact.getSoftwareQuality()).isEqualTo(SoftwareQuality.MAINTAINABILITY);
    assertThat(impact.getSeverity()).isEqualTo(Severity.LOW);
    assertThat(impact.isManualSeverity()).isTrue();
    assertThat(issueChangePostProcessor.calledComponents())
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(issueDto.getComponentUuid());
  }

  @Test
  void set_severity_whenImpactAndManualFlagNotChanged_shouldNotTriggerUpdate() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.replaceAllImpacts(List.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY,
      Severity.LOW, true))));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), null, "MAINTAINABILITY=LOW");

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class),
      any(Response.class), eq(true));

    verify(issueChangeEventService, times(0)).distributeIssueChangeEvent(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void set_severity_whenWrongSoftwareQualityReceived_shouldThrowException() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);
    String key = issueDto.getKey();

    assertThatThrownBy(() -> call(key, null, "SECURITY=HIGH"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Issue does not support impact SECURITY");
  }

  @Test
  void set_severity_whenSeverityAndImpactsReceived_shouldThrowException() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR).setType(CODE_SMELL));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);
    String key = issueDto.getKey();

    assertThatThrownBy(() -> call(key, "BLOCKER", "MAINTAINABILITY=LOW"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameters 'severity' and 'impact' cannot be used at the same time");
  }

  @Test
  void set_severity_whenWrongImpactFormat_shouldThrowException() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR).setType(CODE_SMELL));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);
    String key = issueDto.getKey();

    assertThatThrownBy(() -> call(key, null, "MAINTAINABILITY"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid impact format: MAINTAINABILITY");
    assertThatThrownBy(() -> call(key, null, "MAINTAINABILITY=HIGH,RELIABILITY=LOW"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid impact format: MAINTAINABILITY=HIGH,RELIABILITY=LOW");
    assertThatThrownBy(() -> call(key, null, "MAINTAINABILITY:HIGH"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid impact format: MAINTAINABILITY:HIGH");
    assertThatThrownBy(() -> call(key, null, "MAINTAINABILITY=MAJOR"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.issue.impact.Severity.MAJOR");
  }

  @Test
  void set_severity_is_not_distributed_for_pull_request() {
    RuleDto rule = dbTester.rules().insertIssueRule();
    ComponentDto mainBranch = dbTester.components().insertPrivateProject().getMainBranchComponent();

    ComponentDto pullRequest = dbTester.components().insertProjectBranch(mainBranch, b -> b.setKey("myBranch1")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(mainBranch.uuid()));

    ComponentDto file = dbTester.components().insertComponent(newFileDto(pullRequest));
    IssueDto issue = newIssue(rule, pullRequest, file).setType(CODE_SMELL).setSeverity(MAJOR);
    issueDbTester.insertIssue(issue);

    setUserWithBrowseAndAdministerIssuePermission(issue);

    call(issue.getKey(), MINOR, null);

    verifyNoInteractions(issueChangeEventService);
  }

  @Test
  void insert_entry_in_changelog_when_setting_severity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity(MAJOR).setType(CODE_SMELL));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), MINOR, null);

    List<FieldDiffs> fieldDiffs = dbClient.issueChangeDao().selectChangelogByIssue(dbTester.getSession(), issueDto.getKey());
    assertThat(fieldDiffs).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs()).hasSize(2);
    assertThat(fieldDiffs.get(0).diffs().get("severity").newValue()).isEqualTo(MINOR);
    assertThat(fieldDiffs.get(0).diffs().get("severity").oldValue()).isEqualTo(MAJOR);
    assertThat(fieldDiffs.get(0).diffs().get("impactSeverity").oldValue()).isEqualTo("MAINTAINABILITY:HIGH");
    assertThat(fieldDiffs.get(0).diffs().get("impactSeverity").newValue()).isEqualTo("MAINTAINABILITY:LOW");
  }

  @Test
  void fail_if_bad_severity() {
    IssueDto issueDto = issueDbTester.insertIssue(i -> i.setSeverity("unknown"));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);
    String key = issueDto.getKey();
    assertThatThrownBy(() -> call(key, "unknown", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'severity' (unknown) must be one of: [INFO, MINOR, MAJOR, CRITICAL, BLOCKER]");
  }

  @Test
  void fail_NFE_if_hotspot() {
    IssueDto hotspot = issueDbTester.insertHotspot(h -> h.setSeverity("CRITICAL"));
    setUserWithBrowseAndAdministerIssuePermission(hotspot);

    String hotspotKey = hotspot.getKey();
    assertThatThrownBy(() -> call(hotspotKey, "MAJOR", null))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Issue with key '%s' does not exist", hotspotKey);
  }

  @Test
  void fail_when_not_authenticated() {
    assertThatThrownBy(() -> call("ABCD", MAJOR, null))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void fail_when_missing_browse_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    logInAndAddProjectPermission(issueDto, ISSUE_ADMIN);
    String key = issueDto.getKey();
    assertThatThrownBy(() -> call(key, MAJOR, null))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void fail_when_missing_administer_issue_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    logInAndAddProjectPermission(issueDto, USER);
    String key = issueDto.getKey();
    assertThatThrownBy(() -> call(key, MAJOR, null))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("set_severity");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(3);
    assertThat(action.responseExample()).isNotNull();
  }

  private TestResponse call(@Nullable String issueKey, @Nullable String severity, @Nullable String impact) {
    TestRequest request = tester.newRequest();
    ofNullable(issueKey).ifPresent(issue -> request.setParam("issue", issue));
    ofNullable(severity).ifPresent(value -> request.setParam("severity", value));
    ofNullable(impact).ifPresent(value -> request.setParam("impact", value));
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
