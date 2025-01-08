/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserId;

public class UserPermissionNewValue extends PermissionNewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private final String userUuid;

  @Nullable
  private final String userLogin;

  public UserPermissionNewValue(UserPermissionDto permissionDto, @Nullable String componentKey, @Nullable String componentName,
    @Nullable UserId userId, @Nullable String qualifier, @Nullable PermissionTemplateDto templateDto) {
    super(permissionDto.getUuid(), permissionDto.getEntityUuid(), componentKey, componentName, permissionDto.getPermission(), qualifier, templateDto);
    this.userUuid = userId != null ? userId.getUuid() : null;
    this.userLogin = userId != null ? userId.getLogin() : null;
  }

  public UserPermissionNewValue(UserId userId, @Nullable String qualifier) {
    this(null, null, null, null, userId, qualifier);
  }

  public UserPermissionNewValue(@Nullable String role, @Nullable String componentUuid, @Nullable  String componentKey, @Nullable String componentName,
    UserId userId, @Nullable String qualifier) {
    super(null, componentUuid, componentKey, componentName, role, qualifier, null);
    this.userUuid = userId != null ? userId.getUuid() : null;
    this.userLogin = userId != null ? userId.getLogin() : null;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
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
    addField(sb, "\"componentKey\": ", this.componentKey, true);
    addField(sb, "\"componentName\": ", this.componentName, true);
    addField(sb, "\"permissionTemplateUuid\": ", this.permissionTemplateId, true);
    addField(sb, "\"permissionTemplateName\": ", this.permissionTemplateName, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    addField(sb, "\"qualifier\": ", getQualifier(this.qualifier), true);
    endString(sb);
    return sb.toString();
  }

}
