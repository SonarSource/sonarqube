/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission;

import com.google.common.base.Strings;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.BadRequestException;

public class PermissionChange {

  static final String USER_KEY = "user";
  static final String GROUP_KEY = "group";
  static final String PERMISSION_KEY = "permission";
  static final String COMPONENT_KEY = "component";

  private String userLogin;
  private String groupName;
  private String componentKey;
  private String permission;

  public PermissionChange setUserLogin(@Nullable String login) {
    this.userLogin = login;
    return this;
  }

  public PermissionChange setGroupName(@Nullable String name) {
    this.groupName = name;
    return this;
  }

  public PermissionChange setComponentKey(@Nullable String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  public PermissionChange setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public static PermissionChange buildFromParams(Map<String, Object> params) {
    return new PermissionChange()
      .setUserLogin((String) params.get(USER_KEY))
      .setGroupName((String) params.get(GROUP_KEY))
      .setComponentKey((String) params.get(COMPONENT_KEY))
      .setPermission((String) params.get(PERMISSION_KEY));
  }

  void validate() {
    validatePermission();
    validateUserGroup();
  }

  private void validateUserGroup() {
    if (StringUtils.isBlank(userLogin) && StringUtils.isBlank(groupName)) {
      throw new BadRequestException("Missing user or group parameter");
    }
    if (StringUtils.isNotBlank(userLogin) && StringUtils.isNotBlank(groupName)) {
      throw new BadRequestException("Only one of user or group parameter should be provided");
    }
  }

  private void validatePermission() {
    if (StringUtils.isBlank(permission)) {
      throw new BadRequestException("Missing permission parameter");
    }
    if (Strings.isNullOrEmpty(componentKey)) {
      if (!GlobalPermissions.ALL.contains(permission)) {
        throw new BadRequestException(String.format("Invalid global permission key %s. Valid values are %s", permission, GlobalPermissions.ALL));
      }
    } else {
      if (!ProjectPermissions.ALL.contains(permission)) {
        throw new BadRequestException(String.format("Invalid component permission key %s. Valid values are %s", permission, ProjectPermissions.ALL));
      }
    }
  }

  @CheckForNull
  public String userLogin() {
    return userLogin;
  }

  @CheckForNull
  public String groupName() {
    return groupName;
  }

  @CheckForNull
  public String componentKey() {
    return componentKey;
  }

  public String permission() {
    return permission;
  }

  @Override
  public String toString() {
    return "PermissionChange{" +
      "user='" + userLogin + '\'' +
      ", group='" + groupName + '\'' +
      ", componentKey='" + componentKey + '\'' +
      ", permission='" + permission + '\'' +
      '}';
  }
}
