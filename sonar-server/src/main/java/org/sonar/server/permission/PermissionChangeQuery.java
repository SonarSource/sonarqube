/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.apache.commons.lang.StringUtils;
import org.sonar.core.permission.ComponentPermissions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.BadRequestException;

import javax.annotation.Nullable;

import java.util.Map;

public class PermissionChangeQuery {

  private static final String USER_KEY = "user";
  private static final String GROUP_KEY = "group";
  private static final String PERMISSION_KEY = "permission";
  private static final String COMPONENT_KEY = "component";

  private final String user;
  private final String group;
  private final String component;
  private final String permission;

  private PermissionChangeQuery(@Nullable String user, @Nullable String group, @Nullable String component, String permission) {
    this.user = user;
    this.group = group;
    this.component = component;
    this.permission = permission;
  }

  public static PermissionChangeQuery buildFromParams(Map<String, Object> params) {
    return new PermissionChangeQuery((String) params.get(USER_KEY), (String) params.get(GROUP_KEY), (String) params.get(COMPONENT_KEY), (String) params.get(PERMISSION_KEY));
  }

  public void validate() {
    validatePermission();
    validateUserGroup();
  }

  private void validateUserGroup() {
    if (StringUtils.isBlank(user) && StringUtils.isBlank(group)) {
      throw new BadRequestException("Missing user or group parameter");
    }
    if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(group)) {
      throw new BadRequestException("Only one of user or group parameter should be provided");
    }
  }

  private void validatePermission() {
    if (StringUtils.isBlank(permission)) {
      throw new BadRequestException("Missing permission parameter");
    }
    if (Strings.isNullOrEmpty(component)){
      if (!GlobalPermissions.ALL.contains(permission)) {
        throw new BadRequestException(String.format("Invalid global permission key %s. Valid values are %s", permission, GlobalPermissions.ALL));
      }
    } else {
      if (!ComponentPermissions.ALL.contains(permission)) {
        throw new BadRequestException(String.format("Invalid component permission key %s. Valid values are %s", permission, ComponentPermissions.ALL));
      }
    }
  }

  public boolean targetsUser() {
    return user != null;
  }

  @Nullable
  public String user() {
    return user;
  }

  @Nullable
  public String group() {
    return group;
  }

  @Nullable
  public String component() {
    return component;
  }

  public String permission() {
    return permission;
  }
}
