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
package org.sonar.core.issue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.DateUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Send notifications related to issues.
 *
 * @since 3.6
 */
public class IssueNotifications implements BatchComponent, ServerComponent {

  private final NotificationManager notificationsManager;

  public IssueNotifications(NotificationManager notificationsManager) {
    this.notificationsManager = notificationsManager;
  }

  public Notification sendNewIssues(Project project, IssuesBySeverity newIssues) {
    Notification notification = newNotification(project, "new-issues")
      .setDefaultMessage(newIssues.size() + " new issues on " + project.getLongName() + ".\n")
      .setFieldValue("projectDate", DateUtils.formatDateTime(project.getAnalysisDate()))
      .setFieldValue("count", String.valueOf(newIssues.size()));
    for (String severity : Severity.ALL) {
      notification.setFieldValue("count-" + severity, String.valueOf(newIssues.issues(severity)));
    }
    notificationsManager.scheduleForSending(notification);
    return notification;
  }

  @CheckForNull
  public List<Notification> sendChanges(DefaultIssue issue, IssueChangeContext context, IssueQueryResult queryResult) {
    Map<DefaultIssue, Rule> issues = Maps.newHashMap();
    issues.put(issue, queryResult.rule(issue));
    return sendChanges(issues, context, queryResult.project(issue), queryResult.component(issue));
  }

  @CheckForNull
  public List<Notification> sendChanges(Map<DefaultIssue, Rule> issues, IssueChangeContext context, Component project, @Nullable Component component) {
    List<Notification> notifications = Lists.newArrayList();
    for (Entry<DefaultIssue, Rule> entry : issues.entrySet()) {
      Notification notification = createChangeNotification(entry.getKey(), context, entry.getValue(), project, component, null);
      if (notification != null) {
        notifications.add(notification);
      }
    }
    notificationsManager.scheduleForSending(notifications);
    return notifications;
  }

  @CheckForNull
  public Notification sendChanges(DefaultIssue issue, IssueChangeContext context, IssueQueryResult queryResult, @Nullable String comment) {
    Notification notification = createChangeNotification(issue, context, queryResult.rule(issue), queryResult.project(issue), queryResult.component(issue), comment);
    if (notification != null) {
      notificationsManager.scheduleForSending(notification);
    }
    return notification;
  }

  @CheckForNull
  private Notification createChangeNotification(DefaultIssue issue, IssueChangeContext context, Rule rule, Component project,
                                                @Nullable Component component, @Nullable String comment) {
    Notification notification = null;
    if (comment != null || issue.mustSendNotifications()) {
      FieldDiffs currentChange = issue.currentChange();
      notification = newNotification(project, "issue-changes");
      notification.setFieldValue("key", issue.key());
      notification.setFieldValue("changeAuthor", context.login());
      notification.setFieldValue("reporter", issue.reporter());
      notification.setFieldValue("assignee", issue.assignee());
      notification.setFieldValue("message", issue.message());
      notification.setFieldValue("ruleName", ruleName(rule));
      notification.setFieldValue("componentKey", issue.componentKey());
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

  private String ruleName(@Nullable Rule rule) {
    // this code should definitely be shared in api
    if (rule == null) {
      return null;
    }
    return rule.getName();
  }

  private Notification newNotification(Component project, String key) {
    return new Notification(key)
      .setFieldValue("projectName", project.longName())
      .setFieldValue("projectKey", project.key());
  }

}
