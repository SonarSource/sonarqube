/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.user;

import org.junit.Test;
import org.sonar.core.user.DefaultUser;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultUserTest {
  @Test
  public void test_object_methods() {
    DefaultUser john = new DefaultUser().setLogin("john").setName("John");
    DefaultUser eric = new DefaultUser().setLogin("eric").setName("Eric");

    assertThat(john)
      .isEqualTo(john)
      .isNotEqualTo(eric)
      .hasSameHashCodeAs(john);
    assertThat(john.toString()).contains("login=john").contains("name=John");
  }

  @Test
  public void test_email() {
    DefaultUser user = new DefaultUser();
    assertThat(user.email()).isNull();

    user.setEmail("");
    assertThat(user.email()).isNull();

    user.setEmail("  ");
    assertThat(user.email()).isNull();

    user.setEmail("s@b.com");
    assertThat(user.email()).isEqualTo("s@b.com");
  }
}
