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
package org.sonar.ce.task.projectanalysis.notification;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.annotation.CheckForNull;
import org.sonar.api.notifications.Notification;
import org.sonar.api.platform.Server;
import org.sonar.server.issue.notification.EmailMessage;
import org.sonar.server.issue.notification.EmailTemplate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.api.utils.DateUtils.formatDateTime;

public class ReportAnalysisFailureNotificationEmailTemplate implements EmailTemplate {
  private static final char LINE_RETURN = '\n';
  private static final char TAB = '\t';

  private final ReportAnalysisFailureNotificationSerializer serializer;
  private final Server server;

  public ReportAnalysisFailureNotificationEmailTemplate(ReportAnalysisFailureNotificationSerializer serializer, Server server) {
    this.serializer = serializer;
    this.server = server;
  }

  @Override
  @CheckForNull
  public EmailMessage format(Notification notification) {
    if (!(notification instanceof ReportAnalysisFailureNotification)) {
      return null;
    }

    ReportAnalysisFailureNotificationBuilder taskFailureNotification = serializer.fromNotification((ReportAnalysisFailureNotification) notification);
    String projectUuid = taskFailureNotification.project().uuid();
    String projectFullName = computeProjectFullName(taskFailureNotification.project());

    return new EmailMessage()
      .setMessageId(notification.getType() + "/" + projectUuid)
      .setSubject(subject(projectFullName))
      .setPlainTextMessage(message(projectFullName, taskFailureNotification));
  }

  private static String computeProjectFullName(ReportAnalysisFailureNotificationBuilder.Project project) {
    String branchName = project.branchName();
    if (branchName != null) {
      return String.format("%s (%s)", project.name(), branchName);
    }
    return project.name();
  }

  private static String subject(String projectFullName) {
    return String.format("%s: Background task in failure", projectFullName);
  }

  private String message(String projectFullName, ReportAnalysisFailureNotificationBuilder taskFailureNotification) {
    ReportAnalysisFailureNotificationBuilder.Project project = taskFailureNotification.project();
    ReportAnalysisFailureNotificationBuilder.Task task = taskFailureNotification.task();

    StringBuilder res = new StringBuilder();
    res.append("Project:").append(TAB).append(projectFullName).append(LINE_RETURN);
    res.append("Background task:").append(TAB).append(task.uuid()).append(LINE_RETURN);
    res.append("Submission time:").append(TAB).append(formatDateTime(task.createdAt())).append(LINE_RETURN);
    res.append("Failure time:").append(TAB).append(formatDateTime(task.failedAt())).append(LINE_RETURN);

    String errorMessage = taskFailureNotification.errorMessage();
    if (errorMessage != null) {
      res.append(LINE_RETURN);
      res.append("Error message:").append(TAB).append(errorMessage).append(LINE_RETURN);
    }

    res.append(LINE_RETURN);
    res.append("More details at: ").append(String.format("%s/project/background_tasks?id=%s", server.getPublicRootUrl(), encode(project.key())));

    return res.toString();
  }

  private static String encode(String toEncode) {
    try {
      return URLEncoder.encode(toEncode, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }
}
