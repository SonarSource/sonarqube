/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.notification.ws;

import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.server.notification.DbGroupNotificationMembershipProvider;
import org.sonar.server.notification.DbGroupNotificationSubscriptionsStore;
import org.sonar.server.notification.email.EmailNotificationChannel;
import com.sonarsource.groupnotifications.api.GroupNotificationSubscriptionsService;
import com.sonarsource.groupnotifications.api.model.GroupNotificationType;
import com.sonarsource.groupnotifications.shared.DefaultGroupNotificationSubscriptionsService;

final class GroupNotificationSubscriptionsTestSupport {

  private GroupNotificationSubscriptionsTestSupport() {
  }

  static GroupNotificationSubscriptionsService service(DbClient dbClient, List<String> availableTypes) {
    String channelKey = EmailNotificationChannel.class.getSimpleName();
    return new DefaultGroupNotificationSubscriptionsService(
      new DbGroupNotificationSubscriptionsStore(dbClient),
      new DbGroupNotificationMembershipProvider(dbClient),
      () -> availableTypes.stream().map(type -> new GroupNotificationType(type, channelKey)).toList());
  }
}
