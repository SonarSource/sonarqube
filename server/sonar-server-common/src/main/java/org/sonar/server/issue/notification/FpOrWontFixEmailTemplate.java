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
package org.sonar.server.issue.notification;

import javax.annotation.CheckForNull;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.core.i18n.I18n;
import org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;

import static org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix.FP;
import static org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix.WONT_FIX;

/**
 * Creates email message for notification "issue-changes".
 */
public class FpOrWontFixEmailTemplate extends IssueChangesEmailTemplate {

  private static final String NOTIFICATION_NAME_I18N_KEY = "notification.dispatcher.NewFalsePositiveIssue";

  public FpOrWontFixEmailTemplate(I18n i18n, EmailSettings settings) {
    super(i18n, settings);
  }

  @Override
  @CheckForNull
  public EmailMessage format(Notification notif) {
    if (!(notif instanceof FPOrWontFixNotification)) {
      return null;
    }

    FPOrWontFixNotification notification = (FPOrWontFixNotification) notif;

    EmailMessage emailMessage = new EmailMessage()
      .setMessageId(getMessageId(notification.getResolution()))
      .setSubject(buildSubject(notification))
      .setHtmlMessage(buildMessage(notification));
    if (notification.getChange() instanceof UserChange) {
      User user = ((UserChange) notification.getChange()).getUser();
      emailMessage.setFrom(user.getName().orElse(user.getLogin()));
    }
    return emailMessage;
  }

  private static String getMessageId(FpOrWontFix resolution) {
    if (resolution == WONT_FIX) {
      return "wontfix-issue-changes";
    }
    if (resolution == FP) {
      return "fp-issue-changes";
    }
    throw new IllegalArgumentException("Unsupported resolution " + resolution);
  }

  private static String buildSubject(FPOrWontFixNotification notification) {
    return "Issues marked as " + resolutionLabel(notification.getResolution());
  }

  private String buildMessage(FPOrWontFixNotification notification) {
    StringBuilder sb = new StringBuilder();
    paragraph(sb, s -> s.append("Hi,"));
    paragraph(sb, s -> s.append("A manual change has resolved ").append(notification.getChangedIssues().size() > 1 ? "issues" : "an issue")
      .append(" as ").append(resolutionLabel(notification.getResolution())).append(":"));

    addIssuesByProjectThenRule(sb, notification.getChangedIssues());

    addFooter(sb, NOTIFICATION_NAME_I18N_KEY);

    return sb.toString();
  }

  private static String resolutionLabel(FpOrWontFix resolution) {
    if (resolution == WONT_FIX) {
      return "Won't Fix";
    }
    if (resolution == FP) {
      return "False Positive";
    }
    throw new IllegalArgumentException("Unsupported resolution " + resolution);
  }

}
