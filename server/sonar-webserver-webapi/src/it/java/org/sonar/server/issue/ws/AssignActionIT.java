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

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.rules.RuleType;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueDto;
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
import org.sonar.server.issue.notification.IssuesChangesNotification;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.tester.UserSessionRule.standalone;

public class AssignActionIT {

  private static final String PREVIOUS_ASSIGNEE = "previous";
  private static final String CURRENT_USER_LOGIN = "john";

  private static final long PAST = 10_000_000_000L;
  private static final long NOW = 50_000_000_000L;

  private TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public UserSessionRule userSession = standalone();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(system2);
  public DbClient dbClient = db.getDbClient();
  private DbSession session = db.getSession();
  private NotificationManager notificationManager = mock(NotificationManager.class);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), null);
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private IssuesChangesNotificationSerializer issuesChangesSerializer = new IssuesChangesNotificationSerializer();
  private AssignAction underTest = new AssignAction(system2, userSession, dbClient, new IssueFinder(dbClient, userSession), new IssueFieldsSetter(),
    new IssueUpdater(dbClient,
      new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, new SequenceUuidFactory()),
      notificationManager, issueChangePostProcessor, issuesChangesSerializer),
    responseWriter);
  private WsActionTester ws = new WsActionTester(underTest);

  private UserDto currentUser;

  @Test
  public void assign_to_someone() {
    IssueDto issue = newIssueWithBrowsePermission();
    UserDto arthur = insertUser("arthur");

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", arthur.getLogin())
      .execute();

    checkIssueAssignee(issue.getKey(), arthur.getUuid());
    Optional<IssueDto> optionalIssueDto = dbClient.issueDao().selectByKey(session, issue.getKey());
    assertThat(optionalIssueDto).isPresent();
    assertThat(optionalIssueDto.get().getAssigneeUuid()).isEqualTo(arthur.getUuid());
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void assign_to_me() {
    IssueDto issue = newIssueWithBrowsePermission();

    Issues.AssignResponse assignResponse = ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "_me")
      .executeProtobuf(Issues.AssignResponse.class);

    checkIssueAssignee(issue.getKey(), currentUser.getUuid());
    Optional<IssueDto> optionalIssueDto = dbClient.issueDao().selectByKey(session, issue.getKey());
    assertThat(optionalIssueDto).isPresent();
    assertThat(optionalIssueDto.get().getAssigneeUuid()).isEqualTo(currentUser.getUuid());
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void unassign() {
    IssueDto issue = newIssueWithBrowsePermission();

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .execute();

    checkIssueAssignee(issue.getKey(), null);
    Optional<IssueDto> optionalIssueDto = dbClient.issueDao().selectByKey(session, issue.getKey());
    assertThat(optionalIssueDto).isPresent();
    assertThat(optionalIssueDto.get().getAssigneeUuid()).isNull();
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void unassign_with_empty_assignee_param() {
    IssueDto issue = newIssueWithBrowsePermission();

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "")
      .execute();

    checkIssueAssignee(issue.getKey(), null);
    Optional<IssueDto> optionalIssueDto = dbClient.issueDao().selectByKey(session, issue.getKey());
    assertThat(optionalIssueDto).isPresent();
    assertThat(optionalIssueDto.get().getAssigneeUuid()).isNull();
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void nothing_to_do_when_new_assignee_is_same_as_old_one() {
    UserDto user = insertUser("Bob");
    IssueDto issue = newIssue(user.getUuid());
    setUserWithBrowsePermission(issue);

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", user.getLogin())
      .execute();

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issue.getKey()).get();
    assertThat(issueReloaded.getAssigneeUuid()).isEqualTo(user.getUuid());
    assertThat(issueReloaded.getUpdatedAt()).isEqualTo(PAST);
    assertThat(issueReloaded.getIssueUpdateTime()).isEqualTo(PAST);
  }

  @Test
  public void send_notification() {
    IssueDto issue = newIssueWithBrowsePermission();
    UserDto arthur = insertUser("arthur");

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", arthur.getLogin())
      .execute();

    verify(notificationManager).scheduleForSending(any(IssuesChangesNotification.class));
  }

  @Test
  public void fail_when_assignee_does_not_exist() {
    IssueDto issue = newIssueWithBrowsePermission();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("issue", issue.getKey())
        .setParam("assignee", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_trying_assign_to_hotspot() {
    IssueDto hotspot = db.issues().insertHotspot(
      h -> h
        .setAssigneeUuid(PREVIOUS_ASSIGNEE)
        .setCreatedAt(PAST).setIssueCreationTime(PAST)
        .setUpdatedAt(PAST).setIssueUpdateTime(PAST));

    setUserWithBrowsePermission(hotspot);
    UserDto arthur = insertUser("arthur");

    TestRequest request = ws.newRequest()
      .setParam("issue", hotspot.getKey())
      .setParam("assignee", arthur.getLogin());
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Issue with key '%s' does not exist", hotspot.getKey());
  }

  @Test
  public void fail_when_assignee_is_disabled() {
    IssueDto issue = newIssueWithBrowsePermission();
    db.users().insertUser(user -> user.setActive(false));

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("issue", issue.getKey())
        .setParam("assignee", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_not_authenticated() {
    IssueDto issue = newIssue(PREVIOUS_ASSIGNEE);
    userSession.anonymous();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("issue", issue.getKey())
        .setParam("assignee", "_me")
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_browse_permission() {
    IssueDto issue = newIssue(PREVIOUS_ASSIGNEE);
    setUserWithPermission(issue, CODEVIEWER);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("issue", issue.getKey())
        .setParam("assignee", "_me")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  private UserDto insertUser(String login) {
    return db.users().insertUser(login);
  }

  private IssueDto newIssue(String assignee) {
    return newIssue(assignee, CODE_SMELL);
  }

  private IssueDto newIssue(String assignee, RuleType ruleType) {
    return db.issues().insertIssue(
      issueDto -> issueDto
        .setAssigneeUuid(assignee)
        .setCreatedAt(PAST).setIssueCreationTime(PAST)
        .setUpdatedAt(PAST).setIssueUpdateTime(PAST)
        .setType(ruleType));
  }

  private IssueDto newIssueWithBrowsePermission() {
    IssueDto issue = newIssue(PREVIOUS_ASSIGNEE);
    setUserWithBrowsePermission(issue);
    return issue;
  }

  private void setUserWithBrowsePermission(IssueDto issue) {
    setUserWithPermission(issue, USER);
  }

  private void setUserWithPermission(IssueDto issue, String permission) {
    currentUser = insertUser(CURRENT_USER_LOGIN);
    userSession.logIn(currentUser)
      .addProjectPermission(permission,
        dbClient.componentDao().selectByUuid(db.getSession(), issue.getProjectUuid()).get(),
        dbClient.componentDao().selectByUuid(db.getSession(), issue.getComponentUuid()).get());
  }

  private void checkIssueAssignee(String issueKey, @Nullable String expectedAssignee) {
    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issueKey).get();
    assertThat(issueReloaded.getAssigneeUuid()).isEqualTo(expectedAssignee);
    assertThat(issueReloaded.getIssueUpdateTime()).isEqualTo(NOW);
    assertThat(issueReloaded.getUpdatedAt()).isEqualTo(NOW);
  }
}
