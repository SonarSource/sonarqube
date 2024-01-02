/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.user;

import java.util.List;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.project.ProjectDto;

public class UserDismissedMessagesDao implements Dao {
  private final System2 system2;

  public UserDismissedMessagesDao(System2 system2) {
    this.system2 = system2;
  }

  public UserDismissedMessageDto insert(DbSession session, UserDismissedMessageDto dto) {
    long now = system2.now();
    mapper(session).insert(dto.setCreatedAt(now));
    return dto;
  }

  public Optional<UserDismissedMessageDto> selectByUserAndProjectAndMessageType(DbSession session, String userUuid, ProjectDto project,
    MessageType messageType) {
    return mapper(session).selectByUserUuidAndProjectUuidAndMessageType(userUuid, project.getUuid(), messageType.name());
  }

  public Optional<UserDismissedMessageDto> selectByUserUuidAndMessageType(DbSession session, String userUuid,
    MessageType messageType) {
    return mapper(session).selectByUserUuidAndMessageType(userUuid, messageType.name());
  }

  public List<UserDismissedMessageDto> selectByUser(DbSession session, UserDto user) {
    return mapper(session).selectByUserUuid(user.getUuid());
  }

  public void deleteByUser(DbSession session, UserDto user) {
    mapper(session).deleteByUserUuid(user.getUuid());
  }

  public void deleteByType(DbSession session, MessageType type) {
    mapper(session).deleteByType(type.name());
  }

  private static UserDismissedMessagesMapper mapper(DbSession session) {
    return session.getMapper(UserDismissedMessagesMapper.class);
  }
}
