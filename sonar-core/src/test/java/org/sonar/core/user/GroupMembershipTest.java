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
package org.sonar.core.user;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupMembershipTest {

  @Test
  public void test_setters_and_getters() throws Exception {
    GroupMembership group = new GroupMembership()
      .setId(1L)
      .setName("users")
      .setMember(true);

    assertThat(group.id()).isEqualTo(1L);
    assertThat(group.name()).isEqualTo("users");
    assertThat(group.isMember()).isTrue();
  }

  @Test
  public void test_equals() throws Exception {
    assertThat(new GroupMembership().setName("users")).isEqualTo(new GroupMembership().setName("users"));
    assertThat(new GroupMembership().setName("users")).isNotEqualTo(new GroupMembership().setName("reviewers"));

    GroupMembership group = new GroupMembership()
      .setId(1L)
      .setName("users")
      .setMember(true);
    assertThat(group).isEqualTo(group);
  }
}
