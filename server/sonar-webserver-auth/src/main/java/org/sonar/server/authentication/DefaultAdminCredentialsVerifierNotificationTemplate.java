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
package org.sonar.server.authentication;

import javax.annotation.CheckForNull;
import org.sonar.api.notifications.Notification;
import org.sonar.server.issue.notification.EmailMessage;
import org.sonar.server.issue.notification.EmailTemplate;

public class DefaultAdminCredentialsVerifierNotificationTemplate implements EmailTemplate {

  static final String SUBJECT = "Default Administrator credentials are still used";
  static final String BODY_FORMAT = """
    Hello,

    Your SonarQube instance is still using default administrator credentials.
    Make sure to change the password for the 'admin' account or deactivate this account.""";

  @Override
  @CheckForNull
  public EmailMessage format(Notification notification) {
    if (!DefaultAdminCredentialsVerifierNotification.TYPE.equals(notification.getType())) {
      return null;
    }

    return new EmailMessage()
      .setMessageId(DefaultAdminCredentialsVerifierNotification.TYPE)
      .setSubject(SUBJECT)
      .setPlainTextMessage(BODY_FORMAT);
  }

}
