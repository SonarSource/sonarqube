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
package org.sonar.server.notification;

import java.util.List;
import java.util.stream.Stream;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.ws.Dispatchers;
import com.sonarsource.groupnotifications.api.GroupNotificationTypeProvider;
import com.sonarsource.groupnotifications.api.model.GroupNotificationType;

/** Adapts Server's registered group-subscription dispatchers to the shared type provider. */
public class DispatchersGroupNotificationTypeProvider implements GroupNotificationTypeProvider {

  private static final String SECURITY_ALERT_RAISED = "security-alert-raised";
  private static final String BANNER_CHANNEL = "BannerChannel";

  private final Dispatchers dispatchers;

  public DispatchersGroupNotificationTypeProvider(Dispatchers dispatchers) {
    this.dispatchers = dispatchers;
  }

  @Override
  public List<GroupNotificationType> selectAvailableTypes() {
    String emailChannel = EmailNotificationChannel.class.getSimpleName();
    List<String> dispatchableTypes = dispatchers.getGroupSubscriptionDispatchers();
    List<GroupNotificationType> emailTypes = dispatchableTypes.stream()
      .map(type -> new GroupNotificationType(type, emailChannel))
      .toList();
    return dispatchableTypes.contains(SECURITY_ALERT_RAISED)
      ? Stream.concat(emailTypes.stream(), Stream.of(new GroupNotificationType(SECURITY_ALERT_RAISED, BANNER_CHANNEL))).toList()
      : emailTypes;
  }
}
