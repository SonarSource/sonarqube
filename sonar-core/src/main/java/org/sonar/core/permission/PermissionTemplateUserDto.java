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

package org.sonar.core.permission;

import java.util.Date;

public class PermissionTemplateUserDto {

  private Long id;
  private Long templateId;
  private Long userId;
  private String permission;
  private String userName;
  private String userLogin;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public PermissionTemplateUserDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public PermissionTemplateUserDto setTemplateId(Long templateId) {
    this.templateId = templateId;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public PermissionTemplateUserDto setUserId(Long userId) {
    this.userId = userId;
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
