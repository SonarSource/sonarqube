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
import org.sonar.db.permission.template.PermissionTemplateDto;

public abstract class PermissionNewValue extends NewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  protected String permissionUuid;

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  protected String componentUuid;

  @Nullable
  protected String componentKey;

  @Nullable
  protected String componentName;

  @Nullable
  protected String permission;

  @Nullable
  protected String qualifier;

  @Nullable
  protected String permissionTemplateId;

  @Nullable
  protected String permissionTemplateName;

  protected PermissionNewValue(@Nullable String permissionUuid, @Nullable String componentUuid, @Nullable String componentKey,
    @Nullable String componentName, @Nullable String permission, @Nullable String qualifier, @Nullable PermissionTemplateDto permissionTemplateDto) {
    this.permissionUuid = permissionUuid;
    this.componentUuid = componentUuid;
    this.componentKey = componentKey;
    this.componentName = componentName;
    this.qualifier = qualifier;
    this.permission = permission;
    this.permissionTemplateId = permissionTemplateDto == null ? null : permissionTemplateDto.getUuid();
    this.permissionTemplateName = permissionTemplateDto == null ? null : permissionTemplateDto.getName();
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getPermissionUuid() {
    return this.permissionUuid;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getComponentUuid() {
    return this.componentUuid;
  }

  @CheckForNull
  public String getPermission() {
    return this.permission;
  }

  @CheckForNull
  public String getComponentName() {
    return this.componentName;
  }

  @CheckForNull
  public String getComponentKey() {
    return this.componentKey;
  }

  @CheckForNull
  public String getQualifier() {
    return this.qualifier;
  }

  @CheckForNull
  public String getPermissionTemplateId() {
    return this.permissionTemplateId;
  }

  @CheckForNull
  public String getPermissionTemplateName() {
    return this.permissionTemplateName;
  }

}
