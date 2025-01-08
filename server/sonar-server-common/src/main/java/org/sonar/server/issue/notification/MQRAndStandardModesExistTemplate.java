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

import org.jetbrains.annotations.Nullable;
import org.sonar.api.notifications.Notification;

public class MQRAndStandardModesExistTemplate implements EmailTemplate {
  @Nullable
  @Override
  public EmailMessage format(Notification notification) {
    if (!MQRAndStandardModesExistNotification.TYPE.equals(notification.getType())) {
      return null;
    }

    boolean isMQREnabled = ((MQRAndStandardModesExistNotification) notification).isMQRModeEnabled();
    String message = """
      In this version of SonarQube, there are two options to reflect the health of all the projects: Multi-Quality Rule (MQR) Mode and Standard Experience.
      The SonarQube documentation explains more.

      Your instance is currently using the %s.

      To change it, go to Administration > Configuration > General Settings > Mode.
      """
      .formatted(isMQREnabled ? "Multi-Quality Rule (MQR) Mode" : "Standard Experience");

    // And finally return the email that will be sent
    return new EmailMessage()
      .setMessageId(MQRAndStandardModesExistNotification.TYPE)
      .setSubject("Your SonarQube instance is in %s"
        .formatted(isMQREnabled ? "Multi-Quality Rule (MQR) Mode" : "Standard Experience"))
      .setPlainTextMessage(message);
  }
}
