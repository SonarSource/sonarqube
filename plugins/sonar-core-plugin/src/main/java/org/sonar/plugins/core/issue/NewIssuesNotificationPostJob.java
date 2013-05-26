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

import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.DefaultIssue;

/**
 * @since 3.6
 */
public class NewIssuesNotificationPostJob implements PostJob {

  private final IssueCache issueCache;
  private final NotificationManager notifications;

  public NewIssuesNotificationPostJob(IssueCache issueCache, NotificationManager notifications) {
    this.issueCache = issueCache;
    this.notifications = notifications;
  }

  @Override
  public void executeOn(Project project, SensorContext context) {
    if (project.isLatestAnalysis()) {
      sendNotifications(project);
    }
  }

  private void sendNotifications(Project project) {
    int newIssues = 0;
    for (DefaultIssue issue : issueCache.all()) {
      if (issue.isNew()) {
        newIssues++;
      }
    }
    if (newIssues > 0) {
      Notification notification = new Notification("new-issues")
        .setDefaultMessage(newIssues + " new issues on " + project.getLongName() + ".")
        .setFieldValue("count", String.valueOf(newIssues))
        .setFieldValue("projectName", project.getLongName())
        .setFieldValue("projectKey", project.getKey())
        .setFieldValue("projectId", String.valueOf(project.getId()));
      notifications.scheduleForSending(notification);
    }
  }

}
