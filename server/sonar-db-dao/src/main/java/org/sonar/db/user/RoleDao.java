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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sonar.api.web.UserRole;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;

public class RoleDao implements Dao {
  private static final Set<String> UNSUPPORTED_PROJECT_PERMISSIONS = ImmutableSet.of(USER, CODEVIEWER);

  /**
   * All the entities on which the user has {@code permission}, directly or through
   * groups.
   *
   * @throws IllegalArgumentException this method does not support permissions {@link UserRole#USER user} nor
   *         {@link UserRole#CODEVIEWER codeviewer} because it does not support public root components.
   */
  public List<String> selectEntityUuidsByPermissionAndUserUuidAndQualifier(DbSession dbSession, String permission, String userUuid, Collection<String> qualifiers) {
    checkArgument(
      !UNSUPPORTED_PROJECT_PERMISSIONS.contains(permission),
      "Permissions %s are not supported by selectEntityUuidsByPermissionAndUserUuidAndQualifier", UNSUPPORTED_PROJECT_PERMISSIONS);
    return mapper(dbSession).selectEntityUuidsByPermissionAndUserUuidAndQualifier(permission, userUuid, qualifiers);
  }

  public void deleteGroupRolesByGroupUuid(DbSession session, String groupUuid) {
    mapper(session).deleteGroupRolesByGroupUuid(groupUuid);
  }

  private static RoleMapper mapper(DbSession session) {
    return session.getMapper(RoleMapper.class);
  }
}
