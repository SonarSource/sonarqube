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
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.notification.NotificationGroupSubscriptionDto;
import org.jspecify.annotations.Nullable;
import com.sonarsource.groupnotifications.api.GroupNotificationSubscriptionsStore;
import com.sonarsource.groupnotifications.api.model.GroupNotificationSubscription;

import static org.sonar.server.exceptions.NotFoundException.checkFound;

/** Server database adapter for the shared group-notifications capability. */
public class DbGroupNotificationSubscriptionsStore implements GroupNotificationSubscriptionsStore {

  private final DbClient dbClient;

  public DbGroupNotificationSubscriptionsStore(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public List<GroupNotificationSubscription> selectByOrganization(@Nullable String organizationUuid) {
    requireHostWideScope(organizationUuid);
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.notificationGroupSubscriptionsDao().selectAll(session).stream()
        .map(DbGroupNotificationSubscriptionsStore::toSubscription)
        .toList();
    }
  }

  @Override
  public List<GroupNotificationSubscription> selectByGroupUuids(@Nullable String organizationUuid, Set<String> groupUuids) {
    requireHostWideScope(organizationUuid);
    if (groupUuids.isEmpty()) {
      return List.of();
    }
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.notificationGroupSubscriptionsDao().selectByGroupUuids(session, groupUuids).stream()
        .map(DbGroupNotificationSubscriptionsStore::toSubscription)
        .toList();
    }
  }

  @Override
  public void addIfAbsent(GroupNotificationSubscription subscription) {
    requireHostWideScope(subscription.organizationUuid());
    try (DbSession session = dbClient.openSession(false)) {
      checkFound(dbClient.groupDao().selectByUuid(session, subscription.groupUuid()), "Group '%s' not found", subscription.groupUuid());
      // The unique key is group/type/channel. Delete-and-insert preserves the historical API's
      // behavior and makes a retry safe if a prior request committed but its response was lost.
      dbClient.notificationGroupSubscriptionsDao().deleteByGroupAndTypeAndChannel(
        session, subscription.groupUuid(), subscription.notificationType(), subscription.channelKey());
      dbClient.notificationGroupSubscriptionsDao().insert(
        session, subscription.groupUuid(), subscription.notificationType(), subscription.channelKey());
      session.commit();
    }
  }

  @Override
  public void removeIfPresent(GroupNotificationSubscription subscription) {
    requireHostWideScope(subscription.organizationUuid());
    try (DbSession session = dbClient.openSession(false)) {
      dbClient.notificationGroupSubscriptionsDao().deleteByGroupAndTypeAndChannel(
        session, subscription.groupUuid(), subscription.notificationType(), subscription.channelKey());
      session.commit();
    }
  }

  @Override
  public void deleteByGroup(String groupUuid) {
    try (DbSession session = dbClient.openSession(false)) {
      dbClient.notificationGroupSubscriptionsDao().deleteByGroupUuid(session, groupUuid);
      session.commit();
    }
  }

  @Override
  public void deleteByOrganization(String organizationUuid) {
    throw new UnsupportedOperationException("Server group notification subscriptions are host-wide");
  }

  private static GroupNotificationSubscription toSubscription(NotificationGroupSubscriptionDto dto) {
    return new GroupNotificationSubscription(null, dto.getGroupUuid(), dto.getNotificationType(), dto.getChannelKey());
  }

  private static void requireHostWideScope(@Nullable String organizationUuid) {
    if (organizationUuid != null) {
      throw new IllegalArgumentException("Server group notification subscriptions are host-wide");
    }
  }
}
