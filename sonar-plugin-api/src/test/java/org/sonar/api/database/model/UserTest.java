/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.database.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

  @Test
  public void testToString() {
    User user = new User()
      .setEmail("super@m.an")
      .setLogin("superman")
      .setName("Superman");
    assertThat(user.toString()).contains("super@m.an");
    assertThat(user.toString()).contains("superman");
    assertThat(user.toString()).contains("Superman");
  }

  @Test
  public void testEquals() {
    User one = new User()
      .setLogin("one")
      .setName("One");

    User two = new User()
      .setLogin("two")
      .setName("Two");

    assertThat(one.equals(one)).isTrue();
    assertThat(one.equals(new User().setLogin("one"))).isTrue();
    assertThat(one.equals(two)).isFalse();

    assertThat(one.hashCode()).isEqualTo(new User().setLogin("one").hashCode());
  }
}
