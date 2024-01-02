/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.db.permission.template.PermissionTemplateDto;

public class PermissionTemplateNewValue extends NewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  private String templateUuid;
  private String name;

  @Nullable
  private String keyPattern;

  @Nullable
  private String description;

  @Nullable
  private String permission;


  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String userUuid;

  @Nullable
  private String userLogin;

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String groupUuid;

  @Nullable
  private String groupName;

  @Nullable
  private Boolean withProjectCreator;

  public PermissionTemplateNewValue(String templateUuid, String name) {
    this.templateUuid = templateUuid;
    this.name = name;
  }

  public PermissionTemplateNewValue(PermissionTemplateDto permissionTemplateDto) {
    this.templateUuid = permissionTemplateDto.getUuid();
    this.name = permissionTemplateDto.getName();
    this.keyPattern = permissionTemplateDto.getKeyPattern();
    this.description = permissionTemplateDto.getDescription();
  }

  public PermissionTemplateNewValue(@Nullable String templateUuid, @Nullable String name, @Nullable String permission,
    @Nullable String userUuid, @Nullable String userLogin, @Nullable String groupUuid, @Nullable String groupName) {
    this.templateUuid = templateUuid;
    this.name = name;
    this.permission = permission;
    this.userUuid = userUuid;
    this.userLogin = userLogin;
    this.groupUuid = groupUuid;
    this.groupName = groupName;
  }

  public PermissionTemplateNewValue(String templateUuid, String permission, String name, boolean withProjectCreator) {
    this.templateUuid = templateUuid;
    this.name = name;
    this.permission = permission;
    this.withProjectCreator = withProjectCreator;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  public String getTemplateUuid() {
    return this.templateUuid;
  }

  public String getName() {
    return this.name;
  }

  @CheckForNull
  public String getKeyPattern() {
    return this.keyPattern;
  }

  @CheckForNull
  public String getDescription() {
    return this.description;
  }

  @CheckForNull
  public String getPermission() {
    return this.permission;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getUserUuid() {
    return this.userUuid;
  }

  @CheckForNull
  public String getUserLogin() {
    return this.userLogin;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getGroupUuid() {
    return this.groupUuid;
  }

  @CheckForNull
  public String getGroupName() {
    return this.groupName;
  }

  @CheckForNull
  public Boolean isWithProjectCreator() {
    return this.withProjectCreator;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"templateUuid\": ", this.templateUuid, true);
    addField(sb, "\"name\": ", this.name, true);
    addField(sb, "\"keyPattern\": ", this.keyPattern, true);
    addField(sb, "\"description\": ", this.description, true);
    addField(sb, "\"permission\": ", this.permission, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    addField(sb, "\"groupUuid\": ", this.groupUuid, true);
    addField(sb, "\"groupName\": ", this.groupName, true);
    addField(sb, "\"withProjectCreator\": ", ObjectUtils.toString(this.withProjectCreator), false);
    endString(sb);
    return sb.toString();
  }
}
