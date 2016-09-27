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

import javax.annotation.CheckForNull;

public class ExtendedUserPermissionDto {

  private long userId;
  private Long componentId;
  private String permission;

  // join columns
  private String userLogin;
  private String componentUuid;

  public long getUserId() {
    return userId;
  }

  @CheckForNull
  public Long getComponentId() {
    return componentId;
  }

  /**
   * Permission can be null when {@link PermissionQuery#withAtLeastOnePermission()} is false.
   */
  @CheckForNull
  public String getPermission() {
    return permission;
  }

  public String getUserLogin() {
    return userLogin;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ExtendedUserPermissionDto{");
    sb.append("userId=").append(userId);
    sb.append(", componentId=").append(componentId);
    sb.append(", permission='").append(permission).append('\'');
    sb.append(", userLogin='").append(userLogin).append('\'');
    sb.append(", componentUuid='").append(componentUuid).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
