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
package org.sonar.server.computation.step;

import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.Component.Type;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.RuleRepository;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.issue.notification.NewIssuesStatistics;
import org.sonar.server.notification.NotificationService;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class SendIssueNotificationsStepTest extends BaseStepTest {

  static final String PROJECT_UUID = "PROJECT_UUID";
  static final String PROJECT_KEY = "PROJECT_KEY";
  static final String PROJECT_NAME = "PROJECT_NAME";

  static final long ANALYSE_DATE = 123L;

  static final Duration ISSUE_DURATION = Duration.create(100L);
  static final String ISSUE_ASSIGNEE = "John";

  static final Component PROJECT = builder(Type.PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).setName(PROJECT_NAME).build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(PROJECT);

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setAnalysisDate(new Date(ANALYSE_DATE));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  NotificationService notificationService = mock(NotificationService.class);
  NewIssuesNotificationFactory newIssuesNotificationFactory = mock(NewIssuesNotificationFactory.class);
  NewIssuesNotification newIssuesNotificationMock = createNewIssuesNotificationMock();
  MyNewIssuesNotification myNewIssuesNotificationMock = createMyNewIssuesNotificationMock();

  IssueCache issueCache;
  SendIssueNotificationsStep underTest;

  @Before
  public void setUp() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    underTest = new SendIssueNotificationsStep(issueCache, mock(RuleRepository.class), treeRootHolder, notificationService, analysisMetadataHolder,
      newIssuesNotificationFactory);

    when(newIssuesNotificationFactory.newNewIssuesNotication()).thenReturn(newIssuesNotificationMock);
    when(newIssuesNotificationFactory.newMyNewIssuesNotification()).thenReturn(myNewIssuesNotificationMock);
  }

  @Test
  public void do_not_send_notifications_if_no_subscribers() {
    when(notificationService.hasProjectSubscribersForTypes(PROJECT_UUID, SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(false);

    underTest.execute();

    verify(notificationService, never()).deliver(any(Notification.class));
  }

  @Test
  public void send_global_new_issues_notification() throws Exception {
    issueCache.newAppender().append(
      new DefaultIssue().setSeverity(Severity.BLOCKER).setEffort(ISSUE_DURATION)
      ).close();

    when(notificationService.hasProjectSubscribersForTypes(PROJECT_UUID, SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService).deliver(any(NewIssuesNotification.class));
    verify(newIssuesNotificationMock).setProject(PROJECT_KEY, PROJECT_UUID, PROJECT_NAME);
    verify(newIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(newIssuesNotificationMock).setStatistics(eq(PROJECT_NAME), any(NewIssuesStatistics.Stats.class));
    verify(newIssuesNotificationMock).setDebt(ISSUE_DURATION);
  }

  @Test
  public void send_new_issues_notification_to_user() throws Exception {
    issueCache.newAppender().append(
      new DefaultIssue().setSeverity(Severity.BLOCKER).setEffort(ISSUE_DURATION).setAssignee(ISSUE_ASSIGNEE)
      ).close();

    when(notificationService.hasProjectSubscribersForTypes(PROJECT_UUID, SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService, times(2)).deliver(any(Notification.class));
    verify(myNewIssuesNotificationMock).setAssignee(ISSUE_ASSIGNEE);
    verify(myNewIssuesNotificationMock).setProject(PROJECT_KEY, PROJECT_UUID, PROJECT_NAME);
    verify(myNewIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(myNewIssuesNotificationMock).setStatistics(eq(PROJECT_NAME), any(NewIssuesStatistics.Stats.class));
    verify(myNewIssuesNotificationMock).setDebt(ISSUE_DURATION);
  }

  @Test
  public void send_issues_change_notification() throws Exception {
    DefaultIssue issue = new DefaultIssue().setSeverity(Severity.BLOCKER).setEffort(ISSUE_DURATION).setChanged(true).setSendNotifications(true);
    issueCache.newAppender().append(issue).close();

    when(notificationService.hasProjectSubscribersForTypes(PROJECT_UUID, SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService).deliver(any(IssueChangeNotification.class));
  }

  private NewIssuesNotification createNewIssuesNotificationMock() {
    NewIssuesNotification notification = mock(NewIssuesNotification.class);
    when(notification.setProject(anyString(), anyString(), anyString())).thenReturn(notification);
    when(notification.setAnalysisDate(any(Date.class))).thenReturn(notification);
    when(notification.setStatistics(anyString(), any(NewIssuesStatistics.Stats.class))).thenReturn(notification);
    when(notification.setDebt(any(Duration.class))).thenReturn(notification);
    return notification;
  }

  private MyNewIssuesNotification createMyNewIssuesNotificationMock() {
    MyNewIssuesNotification notification = mock(MyNewIssuesNotification.class);
    when(notification.setAssignee(anyString())).thenReturn(notification);
    when(notification.setProject(anyString(), anyString(), anyString())).thenReturn(notification);
    when(notification.setAnalysisDate(any(Date.class))).thenReturn(notification);
    when(notification.setStatistics(anyString(), any(NewIssuesStatistics.Stats.class))).thenReturn(notification);
    when(notification.setDebt(any(Duration.class))).thenReturn(notification);
    return notification;
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
