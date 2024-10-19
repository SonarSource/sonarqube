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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.Action;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssuesChangesNotification;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.pushapi.issues.IssueChangeEventService;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues.BulkChangeWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.issue.DefaultTransitions.RESOLVE_AS_REVIEWED;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.SECURITYHOTSPOT_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueChangeDto.TYPE_COMMENT;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.projectBranchOf;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.projectOf;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.ruleOf;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.userOf;

public class BulkChangeActionIT {

  private static long NOW = 2_000_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();

  private IssueChangeEventService issueChangeEventService = mock(IssueChangeEventService.class);
  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter);
  private WebIssueStorage issueStorage = new WebIssueStorage(system2, dbClient,
    new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)),
    new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), null), new SequenceUuidFactory());
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private IssuesChangesNotificationSerializer issuesChangesSerializer = new IssuesChangesNotificationSerializer();
  private ArgumentCaptor<IssuesChangesNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssuesChangesNotification.class);
  private List<Action> actions = new ArrayList<>();

  private WsActionTester tester = new WsActionTester(new BulkChangeAction(system2, userSession, dbClient, issueStorage, notificationManager, actions,
    issueChangePostProcessor, issuesChangesSerializer, issueChangeEventService));

  @Before
  public void setUp() {
    issueWorkflow.start();
    addActions();
  }

  @Test
  public void set_type() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setSetType(CODE_SMELL.name())
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issue.getKey()).get(0);
    assertThat(reloaded.getType()).isEqualTo(CODE_SMELL.getDbConstant());
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);

    verifyPostProcessorCalled(file);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any());
  }

  @Test
  public void set_severity() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setSeverity(MAJOR).setType(CODE_SMELL)
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setSetSeverity(MINOR)
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issue.getKey()).get(0);
    assertThat(reloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);

    verifyPostProcessorCalled(file);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any());
  }

  @Test
  public void add_tags() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setTags(asList("tag1", "tag2"))
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setAddTags(singletonList("tag3"))
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issue.getKey()).get(0);
    assertThat(reloaded.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);

    // no need to refresh measures
    verifyPostProcessorNotCalled();
    verifyNoInteractions(issueChangeEventService);
  }

  @Test
  public void add_tags_when_issue_is_resolved_is_accepted() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setTags(asList("tag1", "tag2"))
      .setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FALSE_POSITIVE));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setAddTags(singletonList("tag3"))
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issue.getKey()).get(0);
    assertThat(reloaded.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
    assertThat(reloaded.getResolution()).isEqualTo("FALSE-POSITIVE");
  }

  @Test
  public void remove_assignee() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    UserDto assignee = db.users().insertUser();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setAssigneeUuid(assignee.getUuid())
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setAssign("")
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueDto reloaded = getIssueByKeys(issue.getKey()).get(0);
    assertThat(reloaded.getAssigneeUuid()).isNull();
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);

    // no need to refresh measures
    verifyPostProcessorNotCalled();
    verifyNoInteractions(issueChangeEventService);
  }

  @Test
  public void bulk_change_with_comment() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setDoTransition("confirm")
      .setComment("type was badly defined")
      .build());

    checkResponse(response, 1, 1, 0, 0);
    IssueChangeDto issueComment = dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue.getKey()), TYPE_COMMENT).get(0);
    assertThat(issueComment.getUserUuid()).isEqualTo(user.getUuid());
    assertThat(issueComment.getChangeData()).isEqualTo("type was badly defined");

    verifyPostProcessorCalled(file);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any());
  }

  @Test
  public void bulk_change_many_issues() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    UserDto oldAssignee = db.users().insertUser();
    UserDto userToAssign = db.users().insertUser();
    IssueDto issue1 = db.issues().insertIssue(rule, project, file,
      i -> i.setAssigneeUuid(oldAssignee.getUuid()).setType(BUG).setSeverity(MINOR).setStatus(STATUS_OPEN).setResolution(null));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file,
      i -> i.setAssigneeUuid(userToAssign.getUuid()).setType(CODE_SMELL).setSeverity(MAJOR).setStatus(STATUS_OPEN).setResolution(null));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file,
      i -> i.setAssigneeUuid(null).setType(VULNERABILITY).setSeverity(MAJOR).setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setAssign(userToAssign.getLogin())
      .setSetSeverity(MINOR)
      .setSetType(VULNERABILITY.name())
      .build());

    checkResponse(response, 3, 3, 0, 0);
    assertThat(getIssueByKeys(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .extracting(IssueDto::getKey, IssueDto::getAssigneeUuid, IssueDto::getType, IssueDto::getSeverity, IssueDto::getUpdatedAt)
      .containsOnly(
        tuple(issue1.getKey(), userToAssign.getUuid(), VULNERABILITY.getDbConstant(), MINOR, NOW),
        tuple(issue2.getKey(), userToAssign.getUuid(), VULNERABILITY.getDbConstant(), MINOR, NOW),
        tuple(issue3.getKey(), userToAssign.getUuid(), VULNERABILITY.getDbConstant(), MINOR, NOW));

    verifyPostProcessorCalled(file);
    verify(issueChangeEventService).distributeIssueChangeEvent(any(), any(), any());
  }

  @Test
  public void send_notification() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setDoTransition("confirm")
      .setSendNotifications(true)
      .build());

    checkResponse(response, 1, 1, 0, 0);
    verify(notificationManager).scheduleForSending(issueChangeNotificationCaptor.capture());
    IssuesChangesNotificationBuilder builder = issuesChangesSerializer.from(issueChangeNotificationCaptor.getValue());
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changedIssue = builder.getIssues().iterator().next();
    assertThat(changedIssue.getKey()).isEqualTo(issue.getKey());
    assertThat(changedIssue.getProject().getUuid()).isEqualTo(project.uuid());
    assertThat(changedIssue.getProject().getKey()).isEqualTo(project.getKey());
    assertThat(changedIssue.getProject().getProjectName()).isEqualTo(project.name());
    assertThat(changedIssue.getProject().getBranchName()).isEmpty();
    assertThat(changedIssue.getRule().getKey()).isEqualTo(rule.getKey());
    assertThat(changedIssue.getRule().getName()).isEqualTo(rule.getName());
    assertThat(builder.getChange().getDate()).isEqualTo(NOW);
    assertThat(builder.getChange()).isInstanceOf(UserChange.class);
    UserChange userChange = (UserChange) builder.getChange();
    assertThat(userChange.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(userChange.getUser().getLogin()).isEqualTo(user.getLogin());
    assertThat(userChange.getUser().getName()).contains(user.getName());
  }

  @Test
  public void should_ignore_hotspots() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, SECURITYHOTSPOT_ADMIN);
    IssueDto issue = db.issues().insertHotspot(project, file);

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setDoTransition(RESOLVE_AS_REVIEWED)
      .setSendNotifications(true)
      .build());

    checkResponse(response, 0, 0, 0, 0);
    verifyNoInteractions(notificationManager);
  }

  @Test
  public void send_notification_on_branch() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature").setBranchType(BranchType.BRANCH));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    ComponentDto fileOnBranch = db.components().insertComponent(newFileDto(branch, project.uuid()));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, branch, fileOnBranch, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setDoTransition("confirm")
      .setSendNotifications(true)
      .build());

    checkResponse(response, 1, 1, 0, 0);
    verify(notificationManager).scheduleForSending(issueChangeNotificationCaptor.capture());
    IssuesChangesNotificationBuilder builder = issuesChangesSerializer.from(issueChangeNotificationCaptor.getValue());
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changedIssue = builder.getIssues().iterator().next();
    assertThat(changedIssue.getKey()).isEqualTo(issue.getKey());
    assertThat(changedIssue.getNewStatus()).isEqualTo(STATUS_CONFIRMED);
    assertThat(changedIssue.getNewIssueStatus()).contains(IssueStatus.CONFIRMED);
    assertThat(changedIssue.getOldIssueStatus()).contains(IssueStatus.OPEN);
    assertThat(changedIssue.getAssignee()).isEmpty();
    assertThat(changedIssue.getRule()).isEqualTo(ruleOf(rule));
    assertThat(changedIssue.getProject()).isEqualTo(projectBranchOf(db, branch));
    assertThat(builder.getChange()).isEqualTo(new UserChange(NOW, userOf(user)));
    verifyPostProcessorCalled(fileOnBranch);
  }

  @Test
  public void send_no_notification_on_PR() {
    verifySendNoNotification(BranchType.PULL_REQUEST);
  }

  private void verifySendNoNotification(BranchType branchType) {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature").setBranchType(branchType));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    ComponentDto fileOnBranch = db.components().insertComponent(newFileDto(branch, project.uuid()));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, branch, fileOnBranch, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(singletonList(issue.getKey()))
      .setDoTransition("confirm")
      .setSendNotifications(true)
      .build());

    checkResponse(response, 1, 1, 0, 0);
    verifyNoInteractions(notificationManager);
    verifyPostProcessorCalled(fileOnBranch);
  }

  @Test
  public void send_notification_only_on_changed_issues() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue1 = db.issues().insertIssue(rule, project, file, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file, i -> i.setType(VULNERABILITY)
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(RuleType.BUG.name())
      .setSendNotifications(true)
      .build());

    checkResponse(response, 3, 1, 2, 0);
    verify(notificationManager).scheduleForSending(issueChangeNotificationCaptor.capture());
    assertThat(issueChangeNotificationCaptor.getAllValues()).hasSize(1);
    IssuesChangesNotificationBuilder builder = issuesChangesSerializer.from(issueChangeNotificationCaptor.getValue());
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changedIssue = builder.getIssues().iterator().next();
    assertThat(changedIssue.getKey()).isEqualTo(issue3.getKey());
    assertThat(changedIssue.getNewStatus()).isEqualTo(STATUS_OPEN);
    assertThat(changedIssue.getNewIssueStatus()).contains(IssueStatus.OPEN);
    assertThat(changedIssue.getOldIssueStatus()).contains(IssueStatus.OPEN);
    assertThat(changedIssue.getAssignee()).isEmpty();
    assertThat(changedIssue.getRule()).isEqualTo(ruleOf(rule));
    assertThat(changedIssue.getProject()).isEqualTo(projectOf(project));
    assertThat(builder.getChange()).isEqualTo(new UserChange(NOW, userOf(user)));
  }

  @Test
  public void ignore_the_issues_that_do_not_match_conditions() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue1 = db.issues().insertIssue(rule, project, file1, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));
    // These 2 issues will be ignored as they are resolved, changing type is not possible
    IssueDto issue2 = db.issues().insertIssue(rule, project, file1, i -> i.setType(BUG)
      .setStatus(STATUS_CLOSED).setResolution(RESOLUTION_FIXED));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file2, i -> i.setType(BUG)
      .setStatus(STATUS_CLOSED).setResolution(RESOLUTION_FIXED));

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(VULNERABILITY.name())
      .build());

    checkResponse(response, 3, 1, 2, 0);
    assertThat(getIssueByKeys(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getUpdatedAt)
      .containsOnly(
        tuple(issue1.getKey(), VULNERABILITY.getDbConstant(), NOW),
        tuple(issue2.getKey(), BUG.getDbConstant(), issue2.getUpdatedAt()),
        tuple(issue3.getKey(), BUG.getDbConstant(), issue3.getUpdatedAt()));

    // file2 is not refreshed
    verifyPostProcessorCalled(file1);
  }

  @Test
  public void ignore_issues_when_there_is_nothing_to_do() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue1 = db.issues().insertIssue(rule, project, file1, i -> i.setType(BUG).setSeverity(MINOR)
      .setStatus(STATUS_OPEN).setResolution(null));
    // These 2 issues will be ignored as there's nothing to do
    IssueDto issue2 = db.issues().insertIssue(rule, project, file1, i -> i.setType(VULNERABILITY)
      .setStatus(STATUS_OPEN).setResolution(null));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file2, i -> i.setType(VULNERABILITY)
      .setStatus(STATUS_OPEN).setResolution(null));

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
    verifyPostProcessorCalled(file1);
  }

  @Test
  public void add_comment_only_on_changed_issues() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue1 = db.issues().insertIssue(rule, project, file1, i -> i.setType(BUG).setSeverity(MINOR)
      .setStatus(STATUS_OPEN).setResolution(null));
    // These 2 issues will be ignored as there's nothing to do
    IssueDto issue2 = db.issues().insertIssue(rule, project, file1, i -> i.setType(VULNERABILITY)
      .setStatus(STATUS_OPEN).setResolution(null));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file2, i -> i.setType(VULNERABILITY)
      .setStatus(STATUS_OPEN).setResolution(null));

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(issue1.getKey(), issue2.getKey(), issue3.getKey()))
      .setSetType(VULNERABILITY.name())
      .setComment("test")
      .build());

    checkResponse(response, 3, 1, 2, 0);
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue1.getKey()), TYPE_COMMENT)).hasSize(1);
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue2.getKey()), TYPE_COMMENT)).isEmpty();
    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), singletonList(issue3.getKey()), TYPE_COMMENT)).isEmpty();

    verifyPostProcessorCalled(file1);
  }

  @Test
  public void bulk_change_includes_external_issue() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    addUserProjectPermissions(user, projectData, USER, ISSUE_ADMIN);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, project, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    RuleDto externalRule = db.rules().insertIssueRule(r -> r.setIsExternal(true));
    IssueDto externalIssue = db.issues().insertIssue(externalRule, project, project, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));

    BulkChangeWsResponse response = call(builder()
      .setIssues(asList(issue.getKey(), externalIssue.getKey()))
      .setDoTransition("confirm")
      .build());

    checkResponse(response, 2, 2, 0, 0);
  }

  @Test
  public void issues_on_which_user_has_not_browse_permission_are_ignored() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    addUserProjectPermissions(user, projectData1, USER, ISSUE_ADMIN);
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto authorizedIssue = db.issues().insertIssue(rule, project1, project1, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));
    // User has not browse permission on these 2 issues
    IssueDto notAuthorizedIssue1 = db.issues().insertIssue(rule, project2, project2, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));
    IssueDto notAuthorizedIssue2 = db.issues().insertIssue(rule, project2, project2, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));

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

    verifyPostProcessorCalled(project1);
  }

  @Test
  public void does_not_update_type_when_no_issue_admin_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    addUserProjectPermissions(user, projectData1, USER, ISSUE_ADMIN);
    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    addUserProjectPermissions(user, projectData2, USER);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto authorizedIssue1 = db.issues().insertIssue(rule, project1, project1, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));
    // User has not issue admin permission on these 2 issues
    IssueDto notAuthorizedIssue1 = db.issues().insertIssue(rule, project2, project2, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));
    IssueDto notAuthorizedIssue2 = db.issues().insertIssue(rule, project2, project2, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));

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
    verifyPostProcessorCalled(project1);
  }

  @Test
  public void does_not_update_severity_when_no_issue_admin_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    addUserProjectPermissions(user, projectData1, USER, ISSUE_ADMIN);
    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    addUserProjectPermissions(user, projectData2, USER);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto authorizedIssue1 = db.issues().insertIssue(rule, project1, project1, i -> i.setSeverity(MAJOR)
      .setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    // User has not issue admin permission on these 2 issues
    IssueDto notAuthorizedIssue1 = db.issues().insertIssue(rule, project2, project2, i -> i.setSeverity(MAJOR)
      .setStatus(STATUS_OPEN).setResolution(null).setType(BUG));
    IssueDto notAuthorizedIssue2 = db.issues().insertIssue(rule, project2, project2, i -> i.setSeverity(MAJOR)
      .setStatus(STATUS_OPEN).setResolution(null).setType(VULNERABILITY));

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
    verifyPostProcessorCalled(project1);
  }

  @Test
  public void fail_when_only_comment_action() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    addUserProjectPermissions(user, projectData, USER);
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, project, i -> i.setType(BUG)
      .setStatus(STATUS_OPEN).setResolution(null));

    assertThatThrownBy(() -> {
      call(builder()
        .setIssues(singletonList(issue.getKey()))
        .setComment("type was badly defined")
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("At least one action must be provided");
  }

  @Test
  public void fail_when_number_of_issues_is_more_than_500() {
    userSession.logIn("john");

    assertThatThrownBy(() -> {
      call(builder()
        .setIssues(IntStream.range(0, 510).mapToObj(String::valueOf).toList())
        .setSetSeverity(MINOR)
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Number of issues is limited to 500");
  }

  @Test
  public void fail_when_not_authenticated() {
    assertThatThrownBy(() -> {
      call(builder().setIssues(singletonList("ABCD")).build());
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("bulk_change");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(9);
    assertThat(action.responseExample()).isNotNull();
  }

  private BulkChangeWsResponse call(BulkChangeRequest bulkChangeRequest) {
    TestRequest request = tester.newRequest();
    ofNullable(bulkChangeRequest.getIssues()).ifPresent(value6 -> request.setParam("issues", String.join(",", value6)));
    ofNullable(bulkChangeRequest.getAssign()).ifPresent(value5 -> request.setParam("assign", value5));
    ofNullable(bulkChangeRequest.getSetSeverity()).ifPresent(value4 -> request.setParam("set_severity", value4));
    ofNullable(bulkChangeRequest.getSetType()).ifPresent(value3 -> request.setParam("set_type", value3));
    ofNullable(bulkChangeRequest.getDoTransition()).ifPresent(value2 -> request.setParam("do_transition", value2));
    ofNullable(bulkChangeRequest.getComment()).ifPresent(value1 -> request.setParam("comment", value1));
    ofNullable(bulkChangeRequest.getSendNotifications()).ifPresent(value -> request.setParam("sendNotifications", value != null ? value ? "true" : "false" : null));
    if (!bulkChangeRequest.getAddTags().isEmpty()) {
      request.setParam("add_tags", String.join(",", bulkChangeRequest.getAddTags()));
    }
    if (!bulkChangeRequest.getRemoveTags().isEmpty()) {
      request.setParam("remove_tags", String.join(",", bulkChangeRequest.getRemoveTags()));
    }
    return request.executeProtobuf(BulkChangeWsResponse.class);
  }

  private void addUserProjectPermissions(UserDto user, ProjectData project, String... permissions) {
    for (String permission : permissions) {
      db.users().insertProjectPermissionOnUser(user, permission, project.getProjectDto());
      userSession.addProjectPermission(permission, project.getProjectDto());
      userSession.addProjectBranchMapping(project.projectUuid(), project.getMainBranchComponent());
    }
  }

  private void checkResponse(BulkChangeWsResponse response, long total, long success, long ignored, long failure) {
    assertThat(response)
      .extracting(BulkChangeWsResponse::getTotal, BulkChangeWsResponse::getSuccess, BulkChangeWsResponse::getIgnored, BulkChangeWsResponse::getFailures)
      .as("Total, success, ignored, failure")
      .containsExactly(total, success, ignored, failure);
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
