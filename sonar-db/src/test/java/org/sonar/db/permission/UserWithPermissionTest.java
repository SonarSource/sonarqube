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

import org.junit.Test;
import org.sonar.core.permission.UserWithPermission;

import static org.assertj.core.api.Assertions.assertThat;

public class UserWithPermissionTest {

  @Test
  public void test_setters_and_getters() throws Exception {
    UserWithPermission user = new UserWithPermission()
      .setName("Arthur")
      .setLogin("arthur")
      .hasPermission(true);

    assertThat(user.name()).isEqualTo("Arthur");
    assertThat(user.login()).isEqualTo("arthur");
    assertThat(user.hasPermission()).isTrue();
  }

  @Test
  public void test_equals() throws Exception {
    assertThat(new UserWithPermission().setLogin("arthur")).isEqualTo(new UserWithPermission().setLogin("arthur"));
    assertThat(new UserWithPermission().setLogin("arthur")).isNotEqualTo(new UserWithPermission().setLogin("john"));

    UserWithPermission user = new UserWithPermission()
      .setName("Arthur")
      .setLogin("arthur")
      .hasPermission(true);
    assertThat(user).isEqualTo(user);
  }

  @Test
  public void test_hashcode() throws Exception {
    assertThat(new UserWithPermission().setLogin("arthur").hashCode()).isEqualTo(new UserWithPermission().setLogin("arthur").hashCode());
    assertThat(new UserWithPermission().setLogin("arthur").hashCode()).isNotEqualTo(new UserWithPermission().setLogin("john").hashCode());
  }

}
