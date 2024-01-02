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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.ws.IssueUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
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
public class AssignActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final DbClient dbClient = dbTester.getDbClient();
  private final IssueUpdater issueUpdater = mock(IssueUpdater.class);
  private final System2 system2 = mock(System2.class);
  private final IssueFieldsSetter issueFieldsSetter = mock(IssueFieldsSetter.class);
  private final HotspotWsSupport hotspotWsSupport = new HotspotWsSupport(dbClient, userSessionRule, system2);

  private final AssignAction underTest = new AssignAction(dbClient, hotspotWsSupport, issueFieldsSetter, issueUpdater);
  private final WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void ws_definition_check() {
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
  public void assign_hotspot_to_someone_for_public_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(userDto).registerComponents(project);

    UserDto assignee = insertUser(randomAlphanumeric(15));
    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, assignee.getLogin(), null);

    verifyFieldSetters(assignee, null);
  }

  @Test
  public void unassign_hotspot_for_public_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    UserDto assignee = insertUser(randomAlphanumeric(15));

    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> h.setAssigneeUuid(assignee.getUuid()));

    UserDto userDto = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(userDto).registerComponents(project);
    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), isNull(), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, null, null);

    verifyFieldSetters(null, null);
  }

  @Test
  public void assign_hotspot_to_me_for_public_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto me = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(me).registerComponents(project);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(me), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, me.getLogin(), null);

    verifyFieldSetters(me, null);
  }

  @Test
  public void unassign_hotspot_myself_for_public_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    UserDto me = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(me).registerComponents(project);
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> h.setAssigneeUuid(me.getUuid()));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), isNull(), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, null, null);

    verifyFieldSetters(null, null);
  }

  @Test
  public void assign_hotspot_to_someone_for_private_project() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    insertAndLoginAsUserWithProjectUserPermission(randomAlphanumeric(10), project, UserRole.USER);
    UserDto assignee = insertUserWithProjectUserPermission(randomAlphanumeric(15), project);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, assignee.getLogin(), null);

    verifyFieldSetters(assignee, null);
  }

  @Test
  public void unassign_hotspot_for_private_project() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    UserDto assignee = insertUser(randomAlphanumeric(15));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> h.setAssigneeUuid(assignee.getUuid()));

    insertAndLoginAsUserWithProjectUserPermission(randomAlphanumeric(10), project, UserRole.USER);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), isNull(), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, null, null);

    verifyFieldSetters(null, null);
  }

  @Test
  public void assign_hotspot_to_someone_for_private_project_branch() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(branch));
    IssueDto hotspot = dbTester.issues().insertHotspot(branch, file);

    insertAndLoginAsUserWithProjectUserPermission(randomAlphanumeric(10), project, UserRole.USER);
    UserDto assignee = insertUserWithProjectUserPermission(randomAlphanumeric(15), project);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, assignee.getLogin(), null);

    verifyFieldSetters(assignee, null);
  }

  @Test
  public void fail_if_assignee_does_not_have_access_for_private_project() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    insertAndLoginAsUserWithProjectUserPermission(randomAlphanumeric(10), project, UserRole.USER);
    UserDto assignee = insertUser(randomAlphanumeric(15));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    String login = assignee.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provided user with login '%s' does not have 'Browse' permission to project", login);
  }

  @Test
  public void fail_if_assignee_does_not_have_access_for_private_project_branch() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(branch));
    IssueDto hotspot = dbTester.issues().insertHotspot(branch, file);

    insertAndLoginAsUserWithProjectUserPermission(randomAlphanumeric(10), project, UserRole.USER);
    UserDto assignee = insertUser(randomAlphanumeric(15));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    String login = assignee.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provided user with login '%s' does not have 'Browse' permission to project", login);
  }

  @Test
  public void assign_hotspot_to_me_for_private_project() {
    ComponentDto project = dbTester.components().insertPrivateProject();

    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto me = insertAndLoginAsUserWithProjectUserPermission(randomAlphanumeric(10), project, UserRole.USER);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(me), any(IssueChangeContext.class))).thenReturn(true);

    executeRequest(hotspot, me.getLogin(), null);

    verifyFieldSetters(me, null);
  }

  @Test
  public void assign_hotspot_with_comment() {
    ComponentDto project = dbTester.components().insertPublicProject();

    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(userDto).registerComponents(project);

    UserDto assignee = insertUser(randomAlphanumeric(15));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(true);

    String comment = "some comment";
    executeRequest(hotspot, assignee.getLogin(), comment);

    verifyFieldSetters(assignee, comment);
  }

  @Test
  public void assign_twice_same_user_to_hotspot_does_not_reload() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(userDto).registerComponents(project);

    UserDto assignee = insertUser(randomAlphanumeric(15));

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class))).thenReturn(false);

    executeRequest(hotspot, assignee.getLogin(), "some comment");

    verify(issueFieldsSetter).assign(eq(hotspot.toDefaultIssue()), userMatcher(assignee), any(IssueChangeContext.class));
    verifyNoMoreInteractions(issueUpdater);
  }

  @Test
  public void fail_if_assigning_to_not_existing_user() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto userDto = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(userDto).registerComponents(project);

    String notExistingUserLogin = randomAlphanumeric(10);

    assertThatThrownBy(() -> executeRequest(hotspot, notExistingUserLogin, null))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Unknown user: " + notExistingUserLogin);
  }

  @Test
  @UseDataProvider("allIssueStatusesAndResolutionsThatThrowOnAssign")
  public void fail_if_assign_user_to_hotspot_for_which_it_is_forbidden(String status, String resolution) {
    ComponentDto project = dbTester.components().insertPublicProject();

    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> {
      h.setStatus(status);
      h.setResolution(resolution);
    });

    UserDto userDto = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(userDto).registerComponents(project);

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
  public void fail_if_assign_user_to_hotspot_for_which_it_is_allowed(String status, String resolution) {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file, h -> {
      h.setStatus(status);
      h.setResolution(resolution);
    });

    UserDto userDto = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn(userDto).registerComponents(project);

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
  public void fail_if_not_authenticated() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    userSessionRule.anonymous();

    UserDto assignee = insertUser(randomAlphanumeric(15));

    String login = assignee.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_missing_browse_permission() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertHotspot(project, file);

    UserDto me = insertAndLoginAsUserWithProjectUserPermission(randomAlphanumeric(10), project, UserRole.CODEVIEWER);

    when(issueFieldsSetter.assign(eq(hotspot.toDefaultIssue()), userMatcher(me), any(IssueChangeContext.class))).thenReturn(true);

    String login = me.getLogin();
    assertThatThrownBy(() -> executeRequest(hotspot, login, null))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_hotspot_does_not_exist() {
    ComponentDto project = dbTester.components().insertPublicProject();

    UserDto me = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn().registerComponents(project);

    String notExistingHotspotKey = randomAlphanumeric(10);
    String login = me.getLogin();
    assertThatThrownBy(() -> executeRequest(notExistingHotspotKey, login, null))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", notExistingHotspotKey);
  }

  @Test
  @UseDataProvider("allRuleTypesWithStatusesExceptHotspot")
  public void fail_if_trying_to_assign_issue(RuleType ruleType, String status) {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(ruleType);
    IssueDto issue = dbTester.issues().insertIssue(rule, project, file, i -> i
      .setStatus(status)
      .setType(ruleType));

    UserDto me = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn().registerComponents(project);

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
  public void fail_with_NotFoundException_if_hotspot_is_closed() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto issue = dbTester.issues().insertHotspot(rule, project, file, t -> t.setStatus(STATUS_CLOSED));
    UserDto me = insertUser(randomAlphanumeric(10));
    userSessionRule.logIn().registerComponents(project);

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

  private UserDto insertUserWithProjectPermission(String login, ComponentDto project, String permission) {
    UserDto user = dbTester.users().insertUser(login);
    dbTester.users().insertProjectPermissionOnUser(user, permission, project);
    return user;
  }

  private UserDto insertUserWithProjectUserPermission(String login, ComponentDto project) {
    return insertUserWithProjectPermission(login, project, UserRole.USER);
  }

  private UserDto insertAndLoginAsUserWithProjectUserPermission(String login, ComponentDto project, String permission) {
    UserDto user = insertUserWithProjectUserPermission(login, project);
    userSessionRule.logIn(user)
      .addProjectPermission(permission,
        dbClient.componentDao().selectByUuid(dbTester.getSession(), project.uuid()).get());
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
