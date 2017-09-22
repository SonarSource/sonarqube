/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.issue.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTesting.newUserDto;

public class AvatarResolverImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AvatarResolverImpl underTest = new AvatarResolverImpl();

  @Test
  public void create() throws Exception {
    String avatar = underTest.create(newUserDto("john", "John", "john@doo.com"));

    assertThat(avatar).isEqualTo("9297bfb538f650da6143b604e82a355d");
  }

  @Test
  public void create_when_empty_email() throws Exception {
    String avatar = underTest.create(newUserDto("john", "John", ""));

    assertThat(avatar).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
  }

  @Test
  public void create_is_case_insensitive() throws Exception {
    assertThat(underTest.create(newUserDto("john", "John", "john@doo.com"))).isEqualTo(underTest.create(newUserDto("john", "John", "John@Doo.com")));
  }

  @Test
  public void fail_with_NP_when_user_is_null() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("User cannot be null");

    underTest.create(null);
  }

  @Test
  public void fail_with_NP_when_email_is_null() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Email cannot be null");

    underTest.create(newUserDto("john", "John", null));
  }
}
