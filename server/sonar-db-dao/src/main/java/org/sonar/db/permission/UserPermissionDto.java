/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UserPermissionDto {

  private String organizationUuid;
  private String permission;
  private int userId;
  private Long componentId;

  public UserPermissionDto() {
    // used by MyBatis
  }

  public UserPermissionDto(String organizationUuid, String permission, int userId, @Nullable Long componentId) {
    this.organizationUuid = organizationUuid;
    this.permission = permission;
    this.userId = userId;
    this.componentId = componentId;
  }

  public String getPermission() {
    return permission;
  }

  public int getUserId() {
    return userId;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  /**
   * @return {@code null} if it's a global permission, else return the project id.
   */
  @CheckForNull
  public Long getComponentId() {
    return componentId;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("UserPermissionDto{");
    sb.append("permission='").append(permission).append('\'');
    sb.append(", userId=").append(userId);
    sb.append(", organizationUuid=").append(organizationUuid);
    sb.append(", componentId=").append(componentId);
    sb.append('}');
    return sb.toString();
  }
}
