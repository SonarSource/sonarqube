/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.Action;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueStorage;
import org.sonar.server.issue.ServerIssueStorage;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues.BulkChangeWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
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
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter);
  private IssueStorage issueStorage = new ServerIssueStorage(system2, new DefaultRuleFinder(dbClient, defaultOrganizationProvider), dbClient,
    new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient)));
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private List<Action> actions = new ArrayList<>();

  private RuleDto rule;
  private OrganizationDto organization;
  private ComponentDto project;
  private ComponentDto file;
  private UserDto user;

  private WsActionTester tester = new WsActionTester(new BulkChangeAction(system2, userSession, dbClient, issueStorage, notificationManager, actions, issueChangePostProcessor));

  @Before
  public void setUp() {
    issueWorkflow.start();
    rule = db.rules().insertRule(newRuleDto());
    organization = db.organizations().insert();
    project = db.components().insertMainBranch(organization);
    file = db.components().insertComponent(newFileDto(project));
    user = db.users().insertUser("john");
    when(system2.now()).thenReturn(NOW);
    addActions();
  }

  @Test
  public void set_type() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setSetType(RuleType.CODE_SMELL.name())
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issueDto.getKey()).get(0);
    assertThat(reloaded.getType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);

    verifyPostProcessorCalled(file);
  }

  @Test
  public void set_severity() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setSeverity(MAJOR));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setSetSeverity(MINOR)
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issueDto.getKey()).get(0);
    assertThat(reloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);

    verifyPostProcessorCalled(file);
  }

  @Test
  public void add_tags() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setTags(asList("tag1", "tag2")));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setAddTags(singletonList("tag3"))
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issueDto.getKey()).get(0);
    assertThat(reloaded.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);

    // no need to refresh measures
    verifyPostProcessorNotCalled();
  }

  @Test
  public void remove_assignee() {
    setUserProjectPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setAssignee("arthur"));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setAssign("")
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issueDto.getKey()).get(0);
    assertThat(reloaded.getAssignee()).isNull();
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);

    // no need to refresh measures
    verifyPostProcessorNotCalled();
  }

  @Test
  public void bulk_change_with_comment() {
    setUserProjectPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setDoTransition("confirm")
      .setComment("type was badly defined")
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueChangeDto issueComment = dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issueDto.getKey()), TYPE_COMMENT).get(0);
    assertThat(issueComment.getUserLogin()).isEqualTo("john");
    assertThat(issueComment.getChangeData()).isEqualTo("type was badly defined");

    verifyPostProcessorCalled(file);
  }

  @Test
  public void bulk_change_many_issues() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    UserDto userToAssign = db.users().insertUser("arthur");
    db.organizations().addMember(organization, user);
    db.organizations().addMember(organization, userToAssign);
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setAssignee(user.getLogin())).setType(BUG).setSeverity(MINOR);
    IssueDto issue2 = db.issues().insertIssue(newUnresolvedIssue().setAssignee(userToAssign.getLogin())).setType(BUG).setSeverity(MAJOR);
    IssueDto issue3 = db.issues().insertIssue(newUnresolvedIssue().setAssignee(null)).setType(VULNERABILITY).setSeverity(MAJOR);

    BulkChangeWsResponse response = call(builder()
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

    verifyPostProcessorCalled(file);
  }

  @Test
  public void send_notification() {
    setUserProjectPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setDoTransition("confirm")
      .setSendNotifications(true)
      .build());

    checkResponse(response, 1, 1, 0, 0);

    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);
    verify(notificationManager).scheduleForSending(issueChangeNotificationCaptor.capture());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("key")).isEqualTo(issueDto.getKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("componentName")).isEqualTo(file.longName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectName")).isEqualTo(project.name());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectKey")).isEqualTo(project.getDbKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("ruleName")).isEqualTo(rule.getName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("changeAuthor")).isEqualTo(user.getLogin());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("branch")).isNull();
  }

  @Test
  public void send_notification_on_branch() {
    setUserProjectPermissions(USER);

    String branchName = "feature1";
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));
    ComponentDto fileOnBranch = db.components().insertComponent(newFileDto(branch));
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue(rule, fileOnBranch, branch).setType(BUG));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setDoTransition("confirm")
      .setSendNotifications(true)
      .build());

    checkResponse(response, 1, 1, 0, 0);

    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);
    verify(notificationManager).scheduleForSending(issueChangeNotificationCaptor.capture());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("key")).isEqualTo(issueDto.getKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("componentName")).isEqualTo(fileOnBranch.longName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectName")).isEqualTo(project.name());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectKey")).isEqualTo(project.getDbKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("ruleName")).isEqualTo(rule.getName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("changeAuthor")).isEqualTo(user.getLogin());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("branch")).isEqualTo(branchName);

    verifyPostProcessorCalled(fileOnBranch);
  }

  @Test
  public void send_notification_only_on_changed_issues() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    IssueDto issue2 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    IssueDto issue3 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));
    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(RuleType.BUG.name())
      .setSendNotifications(true)
      .build());

    checkResponse(response, 3, 1, 2, 0);
    verify(notificationManager).scheduleForSending(issueChangeNotificationCaptor.capture());
    assertThat(issueChangeNotificationCaptor.getAllValues()).hasSize(1);
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("key")).isEqualTo(issue3.getKey());

    verifyPostProcessorCalled(file);
  }

  @Test
  public void ignore_the_issues_that_do_not_match_conditions() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    // These 2 issues will be ignored as they are resolved, changing type is not possible
    IssueDto issue2 = db.issues().insertIssue(newResolvedIssue().setType(BUG));
    IssueDto issue3 = db.issues().insertIssue(newResolvedIssue().setType(BUG).setComponent(file2));

    BulkChangeWsResponse response = call(builder()
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

    // file2 is not refreshed
    verifyPostProcessorCalled(file);
  }

  @Test
  public void ignore_issues_when_there_is_nothing_to_do() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG).setSeverity(MINOR));
    // These 2 issues will be ignored as there's nothing to do
    IssueDto issue2 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));
    IssueDto issue3 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY).setComponent(file2));

    BulkChangeWsResponse response = call(builder()
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

    // file2 is not refreshed
    verifyPostProcessorCalled(file);
  }

  @Test
  public void add_comment_only_on_changed_issues() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    IssueDto issue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG).setSeverity(MINOR));
    // These 2 issues will be ignored as there's nothing to do
    IssueDto issue2 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));
    IssueDto issue3 = db.issues().insertIssue(newUnresolvedIssue().setType(VULNERABILITY));

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(VULNERABILITY.name())
      .setComment("test")
      .build());

    checkResponse(response, 3, 1, 2, 0);
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue1.getKey()), TYPE_COMMENT)).hasSize(1);
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue2.getKey()), TYPE_COMMENT)).isEmpty();
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue3.getKey()), TYPE_COMMENT)).isEmpty();

    verifyPostProcessorCalled(file);
  }

  @Test
  public void issues_on_which_user_has_not_browse_permission_are_ignored() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    ComponentDto anotherProject = db.components().insertPrivateProject();
    ComponentDto anotherFile = db.components().insertComponent(newFileDto(anotherProject));
    IssueDto authorizedIssue = db.issues().insertIssue(newUnresolvedIssue(rule, file, project).setType(BUG));
    // User has not browse permission on these 2 issues
    IssueDto notAuthorizedIssue1 = db.issues().insertIssue(newUnresolvedIssue(rule, anotherFile, anotherProject).setType(BUG));
    IssueDto notAuthorizedIssue2 = db.issues().insertIssue(newUnresolvedIssue(rule, anotherFile, anotherProject).setType(BUG));

    BulkChangeWsResponse response = call(builder()
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

    verifyPostProcessorCalled(file);
  }

  @Test
  public void does_not_update_type_when_no_issue_admin_permission() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    ComponentDto anotherProject = db.components().insertPrivateProject();
    ComponentDto anotherFile = db.components().insertComponent(newFileDto(anotherProject));
    addUserProjectPermissions(anotherProject, USER);

    IssueDto authorizedIssue1 = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));
    // User has not issue admin permission on these 2 issues
    IssueDto notAuthorizedIssue1 = db.issues().insertIssue(newUnresolvedIssue(rule, anotherFile, anotherProject).setType(BUG));
    IssueDto notAuthorizedIssue2 = db.issues().insertIssue(newUnresolvedIssue(rule, anotherFile, anotherProject).setType(BUG));

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(authorizedIssue1.getKey(), notAuthorizedIssue1.getKey(), notAuthorizedIssue2.getKey()))
      .setSetType(VULNERABILITY.name())
      .build());

    checkResponse(response, 3, 1, 2, 0);
    assertThat(getIssueByKeys(authorizedIssue1.getKey(), notAuthorizedIssue1.getKey(), notAuthorizedIssue2.getKey()))
      .extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getUpdatedAt)
      .containsOnly(
        tuple(authorizedIssue1.getKey(), VULNERABILITY.getDbConstant(), NOW),
        tuple(notAuthorizedIssue1.getKey(), BUG.getDbConstant(), notAuthorizedIssue1.getUpdatedAt()),
        tuple(notAuthorizedIssue2.getKey(), BUG.getDbConstant(), notAuthorizedIssue2.getUpdatedAt()));
    verifyPostProcessorCalled(file);
  }

  @Test
  public void does_not_update_severity_when_no_issue_admin_permission() {
    setUserProjectPermissions(USER, ISSUE_ADMIN);
    ComponentDto anotherProject = db.components().insertPrivateProject();
    ComponentDto anotherFile = db.components().insertComponent(newFileDto(anotherProject));
    addUserProjectPermissions(anotherProject, USER);

    IssueDto authorizedIssue1 = db.issues().insertIssue(newUnresolvedIssue().setSeverity(MAJOR));
    // User has not issue admin permission on these 2 issues
    IssueDto notAuthorizedIssue1 = db.issues().insertIssue(newUnresolvedIssue(rule, anotherFile, anotherProject).setSeverity(MAJOR));
    IssueDto notAuthorizedIssue2 = db.issues().insertIssue(newUnresolvedIssue(rule, anotherFile, anotherProject).setSeverity(MAJOR));

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(authorizedIssue1.getKey(), notAuthorizedIssue1.getKey(), notAuthorizedIssue2.getKey()))
      .setSetSeverity(MINOR)
      .build());

    checkResponse(response, 3, 1, 2, 0);
    assertThat(getIssueByKeys(authorizedIssue1.getKey(), notAuthorizedIssue1.getKey(), notAuthorizedIssue2.getKey()))
      .extracting(IssueDto::getKey, IssueDto::getSeverity, IssueDto::getUpdatedAt)
      .containsOnly(
        tuple(authorizedIssue1.getKey(), MINOR, NOW),
        tuple(notAuthorizedIssue1.getKey(), MAJOR, notAuthorizedIssue1.getUpdatedAt()),
        tuple(notAuthorizedIssue2.getKey(), MAJOR, notAuthorizedIssue2.getUpdatedAt()));

    verifyPostProcessorCalled(file);
  }

  @Test
  public void fail_when_only_comment_action() {
    setUserProjectPermissions(USER);
    IssueDto issueDto = db.issues().insertIssue(newUnresolvedIssue().setType(BUG));

    expectedException.expectMessage("At least one action must be provided");
    expectedException.expect(IllegalArgumentException.class);

    call(builder()
      .setIssues(singletonList(issueDto.getKey()))
      .setComment("type was badly defined")
      .build());
  }

  @Test
  public void fail_when_number_of_issues_is_more_than_500() {
    userSession.logIn("john");

    expectedException.expectMessage("Number of issues is limited to 500");
    expectedException.expect(IllegalArgumentException.class);

    call(builder()
      .setIssues(IntStream.range(0, 510).mapToObj(String::valueOf).collect(Collectors.toList()))
      .setSetSeverity(MINOR)
      .build());
  }

  @Test
  public void fail_when_not_authenticated() {
    expectedException.expect(UnauthorizedException.class);

    call(builder().setIssues(singletonList("ABCD")).build());
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("bulk_change");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(10);
    assertThat(action.responseExample()).isNotNull();
  }

  private BulkChangeWsResponse call(BulkChangeRequest bulkChangeRequest) {
    TestRequest request = tester.newRequest();
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
    return request.executeProtobuf(BulkChangeWsResponse.class);
  }

  private void setUserProjectPermissions(String... permissions) {
    userSession.logIn(user);
    addUserProjectPermissions(project, permissions);
  }

  private void addUserProjectPermissions(ComponentDto project, String... permissions) {
    for (String permission : permissions) {
      db.users().insertProjectPermissionOnUser(user, permission, project);
      userSession.addProjectPermission(permission, project);
    }
  }

  private void checkResponse(BulkChangeWsResponse response, long total, long success, long ignored, long failure) {
    assertThat(response)
      .extracting(BulkChangeWsResponse::getTotal, BulkChangeWsResponse::getSuccess, BulkChangeWsResponse::getIgnored, BulkChangeWsResponse::getFailures)
      .as("Total, success, ignored, failure")
      .containsOnly(total, success, ignored, failure);
  }

  private List<IssueDto> getIssueByKeys(String... issueKeys) {
    return db.getDbClient().issueDao().selectByKeys(db.getSession(), asList(issueKeys));
  }

  private void verifyPostProcessorCalled(ComponentDto... components) {
    assertThat(issueChangePostProcessor.calledComponents()).containsExactlyInAnyOrder(components);
  }

  private void verifyPostProcessorNotCalled() {
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
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
    actions.add(new org.sonar.server.issue.AssignAction(db.getDbClient(), issueFieldsSetter));
    actions.add(new org.sonar.server.issue.SetSeverityAction(issueFieldsSetter, userSession));
    actions.add(new org.sonar.server.issue.SetTypeAction(issueFieldsSetter, userSession));
    actions.add(new org.sonar.server.issue.TransitionAction(new TransitionService(userSession, issueWorkflow)));
    actions.add(new org.sonar.server.issue.AddTagsAction(issueFieldsSetter));
    actions.add(new org.sonar.server.issue.RemoveTagsAction(issueFieldsSetter));
    actions.add(new org.sonar.server.issue.CommentAction(issueFieldsSetter));
  }

  private static class BulkChangeRequest {

    private final List<String> issues;
    private final String assign;
    private final String setSeverity;
    private final String setType;
    private final String doTransition;
    private final List<String> addTags;
    private final List<String> removeTags;
    private final String comment;
    private final Boolean sendNotifications;

    private BulkChangeRequest(Builder builder) {
      this.issues = builder.issues;
      this.assign = builder.assign;
      this.setSeverity = builder.setSeverity;
      this.setType = builder.setType;
      this.doTransition = builder.doTransition;
      this.addTags = builder.addTags;
      this.removeTags = builder.removeTags;
      this.comment = builder.comment;
      this.sendNotifications = builder.sendNotifications;
    }

    public List<String> getIssues() {
      return issues;
    }

    @CheckForNull
    public String getAssign() {
      return assign;
    }

    @CheckForNull
    public String getSetSeverity() {
      return setSeverity;
    }

    @CheckForNull
    public String getSetType() {
      return setType;
    }

    @CheckForNull
    public String getDoTransition() {
      return doTransition;
    }

    public List<String> getAddTags() {
      return addTags;
    }

    public List<String> getRemoveTags() {
      return removeTags;
    }

    @CheckForNull
    public String getComment() {
      return comment;
    }

    @CheckForNull
    public Boolean getSendNotifications() {
      return sendNotifications;
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<String> issues;
    private String assign;
    private String setSeverity;
    private String setType;
    private String doTransition;
    private List<String> addTags = newArrayList();
    private List<String> removeTags = newArrayList();
    private String comment;
    private Boolean sendNotifications;

    public Builder setIssues(List<String> issues) {
      this.issues = issues;
      return this;
    }

    public Builder setAssign(@Nullable String assign) {
      this.assign = assign;
      return this;
    }

    public Builder setSetSeverity(@Nullable String setSeverity) {
      this.setSeverity = setSeverity;
      return this;
    }

    public Builder setSetType(@Nullable String setType) {
      this.setType = setType;
      return this;
    }

    public Builder setDoTransition(@Nullable String doTransition) {
      this.doTransition = doTransition;
      return this;
    }

    public Builder setAddTags(List<String> addTags) {
      this.addTags = requireNonNull(addTags);
      return this;
    }

    public Builder setRemoveTags(List<String> removeTags) {
      this.removeTags = requireNonNull(removeTags);
      return this;
    }

    public Builder setComment(@Nullable String comment) {
      this.comment = comment;
      return this;
    }

    public Builder setSendNotifications(@Nullable Boolean sendNotifications) {
      this.sendNotifications = sendNotifications;
      return this;
    }

    public BulkChangeRequest build() {
      checkArgument(issues != null && !issues.isEmpty(), "Issue keys must be provided");
      return new BulkChangeRequest(this);
    }
  }
}
