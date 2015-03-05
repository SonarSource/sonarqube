/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.notifications.NotificationService;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SendIssueNotificationsStepTest extends BaseStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  RuleCache ruleCache = mock(RuleCache.class);
  NotificationService notifService = mock(NotificationService.class);
  ComputationContext context = mock(ComputationContext.class, Mockito.RETURNS_DEEP_STUBS);
  IssueCache issueCache;
  NewIssuesNotificationFactory newIssuesNotificationFactory = mock(NewIssuesNotificationFactory.class, Mockito.RETURNS_DEEP_STUBS);
  SendIssueNotificationsStep sut;

  @Before
  public void setUp() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    sut = new SendIssueNotificationsStep(issueCache, ruleCache, notifService, newIssuesNotificationFactory);
  }

  @Test
  public void do_not_send_notifications_if_no_subscribers() throws Exception {
    when(context.getProject().uuid()).thenReturn("PROJECT_UUID");
    when(notifService.hasProjectSubscribersForTypes("PROJECT_UUID", SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(false);

    sut.execute(context);

    verify(notifService, never()).deliver(any(Notification.class));
  }

  @Test
  public void send_notifications_if_subscribers() throws Exception {
    issueCache.newAppender().append(new DefaultIssue()
        .setSeverity(Severity.BLOCKER)).close();

    when(context.getProject().uuid()).thenReturn("PROJECT_UUID");
    when(context.getReportMetadata()).thenReturn(BatchReport.Metadata.newBuilder().build());
    when(notifService.hasProjectSubscribersForTypes("PROJECT_UUID", SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    sut.execute(context);

    verify(notifService).deliver(any(NewIssuesNotification.class));
    verify(notifService, atLeastOnce()).deliver(any(IssueChangeNotification.class));
  }

  @Override
  protected ComputationStep step() throws IOException {
    return sut;
  }
}
