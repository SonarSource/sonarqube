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

import java.util.Map;
import com.sonarsource.groupnotifications.api.model.GroupNotificationSubscription;

record GroupNotificationSubscriptionView(String groupUuid, String groupName, String notificationType, String channelKey) {

  static GroupNotificationSubscriptionView from(GroupNotificationSubscription subscription, Map<String, String> namesByUuid) {
    String groupUuid = subscription.groupUuid();
    return new GroupNotificationSubscriptionView(
      groupUuid,
      namesByUuid.getOrDefault(groupUuid, groupUuid),
      subscription.notificationType(),
      subscription.channelKey());
  }
}
