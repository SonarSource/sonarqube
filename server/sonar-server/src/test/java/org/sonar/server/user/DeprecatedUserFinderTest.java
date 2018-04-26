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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.sonar.db.DbTester.create;

public class DeprecatedUserFinderTest {

  @Rule
  public DbTester dbTester = create(System2.INSTANCE);
  private DeprecatedUserFinder underTest = new DeprecatedUserFinder(dbTester.getDbClient());

  @Test
  public void shouldFindUserByLogin() {

    UserDto simon = dbTester.users().insertUser(u -> u.setLogin("simon").setName("Simon Brandhof").setEmail("simon.brandhof@sonarsource.com"));
    UserDto evgeny = dbTester.users().insertUser(u -> u.setLogin("godin").setName("Evgeny Mandrikov").setEmail("evgeny.mandrikov@sonarsource.com"));

    User user = underTest.findByLogin(simon.getLogin());
    assertThat(user.getId(), is(simon.getId()));
    assertThat(user.getLogin(), is("simon"));
    assertThat(user.getName(), is("Simon Brandhof"));
    assertThat(user.getEmail(), is("simon.brandhof@sonarsource.com"));

    user = underTest.findByLogin(evgeny.getLogin());
    assertThat(user.getId(), is(evgeny.getId()));
    assertThat(user.getLogin(), is("godin"));
    assertThat(user.getName(), is("Evgeny Mandrikov"));
    assertThat(user.getEmail(), is("evgeny.mandrikov@sonarsource.com"));

    user = underTest.findByLogin("user");
    assertThat(user, nullValue());
  }

  @Test
  public void shouldFindUserById() {
    UserDto simon = dbTester.users().insertUser(u -> u.setLogin("simon").setName("Simon Brandhof").setEmail("simon.brandhof@sonarsource.com"));
    UserDto evgeny = dbTester.users().insertUser(u -> u.setLogin("godin").setName("Evgeny Mandrikov").setEmail("evgeny.mandrikov@sonarsource.com"));

    User user = underTest.findById(simon.getId());
    assertThat(user.getId(), is(simon.getId()));
    assertThat(user.getLogin(), is("simon"));
    assertThat(user.getName(), is("Simon Brandhof"));
    assertThat(user.getEmail(), is("simon.brandhof@sonarsource.com"));

    user = underTest.findById(evgeny.getId());
    assertThat(user.getId(), is(evgeny.getId()));
    assertThat(user.getLogin(), is("godin"));
    assertThat(user.getName(), is("Evgeny Mandrikov"));
    assertThat(user.getEmail(), is("evgeny.mandrikov@sonarsource.com"));

    user = underTest.findById(999);
    assertThat(user, nullValue());
  }

}
