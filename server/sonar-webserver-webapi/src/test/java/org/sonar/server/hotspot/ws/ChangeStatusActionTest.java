/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.ws.IssueUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.db.component.ComponentTesting.newFileDto;

@RunWith(DataProviderRunner.class)
public class ChangeStatusActionTest {
  private static final Random RANDOM = new Random();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private TransitionService transitionService = mock(TransitionService.class);
  private IssueUpdater issueUpdater = mock(IssueUpdater.class);
  private System2 system2 = mock(System2.class);
  private ChangeStatusAction underTest = new ChangeStatusAction(dbClient, userSessionRule, transitionService, system2, issueUpdater);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void ws_is_internal() {
    assertThat(actionTester.getDef().isInternal()).isTrue();
  }

  @Test
  public void fails_with_UnauthorizedException_if_user_is_anonymous() {
    userSessionRule.anonymous();

    TestRequest request = actionTester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void fails_with_IAE_if_parameter_hotspot_is_missing() {
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'hotspot' parameter is missing");
  }

  @Test
  public void fails_with_IAE_if_parameter_status_is_missing() {
    String key = randomAlphabetic(12);
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'status' parameter is missing");
  }

  @Test
  @UseDataProvider("badStatuses")
  public void fail_with_IAE_if_status_value_is_neither_REVIEWED_nor_TO_REVIEW(String badStatus) {
    String key = randomAlphabetic(12);
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key)
      .setParam("status", badStatus);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'status' (" + badStatus + ") must be one of: [TO_REVIEW, REVIEWED]");
  }

  @DataProvider
  public static Object[][] badStatuses() {
    return Stream.concat(
      Issue.STATUSES.stream()
        .filter(t -> !t.equals(STATUS_TO_REVIEW))
        .filter(t -> !t.equals(STATUS_REVIEWED)),
      Stream.of(randomAlphabetic(22), ""))
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("badResolutions")
  public void fail_with_IAE_if_resolution_value_is_neither_FIXED_nor_SAFE(String validStatus, String badResolution) {
    String key = randomAlphabetic(12);
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key)
      .setParam("status", validStatus)
      .setParam("resolution", badResolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'resolution' (" + badResolution + ") must be one of: [FIXED, SAFE]");
  }

  @DataProvider
  public static Object[][] badResolutions() {
    return Stream.of(STATUS_TO_REVIEW, STATUS_REVIEWED)
      .flatMap(t -> Stream.concat(Issue.RESOLUTIONS.stream(), Issue.SECURITY_HOTSPOT_RESOLUTIONS.stream())
        .filter(r -> !r.equals(RESOLUTION_FIXED))
        .filter(r -> !r.equals(RESOLUTION_SAFE))
        .map(r -> new Object[] {t, r}))
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("validResolutions")
  public void fail_with_IAE_if_status_is_TO_REVIEW_and_resolution_is_set(String resolution) {
    String key = randomAlphabetic(12);
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key)
      .setParam("status", STATUS_TO_REVIEW)
      .setParam("resolution", resolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'resolution' must not be specified when Parameter 'status' has value 'TO_REVIEW'");
  }

  @DataProvider
  public static Object[][] validResolutions() {
    return new Object[][] {
      {RESOLUTION_FIXED},
      {RESOLUTION_SAFE}
    };
  }

  public void fail_with_IAE_if_status_is_RESOLVED_and_resolution_is_not_set() {
    String key = randomAlphabetic(12);
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key)
      .setParam("status", Issue.STATUS_RESOLVED);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'resolution' must not be specified when Parameter 'status' has value 'TO_REVIEW'");
  }

  @Test
  @UseDataProvider("validStatusAndResolutions")
  public void fails_with_NotFoundException_if_hotspot_does_not_exist(String status, @Nullable String resolution) {
    String key = randomAlphabetic(12);
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key)
      .setParam("status", status);
    if (resolution != null) {
      request.setParam("resolution", resolution);
    }

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", key);
  }

  @DataProvider
  public static Object[][] validStatusAndResolutions() {
    return new Object[][] {
      {STATUS_TO_REVIEW, null},
      {STATUS_REVIEWED, RESOLUTION_FIXED},
      {STATUS_REVIEWED, RESOLUTION_SAFE}
    };
  }

  @Test
  @UseDataProvider("ruleTypesButHotspot")
  public void fails_with_NotFoundException_if_issue_is_not_a_hotspot(String status, @Nullable String resolution, RuleType ruleType) {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(ruleType);
    IssueDto notAHotspot = dbTester.issues().insertIssue(IssueTesting.newIssue(rule, project, file).setType(ruleType));
    userSessionRule.logIn();
    TestRequest request = newRequest(notAHotspot, status, resolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", notAHotspot.getKey());
  }

  @DataProvider
  public static Object[][] ruleTypesButHotspot() {
    return Arrays.stream(RuleType.values())
      .filter(t -> t != SECURITY_HOTSPOT)
      .flatMap(t -> Arrays.stream(validStatusAndResolutions()).map(u -> new Object[] {u[0], u[1], t}))
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("validStatusAndResolutions")
  public void fails_with_ForbiddenException_if_project_is_private_and_not_allowed(String status, @Nullable String resolution) {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSessionRule.logIn().registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule));
    TestRequest request = newRequest(hotspot, status, resolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  @UseDataProvider("validStatusAndResolutions")
  public void succeeds_on_public_project(String status, @Nullable String resolution) {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.logIn().registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule));

    newRequest(hotspot, status, resolution).execute().assertNoContent();
  }

  @Test
  @UseDataProvider("validStatusAndResolutions")
  public void succeeds_on_private_project_with_permission(String status, @Nullable String resolution) {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSessionRule.logIn().registerComponents(project).addProjectPermission(UserRole.USER, project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule));

    newRequest(hotspot, status, resolution).execute().assertNoContent();
  }

  @Test
  @UseDataProvider("validStatusAndResolutions")
  public void no_effect_and_success_if_hotspot_already_has_specified_status_and_resolution(String status, @Nullable String resolution) {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.logIn().registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule).setStatus(status).setResolution(resolution));

    newRequest(hotspot, status, resolution).execute().assertNoContent();
    verifyNoInteractions(transitionService, issueUpdater);
  }

  @Test
  @UseDataProvider("reviewedResolutionsAndExpectedTransitionKey")
  public void success_to_change_hostpot_to_review_into_reviewed_status(String resolution, String expectedTransitionKey, boolean transitionDone) {
    long now = RANDOM.nextInt(232_323);
    when(system2.now()).thenReturn(now);
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.logIn().registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule).setStatus(STATUS_TO_REVIEW).setResolution(null));
    when(transitionService.doTransition(any(), any(), any())).thenReturn(transitionDone);

    newRequest(hotspot, STATUS_REVIEWED, resolution).execute().assertNoContent();
    IssueChangeContext issueChangeContext = IssueChangeContext.createUser(new Date(now), userSessionRule.getUuid());
    DefaultIssueMatcher defaultIssueMatcher = new DefaultIssueMatcher(hotspot);
    verify(transitionService).doTransition(
      argThat(defaultIssueMatcher),
      eq(issueChangeContext),
      eq(expectedTransitionKey));
    if (transitionDone) {
      verify(issueUpdater).saveIssueAndPreloadSearchResponseData(
        any(DbSession.class),
        argThat(defaultIssueMatcher),
        eq(issueChangeContext),
        eq(true));
    } else {
      verifyNoInteractions(issueUpdater);
    }
  }

  @DataProvider
  public static Object[][] reviewedResolutionsAndExpectedTransitionKey() {
    return new Object[][] {
      {RESOLUTION_FIXED, DefaultTransitions.RESOLVE_AS_REVIEWED, true},
      {RESOLUTION_FIXED, DefaultTransitions.RESOLVE_AS_REVIEWED, false},
      {RESOLUTION_SAFE, DefaultTransitions.RESOLVE_AS_SAFE, true},
      {RESOLUTION_SAFE, DefaultTransitions.RESOLVE_AS_SAFE, false}
    };
  }

  @Test
  @UseDataProvider("reviewedResolutions")
  public void success_to_change_reviewed_hotspot_back_to_to_review(String resolution, boolean transitionDone) {
    long now = RANDOM.nextInt(232_323);
    when(system2.now()).thenReturn(now);
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.logIn().registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule).setStatus(STATUS_REVIEWED).setResolution(resolution));
    when(transitionService.doTransition(any(), any(), any())).thenReturn(transitionDone);

    newRequest(hotspot, STATUS_TO_REVIEW, null).execute().assertNoContent();
    IssueChangeContext issueChangeContext = IssueChangeContext.createUser(new Date(now), userSessionRule.getUuid());
    DefaultIssueMatcher defaultIssueMatcher = new DefaultIssueMatcher(hotspot);
    verify(transitionService).doTransition(
      argThat(defaultIssueMatcher),
      eq(issueChangeContext),
      eq(DefaultTransitions.RESET_AS_TO_REVIEW));
    if (transitionDone) {
      verify(issueUpdater).saveIssueAndPreloadSearchResponseData(
        any(DbSession.class),
        argThat(defaultIssueMatcher),
        eq(issueChangeContext),
        eq(true));
    } else {
      verifyNoInteractions(issueUpdater);
    }
  }

  @DataProvider
  public static Object[][] reviewedResolutions() {
    return new Object[][] {
      {RESOLUTION_FIXED, true},
      {RESOLUTION_FIXED, false},
      {RESOLUTION_SAFE, true},
      {RESOLUTION_SAFE, false}
    };
  }

  private static class DefaultIssueMatcher implements ArgumentMatcher<DefaultIssue> {
    private final DefaultIssue expected;

    private DefaultIssueMatcher(IssueDto issueDto) {
      this.expected = issueDto.toDefaultIssue();
    }

    @Override
    public boolean matches(DefaultIssue that) {
      if (expected == that) {
        return true;
      }
      if (that == null || expected.getClass() != that.getClass()) {
        return false;
      }
      return expected.manualSeverity() == that.manualSeverity() &&
        expected.isFromExternalRuleEngine() == that.isFromExternalRuleEngine() &&
        expected.isNew() == that.isNew() &&
        expected.isCopied() == that.isCopied() &&
        expected.isBeingClosed() == that.isBeingClosed() &&
        expected.isOnDisabledRule() == that.isOnDisabledRule() &&
        expected.isChanged() == that.isChanged() &&
        expected.mustSendNotifications() == that.mustSendNotifications() &&
        Objects.equals(expected.key(), that.key()) &&
        expected.type() == that.type() &&
        Objects.equals(expected.componentUuid(), that.componentUuid()) &&
        Objects.equals(expected.componentKey(), that.componentKey()) &&
        Objects.equals(expected.moduleUuid(), that.moduleUuid()) &&
        Objects.equals(expected.moduleUuidPath(), that.moduleUuidPath()) &&
        Objects.equals(expected.projectUuid(), that.projectUuid()) &&
        Objects.equals(expected.projectKey(), that.projectKey()) &&
        Objects.equals(expected.ruleKey(), that.ruleKey()) &&
        Objects.equals(expected.language(), that.language()) &&
        Objects.equals(expected.severity(), that.severity()) &&
        Objects.equals(expected.message(), that.message()) &&
        Objects.equals(expected.line(), that.line()) &&
        Objects.equals(expected.gap(), that.gap()) &&
        Objects.equals(expected.effort(), that.effort()) &&
        Objects.equals(expected.status(), that.status()) &&
        Objects.equals(expected.resolution(), that.resolution()) &&
        Objects.equals(expected.assignee(), that.assignee()) &&
        Objects.equals(expected.checksum(), that.checksum()) &&
        Objects.equals(expected.attributes(), that.attributes()) &&
        Objects.equals(expected.authorLogin(), that.authorLogin()) &&
        Objects.equals(expected.comments(), that.comments()) &&
        Objects.equals(expected.tags(), that.tags()) &&
        Objects.equals(expected.getLocations(), that.getLocations()) &&
        Objects.equals(expected.creationDate(), that.creationDate()) &&
        Objects.equals(expected.updateDate(), that.updateDate()) &&
        Objects.equals(expected.closeDate(), that.closeDate()) &&
        Objects.equals(expected.currentChange(), that.currentChange()) &&
        Objects.equals(expected.changes(), that.changes()) &&
        Objects.equals(expected.selectedAt(), that.selectedAt());
    }
  }

  private static IssueDto newHotspot(ComponentDto project, ComponentDto file, RuleDefinitionDto rule) {
    return IssueTesting.newIssue(rule, project, file).setType(SECURITY_HOTSPOT);
  }

  private TestRequest newRequest(IssueDto hotspot, String newStatus, @Nullable String newResolution) {
    TestRequest res = actionTester.newRequest()
      .setParam("hotspot", hotspot.getKey())
      .setParam("status", newStatus);
    if (newResolution != null) {
      res.setParam("resolution", newResolution);
    }
    return res;
  }

  private RuleDefinitionDto newRule(RuleType ruleType) {
    return newRule(ruleType, t -> {
    });
  }

  private RuleDefinitionDto newRule(RuleType ruleType, Consumer<RuleDefinitionDto> populate) {
    RuleDefinitionDto ruleDefinition = RuleTesting.newRule()
      .setType(ruleType);
    populate.accept(ruleDefinition);
    dbTester.rules().insert(ruleDefinition);
    return ruleDefinition;
  }

}
