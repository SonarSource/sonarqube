/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.audit.model;

import javax.annotation.Nullable;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.UserId;

public class UserPermissionNewValue extends PermissionNewValue {

  @Nullable
  private final String userUuid;
  @Nullable
  private final String userLogin;

  public UserPermissionNewValue(UserPermissionDto permissionDto, @Nullable String projectName, @Nullable UserId userId, String qualifier) {
    super(permissionDto.getUuid(), permissionDto.getComponentUuid(), projectName, permissionDto.getPermission(), qualifier);
    this.userUuid = userId != null ? userId.getUuid() : null;
    this.userLogin = userId != null ? userId.getLogin() : null;
  }

  public UserPermissionNewValue(UserId userId, String qualifier) {
    this(null, null, null, userId, qualifier);
  }

  public UserPermissionNewValue(String role, String projectUuid, String projectName, UserId userId, String qualifier) {
    super(null, projectUuid, projectName, role, qualifier);
    this.userUuid = userId != null ? userId.getUuid() : null;
    this.userLogin = userId != null ? userId.getLogin() : null;
  }

  @Nullable
  public String getUserUuid() {
    return userUuid;
  }

  @Nullable
  public String getUserLogin() {
    return userLogin;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"permissionUuid\": ", this.permissionUuid, true);
    addField(sb, "\"permission\": ", this.permission, true);
    addField(sb, "\"componentUuid\": ", this.componentUuid, true);
    addField(sb, "\"componentName\": ", this.componentName, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    addField(sb, "\"qualifier\": ", this.qualifier, true);
    endString(sb);
    return sb.toString();
  }

}
