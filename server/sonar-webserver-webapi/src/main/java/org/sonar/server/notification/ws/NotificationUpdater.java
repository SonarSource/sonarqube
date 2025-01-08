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
package org.sonar.server.notification.ws;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkArgument;

public class NotificationUpdater {
  private static final String PROP_NOTIFICATION_PREFIX = "notification";
  private static final String PROP_ENABLED_NOTIFICATION_VALUE = "true";
  private static final String PROP_DISABLED_NOTIFICATION_VALUE = "false";

  private final DbClient dbClient;
  private final Dispatchers dispatchers;

  public NotificationUpdater(DbClient dbClient, Dispatchers dispatchers) {
    this.dbClient = dbClient;
    this.dispatchers = dispatchers;
  }

  /**
   * Add a notification to a user.
   */
  public void add(DbSession dbSession, String channel, String dispatcher, UserDto user, @Nullable EntityDto project) {
    String key = String.join(".", PROP_NOTIFICATION_PREFIX, dispatcher, channel);
    String projectUuid = project == null ? null : project.getUuid();
    String projectKey = project == null ? null : project.getKey();
    String projectName = project == null ? null : project.getName();
    String qualifier = project == null ? null : project.getQualifier();

    List<PropertyDto> existingNotification = dbClient.propertiesDao().selectByQuery(
      PropertyQuery.builder()
        .setKey(key)
        .setEntityUuid(projectUuid)
        .setUserUuid(user.getUuid())
        .build(),
      dbSession).stream()
      .filter(notificationScope(project))
      .toList();
    checkArgument(existingNotification.isEmpty()
      || !PROP_ENABLED_NOTIFICATION_VALUE.equals(existingNotification.get(0).getValue()), "Notification already added");

    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey(key)
      .setUserUuid(user.getUuid())
      .setValue(PROP_ENABLED_NOTIFICATION_VALUE)
      .setEntityUuid(projectUuid),
      user.getLogin(), projectKey, projectName, qualifier);
  }

  /**
   * Remove a notification from a user.
   */
  public void remove(DbSession dbSession, String channel, String dispatcher, UserDto user, @Nullable EntityDto project) {
    String key = String.join(".", PROP_NOTIFICATION_PREFIX, dispatcher, channel);
    String projectUuid = project == null ? null : project.getUuid();
    String projectKey = project == null ? null : project.getKey();
    String projectName = project == null ? null : project.getName();
    String qualifier = project == null ? null : project.getQualifier();

    List<PropertyDto> existingNotification = dbClient.propertiesDao().selectByQuery(
      PropertyQuery.builder()
        .setKey(key)
        .setEntityUuid(projectUuid)
        .setUserUuid(user.getUuid())
        .build(),
      dbSession).stream()
      .filter(notificationScope(project))
      .toList();

    if (dispatchers.getEnabledByDefaultDispatchers().contains(dispatcher)) {
      dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
        .setKey(key)
        .setUserUuid(user.getUuid())
        .setValue(PROP_DISABLED_NOTIFICATION_VALUE)
        .setEntityUuid(projectUuid),
        user.getLogin(), projectKey, projectName, qualifier);
    } else {
      checkArgument(!existingNotification.isEmpty() && PROP_ENABLED_NOTIFICATION_VALUE.equals(existingNotification.get(0).getValue()), "Notification doesn't exist");
      dbClient.propertiesDao().delete(dbSession, new PropertyDto()
        .setKey(key)
        .setUserUuid(user.getUuid())
        .setValue(PROP_ENABLED_NOTIFICATION_VALUE)
        .setEntityUuid(projectUuid), user.getLogin(), projectKey, projectName, qualifier);
    }

  }

  private static Predicate<PropertyDto> notificationScope(@Nullable EntityDto project) {
    return prop -> project == null ? (prop.getEntityUuid() == null) : (prop.getEntityUuid() != null);
  }
}
