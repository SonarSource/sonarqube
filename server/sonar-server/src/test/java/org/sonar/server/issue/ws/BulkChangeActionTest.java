/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.Action;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueStorage;
import org.sonar.server.issue.ServerIssueStorage;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.DefaultUserFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues.BulkChangeWsResponse;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.issue.BulkChangeRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueChangeDto.TYPE_COMMENT;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class BulkChangeActionTest {

  private static long NOW = 2_000_000_000_000L;

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();

  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private IssueStorage issueStorage = new ServerIssueStorage(system2, new DefaultRuleFinder(dbClient), dbClient, new IssueIndexer(system2, dbClient, es.client()));
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private List<Action> actions = new ArrayList<>();

  private RuleDto rule;
  private ComponentDto project;
  private ComponentDto file;
  private UserDto user;

  private WsActionTester tester = new WsActionTester(new BulkChangeAction(system2, userSession, dbClient, issueStorage, notificationManager, actions));

  @Before
  public void setUp() throws Exception {
    rule = db.rules().insertRule(newRuleDto());
    project = db.components().insertProject();
    file = db.components().insertComponent(newFileDto(project));
    user = db.users().insertUser("john");
    when(system2.now()).thenReturn(NOW);
    addActions();
  }

  @Test
  public void set_type() throws Exception {
    setUserPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setSetType(RuleType.CODE_SMELL.name())
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issueDto.getKey()).get(0);
    assertThat(reloaded.getType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void set_severity() throws Exception {
    setUserPermissions(USER, ISSUE_ADMIN);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setSeverity(MAJOR));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setSetSeverity(MINOR)
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issueDto.getKey()).get(0);
    assertThat(reloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void add_tags() throws Exception {
    setUserPermissions(USER, ISSUE_ADMIN);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setTags(asList("tag1", "tag2")));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setAddTags(singletonList("tag3"))
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issueDto.getKey()).get(0);
    assertThat(reloaded.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void remove_assignee() throws Exception {
    setUserPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setAssignee("arthur"));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setAssign("")
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issueDto.getKey()).get(0);
    assertThat(reloaded.getAssignee()).isNull();
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void bulk_change_with_comment() throws Exception {
    setUserPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setSetType(RuleType.CODE_SMELL.name())
      .setComment("type was badly defined")
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueChangeDto issueComment = dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issueDto.getKey()), TYPE_COMMENT).get(0);
    assertThat(issueComment.getUserLogin()).isEqualTo("john");
    assertThat(issueComment.getChangeData()).isEqualTo("type was badly defined");
  }

  @Test
  public void bulk_change_many_issues() throws Exception {
    setUserPermissions(USER, ISSUE_ADMIN);
    UserDto userToAssign = db.users().insertUser("arthur");
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setAssignee(user.getLogin())).setType(BUG).setSeverity(MINOR);
    IssueDto issue2 = db.issues().insertIssue(newUnresolvedIssue().setAssignee(userToAssign.getLogin())).setType(BUG).setSeverity(MAJOR);
    IssueDto issue3 = db.issues().insertIssue(newUnresolvedIssue().setAssignee(null)).setType(VULNERABILITY).setSeverity(MAJOR);

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setAssign(userToAssign.getLogin())
      .setSetSeverity(MINOR)
      .setSetType(VULNERABILITY.name())
      .build());

    checkResponse(response, 3, 3, 0, 0);
    assertThat(getIssueByKeys(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .extracting(IssueDto::getKey, IssueDto::getAssignee, IssueDto::getType, IssueDto::getSeverity, IssueDto::getUpdatedAt)
      .containsOnly(
        tuple(issue1.getKey(), userToAssign.getLogin(), VULNERABILITY.getDbConstant(), MINOR, NOW),
        tuple(issue2.getKey(), userToAssign.getLogin(), VULNERABILITY.getDbConstant(), MINOR, NOW),
        tuple(issue3.getKey(), userToAssign.getLogin(), VULNERABILITY.getDbConstant(), MINOR, NOW));
  }

  @Test
  public void send_notification() throws Exception {
    setUserPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setSetType(RuleType.CODE_SMELL.name())
      .setSendNotifications(true)
      .build());

    checkResponse(response, 1, 1, 0, 0);
    verify(notificationManager).scheduleForSending(issueChangeNotificationCaptor.capture());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("key")).isEqualTo(issueDto.getKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("componentName")).isEqualTo(file.longName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectName")).isEqualTo(project.longName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectKey")).isEqualTo(project.key());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("ruleName")).isEqualTo(rule.getName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("changeAuthor")).isEqualTo(user.getLogin());
  }

  @Test
  public void send_notification_only_on_changed_issues() throws Exception {
    setUserPermissions(USER);
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    IssueDto issue2 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    IssueDto issue3 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));
    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(RuleType.BUG.name())
      .setSendNotifications(true)
      .build());

    checkResponse(response, 3, 1, 2, 0);
    verify(notificationManager).scheduleForSending(issueChangeNotificationCaptor.capture());
    assertThat(issueChangeNotificationCaptor.getAllValues()).hasSize(1);
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("key")).isEqualTo(issue3.getKey());
  }

  @Test
  public void ignore_issues_when_condition_does_not_match() throws Exception {
    setUserPermissions(USER);
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    // These 2 issues will be ignored as they are resolved, changing type is not possible
    IssueDto issue2 = db.issues().insertIssue(newResolvedIssue().setType(BUG));
    IssueDto issue3 = db.issues().insertIssue(newResolvedIssue().setType(BUG));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(VULNERABILITY.name())
      .build());

    checkResponse(response, 3, 1, 2, 0);
    assertThat(getIssueByKeys(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getUpdatedAt)
      .containsOnly(
        tuple(issue1.getKey(), VULNERABILITY.getDbConstant(), NOW),
        tuple(issue3.getKey(), BUG.getDbConstant(), issue2.getUpdatedAt()),
        tuple(issue2.getKey(), BUG.getDbConstant(), issue3.getUpdatedAt()));
  }

  @Test
  public void ignore_issues_when_there_is_nothing_to_do() throws Exception {
    setUserPermissions(USER);
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG).setSeverity(MINOR));
    // These 2 issues will be ignored as there's nothing to do
    IssueDto issue2 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));
    IssueDto issue3 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(VULNERABILITY.name())
      .build());

    checkResponse(response, 3, 1, 2, 0);
    assertThat(getIssueByKeys(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getUpdatedAt)
      .containsOnly(
        tuple(issue1.getKey(), VULNERABILITY.getDbConstant(), NOW),
        tuple(issue2.getKey(), VULNERABILITY.getDbConstant(), issue2.getUpdatedAt()),
        tuple(issue3.getKey(), VULNERABILITY.getDbConstant(), issue3.getUpdatedAt()));
  }

  @Test
  public void add_comment_only_on_changed_issues() throws Exception {
    setUserPermissions(USER);
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG).setSeverity(MINOR));
    // These 2 issues will be ignored as there's nothing to do
    IssueDto issue2 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));
    IssueDto issue3 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(VULNERABILITY.name())
      .setComment("test")
      .build());

    checkResponse(response, 3, 1, 2, 0);
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue1.getKey()), TYPE_COMMENT)).hasSize(1);
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue2.getKey()), TYPE_COMMENT)).isEmpty();
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue3.getKey()), TYPE_COMMENT)).isEmpty();
  }

  @Test
  public void not_authorized_issues_are_not_taking_into_account() throws Exception {
    setUserPermissions(USER);
    ComponentDto anotherProject = db.components().insertProject();
    ComponentDto anotherFile = db.components().insertComponent(newFileDto(anotherProject));
    IssueDto authorizedIssue = db.issues().insertIssue(newUnresolvedIssue(rule, file, project).setType(BUG));
    // User has no permission on these 2 issues
    IssueDto notAuthorizedIssue1 = db.issues().insertIssue(newUnresolvedIssue(rule, anotherFile, anotherProject).setType(BUG));
    IssueDto notAuthorizedIssue2 = db.issues().insertIssue(newUnresolvedIssue(rule, anotherFile, anotherProject).setType(BUG));

    BulkChangeWsResponse response = call(BulkChangeRequest.builder()
      .setIssues(asList(authorizedIssue.getKey(), notAuthorizedIssue1.getKey(), notAuthorizedIssue2.getKey()))
      .setSetType(VULNERABILITY.name())
      .build());

    checkResponse(response, 1, 1, 0, 0);
    assertThat(getIssueByKeys(authorizedIssue.getKey(), notAuthorizedIssue1.getKey(), notAuthorizedIssue2.getKey()))
      .extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getUpdatedAt)
      .containsOnly(
        tuple(authorizedIssue.getKey(), VULNERABILITY.getDbConstant(), NOW),
        tuple(notAuthorizedIssue1.getKey(), BUG.getDbConstant(), notAuthorizedIssue1.getUpdatedAt()),
        tuple(notAuthorizedIssue2.getKey(), BUG.getDbConstant(), notAuthorizedIssue2.getUpdatedAt()));
  }

  @Test
  public void fail_when_only_comment_action() throws Exception {
    setUserPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    expectedException.expectMessage("At least one action must be provided");
    expectedException.expect(IllegalArgumentException.class);

    call(BulkChangeRequest.builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setComment("type was badly defined")
      .build());
  }

  @Test
  public void fail_when_number_of_issues_is_more_than_500() throws Exception {
    userSession.login("john");
    expectedException.expectMessage("Number of issues is limited to 500");
    expectedException.expect(IllegalArgumentException.class);

    call(BulkChangeRequest.builder()
      .setIssues(IntStream.range(0, 510).mapToObj(String::valueOf).collect(Collectors.toList()))
      .setSetSeverity(MINOR)
      .build());
  }

  @Test
  public void fail_when_not_authenticated() throws Exception {
    expectedException.expect(UnauthorizedException.class);

    call(BulkChangeRequest.builder().setIssues(singletonList("ABCD")).build());
  }

  @Test
  public void test_definition() throws Exception {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("bulk_change");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(11);
    assertThat(action.responseExample()).isNull();
  }

  private BulkChangeWsResponse call(BulkChangeRequest bulkChangeRequest) {
    TestRequest request = tester.newRequest().setMediaType(MediaTypes.PROTOBUF);
    setNullable(bulkChangeRequest.getIssues(), value -> request.setParam("issues", String.join(",", value)));
    setNullable(bulkChangeRequest.getAssign(), value -> request.setParam("assign", value));
    setNullable(bulkChangeRequest.getSetSeverity(), value -> request.setParam("set_severity", value));
    setNullable(bulkChangeRequest.getSetType(), value -> request.setParam("set_type", value));
    setNullable(bulkChangeRequest.getDoTransition(), value -> request.setParam("do_transition", value));
    setNullable(bulkChangeRequest.getComment(), value -> request.setParam("comment", value));
    setNullable(bulkChangeRequest.getSendNotifications(), value -> request.setParam("sendNotifications", value != null ? value ? "true" : "false" : null));
    if (!bulkChangeRequest.getAddTags().isEmpty()) {
      request.setParam("add_tags", String.join(",", bulkChangeRequest.getAddTags()));
    }
    if (!bulkChangeRequest.getRemoveTags().isEmpty()) {
      request.setParam("remove_tags", String.join(",", bulkChangeRequest.getRemoveTags()));
    }
    try {
      return BulkChangeWsResponse.parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private void setUserPermissions(String... permissions) {
    UserSessionRule userSessionRule = userSession.login(user);
    for (String permission : permissions) {
      db.users().insertProjectPermissionOnUser(user, permission, project);
      userSessionRule.addProjectUuidPermissions(permission, project.uuid());
    }
  }

  private void checkResponse(BulkChangeWsResponse response, int total, int success, int ignored, int failure) {
    assertThat(response.getTotal()).isEqualTo(total);
    assertThat(response.getSuccess()).isEqualTo(success);
    assertThat(response.getIgnored()).isEqualTo(ignored);
    assertThat(response.getFailures()).isEqualTo(failure);
  }

  private List<IssueDto> getIssueByKeys(String... issueKeys) {
    return db.getDbClient().issueDao().selectByKeys(db.getSession(), asList(issueKeys));
  }

  private IssueDto newUnresolvedIssue(RuleDto rule, ComponentDto file, ComponentDto project) {
    return newDto(rule, file, project).setStatus(STATUS_OPEN).setResolution(null);
  }

  private IssueDto newUnresolvedIssue() {
    return newUnresolvedIssue(rule, file, project);
  }

  private IssueDto newResolvedIssue() {
    return newDto(rule, file, project).setStatus(STATUS_CLOSED).setResolution(RESOLUTION_FIXED);
  }

  private void addActions() {
    actions.add(new org.sonar.server.issue.AssignAction(new DefaultUserFinder(db.getDbClient().userDao()), issueFieldsSetter));
    actions.add(new org.sonar.server.issue.SetSeverityAction(issueFieldsSetter, userSession));
    actions.add(new org.sonar.server.issue.SetTypeAction(issueFieldsSetter));
    actions.add(new org.sonar.server.issue.TransitionAction(new TransitionService(userSession, new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter))));
    actions.add(new org.sonar.server.issue.AddTagsAction(issueFieldsSetter));
    actions.add(new org.sonar.server.issue.RemoveTagsAction(issueFieldsSetter));
    actions.add(new org.sonar.server.issue.CommentAction(issueFieldsSetter));
  }

}
