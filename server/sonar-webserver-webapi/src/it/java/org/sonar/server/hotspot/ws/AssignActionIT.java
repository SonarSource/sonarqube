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
package org.sonar.server.hotspot.ws;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.ws.IssueUpdater;
import org.sonar.server.pushapi.hotspots.HotspotChangeEventService;
import org.sonar.server.pushapi.hotspots.HotspotChangedEvent;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUSES;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.db.component.ComponentTesting.newFileDto;

@RunWith(DataProviderRunner.class)
public class AssignActionIT {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final DbClient dbClient = dbTester.getDbClient();
  private final IssueUpdater issueUpdater = mock(IssueUpdater.class);
  private HotspotChangeEventService hotspotChangeEventService = mock(HotspotChangeEventService.class);
  private final System2 system2 = mock(System2.class);
  private final IssueFieldsSetter issueFieldsSetter = mock(IssueFieldsSetter.class);
  private final HotspotWsSupport hotspotWsSupport = new HotspotWsSupport(dbClient, userSessionRule, system2);

  private final AssignAction underTest = new AssignAction(dbClient, hotspotWsSupport, issueFieldsSetter, issueUpdater, hotspotChangeEventService);
  private final WsActionTester actionTester = new WsActionTester(underTest);
  private BranchDto branchDto = mock(BranchDto.class);

  @Before
  public void setMock() {
    when(issueUpdater.getBranch(any(), any())).thenReturn(branchDto);
  }

  @Test
  public void wsExecution_whenDefined() {
    WebService.Action wsDefinition = actionTester.getDef();

    assertThat(wsDefinition.isPost()).isTrue();
    assertThat(wsDefinition.isInternal()).isTrue();
    assertThat(wsDefinition.params()).hasSize(3);
    WebService.Param hotspotParam = wsDefinition.param("hotspot");
    assertThat(hotspotParam).isNotNull();
    assertThat(hotspotParam.isRequired()).isTrue();
    WebService.Param assigneeParam = wsDefinition.param("assignee");
    assertThat(assigneeParam).isNotNull();
    assertThat(assigneeParam.isRequired()).isFalse();
    WebService.Param commentParam = wsDefinition.param("comment");
    assertThat(commentParam).isNotNull();
    assertThat(commentParam.isRequired()).isFalse();
    assertThat(wsDefinition.since()).isEqualTo("8.2");
    assertThat(wsDefinition.changelog())
      .extracting(Change::getVersion, Change::getDescription)
      .contains(tuple("8.9", "Parameter 'assignee' is no longer mandatory"));
  }

  @Test
  public void wsExecution_whenAssignedForPublicProject() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());

    UserDto assignee = insertUser(secure().nextAlphanumeric(15));
    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, assignee.getLogin(), null);

    verifyFieldSetters(assignee, null);
  }

  @Test
  public void wsExecution_whenUnassignedForPublicProject() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    UserDto assignee = insertUser(secure().nextAlphanumeric(15));

    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> h.setAssigneeUuid(assignee.getUuid()));

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());
    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), isNull(), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, null, null);

    verifyFieldSetters(null, null);
  }

  @Test
  public void wsExecution_whenMyselfAssignedForPublicProject() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto me = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(me).registerProjects(projectData.getProjectDto());

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(me), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, me.getLogin(), null);

    verifyFieldSetters(me, null);
  }

  @Test
  public void wsExecution_whenMyselfUnassignedForPublicProject() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    UserDto me = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(me).registerProjects(projectData.getProjectDto());
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> h.setAssigneeUuid(me.getUuid()));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), isNull(), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, null, null);

    verifyFieldSetters(null, null);
  }

  @Test
  public void wsExecution_whenAssigneeForPrivateProject() {
    ProjectData project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    IssueDto hotspot = dbTester.issues().insertHotspot(project.getMainBranchComponent(), file);

    insertAndLoginAsUserWithProjectUserPermission(secure().nextAlphanumeric(10), project.getProjectDto(), UserRole.USER);
    UserDto assignee = insertUserWithProjectUserPermission(secure().nextAlphanumeric(15), project.getProjectDto());

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, assignee.getLogin(), null);

    verifyFieldSetters(assignee, null);
  }

  @Test
  public void wsExecution_whenUnassignedForPrivateProject() {
    ProjectData project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    UserDto assignee = insertUser(secure().nextAlphanumeric(15));
    IssueDto hotspot = dbTester.issues().insertHotspot(project.getMainBranchComponent(), file, h -> h.setAssigneeUuid(assignee.getUuid()));

    insertAndLoginAsUserWithProjectUserPermission(secure().nextAlphanumeric(10), project.getProjectDto(), UserRole.USER);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), isNull(), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, null, null);

    verifyFieldSetters(null, null);
  }

  @Test
  public void wsExecution_whenAssigneeForPrivateProjectBranch() {
    ProjectData project = dbTester.components().insertPrivateProject();
    ComponentDto branch = dbTester.components().insertProjectBranch(project.getMainBranchComponent());
    ComponentDto file = dbTester.components().insertComponent(newFileDto(branch, project.getMainBranchComponent().uuid()));
    IssueDto hotspot = dbTester.issues().insertHotspot(branch, file);

    insertAndLoginAsUserWithProjectUserPermission(secure().nextAlphanumeric(10), project.getProjectDto(), UserRole.USER);
    userSessionRule.addProjectBranchMapping(project.projectUuid(), branch);
    UserDto assignee = insertUserWithProjectUserPermission(secure().nextAlphanumeric(15), project.getProjectDto());

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, assignee.getLogin(), null);

    verifyFieldSetters(assignee, null);
  }

  @Test
  public void wsExecution_whenAssigneeDoesNotHaveAccessToPrivateProject_shouldFail() {
    ProjectData project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    IssueDto hotspot = dbTester.issues().insertHotspot(project.getMainBranchComponent(), file);

    insertAndLoginAsUserWithProjectUserPermission(secure().nextAlphanumeric(10), project.getProjectDto(), UserRole.USER);
    UserDto assignee = insertUser(secure().nextAlphanumeric(15));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    String login = assignee.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provided user with login '%s' does not have 'Browse' permission to project", login);
  }

  @Test
  public void wsExecution_whenAssigneeDoesNotHaveAccessToPrivateProjectBranch_shouldFail() {
    ProjectData project = dbTester.components().insertPrivateProject();
    ComponentDto branch = dbTester.components().insertProjectBranch(project.getMainBranchComponent());
    ComponentDto file = dbTester.components().insertComponent(newFileDto(branch, project.getMainBranchComponent()));
    IssueDto hotspot = dbTester.issues().insertHotspot(branch, file);

    insertAndLoginAsUserWithProjectUserPermission(secure().nextAlphanumeric(10), project.getProjectDto(), UserRole.USER);
    userSessionRule.addProjectBranchMapping(project.projectUuid(), branch);
    UserDto assignee = insertUser(secure().nextAlphanumeric(15));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    String login = assignee.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provided user with login '%s' does not have 'Browse' permission to project", login);
  }

  @Test
  public void wsExecution_whenAssignHotspotToMeForPrivateProject() {
    ProjectData project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    IssueDto hotspot = dbTester.issues().insertHotspot(project.getMainBranchComponent(), file);

    UserDto me = insertAndLoginAsUserWithProjectUserPermission(secure().nextAlphanumeric(10), project.getProjectDto(), UserRole.USER);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(me), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, me.getLogin(), null);

    verifyFieldSetters(me, null);
  }

  @Test
  public void wsExecution_whenAssignHotspotWithComment() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());

    UserDto assignee = insertUser(secure().nextAlphanumeric(15));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    String comment = "some comment";
    executeRequest(hotspot, assignee.getLogin(), comment);

    verifyFieldSetters(assignee, comment);
  }

  @Test
  public void wsExecution_whenAssignTwiceSameUserHotspotDoesNotReload_shouldFail() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());

    UserDto assignee = insertUser(secure().nextAlphanumeric(15));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(false);

    executeRequest(hotspot, assignee.getLogin(), "some comment");

    verify(issueFieldsSetter).assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class));
    verifyNoMoreInteractions(issueUpdater);
  }

  @Test
  public void wsExecution_whenBranchTypeIsBranch_shouldDistributeEvents() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());

    UserDto assignee = insertUser(secure().nextAlphanumeric(15));
    when(branchDto.getBranchType()).thenReturn(BranchType.BRANCH);
    String projectUuid = "projectUuid";
    when(branchDto.getProjectUuid()).thenReturn(projectUuid);
    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, assignee.getLogin(), null);
    verify(hotspotChangeEventService).distributeHotspotChangedEvent(eq(projectUuid), any(HotspotChangedEvent.class));
  }

  @Test
  public void wsExecution_whenBranchIsPullRequest_shouldNotDistributeEvents() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());

    UserDto assignee = insertUser(secure().nextAlphanumeric(15));
    when(branchDto.getBranchType()).thenReturn(BranchType.PULL_REQUEST);
    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, assignee.getLogin(), null);
    verifyNoInteractions(hotspotChangeEventService);
  }


  @Test
  public void wsExecution_whenAssigningToNonExistingUser_shouldFail() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());

    String notExistingUserLogin = secure().nextAlphanumeric(10);

    assertThatThrownBy(() -> executeRequest(hotspot, notExistingUserLogin, null))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Unknown user: " + notExistingUserLogin);
  }

  @Test
  @UseDataProvider("allIssueStatusesAndResolutionsThatThrowOnAssign")
  public void wsExecution_whenAssigningToUserIfForbidden_shouldFail(String status, String resolution) {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> {
      h.setStatus(status);
      h.setResolution(resolution);
    });

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());

    String login = userSessionRule.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot change the assignee of this hotspot given its current status and resolution");
  }

  @DataProvider
  public static Object[][] allIssueStatusesAndResolutionsThatThrowOnAssign() {
    return STATUSES.stream()
      .filter(status -> !STATUS_TO_REVIEW.equals(status))
      .filter(status -> !STATUS_CLOSED.equals(status))
      .flatMap(status -> Arrays.stream(new Object[] {RESOLUTION_SAFE, RESOLUTION_FIXED})
        .map(resolution -> new Object[] {status, resolution}))
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("allIssueStatusesAndResolutionsThatDoNotThrowOnAssign")
  public void wsExecution_whenAssigningToUserIfAllowed_shouldNotFail(String status, String resolution) {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> {
      h.setStatus(status);
      h.setResolution(resolution);
    });

    UserDto userDto = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn(userDto).registerProjects(projectData.getProjectDto());

    String login = userSessionRule.getLogin();
    assertThatNoException().isThrownBy(() -> executeRequest(hotspot, login, null));
  }

  @DataProvider
  public static Object[][] allIssueStatusesAndResolutionsThatDoNotThrowOnAssign() {
    return new Object[][] {
      new Object[] {STATUS_TO_REVIEW, null},
      new Object[] {STATUS_REVIEWED, RESOLUTION_ACKNOWLEDGED}
    };
  }

  @Test
  public void wsExecution_whenNotAuthenticated_shouldFail() {
    ComponentDto project = dbTester.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    userSessionRule.anonymous();

    UserDto assignee = insertUser(secure().nextAlphanumeric(15));

    String login = assignee.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void wsExecution_whenMissingBrowserAthentication_shouldFail() {
    ProjectData project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    IssueDto hotspot = dbTester.issues().insertHotspot(project.getMainBranchComponent(), file);

    UserDto me = insertAndLoginAsUserWithProjectUserPermission(secure().nextAlphanumeric(10), project.getProjectDto(), UserRole.CODEVIEWER);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(me), any(IssueChangeContext.class))).thenReturn(true);

    String login = me.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void wsExecution_whenHotspotDoesNotExist_shouldFail() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    UserDto me = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn().registerProjects(projectData.getProjectDto());

    String notExistingHotspotKey = secure().nextAlphanumeric(10);
    String login = me.getLogin();
    assertThatThrownBy(() -> executeRequest(notExistingHotspotKey, login, null))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", notExistingHotspotKey);
  }

  @Test
  @UseDataProvider("allRuleTypesWithStatusesExceptHotspot")
  public void wsExecution_whenAssigningToNonexistantIssue_shouldFail(RuleType ruleType, String status) {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(ruleType);
    IssueDto issue = dbTester.issues().insertIssue(rule, project, file, i -> i
      .setStatus(status)
      .setType(ruleType));

    UserDto me = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn().registerProjects(projectData.getProjectDto());

    String login = me.getLogin();
    assertThatThrownBy(() -> executeRequest(issue, login, null))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", issue.getKey());
  }

  @DataProvider
  public static Object[][] allRuleTypesWithStatusesExceptHotspot() {
    Set<RuleType> ruleTypes = EnumSet.allOf(RuleType.class)
      .stream()
      .filter(ruleType -> SECURITY_HOTSPOT != ruleType)
      .collect(Collectors.toSet());
    Set<String> statuses = STATUSES
      .stream()
      .filter(status -> !STATUS_TO_REVIEW.equals(status))
      .collect(Collectors.toSet());
    return Sets.cartesianProduct(ruleTypes, statuses)
      .stream()
      .map(elements -> new Object[] {elements.get(0), elements.get(1)})
      .toArray(Object[][]::new);
  }

  @Test
  public void wsExecution_whenHotspotIsClosed_shouldFailWithNotFoundException() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto issue = dbTester.issues().insertHotspot(rule, project, file, t -> t.setStatus(STATUS_CLOSED));
    UserDto me = insertUser(secure().nextAlphanumeric(10));
    userSessionRule.logIn().registerProjects(projectData.getProjectDto());

    String login = me.getLogin();
    assertThatThrownBy(() -> executeRequest(issue, login, null))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", issue.getKey());
  }

  private void verifyFieldSetters(UserDto assignee, @Nullable String comment) {
    ArgumentCaptor<DefaultIssue> defaultIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    short capturedArgsCount = 0;
    if (comment != null) {
      verify(issueFieldsSetter).addComment(defaultIssueCaptor.capture(), eq(comment), any(IssueChangeContext.class));
      capturedArgsCount++;
    }

    verify(issueFieldsSetter).assign(defaultIssueCaptor.capture(), userMatcher(assignee), any(IssueChangeContext.class));
    verify(issueUpdater).saveIssueAndPreloadSearchResponseData(
      any(DbSession.class),
      any(IssueDto.class),
      defaultIssueCaptor.capture(),
      any(IssueChangeContext.class));

    capturedArgsCount += 2;

    // because it is mutated by FieldSetter and IssueUpdater, the same object must be passed to all methods
    List<DefaultIssue> capturedDefaultIssues = defaultIssueCaptor.getAllValues();
    assertThat(capturedDefaultIssues).hasSize(capturedArgsCount);
    assertThat(capturedDefaultIssues)
      .are(new Condition<DefaultIssue>() {
        @Override
        public boolean matches(DefaultIssue value) {
          return value == capturedDefaultIssues.get(0);
        }
      });
  }

  private void executeRequest(IssueDto hotspot, @Nullable String assignee, @Nullable String comment) {
    executeRequest(hotspot.getKey(), assignee, comment);
  }

  private void executeRequest(String hotspotKey, @Nullable String assignee, @Nullable String comment) {
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", hotspotKey);

    if (assignee != null) {
      request.setParam("assignee", assignee);
    }

    if (comment != null) {
      request.setParam("comment", comment);
    }
    request.execute().assertNoContent();
  }

  private RuleDto newRule(RuleType ruleType) {
    RuleDto ruleDto = RuleTesting.newRule()
      .setType(ruleType);
    dbTester.rules().insert(ruleDto);
    return ruleDto;
  }

  private UserDto insertUser(String login) {
    return dbTester.users().insertUser(login);
  }

  private UserDto insertUserWithProjectPermission(String login, EntityDto project, String permission) {
    UserDto user = dbTester.users().insertUser(login);
    dbTester.users().insertProjectPermissionOnUser(user, permission, project);
    return user;
  }

  private UserDto insertUserWithProjectUserPermission(String login, EntityDto project) {
    return insertUserWithProjectPermission(login, project, UserRole.USER);
  }

  private UserDto insertAndLoginAsUserWithProjectUserPermission(String login, ProjectDto project, String permission) {
    UserDto user = insertUserWithProjectUserPermission(login, project);
    userSessionRule.logIn(user).addProjectPermission(permission, project);
    return user;
  }

  private static UserDto userMatcher(UserDto user) {
    if (user == null) {
      return isNull();
    } else {
      return argThat(argument -> argument.getLogin().equals(user.getLogin()) &&
        argument.getUuid().equals(user.getUuid()));
    }
  }

}
