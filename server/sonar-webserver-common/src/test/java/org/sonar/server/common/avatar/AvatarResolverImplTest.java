/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common.avatar;

import org.junit.Test;
import org.sonar.db.user.UserTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AvatarResolverImplTest {

  private AvatarResolverImpl underTest = new AvatarResolverImpl();

  @Test
  public void create() {
    String avatar = underTest.create(UserTesting.newUserDto("john", "John", "john@doo.com"));

    assertThat(avatar).isEqualTo("9297bfb538f650da6143b604e82a355d");
  }

  @Test
  public void create_is_case_insensitive() {
    assertThat(underTest.create(UserTesting.newUserDto("john", "John", "john@doo.com"))).isEqualTo(underTest.create(UserTesting.newUserDto("john", "John", "John@Doo.com")));
  }

  @Test
  public void fail_with_NP_when_user_is_null() {
    assertThatThrownBy(() -> underTest.create(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("User cannot be null");
  }

  @Test
  public void fail_with_NP_when_email_is_null() {
    assertThatThrownBy(() -> underTest.create(UserTesting.newUserDto("john", "John", null)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Email cannot be null");
  }

  @Test
  public void fail_when_email_is_empty() {
    assertThatThrownBy(() -> underTest.create(UserTesting.newUserDto("john", "John", "")))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Email cannot be null");
  }
}
