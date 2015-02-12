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

package org.sonar.server.user.db;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.test.DbTests;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class UserDaoTest {

  @ClassRule
  public static DbTester db = new DbTester();

  private UserDao dao;
  private DbSession session;

  @Before
  public void before() throws Exception {
    db.truncateTables();

    this.session = db.myBatis().openSession(false);
    System2 system2 = mock(System2.class);
    this.dao = new UserDao(db.myBatis(), system2);
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void select_by_login() {
    db.prepareDbUnit(getClass(), "select_by_login.xml");

    UserDto dto = dao.selectByLogin(session, "marius");
    assertThat(dto.getId()).isEqualTo(101);
    assertThat(dto.getLogin()).isEqualTo("marius");
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma","marius33");
    assertThat(dto.getSalt()).isEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(dto.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(dto.getUpdatedAt()).isEqualTo(1418215735485L);
  }

  @Test
  public void select_nullable_by_scm_account() {
    db.prepareDbUnit(getClass(), "select_nullable_by_scm_account.xml");

    List<UserDto> results = dao.selectNullableByScmAccountOrLoginOrEmail(session, "ma");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = dao.selectNullableByScmAccountOrLoginOrEmail(session, "marius");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = dao.selectNullableByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = dao.selectNullableByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = dao.selectNullableByScmAccountOrLoginOrEmail(session, "m");
    assertThat(results).isEmpty();

    results = dao.selectNullableByScmAccountOrLoginOrEmail(session, "unknown");
    assertThat(results).isEmpty();
  }

  @Test
  public void select_nullable_by_scm_account_return_many_results_when_same_email_is_used_by_many_users() {
    db.prepareDbUnit(getClass(), "select_nullable_by_scm_account_return_many_results_when_same_email_is_used_by_many_users.xml");

    List<UserDto> results = dao.selectNullableByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(2);
  }

  @Test
  public void select_by_login_with_unknown_login() {
    try {
      dao.selectByLogin(session, "unknown");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("User with login 'unknown' has not been found");
    }
  }

  @Test
  public void select_nullable_by_login() {
    db.prepareDbUnit(getClass(), "select_by_login.xml");

    assertThat(dao.selectNullableByLogin(session, "marius")).isNotNull();

    assertThat(dao.selectNullableByLogin(session, "unknown")).isNull();
  }

}
