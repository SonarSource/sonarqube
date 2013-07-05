/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.core.user.PermissionTemplateDto;
import org.sonar.core.user.PermissionTemplateGroupDto;
import org.sonar.core.user.PermissionTemplateUserDto;

import java.util.List;

public class PermissionTemplate {

  private final Long id;
  private final String name;
  private final String description;
  private Multimap<String, String> usersByPermission;
  private Multimap<String, String> groupsByPermission;

  private PermissionTemplate(Long id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
    usersByPermission = HashMultimap.create();
    groupsByPermission = HashMultimap.create();
  }

  public static PermissionTemplate create(PermissionTemplateDto permissionTemplateDto) {
    PermissionTemplate permissionTemplate =
      new PermissionTemplate(permissionTemplateDto.getId(), permissionTemplateDto.getName(), permissionTemplateDto.getDescription());
    if(permissionTemplateDto.getUsersPermissions() != null) {
      for (PermissionTemplateUserDto userPermission : permissionTemplateDto.getUsersPermissions()) {
        permissionTemplate.registerUserPermission(userPermission.getPermission(), userPermission.getUserName());
      }
    }
    if(permissionTemplateDto.getGroupsPermissions() != null) {
      for (PermissionTemplateGroupDto groupPermission : permissionTemplateDto.getGroupsPermissions()) {
        permissionTemplate.registerGroupPermission(groupPermission.getPermission(), groupPermission.getGroupName());
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

  public String getDescription() {
    return description;
  }

  public List<String> getUsersForPermission(String permission) {
    return ImmutableList.copyOf(usersByPermission.get(permission));
  }

  public List<String> getGroupsForPermission(String permission) {
    return ImmutableList.copyOf(groupsByPermission.get(permission));
  }

  private void registerUserPermission(String permission, String userName) {
    usersByPermission.put(permission, userName);
  }

  private void registerGroupPermission(String permission, String groupName) {
    groupsByPermission.put(permission, groupName);
  }
}
