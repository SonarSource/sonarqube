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
package org.sonar.server.user;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.database.model.User;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class DeprecatedUserFinderTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DeprecatedUserFinder underTest = new DeprecatedUserFinder(db.getDbClient());

  @Test
  public void shouldFindUserByLogin() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    User user = underTest.findByLogin(user1.getLogin());
    assertThat(user.getId()).isEqualTo(user1.getId());
    assertThat(user.getLogin()).isEqualTo(user1.getLogin());
    assertThat(user.getName()).isEqualTo(user1.getName());
    assertThat(user.getEmail()).isEqualTo(user1.getEmail());

    assertThat(underTest.findByLogin("unknown")).isNull();
  }

  @Test
  public void shouldFindUserById() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    User user = underTest.findById(user1.getId());
    assertThat(user.getId()).isEqualTo(user1.getId());
    assertThat(user.getLogin()).isEqualTo(user1.getLogin());
    assertThat(user.getName()).isEqualTo(user1.getName());
    assertThat(user.getEmail()).isEqualTo(user1.getEmail());

    assertThat(underTest.findById(321)).isNull();
  }

}
