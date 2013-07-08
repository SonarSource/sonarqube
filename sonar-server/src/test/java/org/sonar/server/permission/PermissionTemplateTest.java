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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.core.user.PermissionTemplateDto;
import org.sonar.core.user.PermissionTemplateGroupDto;
import org.sonar.core.user.PermissionTemplateUserDto;

import static org.fest.assertions.Assertions.assertThat;

public class PermissionTemplateTest {

  @Test
  public void should_populate_template_with_permissions() throws Exception {

    PermissionTemplateDto permissionTemplateDto = new PermissionTemplateDto()
      .setId(1L)
      .setName("name")
      .setDescription("description")
      .setUsersPermissions(Lists.newArrayList(
        new PermissionTemplateUserDto().setId(1L).setUserId(1L).setUserName("user1").setUserLogin("login1").setPermission("permission1"),
        new PermissionTemplateUserDto().setId(2L).setUserId(1L).setUserName("user1").setUserLogin("login1").setPermission("permission2"),
        new PermissionTemplateUserDto().setId(3L).setUserId(2L).setUserName("user2").setUserLogin("login2").setPermission("permission1")
      ))
      .setGroupsByPermission(Lists.newArrayList(
        new PermissionTemplateGroupDto().setId(1L).setGroupId(1L).setGroupName("group1").setPermission("permission3"),
        new PermissionTemplateGroupDto().setId(2L).setGroupId(2L).setGroupName("group2").setPermission("permission3")
      ));

    PermissionTemplate permissionTemplate = PermissionTemplate.create(permissionTemplateDto);

    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo("name");
    assertThat(permissionTemplate.getDescription()).isEqualTo("description");
    assertThat(permissionTemplate.getUsersForPermission("unmatchedPermission")).isEmpty();
    assertThat(permissionTemplate.getUsersForPermission("permission1")).onProperty("userName").containsOnly("user1", "user2");
    assertThat(permissionTemplate.getUsersForPermission("permission1")).onProperty("userId").containsOnly(1L, 2L);
    assertThat(permissionTemplate.getUsersForPermission("permission1")).onProperty("userLogin").containsOnly("login1", "login2");
    assertThat(permissionTemplate.getUsersForPermission("permission2")).onProperty("userName").containsOnly("user1");
    assertThat(permissionTemplate.getUsersForPermission("permission2")).onProperty("userId").containsOnly(1L);
    assertThat(permissionTemplate.getUsersForPermission("permission2")).onProperty("userLogin").containsOnly("login1");
    assertThat(permissionTemplate.getGroupsForPermission("permission3")).onProperty("groupName").containsOnly("group1", "group2");
    assertThat(permissionTemplate.getGroupsForPermission("permission3")).onProperty("groupId").containsOnly(1L, 2L);
  }
}
