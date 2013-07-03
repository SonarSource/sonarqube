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
import org.sonar.core.user.Permission;

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
    return new PermissionChangeQuery((String) params.get(USER_KEY), (String) params.get(GROUP_KEY), (String) params.get(ROLE_KEY));
  }

  public void validate() {
    if (StringUtils.isBlank(role)) {
      throw new IllegalArgumentException("Missing role parameter");
    }
    if (StringUtils.isBlank(user) && StringUtils.isBlank(group)) {
      throw new IllegalArgumentException("Missing user or group parameter");
    }
    if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(group)) {
      throw new IllegalArgumentException("Only one of user or group parameter should be provided");
    }
    if (!Permission.allGlobal().keySet().contains(role)) {
      throw new IllegalArgumentException("Invalid role key " + role);
    }
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
}
