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

import java.util.Date;
import javax.annotation.Nullable;

public class PermissionTemplateGroupDto {
  private Long id;
  private Long templateId;
  private Integer groupId;
  private String permission;
  private String groupName;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public PermissionTemplateGroupDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public PermissionTemplateGroupDto setTemplateId(Long templateId) {
    this.templateId = templateId;
    return this;
  }

  public Integer getGroupId() {
    return groupId;
  }

  public PermissionTemplateGroupDto setGroupId(@Nullable Integer groupId) {
    this.groupId = groupId;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  public PermissionTemplateGroupDto setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public String getGroupName() {
    return groupName;
  }

  public PermissionTemplateGroupDto setGroupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public PermissionTemplateGroupDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public PermissionTemplateGroupDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
