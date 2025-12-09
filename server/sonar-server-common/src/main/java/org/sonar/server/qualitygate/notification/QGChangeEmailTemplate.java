/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.notifications.Notification;
import org.sonar.api.platform.Server;
import org.sonar.server.issue.notification.EmailMessage;
import org.sonar.server.issue.notification.EmailTemplate;
import org.sonar.server.measure.Rating;

/**
 * Creates email message for notification "alerts".
 *
 * @since 3.5
 */
public class QGChangeEmailTemplate implements EmailTemplate {

  private final Server server;

  public QGChangeEmailTemplate( Server server) {
    this.server = server;
  }

  @Override
  @CheckForNull
  public EmailMessage format(Notification notification) {
    if (!"alerts".equals(notification.getType())) {
      return null;
    }

    // Retrieve useful values
    String projectId = notification.getFieldValue(QGChangeNotification.FIELD_PROJECT_ID);
    String projectKey = notification.getFieldValue(QGChangeNotification.FIELD_PROJECT_KEY);
    String projectName = notification.getFieldValue(QGChangeNotification.FIELD_PROJECT_NAME);
    String projectVersion = notification.getFieldValue(QGChangeNotification.FIELD_PROJECT_VERSION);
    String branchName = Boolean.parseBoolean(notification.getFieldValue(QGChangeNotification.FIELD_IS_MAIN_BRANCH)) ? null :
      notification.getFieldValue(QGChangeNotification.FIELD_BRANCH);
    String alertName = notification.getFieldValue(QGChangeNotification.FIELD_ALERT_NAME);
    String alertText = notification.getFieldValue(QGChangeNotification.FIELD_ALERT_TEXT);
    String alertLevel = notification.getFieldValue(QGChangeNotification.FIELD_ALERT_LEVEL);
    String ratingMetricsInOneString = notification.getFieldValue(QGChangeNotification.FIELD_RATING_METRICS);
    boolean isNewAlert = Boolean.parseBoolean(notification.getFieldValue(QGChangeNotification.FIELD_IS_NEW_ALERT));
    String fullProjectName = computeFullProjectName(projectName, branchName);

    // Generate text
    String subject = generateSubject(fullProjectName, alertLevel, isNewAlert);
    String messageBody = generateMessageBody(projectName, projectKey, projectVersion, branchName, alertName, alertText, isNewAlert, ratingMetricsInOneString);

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
    String alertName, String alertText, boolean isNewAlert, String ratingMetricsInOneString) {
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
        messageBody.append(": ").append(formatRating(alerts[0].trim(), ratingMetricsInOneString)).append("\n");
      } else {
        messageBody.append("s:\n");
        for (String alert : alerts) {
          messageBody.append("  - ").append(formatRating(alert.trim(), ratingMetricsInOneString)).append("\n");
        }
      }
    }

    messageBody.append("\n").append("More details at: ").append(server.getPublicRootUrl()).append("/dashboard?id=").append(projectKey);
    if (branchName != null) {
      messageBody.append("&branch=").append(branchName);
    }

    return messageBody.toString();
  }

  /**
   * Converts the ratings from digits to a rating letter {@see org.sonar.server.measure.Rating}, based on the
   * raw text passed to this template.
   *
   * Examples:
   * Reliability rating > 4 will be converted to Reliability rating worse than D
   * Security rating on New Code > 1 will be converted to Security rating on New Code worse than A
   * Code Coverage < 50% will not be converted and returned as is.
   *
   * @param alert
   * @param ratingMetricsInOneString
   * @return full raw alert with converted ratings
   */
  private static String formatRating(String alert, String ratingMetricsInOneString) {
    Optional<String> ratingToFormat = Optional.empty();
    for(String rating : ratingMetricsInOneString.split(",")) {
      if (alert.matches(rating + " > \\d$")) {
        ratingToFormat = Optional.of(rating);
        break;
      }
    }
    if(!ratingToFormat.isPresent()){
      return alert;
    }

    StringBuilder builder = new StringBuilder(ratingToFormat.get());
    builder.append(" worse than ");
    String rating = alert.substring(alert.length() - 1);
    builder.append(Rating.valueOf(Integer.parseInt(rating)).name());
    return builder.toString();
  }

}
