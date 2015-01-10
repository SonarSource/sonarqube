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

public class GroupWithPermissionTest {

  @Test
  public void test_setters_and_getters() throws Exception {
    GroupWithPermission user = new GroupWithPermission()
      .setName("users")
      .hasPermission(true);

    assertThat(user.name()).isEqualTo("users");
    assertThat(user.hasPermission()).isTrue();
  }

  @Test
  public void test_equals() throws Exception {
    assertThat(new GroupWithPermission().setName("users")).isEqualTo(new GroupWithPermission().setName("users"));
    assertThat(new GroupWithPermission().setName("users")).isNotEqualTo(new GroupWithPermission().setName("reviewers"));

    GroupWithPermission group = new GroupWithPermission()
      .setName("users")
      .hasPermission(true);
    assertThat(group).isEqualTo(group);
  }

  @Test
  public void test_hashcode() throws Exception {
    assertThat(new GroupWithPermission().setName("users").hashCode()).isEqualTo(new GroupWithPermission().setName("users").hashCode());
    assertThat(new GroupWithPermission().setName("users").hashCode()).isNotEqualTo(new GroupWithPermission().setName("reviewers").hashCode());
  }

}
