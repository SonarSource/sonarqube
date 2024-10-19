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
package org.sonar.db.ce;

import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.dismissmessage.MessageType;

public class CeTaskMessageDao implements Dao {
  public void insert(DbSession dbSession, CeTaskMessageDto dto) {
    getMapper(dbSession).insert(dto);
  }

  public Optional<CeTaskMessageDto> selectByUuid(DbSession dbSession, String uuid) {
    return getMapper(dbSession).selectByUuid(uuid);
  }


  /**
   * @return the non dismissed messages for the specific task and specific user, if any, in ascending order of column {@code CREATED_AT}.
   */
  public List<CeTaskMessageDto> selectNonDismissedByUserAndTask(DbSession dbSession, String taskUuid, String userUuid) {
    return getMapper(dbSession).selectNonDismissedByUserAndTask(taskUuid, userUuid);
  }

  public void deleteByType(DbSession session, MessageType type) {
    getMapper(session).deleteByType(type.name());
  }

  private static CeTaskMessageMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(CeTaskMessageMapper.class);
  }
}
