/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.List;
import org.sonar.api.notifications.Notification;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static org.sonar.server.qualityprofile.BuiltInQualityProfilesUpdateListener.BUILT_IN_QUALITY_PROFILES;

public class BuiltInQualityProfilesNotificationDispatcher extends NotificationDispatcher {

  public static final String KEY = "BuiltInQP";

  private final EmailNotificationChannel emailNotificationChannel;
  private final DbClient dbClient;

  public BuiltInQualityProfilesNotificationDispatcher(EmailNotificationChannel emailNotificationChannel, DbClient dbClient) {
    super(BUILT_IN_QUALITY_PROFILES);
    this.emailNotificationChannel = emailNotificationChannel;
    this.dbClient = dbClient;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    getRecipients().forEach(login -> context.addUser(login, emailNotificationChannel));
  }

  private List<String> getRecipients() {
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.authorizationDao().selectQualityProfileAdministratorLogins(session);
    }
  }
}
