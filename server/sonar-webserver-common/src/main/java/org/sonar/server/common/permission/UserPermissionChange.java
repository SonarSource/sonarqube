/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Optional;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.UserId;
import org.sonar.server.permission.PermissionService;

import static java.util.Objects.requireNonNull;

public class UserPermissionChange extends PermissionChange {

  private final UserId userId;

  public UserPermissionChange(Operation operation, ProjectPermission permission, @Nullable EntityDto entity, UserId userId,
    PermissionService permissionService) {
    this(operation, permission.getKey(), entity, userId, permissionService);
  }

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

  @Override
  public String toString() {
    return new StringJoiner(", ", UserPermissionChange.class.getSimpleName() + "[", "]")
      .add("userId=" + userId)
      .add("operation=" + getOperation())
      .add("permission='" + getPermission() + "'")
      .add("entity=" + Optional.ofNullable(getEntity()).map(EntityDto::getName).orElse("null"))
      .toString();
  }
}
