/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.issue.notification;

import java.util.Date;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.issue.notification.NewIssuesStatistics.Metric;

/**
 * Creates email message for notification "my-new-issues".
 */
public class MyNewIssuesEmailTemplate extends AbstractNewIssuesEmailTemplate {

  public MyNewIssuesEmailTemplate(EmailSettings settings, I18n i18n) {
    super(settings, i18n);
  }

  @Override
  protected boolean shouldNotFormat(Notification notification) {
    return !MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE.equals(notification.getType());
  }

  @Override
  protected void appendAssignees(StringBuilder message, Notification notification) {
    // do nothing as we don't want to print assignees, it's a personalized email for one person
  }

  @Override
  protected String subject(Notification notification, String fullProjectName) {
    int issueCount = Integer.parseInt(notification.getFieldValue(Metric.RULE_TYPE + COUNT));
    return String.format("You have %s new issue%s on project %s",
      issueCount,
      issueCount > 1 ? "s" : "",
      fullProjectName);
  }

  @Override
  protected void appendFooter(StringBuilder message, Notification notification) {
    String projectKey = notification.getFieldValue(FIELD_PROJECT_KEY);
    String dateString = notification.getFieldValue(FIELD_PROJECT_DATE);
    String assignee = notification.getFieldValue(FIELD_ASSIGNEE);
    if (projectKey != null && dateString != null && assignee != null) {
      Date date = DateUtils.parseDateTime(dateString);
      String url = String.format("%s/project/issues?id=%s&assignees=%s",
        settings.getServerBaseURL(),
        encode(projectKey),
        encode(assignee));
      String branchName = notification.getFieldValue("branch");
      if (branchName != null) {
        url += "&branch=" + encode(branchName);
      }
      url += "&createdAt=" + encode(DateUtils.formatDateTime(date));
      message
        .append("More details at: ")
        .append(url)
        .append(NEW_LINE);
    }
  }
}
