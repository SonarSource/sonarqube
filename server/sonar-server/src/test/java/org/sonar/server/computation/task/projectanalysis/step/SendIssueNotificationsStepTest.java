/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.Component.Type;
import org.sonar.server.computation.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCache;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.issue.notification.NewIssuesStatistics;
import org.sonar.server.notification.NotificationService;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectBranch;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class SendIssueNotificationsStepTest extends BaseStepTest {

  private static final String BRANCH_NAME = "feature";

  private static final long ANALYSE_DATE = 123L;

  private static final Duration ISSUE_DURATION = Duration.create(100L);
  private static final String ISSUE_ASSIGNEE = "John";

  private static final Component FILE = builder(Component.Type.FILE, 11).build();
  private static final Component PROJECT = builder(Type.PROJECT, 1).addChildren(FILE).build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(PROJECT);

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setBranch(new DefaultBranchImpl())
    .setAnalysisDate(new Date(ANALYSE_DATE));

  @Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private NotificationService notificationService = mock(NotificationService.class);
  private NewIssuesNotificationFactory newIssuesNotificationFactory = mock(NewIssuesNotificationFactory.class);
  private NewIssuesNotification newIssuesNotificationMock = createNewIssuesNotificationMock();
  private MyNewIssuesNotification myNewIssuesNotificationMock = createMyNewIssuesNotificationMock();

  private IssueCache issueCache;
  private SendIssueNotificationsStep underTest;

  @Before
  public void setUp() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    underTest = new SendIssueNotificationsStep(issueCache, ruleRepository, treeRootHolder, notificationService, analysisMetadataHolder,
      newIssuesNotificationFactory);

    when(newIssuesNotificationFactory.newNewIssuesNotication()).thenReturn(newIssuesNotificationMock);
    when(newIssuesNotificationFactory.newMyNewIssuesNotification()).thenReturn(myNewIssuesNotificationMock);
  }

  @Test
  public void do_not_send_notifications_if_no_subscribers() {
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(false);

    underTest.execute();

    verify(notificationService, never()).deliver(any(Notification.class));
  }

  @Test
  public void send_global_new_issues_notification() throws Exception {
    issueCache.newAppender().append(
      new DefaultIssue().setSeverity(Severity.BLOCKER).setEffort(ISSUE_DURATION)
        .setCreationDate(new Date(ANALYSE_DATE)))
      .close();

    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService).deliver(any(NewIssuesNotification.class));
    verify(newIssuesNotificationMock).setProject(PROJECT.getPublicKey(), PROJECT.getUuid(), PROJECT.getName(), null);
    verify(newIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(newIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), any(NewIssuesStatistics.Stats.class));
    verify(newIssuesNotificationMock).setDebt(ISSUE_DURATION);
  }

  @Test
  public void send_global_new_issues_notification_on_branch() throws Exception {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = newProjectBranch(project, newBranchDto(project).setKey(BRANCH_NAME));
    ComponentDto file = newFileDto(branch);
    treeRootHolder.setRoot(builder(Type.PROJECT, 2).setKey(branch.getDbKey()).setPublicKey(branch.getKey()).setName(branch.longName()).setUuid(branch.uuid()).addChildren(
      builder(Component.Type.FILE, 11).setKey(file.getDbKey()).setPublicKey(file.getKey()).setName(file.longName()).build()).build());
    issueCache.newAppender().append(
      new DefaultIssue().setSeverity(Severity.BLOCKER).setEffort(ISSUE_DURATION)).close();

    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newBranch());

    underTest.execute();

    verify(notificationService).deliver(any(NewIssuesNotification.class));
    verify(newIssuesNotificationMock).setProject(branch.getKey(), branch.uuid(), branch.longName(), BRANCH_NAME);
    verify(newIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(newIssuesNotificationMock).setStatistics(eq(branch.longName()), any(NewIssuesStatistics.Stats.class));
    verify(newIssuesNotificationMock).setDebt(ISSUE_DURATION);
  }

  @Test
  public void send_new_issues_notification_to_user() throws Exception {
    issueCache.newAppender().append(
      new DefaultIssue().setSeverity(Severity.BLOCKER).setEffort(ISSUE_DURATION).setAssignee(ISSUE_ASSIGNEE)
        .setCreationDate(new Date(ANALYSE_DATE)))
      .close();

    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService, times(2)).deliver(any(Notification.class));
    verify(myNewIssuesNotificationMock).setAssignee(ISSUE_ASSIGNEE);
    verify(myNewIssuesNotificationMock).setProject(PROJECT.getPublicKey(), PROJECT.getUuid(), PROJECT.getName(), null);
    verify(myNewIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(myNewIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), any(NewIssuesStatistics.Stats.class));
    verify(myNewIssuesNotificationMock).setDebt(ISSUE_DURATION);
  }

  @Test
  public void send_issues_change_notification() throws Exception {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto()).setDbKey(PROJECT.getKey()).setLongName(PROJECT.getName());
    ComponentDto file = newFileDto(project).setDbKey(FILE.getKey()).setLongName(FILE.getName());
    RuleDefinitionDto ruleDefinitionDto = newRule();
    DefaultIssue issue = newIssue(ruleDefinitionDto, project, file).toDefaultIssue()
      .setNew(false).setChanged(true).setSendNotifications(true).setCreationDate(new Date(ANALYSE_DATE));
    ruleRepository.add(ruleDefinitionDto.getKey()).setName(ruleDefinitionDto.getName());
    issueCache.newAppender().append(issue).close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);
    verify(notificationService).deliver(issueChangeNotificationCaptor.capture());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("key")).isEqualTo(issue.key());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("assignee")).isEqualTo(issue.assignee());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("message")).isEqualTo(issue.message());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("ruleName")).isEqualTo(ruleDefinitionDto.getName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectName")).isEqualTo(project.longName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectKey")).isEqualTo(project.getKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("componentKey")).isEqualTo(file.getKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("componentName")).isEqualTo(file.longName());
  }

  @Test
  public void send_issues_change_notification_on_branch() throws Exception {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = newProjectBranch(project, newBranchDto(project).setKey(BRANCH_NAME));
    ComponentDto file = newFileDto(branch);
    treeRootHolder.setRoot(builder(Type.PROJECT, 2).setKey(branch.getDbKey()).setPublicKey(branch.getKey()).setName(branch.longName()).setUuid(branch.uuid()).addChildren(
      builder(Component.Type.FILE, 11).setKey(file.getDbKey()).setPublicKey(file.getKey()).setName(file.longName()).build()).build());
    RuleDefinitionDto ruleDefinitionDto = newRule();
    DefaultIssue issue = newIssue(ruleDefinitionDto, branch, file).toDefaultIssue()
      .setNew(false)
      .setChanged(true)
      .setSendNotifications(true);
    ruleRepository.add(ruleDefinitionDto.getKey()).setName(ruleDefinitionDto.getName());
    issueCache.newAppender().append(issue).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newBranch());

    underTest.execute();

    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);
    verify(notificationService).deliver(issueChangeNotificationCaptor.capture());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectName")).isEqualTo(branch.longName());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("projectKey")).isEqualTo(branch.getKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("branch")).isEqualTo(BRANCH_NAME);
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("componentKey")).isEqualTo(file.getKey());
    assertThat(issueChangeNotificationCaptor.getValue().getFieldValue("componentName")).isEqualTo(file.longName());
  }

  private NewIssuesNotification createNewIssuesNotificationMock() {
    NewIssuesNotification notification = mock(NewIssuesNotification.class);
    when(notification.setProject(anyString(), anyString(), anyString(), anyString())).thenReturn(notification);
    when(notification.setAnalysisDate(any(Date.class))).thenReturn(notification);
    when(notification.setStatistics(anyString(), any(NewIssuesStatistics.Stats.class))).thenReturn(notification);
    when(notification.setDebt(any(Duration.class))).thenReturn(notification);
    return notification;
  }

  private MyNewIssuesNotification createMyNewIssuesNotificationMock() {
    MyNewIssuesNotification notification = mock(MyNewIssuesNotification.class);
    when(notification.setAssignee(anyString())).thenReturn(notification);
    when(notification.setProject(anyString(), anyString(), anyString(), anyString())).thenReturn(notification);
    when(notification.setAnalysisDate(any(Date.class))).thenReturn(notification);
    when(notification.setStatistics(anyString(), any(NewIssuesStatistics.Stats.class))).thenReturn(notification);
    when(notification.setDebt(any(Duration.class))).thenReturn(notification);
    return notification;
  }

  private static Branch newBranch() {
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    when(branch.getName()).thenReturn(BRANCH_NAME);
    return branch;
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
