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

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssuesChangesNotification;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.projectBranchOf;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.projectOf;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.ruleOf;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.userOf;

public class IssueUpdaterIT {

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  private DbClient dbClient = db.getDbClient();

  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private ArgumentCaptor<IssuesChangesNotification> notificationArgumentCaptor = ArgumentCaptor.forClass(IssuesChangesNotification.class);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), null);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private IssuesChangesNotificationSerializer issuesChangesSerializer = new IssuesChangesNotificationSerializer();
  private IssueUpdater underTest = new IssueUpdater(dbClient,
    new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, new SequenceUuidFactory()), notificationManager,
    issueChangePostProcessor, issuesChangesSerializer);

  @Test
  public void update_issue() {
    IssueDto originalIssueDto = db.issues().insertIssue(i -> i.setSeverity(MAJOR));
    DefaultIssue issue = originalIssueDto.toDefaultIssue();
    UserDto user = db.users().insertUser();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), user.getUuid()).build();
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issue.key()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(BLOCKER);
  }

  @Test
  public void verify_notification_without_resolution() {
    UserDto assignee = db.users().insertUser();
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto originalIssueDto = db.issues().insertIssue(rule, project, file,
      t -> t.setSeverity(MAJOR).setAssigneeUuid(assignee.getUuid()));
    DefaultIssue issue = originalIssueDto
      .toDefaultIssue();
    UserDto changeAuthor = db.users().insertUser();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), changeAuthor.getUuid()).build();
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    IssuesChangesNotification issueChangeNotification = notificationArgumentCaptor.getValue();
    IssuesChangesNotificationBuilder builder = issuesChangesSerializer.from(issueChangeNotification);
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changedIssue = builder.getIssues().iterator().next();
    assertThat(changedIssue.getKey()).isEqualTo(issue.key());
    assertThat(changedIssue.getNewStatus()).isEqualTo(issue.status());
    assertThat(changedIssue.getAssignee()).contains(userOf(assignee));
    assertThat(changedIssue.getRule()).isEqualTo(ruleOf(rule));
    assertThat(changedIssue.getProject()).isEqualTo(projectOf(project));
    assertThat(builder.getChange()).isEqualTo(new UserChange(issue.updateDate().getTime(), userOf(changeAuthor)));
  }

  @Test
  public void verify_notification_with_resolution() {
    UserDto assignee = db.users().insertUser();
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto originalIssueDto = db.issues().insertIssue(rule, project, file,
      t -> t.setSeverity(MAJOR).setAssigneeUuid(assignee.getUuid()));
    DefaultIssue issue = originalIssueDto
      .toDefaultIssue();
    UserDto changeAuthor = db.users().insertUser();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), changeAuthor.getUuid()).build();
    issueFieldsSetter.setResolution(issue, RESOLUTION_FIXED, context);
    issueFieldsSetter.setStatus(issue, STATUS_RESOLVED, context);

    underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    IssuesChangesNotification issueChangeNotification = notificationArgumentCaptor.getValue();
    IssuesChangesNotificationBuilder builder = issuesChangesSerializer.from(issueChangeNotification);
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changedIssue = builder.getIssues().iterator().next();
    assertThat(changedIssue.getKey()).isEqualTo(issue.key());
    assertThat(changedIssue.getNewStatus()).isEqualTo(issue.status());
    assertThat(changedIssue.getOldIssueStatus()).contains(originalIssueDto.getIssueStatus());
    assertThat(changedIssue.getNewIssueStatus()).contains(issue.issueStatus());
    assertThat(changedIssue.getAssignee()).contains(userOf(assignee));
    assertThat(changedIssue.getRule()).isEqualTo(ruleOf(rule));
    assertThat(changedIssue.getProject()).isEqualTo(projectOf(project));
    assertThat(builder.getChange()).isEqualTo(new UserChange(issue.updateDate().getTime(), userOf(changeAuthor)));
  }

  @Test
  public void verify_notification_on_branch() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, t -> t.setBranchType(BRANCH));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.uuid()));
    IssueDto originalIssueDto = db.issues().insertIssue(rule, branch, file,
      t -> t.setSeverity(MAJOR));
    DefaultIssue issue = originalIssueDto.toDefaultIssue();
    UserDto changeAuthor = db.users().insertUser();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), changeAuthor.getUuid()).build();
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    IssuesChangesNotification issueChangeNotification = notificationArgumentCaptor.getValue();
    IssuesChangesNotificationBuilder builder = issuesChangesSerializer.from(issueChangeNotification);
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changedIssue = builder.getIssues().iterator().next();
    assertThat(changedIssue.getKey()).isEqualTo(issue.key());
    assertThat(changedIssue.getNewStatus()).isEqualTo(issue.status());
    assertThat(changedIssue.getAssignee()).isEmpty();
    assertThat(changedIssue.getRule()).isEqualTo(ruleOf(rule));
    assertThat(changedIssue.getProject()).isEqualTo(projectBranchOf(db, branch));
    assertThat(builder.getChange()).isEqualTo(new UserChange(issue.updateDate().getTime(), userOf(changeAuthor)));
  }

  @Test
  public void verify_no_notification_on_pr() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, t -> t.setBranchType(BranchType.PULL_REQUEST));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.uuid()));
    IssueDto originalIssueDto = db.issues().insertIssue(rule, branch, file, t -> t.setSeverity(MAJOR));
    DefaultIssue issue = originalIssueDto.toDefaultIssue();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), "user_uuid").build();
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    verifyNoInteractions(notificationManager);
  }

  @Test
  public void verify_notification_when_issue_is_linked_on_removed_rule() {
    RuleDto rule = db.rules().insertIssueRule(r -> r.setStatus(RuleStatus.REMOVED));
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto originalIssueDto = db.issues().insertIssue(rule, project, file, t -> t.setSeverity(MAJOR));
    DefaultIssue issue = originalIssueDto.toDefaultIssue();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), "user_uuid").build();
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    verifyNoInteractions(notificationManager);
  }

  @Test
  public void verify_notification_when_assignee_has_changed() {
    UserDto oldAssignee = db.users().insertUser();
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto originalIssueDto = db.issues().insertIssue(rule, project, file, t -> t.setAssigneeUuid(oldAssignee.getUuid()));
    DefaultIssue issue = originalIssueDto
      .toDefaultIssue();
    UserDto changeAuthor = db.users().insertUser();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), changeAuthor.getUuid()).build();
    UserDto newAssignee = db.users().insertUser();
    issueFieldsSetter.assign(issue, newAssignee, context);

    underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    IssuesChangesNotification issueChangeNotification = notificationArgumentCaptor.getValue();
    IssuesChangesNotificationBuilder builder = issuesChangesSerializer.from(issueChangeNotification);
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changedIssue = builder.getIssues().iterator().next();
    assertThat(changedIssue.getKey()).isEqualTo(issue.key());
    assertThat(changedIssue.getNewStatus()).isEqualTo(issue.status());
    assertThat(changedIssue.getAssignee()).contains(userOf(newAssignee));
    assertThat(changedIssue.getRule()).isEqualTo(ruleOf(rule));
    assertThat(changedIssue.getProject()).isEqualTo(projectOf(project));
    assertThat(builder.getChange()).isEqualTo(new UserChange(issue.updateDate().getTime(), userOf(changeAuthor)));
  }

  @Test
  public void saveIssue_populates_specified_SearchResponseData_with_rule_project_and_component_retrieved_from_DB() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto originalIssueDto = db.issues().insertIssue(rule, project, file);
    DefaultIssue issue = originalIssueDto.setSeverity(MAJOR).toDefaultIssue();
    UserDto changeAuthor = db.users().insertUser();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), changeAuthor.getUuid()).withRefreshMeasures().build();
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    SearchResponseData preloadedSearchResponseData = underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    assertThat(preloadedSearchResponseData.getIssues())
      .hasSize(1);
    assertThat(preloadedSearchResponseData.getIssues().iterator().next())
      .isNotSameAs(originalIssueDto);
    assertThat(preloadedSearchResponseData.getRules())
      .extracting(RuleDto::getKey)
      .containsOnly(rule.getKey());
    assertThat(preloadedSearchResponseData.getComponents())
      .extracting(ComponentDto::uuid)
      .containsOnly(project.uuid(), file.uuid());
    assertThat(issueChangePostProcessor.calledComponents()).containsExactlyInAnyOrder(file);
  }

  @Test
  public void saveIssue_populates_specified_SearchResponseData_with_no_rule_but_with_project_and_component_if_rule_is_removed() {
    RuleDto rule = db.rules().insertIssueRule(r -> r.setStatus(RuleStatus.REMOVED));
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto originalIssueDto = db.issues().insertIssue(rule, project, file);
    DefaultIssue issue = originalIssueDto.setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), "user_uuid").build();
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    SearchResponseData preloadedSearchResponseData = underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), originalIssueDto, issue, context);

    assertThat(preloadedSearchResponseData.getIssues())
      .hasSize(1);
    assertThat(preloadedSearchResponseData.getIssues().iterator().next())
      .isNotSameAs(originalIssueDto);
    assertThat(preloadedSearchResponseData.getRules()).isNullOrEmpty();
    assertThat(preloadedSearchResponseData.getComponents())
      .extracting(ComponentDto::uuid)
      .containsOnly(project.uuid(), file.uuid());
  }

}
