/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile;

import org.sonar.api.notifications.Notification;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

import static org.sonar.server.qualityprofile.BuiltInQualityProfilesNotification.BUILT_IN_QUALITY_PROFILES;

public class BuiltInQualityProfilesNotificationTemplate extends EmailTemplate {

  @Override
  public EmailMessage format(Notification notification) {
    if (!BUILT_IN_QUALITY_PROFILES.equals(notification.getType())) {
      return null;
    }
    // And finally return the email that will be sent
    return new EmailMessage()
      .setMessageId(BUILT_IN_QUALITY_PROFILES)
      .setSubject("empty")
      .setMessage("This is a test message from SonarQube");
  }

}
