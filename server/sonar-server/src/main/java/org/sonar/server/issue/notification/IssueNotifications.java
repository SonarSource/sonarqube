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
package org.sonar.server.issue.notification;

import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.server.notifications.NotificationService;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;

public class IssueNotifications implements ServerComponent {

  private final NotificationManager asyncService;
  private final NotificationService syncService;

  public IssueNotifications(NotificationManager asyncService, NotificationService syncService) {
    this.asyncService = asyncService;
    this.syncService = syncService;
  }

  @CheckForNull
  public Notification sendChanges(DefaultIssue issue, @Nullable String changeAuthorLogin,
    @Nullable String ruleName, Component project, @Nullable Component component,
    @Nullable String comment, boolean synchronous) {
    Notification notification = createChangeNotification(issue, changeAuthorLogin, ruleName, project, component, comment);
    if (notification != null) {
      send(notification, synchronous);
    }
    return notification;
  }

  public void send(Notification notification, boolean synchronous) {
    if (synchronous) {
      syncService.deliver(notification);
    } else {
      asyncService.scheduleForSending(notification);
    }
  }

  @CheckForNull
  private Notification createChangeNotification(DefaultIssue issue, @Nullable String changeAuthorLogin,
    @Nullable String ruleName, Component project,
    @Nullable Component component, @Nullable String comment) {
    Notification notification = null;
    if (comment != null || issue.mustSendNotifications()) {
      FieldDiffs currentChange = issue.currentChange();
      notification = new Notification("issue-changes")
        .setFieldValue("projectName", project.longName())
        .setFieldValue("projectKey", project.key())
        .setFieldValue("key", issue.key())
        .setFieldValue("changeAuthor", changeAuthorLogin)
        .setFieldValue("reporter", issue.reporter())
        .setFieldValue("assignee", issue.assignee())
        .setFieldValue("message", issue.message())
        .setFieldValue("ruleName", ruleName)
        .setFieldValue("componentKey", issue.componentKey());
      if (component != null) {
        notification.setFieldValue("componentName", component.longName());
      }
      if (comment != null) {
        notification.setFieldValue("comment", comment);
      }
      if (currentChange != null) {
        for (Map.Entry<String, FieldDiffs.Diff> entry : currentChange.diffs().entrySet()) {
          String type = entry.getKey();
          FieldDiffs.Diff diff = entry.getValue();
          Serializable newValue = diff.newValue();
          Serializable oldValue = diff.oldValue();
          notification.setFieldValue("old." + type, oldValue != null ? oldValue.toString() : null);
          notification.setFieldValue("new." + type, newValue != null ? newValue.toString() : null);
        }
      }
    }
    return notification;
  }

}
