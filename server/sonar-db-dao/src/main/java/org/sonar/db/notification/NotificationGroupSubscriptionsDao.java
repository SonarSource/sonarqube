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
package org.sonar.db.notification;

import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class NotificationGroupSubscriptionsDao implements Dao {

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public NotificationGroupSubscriptionsDao(UuidFactory uuidFactory, System2 system2) {
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  public void insert(DbSession session, String groupUuid, String notificationType, String channelKey) {
    mapper(session).insert(new NotificationGroupSubscriptionDto()
      .setUuid(uuidFactory.create())
      .setGroupUuid(groupUuid)
      .setNotificationType(notificationType)
      .setChannelKey(channelKey)
      .setCreatedAt(system2.now()));
  }

  public int deleteByGroupAndTypeAndChannel(DbSession session, String groupUuid, String notificationType, String channelKey) {
    return mapper(session).deleteByGroupAndTypeAndChannel(groupUuid, notificationType, channelKey);
  }

  public int deleteByGroupUuid(DbSession session, String groupUuid) {
    return mapper(session).deleteByGroupUuid(groupUuid);
  }

  public List<NotificationGroupSubscriptionDto> selectByTypeAndChannel(DbSession session, String notificationType, String channelKey) {
    return mapper(session).selectByTypeAndChannel(notificationType, channelKey);
  }

  public List<NotificationGroupSubscriptionDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  private static NotificationGroupSubscriptionMapper mapper(DbSession session) {
    return session.getMapper(NotificationGroupSubscriptionMapper.class);
  }
}
