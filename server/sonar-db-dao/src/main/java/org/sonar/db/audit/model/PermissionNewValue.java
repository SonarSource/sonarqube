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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public abstract class PermissionNewValue extends NewValue {
  @Nullable
  protected String permissionUuid;

  @Nullable
  protected String componentUuid;

  @Nullable
  protected String componentName;

  @Nullable
  protected String role;

  @Nullable
  protected String qualifier;

  protected PermissionNewValue(@Nullable String permissionUuid, @Nullable String componentUuid, @Nullable String componentName,
    @Nullable String role, @Nullable String qualifier) {
    this.permissionUuid = permissionUuid;
    this.componentUuid = componentUuid;
    this.componentName = componentName;
    this.qualifier = getQualifier(qualifier);
    this.role = role;
  }

  @CheckForNull
  public String getPermissionUuid() {
    return this.permissionUuid;
  }

  @CheckForNull
  public String getComponentUuid() {
    return this.componentUuid;
  }

  @CheckForNull
  public String getRole() {
    return this.role;
  }

  @CheckForNull
  public String getComponentName() {
    return this.componentName;
  }

  @CheckForNull
  public String getQualifier() {
    return this.qualifier;
  }
}
