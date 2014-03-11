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

package org.sonar.server.permission;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.permission.PermissionTemplateGroupDto;
import org.sonar.core.permission.PermissionTemplateUserDto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

public class PermissionTemplate {

  private final Long id;
  private final String name;
  private final String key;
  private final String description;
  private final String keyPattern;
  private Multimap<String, PermissionTemplateUser> usersByPermission;
  private Multimap<String, PermissionTemplateGroup> groupsByPermission;

  private PermissionTemplate(Long id, String name, String key, @Nullable String description, @Nullable String keyPattern) {
    this.id = id;
    this.name = name;
    this.key = key;
    this.description = description;
    this.keyPattern = keyPattern;
    usersByPermission = HashMultimap.create();
    groupsByPermission = HashMultimap.create();
  }

  public static PermissionTemplate create(@Nullable PermissionTemplateDto permissionTemplateDto) {
    if (permissionTemplateDto == null) {
      return null;
    }
    PermissionTemplate permissionTemplate = new PermissionTemplate(
      permissionTemplateDto.getId(), permissionTemplateDto.getName(), permissionTemplateDto.getKee(), permissionTemplateDto.getDescription(),
      permissionTemplateDto.getKeyPattern());

    List<PermissionTemplateUserDto> usersPermissions = permissionTemplateDto.getUsersPermissions();
    if (usersPermissions != null) {
      for (PermissionTemplateUserDto userPermission : usersPermissions) {
        permissionTemplate.registerUserPermission(permissionTemplateDto.getId(), userPermission.getUserId(),
          userPermission.getUserName(), userPermission.getUserLogin(), userPermission.getPermission());
      }
    }

    List<PermissionTemplateGroupDto> groupsPermissions = permissionTemplateDto.getGroupsPermissions();
    if (groupsPermissions != null) {
      for (PermissionTemplateGroupDto groupPermission : groupsPermissions) {
        permissionTemplate.registerGroupPermission(groupPermission.getPermission(), permissionTemplateDto.getId(),
          groupPermission.getGroupId(), groupPermission.getGroupName());
      }
    }
    return permissionTemplate;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public String getKey() {
    return key;
  }

  @CheckForNull
  public String getKeyPattern() {
    return keyPattern;
  }

  public List<PermissionTemplateUser> getUsersForPermission(String permission) {
    return ImmutableList.copyOf(usersByPermission.get(permission));
  }

  public List<PermissionTemplateGroup> getGroupsForPermission(String permission) {
    return ImmutableList.copyOf(groupsByPermission.get(permission));
  }

  private void registerUserPermission(Long templateId, Long userId, String userName, String userLogin, String permission) {
    usersByPermission.put(permission, new PermissionTemplateUser(templateId, userId, userName, userLogin, permission));
  }

  private void registerGroupPermission(String permission, Long templateId, Long groupId, String groupName) {
    groupsByPermission.put(permission, new PermissionTemplateGroup(templateId, groupId, groupName, permission));
  }
}
