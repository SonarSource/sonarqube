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
package org.sonar.db.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UserPermissionDto {
  private String uuid;
  private String organizationUuid;
  private String permission;
  private String userUuid;
  private String componentUuid;

  public UserPermissionDto() {
    // used by MyBatis
  }

  public UserPermissionDto(String uuid, String organizationUuid, String permission, String userUuid, @Nullable String componentUuid) {
    this.uuid = uuid;
    this.organizationUuid = organizationUuid;
    this.permission = permission;
    this.userUuid = userUuid;
    this.componentUuid = componentUuid;
  }

  public String getUuid() {
    return uuid;
  }

  public String getPermission() {
    return permission;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  /**
   * @return {@code null} if it's a global permission, otherwise return the project uiid.
   */
  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("UserPermissionDto{");
    sb.append("permission='").append(permission).append('\'');
    sb.append(", userUuid=").append(userUuid);
    sb.append(", organizationUuid=").append(organizationUuid);
    sb.append(", componentUuid=").append(componentUuid);
    sb.append('}');
    return sb.toString();
  }
}
