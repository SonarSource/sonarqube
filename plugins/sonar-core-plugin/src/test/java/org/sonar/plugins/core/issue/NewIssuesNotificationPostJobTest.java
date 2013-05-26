/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.issue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.DefaultIssue;

import java.util.Arrays;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NewIssuesNotificationPostJobTest {
  @Mock
  Project project;

  @Mock
  IssueCache issueCache;

  @Mock
  NotificationManager notifications;

  @Mock
  SensorContext sensorContext;

  @Test
  public void should_not_send_notif_if_past_scan() throws Exception {
    when(project.isLatestAnalysis()).thenReturn(false);

    NewIssuesNotificationPostJob job = new NewIssuesNotificationPostJob(issueCache, notifications);
    job.executeOn(project, sensorContext);

    verifyZeroInteractions(notifications, issueCache, sensorContext);
  }

  @Test
  public void should_send_notif_if_new_issues() throws Exception {
    when(project.isLatestAnalysis()).thenReturn(true);
    when(issueCache.all()).thenReturn(Arrays.asList(
      new DefaultIssue().setNew(true),
      new DefaultIssue().setNew(false)
    ));

    NewIssuesNotificationPostJob job = new NewIssuesNotificationPostJob(issueCache, notifications);
    job.executeOn(project, sensorContext);

    verify(notifications).scheduleForSending(argThat(new ArgumentMatcher<Notification>() {
      @Override
      public boolean matches(Object o) {
        Notification n = (Notification) o;
        return n.getType().equals("new-issues") && n.getFieldValue("count").equals("1");
      }
    }));
  }

  @Test
  public void should_not_send_notif_if_no_new_issues() throws Exception {
    when(project.isLatestAnalysis()).thenReturn(true);
    when(issueCache.all()).thenReturn(Arrays.asList(
      new DefaultIssue().setNew(false)
    ));

    NewIssuesNotificationPostJob job = new NewIssuesNotificationPostJob(issueCache, notifications);
    job.executeOn(project, sensorContext);

    verifyZeroInteractions(notifications);
  }
}
