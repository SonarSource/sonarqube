/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class RoleDao implements Dao {

  /**
   * All the projects on which the user has {@code permission}, directly or through
   * groups.
   */
  public List<Long> selectComponentIdsByPermissionAndUserId(DbSession dbSession, String permission, long userId) {
    return mapper(dbSession).selectComponentIdsByPermissionAndUserId(permission, userId);
  }

  public void deleteGroupRolesByGroupId(DbSession session, long groupId) {
    mapper(session).deleteGroupRolesByGroupId(groupId);
  }

  private static RoleMapper mapper(DbSession session) {
    return session.getMapper(RoleMapper.class);
  }
}
