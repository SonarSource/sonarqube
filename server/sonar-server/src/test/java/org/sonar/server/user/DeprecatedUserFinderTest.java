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
package org.sonar.server.user;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.database.model.User;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDao;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;


public class DeprecatedUserFinderTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Before
  public void init() {
    dbTester.prepareDbUnit(DeprecatedUserFinderTest.class, "fixture.xml");
  }

  @Test
  public void shouldFindUserByLogin() {
    DeprecatedUserFinder finder = new DeprecatedUserFinder(new UserDao(dbTester.myBatis(), mock(System2.class)));
    User user = finder.findByLogin("simon");
    assertThat(user.getId(), is(1));
    assertThat(user.getLogin(), is("simon"));
    assertThat(user.getName(), is("Simon Brandhof"));
    assertThat(user.getEmail(), is("simon.brandhof@sonarsource.com"));

    user = finder.findByLogin("godin");
    assertThat(user.getId(), is(2));
    assertThat(user.getLogin(), is("godin"));
    assertThat(user.getName(), is("Evgeny Mandrikov"));
    assertThat(user.getEmail(), is("evgeny.mandrikov@sonarsource.com"));

    user = finder.findByLogin("user");
    assertThat(user, nullValue());
  }

  @Test
  public void shouldFindUserById() {
    DeprecatedUserFinder finder = new DeprecatedUserFinder(new UserDao(dbTester.myBatis(), mock(System2.class)));
    User user = finder.findById(1);
    assertThat(user.getId(), is(1));
    assertThat(user.getLogin(), is("simon"));
    assertThat(user.getName(), is("Simon Brandhof"));
    assertThat(user.getEmail(), is("simon.brandhof@sonarsource.com"));

    user = finder.findById(2);
    assertThat(user.getId(), is(2));
    assertThat(user.getLogin(), is("godin"));
    assertThat(user.getName(), is("Evgeny Mandrikov"));
    assertThat(user.getEmail(), is("evgeny.mandrikov@sonarsource.com"));

    user = finder.findById(3);
    assertThat(user, nullValue());
  }

}
