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

package org.sonar.wsclient.permissions;

import java.util.HashMap;
import java.util.Map;

public class PermissionParameters {

  private final Map<String, Object> params;

  private PermissionParameters(){
    params = new HashMap<String, Object>();
  }

  public static PermissionParameters create() {
    return new PermissionParameters();
  }

  public PermissionParameters user(String user) {
    params.put("user", user);
    return this;
  }

  public PermissionParameters group(String group) {
    params.put("group", group);
    return this;
  }

  public PermissionParameters component(String component) {
    params.put("component", component);
    return this;
  }

  public PermissionParameters permission(String permission) {
    params.put("permission", permission);
    return this;
  }

  public Map<String, Object> urlParams() {
    return params;
  }
}
