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

import org.apache.commons.lang.StringUtils;
import org.sonar.core.user.Permissions;

import java.util.Map;

public class PermissionChangeQuery {

  private static final String USER_KEY = "user";
  private static final String GROUP_KEY = "group";
  private static final String ROLE_KEY = "permission";

  private final String user;
  private final String group;
  private final String role;

  private PermissionChangeQuery(String user, String group, String role) {
    this.user = user;
    this.group = group;
    this.role = role;
  }

  public static PermissionChangeQuery buildFromParams(Map<String, Object> params) {
    return new PermissionChangeQuery((String)params.get(USER_KEY), (String)params.get(GROUP_KEY), (String)params.get(ROLE_KEY));
  }

  public boolean isValid() {
    return StringUtils.isNotBlank(role) && isValidPermissionReference(role) && (StringUtils.isNotBlank(user) ^ StringUtils.isNotBlank(group));
  }

  public boolean targetsUser() {
    return user != null;
  }

  public String getUser() {
    return user;
  }

  public String getGroup() {
    return group;
  }

  public String getRole() {
    return role;
  }

  private boolean isValidPermissionReference(String role) {
    return Permissions.SYSTEM_ADMIN.equals(role)
      || Permissions.QUALITY_PROFILE_ADMIN.equals(role)
      || Permissions.DASHBOARD_SHARING.equals(role)
      || Permissions.SCAN_EXECUTION.equals(role)
      || Permissions.DRY_RUN_EXECUTION.equals(role);
  }
}
