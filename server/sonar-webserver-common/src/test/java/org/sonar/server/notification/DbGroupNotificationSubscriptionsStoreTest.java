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
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.notification.NotificationGroupSubscriptionDto;
import org.sonar.db.notification.NotificationGroupSubscriptionsDao;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.NotFoundException;
import com.sonarsource.groupnotifications.api.model.GroupNotificationSubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DbGroupNotificationSubscriptionsStoreTest {

  private static final String GROUP_UUID = "group-uuid";
  private static final String NOTIFICATION_TYPE = "security-alert-raised";
  private static final String CHANNEL_KEY = "EmailNotificationChannel";

  @Test
  public void addIfAbsent_whenGroupExists_checkAndWriteInOneSession() {
    DbClient dbClient = mock(DbClient.class);
    DbSession session = mock(DbSession.class);
    GroupDao groupDao = mock(GroupDao.class);
    NotificationGroupSubscriptionsDao subscriptionsDao = mock(NotificationGroupSubscriptionsDao.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.groupDao()).thenReturn(groupDao);
    when(groupDao.selectByUuid(session, GROUP_UUID)).thenReturn(new GroupDto().setUuid(GROUP_UUID));
    when(dbClient.notificationGroupSubscriptionsDao()).thenReturn(subscriptionsDao);

    new DbGroupNotificationSubscriptionsStore(dbClient).addIfAbsent(subscription());

    var order = inOrder(groupDao, subscriptionsDao, session);
    order.verify(groupDao).selectByUuid(session, GROUP_UUID);
    order.verify(subscriptionsDao).deleteByGroupAndTypeAndChannel(session, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY);
    order.verify(subscriptionsDao).insert(session, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY);
    order.verify(session).commit();
  }

  @Test
  public void addIfAbsent_whenGroupDoesNotExist_doNotInsertSubscription() {
    DbClient dbClient = mock(DbClient.class);
    DbSession session = mock(DbSession.class);
    GroupDao groupDao = mock(GroupDao.class);
    NotificationGroupSubscriptionsDao subscriptionsDao = mock(NotificationGroupSubscriptionsDao.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.groupDao()).thenReturn(groupDao);
    when(groupDao.selectByUuid(session, GROUP_UUID)).thenReturn(null);
    when(dbClient.notificationGroupSubscriptionsDao()).thenReturn(subscriptionsDao);
    DbGroupNotificationSubscriptionsStore underTest = new DbGroupNotificationSubscriptionsStore(dbClient);
    GroupNotificationSubscription subscription = subscription();

    assertThatThrownBy(() -> underTest.addIfAbsent(subscription))
      .isInstanceOf(NotFoundException.class);

    verify(subscriptionsDao, never()).insert(session, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY);
    verify(session, never()).commit();
  }

  @Test
  public void selectByOrganization_mapsHostWideRows() {
    DbClient dbClient = mock(DbClient.class);
    DbSession session = mock(DbSession.class);
    NotificationGroupSubscriptionsDao subscriptionsDao = mock(NotificationGroupSubscriptionsDao.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.notificationGroupSubscriptionsDao()).thenReturn(subscriptionsDao);
    when(subscriptionsDao.selectAll(session)).thenReturn(List.of(new NotificationGroupSubscriptionDto()
      .setGroupUuid(GROUP_UUID)
      .setNotificationType(NOTIFICATION_TYPE)
      .setChannelKey(CHANNEL_KEY)));

    List<GroupNotificationSubscription> result = new DbGroupNotificationSubscriptionsStore(dbClient).selectByOrganization(null);

    assertThat(result).containsExactly(subscription());
  }

  @Test
  public void selectByGroupUuids_mapsOnlyRequestedGroupRows() {
    DbClient dbClient = mock(DbClient.class);
    DbSession session = mock(DbSession.class);
    NotificationGroupSubscriptionsDao subscriptionsDao = mock(NotificationGroupSubscriptionsDao.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.notificationGroupSubscriptionsDao()).thenReturn(subscriptionsDao);
    when(subscriptionsDao.selectByGroupUuids(session, Set.of(GROUP_UUID))).thenReturn(List.of(new NotificationGroupSubscriptionDto()
      .setGroupUuid(GROUP_UUID)
      .setNotificationType(NOTIFICATION_TYPE)
      .setChannelKey(CHANNEL_KEY)));

    assertThat(new DbGroupNotificationSubscriptionsStore(dbClient).selectByGroupUuids(null, Set.of(GROUP_UUID)))
      .containsExactly(subscription());
  }

  @Test
  public void selectByGroupUuids_whenNoGroups_shouldNotQueryStore() {
    DbClient dbClient = mock(DbClient.class);

    assertThat(new DbGroupNotificationSubscriptionsStore(dbClient).selectByGroupUuids(null, Set.of())).isEmpty();
  }

  @Test
  public void selectByOrganization_whenScopedToOrganization_rejectCloudScope() {
    DbClient dbClient = mock(DbClient.class);
    DbGroupNotificationSubscriptionsStore underTest = new DbGroupNotificationSubscriptionsStore(dbClient);

    assertThatThrownBy(() -> underTest.selectByOrganization("organization-uuid"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private static GroupNotificationSubscription subscription() {
    return new GroupNotificationSubscription(null, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY);
  }
}
