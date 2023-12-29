/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.issue.IssueChangeWSSupport;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.Changelog.Diff;
import org.sonarqube.ws.Issues.ChangelogWsResponse;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;

public class ChangelogActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ComponentDto project;
  private ComponentDto file;
  private final IssueFinder issueFinder = new IssueFinder(db.getDbClient(), userSession);
  private final IssueChangeWSSupport issueChangeSupport = new IssueChangeWSSupport(db.getDbClient(), new AvatarResolverImpl(), userSession);
  private final ChangelogAction underTest = new ChangelogAction(db.getDbClient(), issueFinder, issueChangeSupport);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject().getMainBranchComponent();
    file = db.components().insertComponent(newFileDto(project));
  }

  @Test
  public void return_changelog() {
    UserDto user = insertUser();
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    FieldDiffs fieldDiffs = createFieldDiffs(user);
    db.issues().insertFieldDiffs(issueDto, fieldDiffs);

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    Common.Changelog changelog = result.getChangelogList().get(0);
    assertThat(changelog.getUser()).isNotNull().isEqualTo(user.getLogin());
    assertThat(changelog.getUserName()).isNotNull().isEqualTo(user.getName());
    assertThat(changelog.getIsUserActive()).isTrue();
    assertThat(changelog.getAvatar()).isNotNull().isEqualTo("93942e96f5acd83e2e047ad8fe03114d");
    assertThat(changelog.getCreationDate()).isNotEmpty();
    assertThat(changelog.getExternalUser()).isEqualTo(fieldDiffs.externalUser().orElse(null));
    assertThat(changelog.getWebhookSource()).isEqualTo(fieldDiffs.webhookSource().orElse(null));
    assertThat(changelog.getDiffsList()).extracting(Diff::getKey, Diff::getOldValue, Diff::getNewValue).containsOnly(tuple("severity", "MAJOR", "BLOCKER"));
  }

  private static FieldDiffs createFieldDiffs(UserDto user) {
    return new FieldDiffs()
      .setUserUuid(user.getUuid())
      .setDiff("severity", "MAJOR", "BLOCKER")
      .setCreationDate(new Date())
      .setExternalUser("toto")
      .setWebhookSource("github");
  }

  @Test
  public void return_changelog_on_user_without_email() {
    UserDto user = db.users().insertUser(UserTesting.newUserDto("john", "John", null));
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs().setUserUuid(user.getUuid()).setDiff("severity", "MAJOR", "BLOCKER").setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    assertThat(result.getChangelogList().get(0).getUser()).isNotNull().isEqualTo(user.getLogin());
    assertThat(result.getChangelogList().get(0).getUserName()).isNotNull().isEqualTo(user.getName());
    assertThat(result.getChangelogList().get(0).hasAvatar()).isFalse();
  }

  @Test
  public void return_changelog_not_having_user() {
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs().setUserUuid(null).setDiff("severity", "MAJOR", "BLOCKER").setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    assertThat(result.getChangelogList().get(0).hasUser()).isFalse();
    assertThat(result.getChangelogList().get(0).hasUserName()).isFalse();
    assertThat(result.getChangelogList().get(0).hasAvatar()).isFalse();
    assertThat(result.getChangelogList().get(0).getDiffsList()).isNotEmpty();
  }

  @Test
  public void return_changelog_on_none_existing_user() {
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs().setUserUuid("UNKNOWN").setDiff("severity", "MAJOR", "BLOCKER").setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    assertThat(result.getChangelogList().get(0).hasUser()).isFalse();
    assertThat(result.getChangelogList().get(0).hasUserName()).isFalse();
    assertThat(result.getChangelogList().get(0).hasAvatar()).isFalse();
    assertThat(result.getChangelogList().get(0).getDiffsList()).isNotEmpty();
  }

  @Test
  public void return_changelog_on_deactivated_user() {
    UserDto user = db.users().insertDisabledUser();
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs().setUserUuid(user.getUuid()).setDiff("severity", "MAJOR", "BLOCKER").setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    assertThat(result.getChangelogList().get(0).getUser()).isEqualTo(user.getLogin());
    assertThat(result.getChangelogList().get(0).getIsUserActive()).isFalse();
    assertThat(result.getChangelogList().get(0).getUserName()).isEqualTo(user.getName());
    assertThat(result.getChangelogList().get(0).hasAvatar()).isFalse();
    assertThat(result.getChangelogList().get(0).getDiffsList()).isNotEmpty();
  }

  @Test
  public void return_multiple_diffs() {
    UserDto user = insertUser();
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs().setUserUuid(user.getUuid())
      .setDiff("severity", "MAJOR", "BLOCKER").setCreationDate(new Date())
      .setDiff("status", "RESOLVED", "CLOSED").setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    assertThat(result.getChangelogList().get(0).getDiffsList()).extracting(Diff::getKey, Diff::getOldValue, Diff::getNewValue)
      .containsOnly(tuple("severity", "MAJOR", "BLOCKER"), tuple("status", "RESOLVED", "CLOSED"));
  }

  @Test
  public void return_changelog_when_no_old_value() {
    UserDto user = insertUser();
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs().setUserUuid(user.getUuid()).setDiff("severity", null, "BLOCKER").setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    assertThat(result.getChangelogList().get(0).getDiffsList().get(0).hasOldValue()).isFalse();
  }

  @Test
  public void return_changelog_when_no_new_value() {
    UserDto user = insertUser();
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs().setUserUuid(user.getUuid()).setDiff("severity", "MAJOR", null).setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    assertThat(result.getChangelogList().get(0).getDiffsList().get(0).hasNewValue()).isFalse();
  }

  @Test
  public void return_many_changelog() {
    UserDto user = insertUser();
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto,
      new FieldDiffs().setUserUuid(user.getUuid()).setDiff("severity", "MAJOR", "BLOCKER").setCreationDate(new Date()),
      new FieldDiffs().setDiff("status", "RESOLVED", "CLOSED").setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(2);
  }

  @Test
  public void replace_technical_debt_key_by_effort() {
    UserDto user = insertUser();
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs().setUserUuid(user.getUuid()).setDiff("technicalDebt", "10", "20").setCreationDate(new Date()));

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).hasSize(1);
    assertThat(result.getChangelogList().get(0).getDiffsList()).extracting(Diff::getKey, Diff::getOldValue, Diff::getNewValue).containsOnly(tuple("effort", "10", "20"));
  }

  @Test
  public void return_empty_changelog_when_no_changes_on_issue() {
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john").addProjectPermission(USER, project, file);

    ChangelogWsResponse result = call(issueDto.getKey());

    assertThat(result.getChangelogList()).isEmpty();
  }

  @Test
  public void fail_when_not_enough_permission() {
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john").addProjectPermission(CODEVIEWER, project, file);

    assertThatThrownBy(() -> call(issueDto.getKey()))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_trying_to_get_changelog_of_hotspot() {
    IssueDto issueDto = db.issues().insertHotspot();
    userSession.logIn("john").addProjectPermission(USER, project, file);

    String issueDtoKey = issueDto.getKey();
    assertThatThrownBy(() -> call(issueDtoKey))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Issue with key '%s' does not exist", issueDtoKey);
  }

  @Test
  public void test_example() {
    UserDto user = db.users().insertUser(newUserDto("john.smith", "John Smith", "john@smith.com"));
    IssueDto issueDto = insertNewIssue();
    userSession.logIn("john")
      .addProjectPermission(USER, project, file);
    db.issues().insertFieldDiffs(issueDto, new FieldDiffs()
        .setUserUuid(user.getUuid())
        .setDiff("issueStatus", "OPEN", "ACCEPTED")
        .setWebhookSource("github")
        .setExternalUser("ext-user")
        .setCreationDate(DateUtils.parseDateTime("2014-03-04T23:03:44+0100")),
      new FieldDiffs()
        .setDiff("from_branch", "branch-123", "main")
        .setDiff("assignee", null, "assignee123")
        .setCreationDate(DateUtils.parseDateTime("2014-03-04T23:05:44+0100")));

    String result = tester.newRequest().setParam("issue", issueDto.getKey()).execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("changelog-example.json"));
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("changelog");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(1);
    assertThat(action.responseExample()).isNotNull();
  }

  private ChangelogWsResponse call(@Nullable String issueKey) {
    TestRequest request = tester.newRequest();
    ofNullable(issueKey).ifPresent(e -> request.setParam("issue", e));
    return request.executeProtobuf(ChangelogWsResponse.class);
  }

  private IssueDto insertNewIssue() {
    RuleDto rule = db.rules().insertIssueRule();
    return db.issues().insertIssue(rule, project, file);
  }

  private UserDto insertUser() {
    return db.users().insertUser(user -> user.setEmail("test@email.com"));
  }

}
