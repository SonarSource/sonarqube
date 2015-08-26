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

package org.sonar.db.permission;

import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class PermissionTemplateDto {

  public static final PermissionTemplateDto DEFAULT = new PermissionTemplateDto()
    .setName("Default template")
    .setKee("default_template")
    .setDescription("This permission template will be used as default when no other permission configuration is available")
    .setCreatedAt(new Date())
    .setUpdatedAt(new Date());

  private Long id;
  private String name;
  private String kee;
  private String description;
  private String keyPattern;
  private List<PermissionTemplateUserDto> usersPermissions;
  private List<PermissionTemplateGroupDto> groupsPermissions;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public PermissionTemplateDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public PermissionTemplateDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getKee() {
    return kee;
  }

  public PermissionTemplateDto setKee(String kee) {
    this.kee = kee;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public PermissionTemplateDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @CheckForNull
  public String getKeyPattern() {
    return keyPattern;
  }

  public PermissionTemplateDto setKeyPattern(@Nullable String regexp) {
    this.keyPattern = regexp;
    return this;
  }

  @CheckForNull
  public List<PermissionTemplateUserDto> getUsersPermissions() {
    return usersPermissions;
  }

  public PermissionTemplateDto setUsersPermissions(@Nullable List<PermissionTemplateUserDto> usersPermissions) {
    this.usersPermissions = usersPermissions;
    return this;
  }

  @CheckForNull
  public List<PermissionTemplateGroupDto> getGroupsPermissions() {
    return groupsPermissions;
  }

  public PermissionTemplateDto setGroupsByPermission(@Nullable List<PermissionTemplateGroupDto> groupsPermissions) {
    this.groupsPermissions = groupsPermissions;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public PermissionTemplateDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public PermissionTemplateDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
