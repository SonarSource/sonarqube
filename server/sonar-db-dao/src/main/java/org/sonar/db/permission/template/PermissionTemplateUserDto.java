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
package org.sonar.db.permission.template;

import java.util.Date;

public class PermissionTemplateUserDto {
  private String uuid;
  private String templateUuid;
  private String userUuid;
  private String permission;
  private String userName;
  private String userLogin;
  private Date createdAt;
  private Date updatedAt;

  public String getUuid() {
    return uuid;
  }

  public PermissionTemplateUserDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getTemplateUuid() {
    return templateUuid;
  }

  public PermissionTemplateUserDto setTemplateUuid(String templateUuid) {
    this.templateUuid = templateUuid;
    return this;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public PermissionTemplateUserDto setUserUuid(String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public String getUserName() {
    return userName;
  }

  public PermissionTemplateUserDto setUserName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public PermissionTemplateUserDto setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  public PermissionTemplateUserDto setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public PermissionTemplateUserDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public PermissionTemplateUserDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
