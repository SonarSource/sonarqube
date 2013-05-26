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

import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

/**
 * Creates email message for notification "new-violations".
 * 
 * @since 2.10
 */
public class NewIssuesEmailTemplate extends EmailTemplate {

  private final EmailSettings settings;

  public NewIssuesEmailTemplate(EmailSettings settings) {
    this.settings = settings;
  }

  @Override
  public EmailMessage format(Notification notification) {
    if (!"new-issues".equals(notification.getType())) {
      return null;
    }
    String projectName = notification.getFieldValue("projectName");
    String violationsCount = notification.getFieldValue("count");

    StringBuilder sb = new StringBuilder();
    sb.append("Project: ").append(projectName).append('\n');
    sb.append(violationsCount).append(" new issues").append('\n');
    appendFooter(sb, notification);

    EmailMessage message = new EmailMessage()
        .setMessageId("new-issues/" + notification.getFieldValue("projectId"))
        .setSubject("New issues for project " + projectName)
        .setMessage(sb.toString());

    return message;
  }

  private void appendFooter(StringBuilder sb, Notification notification) {
    String projectKey = notification.getFieldValue("projectKey");
    sb.append("\n")
        .append("See it in Sonar: ").append(settings.getServerBaseURL()).append("/drilldown/measures/").append(projectKey)
        .append("?metric=new_violations\n");
  }

}
