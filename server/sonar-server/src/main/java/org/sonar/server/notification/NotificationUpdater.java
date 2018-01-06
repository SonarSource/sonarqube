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
package org.sonar.server.notification;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkArgument;

public class NotificationUpdater {
  private static final String PROP_NOTIFICATION_PREFIX = "notification";
  private static final String PROP_NOTIFICATION_VALUE = "true";

  private final DbClient dbClient;

  public NotificationUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Add a notification to a user.
   */
  public void add(DbSession dbSession, String channel, String dispatcher, UserDto user, @Nullable ComponentDto project) {
    String key = String.join(".", PROP_NOTIFICATION_PREFIX, dispatcher, channel);
    Long projectId = project == null ? null : project.getId();

    List<PropertyDto> existingNotification = dbClient.propertiesDao().selectByQuery(
      PropertyQuery.builder()
        .setKey(key)
        .setComponentId(projectId)
        .setUserId(user.getId())
        .build(),
      dbSession).stream()
      .filter(notificationScope(project))
      .collect(MoreCollectors.toList());
    checkArgument(existingNotification.isEmpty()
      || !PROP_NOTIFICATION_VALUE.equals(existingNotification.get(0).getValue()), "Notification already added");

    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey(key)
      .setUserId(user.getId())
      .setValue(PROP_NOTIFICATION_VALUE)
      .setResourceId(projectId));
  }

  /**
   * Remove a notification from a user.
   */
  public void remove(DbSession dbSession, String channel, String dispatcher, UserDto user, @Nullable ComponentDto project) {
    String key = String.join(".", PROP_NOTIFICATION_PREFIX, dispatcher, channel);
    Long projectId = project == null ? null : project.getId();

    List<PropertyDto> existingNotification = dbClient.propertiesDao().selectByQuery(
      PropertyQuery.builder()
        .setKey(key)
        .setComponentId(projectId)
        .setUserId(user.getId())
        .build(),
      dbSession).stream()
      .filter(notificationScope(project))
      .collect(MoreCollectors.toList());
    checkArgument(!existingNotification.isEmpty() && PROP_NOTIFICATION_VALUE.equals(existingNotification.get(0).getValue()), "Notification doesn't exist");

    dbClient.propertiesDao().delete(dbSession, new PropertyDto()
      .setKey(key)
      .setUserId(user.getId())
      .setValue(PROP_NOTIFICATION_VALUE)
      .setResourceId(projectId));
  }

  private static Predicate<PropertyDto> notificationScope(@Nullable ComponentDto project) {
    return prop -> project == null ? (prop.getResourceId() == null) : (prop.getResourceId() != null);
  }
}
