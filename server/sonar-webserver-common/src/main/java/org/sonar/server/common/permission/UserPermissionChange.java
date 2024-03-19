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
package org.sonar.server.common.permission;

import javax.annotation.Nullable;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.user.UserId;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.permission.PermissionService;

import static java.util.Objects.requireNonNull;

public class UserPermissionChange extends PermissionChange {

  private final UserId userId;

  public UserPermissionChange(Operation operation, String permission, @Nullable EntityDto entity, UserId userId,
    PermissionService permissionService) {
    super(operation, permission, entity, permissionService);
    this.userId = requireNonNull(userId);
  }

  public UserId getUserId() {
    return userId;
  }

  @Override
  public String getUuidOfGrantee() {
    return userId.getUuid();
  }
}
