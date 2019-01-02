/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.permission.template;

import static com.google.common.base.Preconditions.checkArgument;

public class PermissionTemplateCharacteristicDto {

  private static final int MAX_PERMISSION_KEY_LENGTH = 64;

  private Long id;
  private long templateId;
  private String permission;
  private boolean withProjectCreator;
  private long createdAt;
  private long updatedAt;

  public Long getId() {
    return id;
  }

  public PermissionTemplateCharacteristicDto setId(Long id) {
    this.id = id;
    return this;
  }

  public long getTemplateId() {
    return templateId;
  }

  public PermissionTemplateCharacteristicDto setTemplateId(long templateId) {
    this.templateId = templateId;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  public PermissionTemplateCharacteristicDto setPermission(String permission) {
    checkArgument(permission.length() <= MAX_PERMISSION_KEY_LENGTH, "Permission key length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      permission.length(), MAX_PERMISSION_KEY_LENGTH, permission);
    this.permission = permission;
    return this;
  }

  public boolean getWithProjectCreator() {
    return withProjectCreator;
  }

  public PermissionTemplateCharacteristicDto setWithProjectCreator(boolean withProjectCreator) {
    this.withProjectCreator = withProjectCreator;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public PermissionTemplateCharacteristicDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public PermissionTemplateCharacteristicDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
