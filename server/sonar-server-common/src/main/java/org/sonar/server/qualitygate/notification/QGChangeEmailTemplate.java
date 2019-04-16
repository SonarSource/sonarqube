/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualitygate.notification;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.measures.Metric;
import org.sonar.api.notifications.Notification;
import org.sonar.server.issue.notification.EmailMessage;
import org.sonar.server.issue.notification.EmailTemplate;

/**
 * Creates email message for notification "alerts".
 *
 * @since 3.5
 */
public class QGChangeEmailTemplate implements EmailTemplate {

  private EmailSettings configuration;

  public QGChangeEmailTemplate(EmailSettings configuration) {
    this.configuration = configuration;
  }

  @Override
  @CheckForNull
  public EmailMessage format(Notification notification) {
    if (!"alerts".equals(notification.getType())) {
      return null;
    }

    // Retrieve useful values
    String projectId = notification.getFieldValue("projectId");
    String projectKey = notification.getFieldValue("projectKey");
    String projectName = notification.getFieldValue("projectName");
    String projectVersion = notification.getFieldValue("projectVersion");
    String branchName = notification.getFieldValue("branch");
    String alertName = notification.getFieldValue("alertName");
    String alertText = notification.getFieldValue("alertText");
    String alertLevel = notification.getFieldValue("alertLevel");
    boolean isNewAlert = Boolean.parseBoolean(notification.getFieldValue("isNewAlert"));
    String fullProjectName = computeFullProjectName(projectName, branchName);

    // Generate text
    String subject = generateSubject(fullProjectName, alertLevel, isNewAlert);
    String messageBody = generateMessageBody(projectName, projectKey, projectVersion, branchName, alertName, alertText, isNewAlert);

    // And finally return the email that will be sent
    return new EmailMessage()
      .setMessageId("alerts/" + projectId)
      .setSubject(subject)
      .setPlainTextMessage(messageBody);
  }

  private static String computeFullProjectName(String projectName, @Nullable String branchName) {
    if (branchName == null || branchName.isEmpty()) {
      return projectName;
    }
    return String.format("%s (%s)", projectName, branchName);
  }

  private static String generateSubject(String fullProjectName, String alertLevel, boolean isNewAlert) {
    StringBuilder subjectBuilder = new StringBuilder();
    if (Metric.Level.OK.toString().equals(alertLevel)) {
      subjectBuilder.append("\"").append(fullProjectName).append("\" is back to green");
    } else if (isNewAlert) {
      subjectBuilder.append("New quality gate threshold reached on \"").append(fullProjectName).append("\"");
    } else {
      subjectBuilder.append("Quality gate status changed on \"").append(fullProjectName).append("\"");
    }
    return subjectBuilder.toString();
  }

  private String generateMessageBody(String projectName, String projectKey,
    @Nullable String projectVersion, @Nullable String branchName,
    String alertName, String alertText, boolean isNewAlert) {
    StringBuilder messageBody = new StringBuilder();
    messageBody.append("Project: ").append(projectName).append("\n");
    if (branchName != null) {
      messageBody.append("Branch: ").append(branchName).append("\n");
    }
    if (projectVersion != null) {
      messageBody.append("Version: ").append(projectVersion).append("\n");
    }
    messageBody.append("Quality gate status: ").append(alertName).append("\n\n");

    String[] alerts = StringUtils.split(alertText, ",");
    if (alerts.length > 0) {
      if (isNewAlert) {
        messageBody.append("New quality gate threshold");
      } else {
        messageBody.append("Quality gate threshold");
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

    messageBody.append("\n").append("More details at: ").append(configuration.getServerBaseURL()).append("/dashboard?id=").append(projectKey);
    if (branchName != null) {
      messageBody.append("&branch=").append(branchName);
    }

    return messageBody.toString();
  }

}
