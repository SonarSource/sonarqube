/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.emailnotifications.templates.alerts;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.measures.Metric;
import org.sonar.api.notifications.Notification;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

/**
 * Creates email message for notification "alerts".
 * 
 * @since 3.5
 */
public class AlertsEmailTemplate extends EmailTemplate {

  private EmailSettings configuration;

  public AlertsEmailTemplate(EmailSettings configuration) {
    this.configuration = configuration;
  }

  @Override
  public EmailMessage format(Notification notification) {
    if (!"alerts".equals(notification.getType())) {
      return null;
    }

    // Retrieve useful values
    String projectId = notification.getFieldValue("projectId");
    String projectKey = notification.getFieldValue("projectKey");
    String projectName = notification.getFieldValue("projectName");
    String alertName = notification.getFieldValue("alertName");
    String alertText = notification.getFieldValue("alertText");
    String alertLevel = notification.getFieldValue("alertLevel");
    boolean isNewAlert = Boolean.valueOf(notification.getFieldValue("isNewAlert"));

    // Generate text
    String subject = generateSubject(projectName, alertLevel, isNewAlert);
    String messageBody = generateMessageBody(projectName, projectKey, alertName, alertText, isNewAlert);

    // And finally return the email that will be sent
    return new EmailMessage()
        .setMessageId("alerts/" + projectId)
        .setSubject(subject)
        .setMessage(messageBody);
  }

  private String generateSubject(String projectName, String alertLevel, boolean isNewAlert) {
    StringBuilder subjectBuilder = new StringBuilder();
    if (Metric.Level.OK.toString().equals(alertLevel)) {
      subjectBuilder.append("\"").append(projectName).append("\" is back to green");
    } else if (isNewAlert) {
      subjectBuilder.append("New alert on \"").append(projectName).append("\"");
    } else {
      subjectBuilder.append("Alert level changed on \"").append(projectName).append("\"");
    }
    return subjectBuilder.toString();
  }

  private String generateMessageBody(String projectName, String projectKey, String alertName, String alertText, boolean isNewAlert) {
    StringBuilder messageBody = new StringBuilder();
    messageBody.append("Project: ").append(projectName).append("\n");
    messageBody.append("Alert level: ").append(alertName).append("\n\n");

    String[] alerts = StringUtils.split(alertText, ",");
    if (alerts.length > 0) {
      if (isNewAlert) {
        messageBody.append("New alert");
      } else {
        messageBody.append("Alert");
      }
      if (alerts.length == 1) {
        messageBody.append(": ").append(alerts[0].trim()).append("\n");
      } else {
        messageBody.append("s:\n");
        for (String alert : alerts) {
          messageBody.append("  - ").append(alert.trim()).append("\n");
        }
      }
    }

    messageBody.append("\n").append("See it in Sonar: ").append(configuration.getServerBaseURL()).append("/dashboard/index/").append(projectKey);

    return messageBody.toString();
  }

}
