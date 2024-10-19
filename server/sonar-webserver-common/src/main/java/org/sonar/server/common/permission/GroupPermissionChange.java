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

import java.util.Optional;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.permission.GroupUuidOrAnyone;
import org.sonar.server.permission.PermissionService;

public class GroupPermissionChange extends PermissionChange {

  private final GroupDto groupDto;

  public GroupPermissionChange(Operation operation, String organizationUuid, String permission, @Nullable EntityDto entityDto,
    @Nullable GroupDto groupDto, PermissionService permissionService) {
    super(operation, organizationUuid, permission, entityDto, permissionService);
    this.groupDto = groupDto;
  }

  public GroupUuidOrAnyone getGroupUuidOrAnyone() {
    return GroupUuidOrAnyone.from(groupDto);
  }

  public Optional<String> getGroupName() {
    return Optional.ofNullable(groupDto).map(GroupDto::getName);
  }

  @Override
  public String getUuidOfGrantee() {
    return getGroupUuidOrAnyone().getUuid();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", GroupPermissionChange.class.getSimpleName() + "[", "]")
      .add("organizationUuid=" + getOrganizationUuid())
      .add("groupDto=" + groupDto)
      .add("operation=" + getOperation())
      .add("permission='" + getPermission() + "'")
      .add("entity=" + getEntity())
      .add("permissionService=" + permissionService)
      .toString();
  }
}
