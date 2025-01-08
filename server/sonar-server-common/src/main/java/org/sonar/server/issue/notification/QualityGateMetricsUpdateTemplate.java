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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.notifications.Notification;

public class QualityGateMetricsUpdateTemplate implements EmailTemplate {

  private static final String STANDARD_EXPERIENCE = "Standard Experience";
  private static final String MQR_MODE = "Multi-Quality Rule (MQR) Mode";

  @Nullable
  @Override
  public EmailMessage format(Notification notification) {
    if (!QualityGateMetricsUpdateNotification.TYPE.equals(notification.getType())) {
      return null;
    }

    String message = retrieveMessage((QualityGateMetricsUpdateNotification) notification);

    // And finally return the email that will be sent
    return new EmailMessage()
      .setMessageId(MQRAndStandardModesExistNotification.TYPE)
      .setSubject("Information about your SonarQube Quality Gate metrics")
      .setPlainTextMessage(message);
  }

  @NotNull
  private static String retrieveMessage(QualityGateMetricsUpdateNotification notification) {
    StringBuilder message = new StringBuilder();
    message.append("We are sending this message because this version of SonarQube is in ");
    message.append(notification.isMQRModeEnabled() ? MQR_MODE : STANDARD_EXPERIENCE);
    message.append(" and some of your quality gates conditions are using metrics from ");
    message.append(notification.isMQRModeEnabled() ? STANDARD_EXPERIENCE : MQR_MODE);
    message.append(".\n\nWe recommend you update them to ensure the most accurate categorization and ranking of your issues.\n\n");
    message.append("If you would like to update your quality gates, go to the Quality Gates page in the SonarQube UI and we will guide you through the process.");
    return message.toString();
  }
}
