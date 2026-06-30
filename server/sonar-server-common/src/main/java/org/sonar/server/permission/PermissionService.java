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
package org.sonar.server.permission;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

public interface PermissionService {

  List<GlobalPermission> getGlobalPermissions();

  List<ProjectPermission> getAllProjectPermissions();

  /**
   * Returns the permissions held by the given groups, filtered to only include permissions
   * that are enabled for the current edition/configuration.
   */
  List<GroupPermissionDto> findGroupPermissions(DbSession dbSession, List<GroupDto> groups, @Nullable EntityDto entity);

  /**
   * Returns the permissions held by the given users, filtered to only include permissions
   * that are enabled for the current edition/configuration.
   */
  List<UserPermissionDto> findUserPermissions(DbSession dbSession, List<UserDto> users, @Nullable EntityDto entity);

}
