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
import org.apache.ibatis.annotations.Param;

public interface NotificationGroupSubscriptionMapper {

  void insert(NotificationGroupSubscriptionDto dto);

  int deleteByGroupAndTypeAndChannel(
    @Param("groupUuid") String groupUuid,
    @Param("notificationType") String notificationType,
    @Param("channelKey") String channelKey);

  int deleteByGroupUuid(@Param("groupUuid") String groupUuid);

  List<NotificationGroupSubscriptionDto> selectByTypeAndChannel(
    @Param("notificationType") String notificationType,
    @Param("channelKey") String channelKey);

  List<NotificationGroupSubscriptionDto> selectAll();
}
