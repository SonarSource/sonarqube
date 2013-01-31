/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.emailnotifications.templates.violations;

import org.sonar.api.notifications.Notification;
import org.sonar.api.config.EmailSettings;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

/**
 * Creates email message for notification "new-violations".
 * 
 * @since 2.10
 */
public class NewViolationsEmailTemplate extends EmailTemplate {

  private EmailSettings configuration;

  public NewViolationsEmailTemplate(EmailSettings configuration) {
    this.configuration = configuration;
  }

  @Override
  public EmailMessage format(Notification notification) {
    if (!"new-violations".equals(notification.getType())) {
      return null;
    }
    StringBuilder sb = new StringBuilder();

    String projectName = notification.getFieldValue("projectName");
    String violationsCount = notification.getFieldValue("count");
    String fromDate = notification.getFieldValue("fromDate");

    sb.append("Project: ").append(projectName).append('\n');
    sb.append(violationsCount).append(" new violations introduced since ").append(fromDate).append('\n');
    appendFooter(sb, notification);

    EmailMessage message = new EmailMessage()
        .setMessageId("new-violations/" + notification.getFieldValue("projectId"))
        .setSubject("New violations for project " + projectName)
        .setMessage(sb.toString());

    return message;
  }

  private void appendFooter(StringBuilder sb, Notification notification) {
    String projectKey = notification.getFieldValue("projectKey");
    sb.append("\n")
        .append("See it in Sonar: ").append(configuration.getServerBaseURL()).append("/drilldown/measures/").append(projectKey)
        .append("?metric=new_violations&period=1\n");
  }

}
