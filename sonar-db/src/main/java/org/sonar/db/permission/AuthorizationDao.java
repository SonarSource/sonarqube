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
package org.sonar.db.permission;

import java.util.Set;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

/**
 * The SQL requests used to verify authorization (the permissions
 * granted to users)
 *
 * @see GroupPermissionDao for CRUD of table group_roles
 * @see UserPermissionDao for CRUD of table user_roles
 */
public class AuthorizationDao implements Dao {

  /**
   * Loads all the permissions granted to logged-in user for the specified organization
   */
  public Set<String> selectOrganizationPermissions(DbSession dbSession, String organizationUuid, long userId) {
    return mapper(dbSession).selectOrganizationPermissions(organizationUuid, userId);
  }

  /**
   * Loads all the permissions granted to anonymous user for the specified organization
   */
  public Set<String> selectOrganizationPermissionsOfAnonymous(DbSession dbSession, String organizationUuid) {
    return mapper(dbSession).selectOrganizationPermissionsOfAnonymous(organizationUuid);
  }

  /**
   * Loads all the permissions granted to logged-in user for the specified root component (project)
   */
  public Set<String> selectRootComponentPermissions(DbSession dbSession, long rootComponentId, long userId) {
    return mapper(dbSession).selectRootComponentPermissions(rootComponentId, userId);
  }

  /**
   * Loads all the permissions granted to anonymous user for the specified root component (project)
   */
  public Set<String> selectRootComponentPermissionsOfAnonymous(DbSession dbSession, long rootComponentId) {
    return mapper(dbSession).selectRootComponentPermissionsOfAnonymous(rootComponentId);
  }

  private static AuthorizationMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(AuthorizationMapper.class);
  }
}
