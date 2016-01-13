/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.user;

import org.junit.Test;
import org.sonar.core.user.GroupMembership;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupMembershipDtoTest {

  @Test
  public void to_group_with_permission_having_permission() {
    GroupMembership group = new GroupMembershipDto()
      .setId(1L)
      .setName("users")
      .setDescription("description")
      .setUserId(10L)
      .toGroupMembership();

    assertThat(group.id()).isEqualTo(1);
    assertThat(group.name()).isEqualTo("users");
    assertThat(group.description()).isEqualTo("description");
    assertThat(group.isMember()).isTrue();
  }

  @Test
  public void to_group_with_permission_not_having_permission() {
    GroupMembership group = new GroupMembershipDto()
      .setId(1L)
      .setName("users")
      .setDescription("description")
      .setUserId(null)
      .toGroupMembership();

    assertThat(group.id()).isEqualTo(1);
    assertThat(group.name()).isEqualTo("users");
    assertThat(group.description()).isEqualTo("description");
    assertThat(group.isMember()).isFalse();
  }

}
