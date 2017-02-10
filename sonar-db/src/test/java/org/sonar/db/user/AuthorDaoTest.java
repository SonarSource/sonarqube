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
package org.sonar.db.user;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class AuthorDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  DbSession dbSession = dbTester.getSession();
  AuthorDao dao = dbTester.getDbClient().authorDao();

  @After
  public void tearDown() throws Exception {
    dbSession.close();
  }

  @Test
  public void shouldSelectByLogin() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectByLogin.xml");
    dbSession.commit();

    AuthorDto authorDto = dao.selectByLogin(dbSession, "godin");
    assertThat(authorDto.getId()).isEqualTo(1L);
    assertThat(authorDto.getPersonId()).isEqualTo(13L);
    assertThat(authorDto.getLogin()).isEqualTo("godin");

    assertThat(dao.selectByLogin(dbSession, "simon")).isNull();
  }

  @Test
  public void shouldInsertAuthor() {
    dbTester.prepareDbUnit(getClass(), "shouldInsertAuthor.xml");
    dbSession.commit();

    dao.insertAuthor(dbSession, "godin", 13L);
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "shouldInsertAuthor-result.xml", new String[] {"created_at", "updated_at"}, "authors");
  }

  @Test
  public void countDeveloperLogins() {
    dbTester.prepareDbUnit(getClass(), "countDeveloperLogins.xml");
    dbSession.commit();

    assertThat(dao.countDeveloperLogins(dbSession, 1L)).isEqualTo(2);
    assertThat(dao.countDeveloperLogins(dbSession, 98765L)).isEqualTo(0);
  }

  @Test
  public void shouldPreventAuthorsDuplication() {
    dbTester.prepareDbUnit(getClass(), "shouldPreventAuthorsDuplication.xml");
    dbSession.commit();

    expectedException.expect(RuntimeException.class);

    try {
      dao.insertAuthor(dbSession, "godin", 20L);
    } catch (RuntimeException ex) {
      dbSession.commit();
      dbTester.assertDbUnit(getClass(), "shouldPreventAuthorsDuplication-result.xml", new String[] {"created_at", "updated_at"}, "authors");
      throw ex;
    }
  }

}
