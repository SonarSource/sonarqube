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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserGroupNewValue;

public class UserGroupDao implements Dao {
  private final Logger logger = LoggerFactory.getLogger(UserGroupDao.class);

  private final AuditPersister auditPersister;
  private final UuidFactory uuidFactory;

  public UserGroupDao(AuditPersister auditPersister, UuidFactory uuidFactory) {
    this.auditPersister = auditPersister;
    this.uuidFactory = uuidFactory;
  }

  public UserGroupDto insert(DbSession session, UserGroupDto dto, String groupName, String login, String organizationUuid) {
    dto.setUuid(uuidFactory.create());
    mapper(session).insert(dto);
    logger.debug("Added User : {} to User Group : {}", login, dto.getGroupUuid());
    auditPersister.addUserToGroup(session, organizationUuid, new UserGroupNewValue(dto, groupName, login));
    return dto;
  }

  public Set<String> selectUserUuidsInGroup(DbSession session, String groupUuid) {
    return mapper(session).selectUserUuidsInGroup(groupUuid);
  }

  public List<UserGroupDto> selectByQuery(DbSession session, UserGroupQuery query, int page, int pageSize) {
    return mapper(session).selectByQuery(query, Pagination.forPage(page).andSize(pageSize));
  }

  public int countByQuery(DbSession session, UserGroupQuery query) {
    return mapper(session).countByQuery(query);
  }

  public void delete(DbSession session, GroupDto group, UserDto user) {
    int deletedRows = mapper(session).delete(group.getUuid(), user.getUuid());
    logger.debug("Removed User : {} from User Group : {}", user.getName(), group.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserFromGroup(session, group.getOrganizationUuid(), new UserGroupNewValue(group, user));
    }
  }

  public void deleteByGroupUuid(DbSession session, GroupDto group) {
    int deletedRows = mapper(session).deleteByGroupUuid(group.getUuid());
    if (deletedRows > 0) {
      auditPersister.deleteUserFromGroup(session, group.getOrganizationUuid(),
          new UserGroupNewValue(group.getUuid(), group.getName()));
    }
  }

  public void deleteByOrganizationAndUser(DbSession dbSession, String organizationUuid, String userUuid) {
    mapper(dbSession).deleteByOrganizationAndUser(organizationUuid, userUuid);
  }

  public void deleteByUserUuid(DbSession dbSession, UserDto userDto) {
    // Get all groups that the user belongs to before deletion
    List<UserGroupDto> userGroups = mapper(dbSession).selectByQuery(
        new UserGroupQuery(null, null, userDto.getUuid()), Pagination.all());

    // Delete all groups for this user
    int deletedRows = mapper(dbSession).deleteByUserUuid(userDto.getUuid());

    if (deletedRows > 0) {
      // Process each group for audit logging
      userGroups.forEach(ug -> {
        GroupDto group = dbSession.getMapper(GroupMapper.class).selectByUuid(ug.getGroupUuid());
        if (group != null) {
          auditPersister.deleteUserFromGroup(dbSession, group.getOrganizationUuid(), new UserGroupNewValue(userDto));
        }
      });
    }
  }

  private static UserGroupMapper mapper(DbSession session) {
    return session.getMapper(UserGroupMapper.class);
  }

}
