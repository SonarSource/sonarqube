/*
 * SonarQube
 * Copyright (C) 2026 SonarSource Sàrl
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
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.notification.NotificationGroupSubscriptionDto;
import com.sonarsource.groupnotifications.api.GroupNotificationSubscriptionsService;
import com.sonarsource.groupnotifications.api.model.GroupNotificationSubscription;
import com.sonarsource.groupnotifications.api.model.GroupNotificationType;

/** Database-backed capability test double for the notification Web API integration tests. */
public class TestGroupNotificationSubscriptionsService implements GroupNotificationSubscriptionsService {
  private final DbClient dbClient;

  public TestGroupNotificationSubscriptionsService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public List<GroupNotificationSubscription> selectByOrganization(String organizationUuid) {
    requireHostWide(organizationUuid);
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.notificationGroupSubscriptionsDao().selectAll(session).stream()
        .map(TestGroupNotificationSubscriptionsService::toSubscription)
        .toList();
    }
  }

  @Override
  public List<GroupNotificationSubscription> selectForUser(String userUuid, String organizationUuid) {
    requireHostWide(organizationUuid);
    try (DbSession session = dbClient.openSession(false)) {
      Set<String> groupUuids = Set.copyOf(dbClient.groupMembershipDao().selectGroupUuidsByUserUuid(session, userUuid));
      return selectByGroupUuids(session, groupUuids);
    }
  }

  @Override
  public List<GroupNotificationType> selectAvailableTypes() {
    return List.of();
  }

  @Override
  public void add(GroupNotificationSubscription subscription) {
    requireHostWide(subscription.organizationUuid());
    try (DbSession session = dbClient.openSession(false)) {
      dbClient.notificationGroupSubscriptionsDao().deleteByGroupAndTypeAndChannel(
        session, subscription.groupUuid(), subscription.notificationType(), subscription.channelKey());
      dbClient.notificationGroupSubscriptionsDao().insert(
        session, subscription.groupUuid(), subscription.notificationType(), subscription.channelKey());
      session.commit();
    }
  }

  @Override
  public void remove(GroupNotificationSubscription subscription) {
    requireHostWide(subscription.organizationUuid());
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
    throw new IllegalArgumentException("SonarQube Server group notifications are host-wide");
  }

  private List<GroupNotificationSubscription> selectByGroupUuids(DbSession session, Set<String> groupUuids) {
    if (groupUuids.isEmpty()) {
      return List.of();
    }
    return dbClient.notificationGroupSubscriptionsDao().selectAll(session).stream()
      .filter(subscription -> groupUuids.contains(subscription.getGroupUuid()))
      .map(TestGroupNotificationSubscriptionsService::toSubscription)
      .toList();
  }

  private static GroupNotificationSubscription toSubscription(NotificationGroupSubscriptionDto dto) {
    return new GroupNotificationSubscription(null, dto.getGroupUuid(), dto.getNotificationType(), dto.getChannelKey());
  }

  private static void requireHostWide(String organizationUuid) {
    if (organizationUuid != null) {
      throw new IllegalArgumentException("SonarQube Server group notifications are host-wide");
    }
  }
}
