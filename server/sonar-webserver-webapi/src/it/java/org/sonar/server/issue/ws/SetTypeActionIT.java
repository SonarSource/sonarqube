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

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rules.RuleType;
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
import static org.mockito.Mockito.when;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;

@RunWith(DataProviderRunner.class)
public class SetTypeActionIT {

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
  private WsActionTester tester = new WsActionTester(new SetTypeAction(userSession, dbClient, issueChangeEventService,
    new IssueFinder(dbClient, userSession), new IssueFieldsSetter(),
    new IssueUpdater(dbClient,
      new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, new SequenceUuidFactory()),
      mock(NotificationManager.class), issueChangePostProcessor, issuesChangesSerializer),
    responseWriter, system2));

  @Test
  @UseDataProvider("allTypesFromToExceptHotspots")
  public void set_type(RuleType from, RuleType to) {
    long now = 1_999_777_234L;
    when(system2.now()).thenReturn(now);
    IssueDto issueDto = newIssueWithProject(from);
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), to.name());

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class), eq(true));
    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getType()).isEqualTo(to.getDbConstant());

    if (from != to) {
      verifyContentOfPreloadedSearchResponseData(issueDto);
      assertThat(issueChangePostProcessor.calledComponents())
        .extracting(ComponentDto::uuid)
        .containsExactlyInAnyOrder(issueDto.getComponentUuid());
      verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any(), any(), any(), any(), any());
    } else {
      assertThat(issueChangePostProcessor.wasCalled())
        .isFalse();
    }
  }

  @Test
  public void set_type_is_not_distributed_for_pull_request() {
    RuleDto rule = dbTester.rules().insertIssueRule();
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();

    ComponentDto pullRequest = dbTester.components().insertProjectBranch(project, b -> b.setKey("myBranch1")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.uuid()));

    ComponentDto file = dbTester.components().insertComponent(newFileDto(pullRequest));
    IssueDto issue = newIssue(rule, pullRequest, file).setType(CODE_SMELL).setSeverity(MAJOR);
    issueDbTester.insertIssue(issue);

    setUserWithBrowseAndAdministerIssuePermission(issue);

    call(issue.getKey(), BUG.name());

    verifyNoInteractions(issueChangeEventService);
  }

  @Test
  public void insert_entry_in_changelog_when_setting_type() {
    IssueDto issueDto = newIssueWithProject(CODE_SMELL);
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), BUG.name());

    List<FieldDiffs> fieldDiffs = dbClient.issueChangeDao().selectChangelogByIssue(dbTester.getSession(), issueDto.getKey());
    assertThat(fieldDiffs).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs()).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs().get("type").newValue()).isEqualTo(BUG.name());
    assertThat(fieldDiffs.get(0).diffs().get("type").oldValue()).isEqualTo(CODE_SMELL.name());
  }

  @Test
  public void fail_if_bad_type_value() {
    IssueDto issueDto = newIssueWithProject(CODE_SMELL);
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    String issueDtoKey = issueDto.getKey();
    assertThatThrownBy(() -> call(issueDtoKey, "unknown"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'type' (unknown) must be one of: [CODE_SMELL, BUG, VULNERABILITY]");
  }

  @Test
  public void fail_if_SECURITY_HOTSPOT_value() {
    IssueDto issueDto = newIssueWithProject(CODE_SMELL);
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    String issueDtoKey = issueDto.getKey();
    assertThatThrownBy(() -> call(issueDtoKey, "SECURITY_HOTSPOT"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'type' (SECURITY_HOTSPOT) must be one of: [CODE_SMELL, BUG, VULNERABILITY]");
  }

  @Test
  public void fail_when_not_authenticated() {
    assertThatThrownBy(() -> call("ABCD", BUG.name()))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_browse_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    String login = "john";
    String permission = ISSUE_ADMIN;
    logInAndAddProjectPermission(login, issueDto, permission);

    assertThatThrownBy(() -> call(issueDto.getKey(), BUG.name()))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  @UseDataProvider("allTypesExceptSecurityHotspot")
  public void fail_type_except_hotspot_when_missing_administer_issue_permission(RuleType type) {
    IssueDto issueDto = issueDbTester.insertIssue(issue -> issue.setType(type));
    logInAndAddProjectPermission("john", issueDto, USER);

    assertThatThrownBy(() -> call(issueDto.getKey(), type.name()))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @UseDataProvider("allTypesExceptSecurityHotspot")
  public void fail_NFE_if_trying_to_change_type_of_a_hotspot(RuleType type) {
    long now = 1_999_777_234L;
    when(system2.now()).thenReturn(now);
    IssueDto hotspot = issueDbTester.insertHotspot();
    setUserWithBrowseAndAdministerIssuePermission(hotspot);

    String hotspotKey = hotspot.getKey();
    String typeName = type.name();
    assertThatThrownBy(() -> call(hotspotKey, typeName))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Issue with key '%s' does not exist", hotspotKey);
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("set_type");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);
    assertThat(action.responseExample()).isNotNull();
  }

  private TestResponse call(@Nullable String issueKey, @Nullable String type) {
    TestRequest request = tester.newRequest();
    ofNullable(issueKey).ifPresent(issue -> request.setParam("issue", issue));
    ofNullable(type).ifPresent(t -> request.setParam("type", t));
    return request.execute();
  }

  private IssueDto newIssueWithProject(RuleType type) {
    RuleDto rule = dbTester.rules().insert();
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    return issueDbTester.insert(rule, project, file, i -> i.setType(type));
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

  private void logInAndAddProjectPermission(String login, IssueDto issueDto, String permission) {
    BranchDto branchDto = dbClient.branchDao().selectByUuid(dbTester.getSession(), issueDto.getProjectUuid())
      .orElseThrow(() -> new IllegalStateException(format("Couldn't find branch with uuid : %s", issueDto.getProjectUuid())));
    userSession.logIn(login)
      .addProjectPermission(permission, dbClient.projectDao().selectByUuid(dbTester.getSession(), branchDto.getProjectUuid())
        .orElseThrow(() -> new IllegalStateException(format("Couldn't find project with uuid %s", branchDto.getProjectUuid()))));
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

  @DataProvider
  public static Object[][] allTypesExceptSecurityHotspot() {
    return EnumSet.allOf(RuleType.class)
      .stream()
      .filter(ruleType -> SECURITY_HOTSPOT != ruleType)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @DataProvider
  public static Object[][] allTypesFromToExceptHotspots() {
    Set<RuleType> set = EnumSet.allOf(RuleType.class)
      .stream()
      .filter(ruleType -> SECURITY_HOTSPOT != ruleType)
      .collect(Collectors.toSet());
    return Sets.cartesianProduct(set, set)
      .stream()
      .map(ruleTypes -> new Object[] {ruleTypes.get(0), ruleTypes.get(1)})
      .toArray(Object[][]::new);
  }
}
