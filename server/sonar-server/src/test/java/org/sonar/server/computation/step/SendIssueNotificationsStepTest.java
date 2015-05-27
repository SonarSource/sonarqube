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
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.notifications.NotificationService;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendIssueNotificationsStepTest extends BaseStepTest {

  private static final String PROJECT_UUID = "PROJECT_UUID";
  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  RuleCache ruleCache = mock(RuleCache.class);
  NotificationService notifService = mock(NotificationService.class);
  IssueCache issueCache;
  DbComponentsRefCache dbComponentsRefCache;
  NewIssuesNotificationFactory newIssuesNotificationFactory = mock(NewIssuesNotificationFactory.class, Mockito.RETURNS_DEEP_STUBS);
  Settings projectSettings = new Settings();
  SendIssueNotificationsStep sut;

  File reportDir;

  @Before
  public void setUp() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    dbComponentsRefCache = new DbComponentsRefCache();
    sut = new SendIssueNotificationsStep(issueCache, ruleCache, dbComponentsRefCache, notifService, newIssuesNotificationFactory);

    dbComponentsRefCache.addComponent(1, new DbComponentsRefCache.DbComponent(1L, PROJECT_KEY, PROJECT_UUID));

    reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project name")
      .build());
  }

  @Test
  public void do_not_send_notifications_if_no_subscribers() throws IOException {
    when(notifService.hasProjectSubscribersForTypes(PROJECT_UUID, SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(false);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
        mock(DbClient.class), ComponentTreeBuilders.from(new DumbComponent(Component.Type.PROJECT, 1, null, null)), mock(LanguageRepository.class)));

    verify(notifService, never()).deliver(any(Notification.class));
  }

  @Test
  public void send_notifications_if_subscribers() {
    issueCache.newAppender().append(new DefaultIssue()
        .setSeverity(Severity.BLOCKER)).close();

    when(notifService.hasProjectSubscribersForTypes(PROJECT_UUID, SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
        mock(DbClient.class), ComponentTreeBuilders.from(new DumbComponent(Component.Type.PROJECT, 1, null, null)), mock(LanguageRepository.class)));

    verify(notifService).deliver(any(NewIssuesNotification.class));
    verify(notifService, atLeastOnce()).deliver(any(IssueChangeNotification.class));
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}
