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
package org.sonar.server.hotspot.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.ws.IssueUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.component.ComponentTesting.newFileDto;

@RunWith(DataProviderRunner.class)
public class AddCommentActionIT {
  private static final Random RANDOM = new Random();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private IssueUpdater issueUpdater = mock(IssueUpdater.class);
  private System2 system2 = mock(System2.class);
  private IssueFieldsSetter issueFieldsSetter = mock(IssueFieldsSetter.class);
  private HotspotWsSupport hotspotWsSupport = new HotspotWsSupport(dbClient, userSessionRule, system2);
  private AddCommentAction underTest = new AddCommentAction(dbClient, hotspotWsSupport, issueFieldsSetter, issueUpdater);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void ws_is_internal() {
    assertThat(actionTester.getDef().isInternal()).isTrue();
    assertThat(actionTester.getDef().isPost()).isTrue();
    assertThat(actionTester.getDef().param("comment").maximumLength()).isEqualTo(1000);
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
  public void fails_with_IAE_if_parameter_comment_is_missing() {
    String key = secure().nextAlphabetic(12);
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'comment' parameter is missing");
  }

  @Test
  public void fails_with_NotFoundException_if_hotspot_does_not_exist() {
    String key = secure().nextAlphabetic(12);
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key)
      .setParam("comment", secure().nextAlphabetic(10));

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", key);
  }

  @Test
  @UseDataProvider("ruleTypesByHotspot")
  public void fails_with_NotFoundException_if_issue_is_not_a_hotspot(RuleType ruleType) {
    ComponentDto project = dbTester.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = dbTester.rules().insert(t -> t.setType(ruleType));
    IssueDto notAHotspot = dbTester.issues().insertIssue(rule, project, file, i -> i.setType(ruleType));
    userSessionRule.logIn();
    TestRequest request = newRequest(notAHotspot, secure().nextAlphabetic(12));

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", notAHotspot.getKey());
  }

  @DataProvider
  public static Object[][] ruleTypesByHotspot() {
    return Arrays.stream(RuleType.values())
      .filter(t -> t != RuleType.SECURITY_HOTSPOT)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void fails_with_NotFoundException_if_hotspot_is_closed() {
    ComponentDto project = dbTester.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = dbTester.rules().insertHotspotRule();
    IssueDto hotspot = dbTester.issues().insertHotspot(rule, project, file, t -> t.setStatus(STATUS_CLOSED));
    userSessionRule.logIn();
    TestRequest request = newRequest(hotspot, secure().nextAlphabetic(12));

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", hotspot.getKey());
  }

  @Test
  public void fails_with_ForbiddenException_if_project_is_private_and_not_allowed() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.logIn().registerProjects(projectData.getProjectDto());
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = dbTester.rules().insertHotspotRule();
    IssueDto hotspot = dbTester.issues().insertHotspot(rule, project, file);
    String comment = secure().nextAlphabetic(12);
    TestRequest request = newRequest(hotspot, comment);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void succeeds_on_public_project() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.logIn().registerProjects(projectData.getProjectDto());
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = dbTester.rules().insertHotspotRule();
    IssueDto hotspot = dbTester.issues().insertHotspot(rule, project, file);
    String comment = secure().nextAlphabetic(12);

    newRequest(hotspot, comment).execute().assertNoContent();
  }

  @Test
  public void succeeds_on_private_project_with_permission() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.logIn().registerProjects(projectData.getProjectDto())
      .addProjectPermission(UserRole.USER, projectData.getProjectDto());
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = dbTester.rules().insertHotspotRule();
    IssueDto hotspot = dbTester.issues().insertHotspot(rule, project, file);
    String comment = secure().nextAlphabetic(12);

    newRequest(hotspot, comment).execute().assertNoContent();
  }

  @Test
  @UseDataProvider("validStatusAndResolutions")
  public void persists_comment_if_hotspot_status_changes_and_transition_done(String currentStatus, @Nullable String currentResolution) {
    long now = RANDOM.nextInt(232_323);
    when(system2.now()).thenReturn(now);
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.logIn().registerProjects(projectData.getProjectDto());
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = dbTester.rules().insertHotspotRule();
    IssueDto hotspot = dbTester.issues().insertHotspot(rule, project, file, t -> t.setStatus(currentStatus).setResolution(currentResolution));
    String comment = secure().nextAlphabetic(12);

    newRequest(hotspot, comment).execute().assertNoContent();

    IssueChangeContext issueChangeContext = issueChangeContextByUserBuilder(new Date(now), userSessionRule.getUuid()).build();
    ArgumentCaptor<DefaultIssue> defaultIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueFieldsSetter).addComment(defaultIssueCaptor.capture(), eq(comment), eq(issueChangeContext));
    verify(issueUpdater).saveIssueAndPreloadSearchResponseData(
      any(DbSession.class),
      any(IssueDto.class),
      defaultIssueCaptor.capture(),
      eq(issueChangeContext));

    // because it is mutated by FieldSetter and IssueUpdater, the same object must be passed to all methods
    List<DefaultIssue> capturedDefaultIssues = defaultIssueCaptor.getAllValues();
    assertThat(capturedDefaultIssues).hasSize(2);
    assertThat(capturedDefaultIssues.get(0))
      .isSameAs(capturedDefaultIssues.get(1));
  }

  @DataProvider
  public static Object[][] validStatusAndResolutions() {
    return new Object[][] {
      {STATUS_TO_REVIEW, null},
      {STATUS_REVIEWED, RESOLUTION_FIXED},
      {STATUS_REVIEWED, RESOLUTION_SAFE}
    };
  }

  private TestRequest newRequest(IssueDto hotspot, String comment) {
    return actionTester.newRequest()
      .setParam("hotspot", hotspot.getKey())
      .setParam("comment", comment);
  }

}
