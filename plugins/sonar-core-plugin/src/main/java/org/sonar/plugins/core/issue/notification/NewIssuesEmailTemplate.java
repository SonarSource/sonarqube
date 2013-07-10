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
package org.sonar.plugins.core.issue.notification;

import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.DateUtils;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

/**
 * Creates email message for notification "new-issues".
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
      .setMessageId("new-issues/" + notification.getFieldValue("projectKey"))
      .setSubject("Project " + projectName + ", new issues")
      .setMessage(sb.toString());

    return message;
  }

  private void appendFooter(StringBuilder sb, Notification notification) {
    String projectKey = notification.getFieldValue("projectKey");
    String dateString = notification.getFieldValue("projectDate");
    if (projectKey != null && dateString != null) {
      Date date = DateUtils.parseDateTime(dateString);
      String url = String.format("%s/issues/search?componentRoots=%s&createdAfter=%s",
        settings.getServerBaseURL(), encode(projectKey), encode(DateUtils.formatDateTime(date)));
      sb.append("\n").append("See it in SonarQube: ").append(url).append("\n");
    }
  }

  public static String encode(String toEncode) {
    try {
      return URLEncoder.encode(toEncode, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

}
