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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupWithPermissionDtoTest {

  @Test
  public void to_group_with_permission_having_permission() throws Exception {
    GroupWithPermission group = new GroupWithPermissionDto()
      .setName("users")
      .setDescription("desc")
      .setPermission("user")
      .toGroupWithPermission();

    assertThat(group.name()).isEqualTo("users");
    assertThat(group.description()).isEqualTo("desc");
    assertThat(group.hasPermission()).isTrue();
  }

  @Test
  public void to_group_with_permission_not_having_permission() throws Exception {
    GroupWithPermission group = new GroupWithPermissionDto()
      .setName("users")
      .setPermission(null)
      .toGroupWithPermission();

    assertThat(group.name()).isEqualTo("users");
    assertThat(group.description()).isNull();
    assertThat(group.hasPermission()).isFalse();
  }
}
