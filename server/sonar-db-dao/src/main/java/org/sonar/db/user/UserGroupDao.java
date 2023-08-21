/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Set;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserGroupNewValue;

public class UserGroupDao implements Dao {
  private final AuditPersister auditPersister;
  private final Logger logger = Loggers.get(UserGroupDao.class);

  public UserGroupDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  public UserGroupDto insert(DbSession session, UserGroupDto dto, String groupName, String login) {
    mapper(session).insert(dto);
    logger.debug("Added User : {} to User Group : {}", login, dto.getGroupUuid());
    auditPersister.addUserToGroup(session, new UserGroupNewValue(dto, groupName, login));
    return dto;
  }

  public Set<String> selectUserUuidsInGroup(DbSession session, String groupUuid) {
    return mapper(session).selectUserUuidsInGroup(groupUuid);
  }

  public void delete(DbSession session, GroupDto group, UserDto user) {
    int deletedRows = mapper(session).delete(group.getUuid(), user.getUuid());
    logger.debug("Removed User : {} from User Group : {}", user.getName(), group.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserFromGroup(session, new UserGroupNewValue(group, user));
    }
  }

  public void deleteByGroupUuid(DbSession session, String groupUuid, String groupName) {
    int deletedRows = mapper(session).deleteByGroupUuid(groupUuid);

    if (deletedRows > 0) {
      auditPersister.deleteUserFromGroup(session, new UserGroupNewValue(groupUuid, groupName));
    }
  }

  public void deleteByOrganizationAndUser(DbSession dbSession, String organizationUuid, String userUuid) {
    mapper(dbSession).deleteByOrganizationAndUser(organizationUuid, userUuid);
  }

  public void deleteByUserUuid(DbSession dbSession, UserDto userDto) {
    int deletedRows = mapper(dbSession).deleteByUserUuid(userDto.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserFromGroup(dbSession, new UserGroupNewValue(userDto));
    }
  }

  private static UserGroupMapper mapper(DbSession session) {
    return session.getMapper(UserGroupMapper.class);
  }
}
