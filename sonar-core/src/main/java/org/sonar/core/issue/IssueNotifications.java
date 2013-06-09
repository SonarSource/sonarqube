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
package org.sonar.core.issue;

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
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.i18n.RuleI18nManager;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * Send notifications related to issues.
 *
 * @since 3.6
 */
public class IssueNotifications implements BatchComponent, ServerComponent {

  private final NotificationManager notificationsManager;
  private final RuleI18nManager ruleI18n;

  public IssueNotifications(NotificationManager notificationsManager, RuleI18nManager ruleI18n) {
    this.notificationsManager = notificationsManager;
    this.ruleI18n = ruleI18n;
  }

  public Notification sendNewIssues(Project project, int newIssues) {
    Notification notification = newNotification(project, "new-issues")
      .setDefaultMessage(newIssues + " new issues on " + project.getLongName() + ".")
      .setFieldValue("projectDate", DateUtils.formatDateTime(project.getAnalysisDate()))
      .setFieldValue("count", String.valueOf(newIssues));
    notificationsManager.scheduleForSending(notification);
    return notification;
  }

  @CheckForNull
  public Notification sendChanges(DefaultIssue issue, IssueChangeContext context, IssueQueryResult queryResult) {
    return sendChanges(issue, context, queryResult.rule(issue), queryResult.project(issue), queryResult.component(issue));
  }

  @CheckForNull
  public Notification sendChanges(DefaultIssue issue, IssueChangeContext context, Rule rule, Component project, @Nullable Component component) {
    Notification notification = createChangeNotification(issue, context, rule, project, component, null);
    if (notification != null) {
      notificationsManager.scheduleForSending(notification);
    }
    return notification;
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
  private Notification createChangeNotification(DefaultIssue issue, IssueChangeContext context, Rule rule, Component project, @Nullable Component component, @Nullable String comment) {
    FieldDiffs currentChange = issue.currentChange();
    if (comment == null && (currentChange == null || currentChange.diffs().isEmpty())) {
      return null;
    }
    Notification notification = newNotification(project, "issue-changes");
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
        notification.setFieldValue("old." + type, diff.oldValue() != null ? diff.oldValue().toString() : null);
        notification.setFieldValue("new." + type, diff.newValue() != null ? diff.newValue().toString() : null);
      }
    }
    return notification;
  }

  private String ruleName(@Nullable Rule rule) {
    // this code should definitely be shared in api
    if (rule == null) {
      return null;
    }
    String name = ruleI18n.getName(rule.getRepositoryKey(), rule.getKey(), Locale.ENGLISH);
    if (name == null) {
      name = rule.getName();
    }
    return name;
  }

  private Notification newNotification(Component project, String key) {
    return new Notification(key)
      .setFieldValue("projectName", project.longName())
      .setFieldValue("projectKey", project.key());
  }
}
