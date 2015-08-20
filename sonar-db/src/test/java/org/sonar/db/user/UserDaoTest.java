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
package org.sonar.db.user;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.test.DbTests;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class UserDaoTest {

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  UserDao underTest = db.getDbClient().userDao();
  DbSession session;

  @Before
  public void before() {
    db.truncateTables();

    this.session = db.getSession();
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void selectUserByLogin_ignore_inactive() {
    db.prepareDbUnit(getClass(), "selectActiveUserByLogin.xml");

    UserDto user = underTest.selectUserById(50);
    assertThat(user.getLogin()).isEqualTo("inactive_user");

    user = underTest.selectActiveUserByLogin("inactive_user");
    assertThat(user).isNull();
  }

  @Test
  public void selectUserByLogin_not_found() {
    db.prepareDbUnit(getClass(), "selectActiveUserByLogin.xml");

    UserDto user = underTest.selectActiveUserByLogin("not_found");
    assertThat(user).isNull();
  }

  @Test
  public void selectUsersByLogins() {
    db.prepareDbUnit(getClass(), "selectUsersByLogins.xml");

    Collection<UserDto> users = underTest.selectByLogins(asList("marius", "inactive_user", "other"));
    assertThat(users).hasSize(2);
    assertThat(users).extracting("login").containsOnly("marius", "inactive_user");
  }

  @Test
  public void selectUsersByLogins_empty_logins() {
    db.truncateTables();

    // no need to access db
    Collection<UserDto> users = underTest.selectByLogins(Collections.<String>emptyList());
    assertThat(users).isEmpty();
  }

  @Test
  public void selectUsersByQuery_all() {
    db.prepareDbUnit(getClass(), "selectUsersByQuery.xml");

    UserQuery query = UserQuery.builder().includeDeactivated().build();
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(2);
  }

  @Test
  public void selectUsersByQuery_only_actives() {
    db.prepareDbUnit(getClass(), "selectUsersByQuery.xml");

    UserQuery query = UserQuery.ALL_ACTIVES;
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getName()).isEqualTo("Marius");
  }

  @Test
  public void selectUsersByQuery_filter_by_login() {
    db.prepareDbUnit(getClass(), "selectUsersByQuery.xml");

    UserQuery query = UserQuery.builder().logins("marius", "john").build();
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getName()).isEqualTo("Marius");
  }

  @Test
  public void selectUsersByQuery_search_by_login_text() {
    db.prepareDbUnit(getClass(), "selectUsersByText.xml");

    UserQuery query = UserQuery.builder().searchText("sbr").build();
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getLogin()).isEqualTo("sbrandhof");
  }

  @Test
  public void selectUsersByQuery_search_by_name_text() {
    db.prepareDbUnit(getClass(), "selectUsersByText.xml");

    UserQuery query = UserQuery.builder().searchText("Simon").build();
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getLogin()).isEqualTo("sbrandhof");
  }

  @Test
  public void selectUsersByQuery_escape_special_characters_in_like() {
    db.prepareDbUnit(getClass(), "selectUsersByText.xml");

    UserQuery query = UserQuery.builder().searchText("%s%").build();
    // we expect really a login or name containing the 3 characters "%s%"

    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).isEmpty();
  }

  @Test
  public void insert_user() {
    Long date = DateUtils.parseDate("2014-06-20").getTime();

    UserDto userDto = new UserDto()
      .setId(1L)
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setScmAccounts(",jo.hn,john2,")
      .setActive(true)
      .setSalt("1234")
      .setCryptedPassword("abcd")
      .setCreatedAt(date)
      .setUpdatedAt(date);
    underTest.insert(db.getSession(), userDto);
    db.getSession().commit();

    UserDto user = underTest.selectActiveUserByLogin("john");
    assertThat(user).isNotNull();
    assertThat(user.getId()).isNotNull();
    assertThat(user.getLogin()).isEqualTo("john");
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("jo@hn.com");
    assertThat(user.isActive()).isTrue();
    assertThat(user.getScmAccounts()).isEqualTo(",jo.hn,john2,");
    assertThat(user.getSalt()).isEqualTo("1234");
    assertThat(user.getCryptedPassword()).isEqualTo("abcd");
    assertThat(user.getCreatedAt()).isEqualTo(date);
    assertThat(user.getUpdatedAt()).isEqualTo(date);
  }

  @Test
  public void update_user() {
    db.prepareDbUnit(getClass(), "update_user.xml");

    Long date = DateUtils.parseDate("2014-06-21").getTime();

    UserDto userDto = new UserDto()
      .setId(1L)
      .setLogin("john")
      .setName("John Doo")
      .setEmail("jodoo@hn.com")
      .setScmAccounts(",jo.hn,john2,johndoo,")
      .setActive(false)
      .setSalt("12345")
      .setCryptedPassword("abcde")
      .setUpdatedAt(date);
    underTest.update(db.getSession(), userDto);
    db.getSession().commit();

    UserDto user = underTest.selectUserById(1);
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo(1L);
    assertThat(user.getLogin()).isEqualTo("john");
    assertThat(user.getName()).isEqualTo("John Doo");
    assertThat(user.getEmail()).isEqualTo("jodoo@hn.com");
    assertThat(user.isActive()).isFalse();
    assertThat(user.getScmAccounts()).isEqualTo(",jo.hn,john2,johndoo,");
    assertThat(user.getSalt()).isEqualTo("12345");
    assertThat(user.getCryptedPassword()).isEqualTo("abcde");
    assertThat(user.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(user.getUpdatedAt()).isEqualTo(date);
  }

  @Test
  public void deactivate_user() {
    db.prepareDbUnit(getClass(), "deactivate_user.xml");

    when(system2.now()).thenReturn(1500000000000L);

    String login = "marius";
    boolean deactivated = underTest.deactivateUserByLogin(login);
    assertThat(deactivated).isTrue();

    assertThat(underTest.selectActiveUserByLogin(login)).isNull();

    UserDto userDto = underTest.selectUserById(100);
    assertThat(userDto.isActive()).isFalse();
    assertThat(userDto.getUpdatedAt()).isEqualTo(1500000000000L);

    db.assertDbUnit(getClass(), "deactivate_user-result.xml",
      "dashboards", "active_dashboards", "groups_users", "issue_filters",
      "issue_filter_favourites", "measure_filters", "measure_filter_favourites",
      "properties", "user_roles");
  }

  @Test
  public void deactivate_missing_user() {
    db.prepareDbUnit(getClass(), "deactivate_user.xml");

    String login = "does_not_exist";
    boolean deactivated = underTest.deactivateUserByLogin(login);
    assertThat(deactivated).isFalse();
    assertThat(underTest.selectActiveUserByLogin(login)).isNull();
  }

  @Test
  public void select_by_login() {
    db.prepareDbUnit(getClass(), "select_by_login.xml");

    UserDto dto = underTest.selectOrFailByLogin(session, "marius");
    assertThat(dto.getId()).isEqualTo(101);
    assertThat(dto.getLogin()).isEqualTo("marius");
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getSalt()).isEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(dto.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(dto.getUpdatedAt()).isEqualTo(1418215735485L);
  }

  @Test
  public void select_nullable_by_scm_account() {
    db.prepareDbUnit(getClass(), "select_nullable_by_scm_account.xml");

    List<UserDto> results = underTest.selectByScmAccountOrLoginOrEmail(session, "ma");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "m");
    assertThat(results).isEmpty();

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "unknown");
    assertThat(results).isEmpty();
  }

  @Test
  public void select_nullable_by_scm_account_return_many_results_when_same_email_is_used_by_many_users() {
    db.prepareDbUnit(getClass(), "select_nullable_by_scm_account_return_many_results_when_same_email_is_used_by_many_users.xml");

    List<UserDto> results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(2);
  }

  @Test
  public void select_by_login_with_unknown_login() {
    try {
      underTest.selectOrFailByLogin(session, "unknown");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(RowNotFoundException.class).hasMessage("User with login 'unknown' has not been found");
    }
  }

  @Test
  public void select_nullable_by_login() {
    db.prepareDbUnit(getClass(), "select_by_login.xml");

    assertThat(underTest.selectByLogin(session, "marius")).isNotNull();

    assertThat(underTest.selectByLogin(session, "unknown")).isNull();
  }
}
