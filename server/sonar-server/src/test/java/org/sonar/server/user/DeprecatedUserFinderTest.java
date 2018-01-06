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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.database.model.User;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


public class DeprecatedUserFinderTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  private DeprecatedUserFinder underTest = new DeprecatedUserFinder(dbTester.getDbClient());
  
  @Before
  public void init() {
    dbTester.prepareDbUnit(DeprecatedUserFinderTest.class, "fixture.xml");
  }

  @Test
  public void shouldFindUserByLogin() {
    
    User user = underTest.findByLogin("simon");
    assertThat(user.getId(), is(1));
    assertThat(user.getLogin(), is("simon"));
    assertThat(user.getName(), is("Simon Brandhof"));
    assertThat(user.getEmail(), is("simon.brandhof@sonarsource.com"));

    user = underTest.findByLogin("godin");
    assertThat(user.getId(), is(2));
    assertThat(user.getLogin(), is("godin"));
    assertThat(user.getName(), is("Evgeny Mandrikov"));
    assertThat(user.getEmail(), is("evgeny.mandrikov@sonarsource.com"));

    user = underTest.findByLogin("user");
    assertThat(user, nullValue());
  }

  @Test
  public void shouldFindUserById() {
    User user = underTest.findById(1);
    assertThat(user.getId(), is(1));
    assertThat(user.getLogin(), is("simon"));
    assertThat(user.getName(), is("Simon Brandhof"));
    assertThat(user.getEmail(), is("simon.brandhof@sonarsource.com"));

    user = underTest.findById(2);
    assertThat(user.getId(), is(2));
    assertThat(user.getLogin(), is("godin"));
    assertThat(user.getName(), is("Evgeny Mandrikov"));
    assertThat(user.getEmail(), is("evgeny.mandrikov@sonarsource.com"));

    user = underTest.findById(3);
    assertThat(user, nullValue());
  }

}
