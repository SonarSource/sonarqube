/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import javax.annotation.CheckForNull;
import org.sonar.api.notifications.Notification;
import org.sonar.api.platform.Server;
import org.sonar.core.i18n.I18n;
import org.sonar.server.issue.notification.FPOrAcceptedNotification.FpPrAccepted;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;

import static org.sonar.server.issue.notification.FPOrAcceptedNotification.FpPrAccepted.ACCEPTED;
import static org.sonar.server.issue.notification.FPOrAcceptedNotification.FpPrAccepted.FP;

/**
 * Creates email message for notification "issue-changes".
 */
public class FpOrAcceptedEmailTemplate extends IssueChangesEmailTemplate {

  private static final String NOTIFICATION_NAME_I18N_KEY = "notification.dispatcher.NewFalsePositiveIssue";

  public FpOrAcceptedEmailTemplate(I18n i18n, Server server) {
    super(i18n, server);
  }

  @Override
  @CheckForNull
  public EmailMessage format(Notification notif) {
    if (!(notif instanceof FPOrAcceptedNotification)) {
      return null;
    }

    FPOrAcceptedNotification notification = (FPOrAcceptedNotification) notif;

    EmailMessage emailMessage = new EmailMessage()
      .setMessageId(getMessageId(notification.getIssueStatusAfterUpdate()))
      .setSubject(buildSubject(notification))
      .setHtmlMessage(buildMessage(notification));
    if (notification.getChange() instanceof UserChange userChange) {
      User user = userChange.getUser();
      emailMessage.setFrom(user.getName().orElse(user.getLogin()));
    }
    return emailMessage;
  }

  private static String getMessageId(FpPrAccepted issueStatusAfterUpdate) {
    if (issueStatusAfterUpdate == ACCEPTED) {
      return "accepted-issue-changes";
    }
    if (issueStatusAfterUpdate == FP) {
      return "fp-issue-changes";
    }
    throw new IllegalArgumentException("Unsupported issue status after update " + issueStatusAfterUpdate);
  }

  private static String buildSubject(FPOrAcceptedNotification notification) {
    return "Issues marked as " + getIssueStatusLabel(notification.getIssueStatusAfterUpdate());
  }

  private String buildMessage(FPOrAcceptedNotification notification) {
    StringBuilder sb = new StringBuilder();
    paragraph(sb, s -> s.append("Hi,"));
    paragraph(sb, s -> s.append("A manual change has resolved ").append(notification.getChangedIssues().size() > 1 ? "issues" : "an issue")
      .append(" as ").append(getIssueStatusLabel(notification.getIssueStatusAfterUpdate())).append(":"));

    addIssuesByProjectThenRule(sb, notification.getChangedIssues());

    addFooter(sb, NOTIFICATION_NAME_I18N_KEY);

    return sb.toString();
  }

  private static String getIssueStatusLabel(FpPrAccepted issueStatusAfterUpdate) {
    if (issueStatusAfterUpdate == ACCEPTED) {
      return "Accepted";
    }
    if (issueStatusAfterUpdate == FP) {
      return "False Positive";
    }
    throw new IllegalArgumentException("Unsupported issue status after update " + issueStatusAfterUpdate);
  }

}
