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
package org.sonar.core.user;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserDaoTest extends AbstractDaoTestCase {

  UserDao dao;

  System2 system2;

  DbSession session;

  @Before
  public void setUp() {
    session = getMyBatis().openSession(false);
    system2 = mock(System2.class);
    dao = new UserDao(getMyBatis(), system2);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void selectUserByLogin_ignore_inactive() {
    setupData("selectActiveUserByLogin");

    UserDto user = dao.getUser(50);
    assertThat(user.getLogin()).isEqualTo("inactive_user");

    user = dao.selectActiveUserByLogin("inactive_user");
    assertThat(user).isNull();
  }

  @Test
  public void selectUserByLogin_not_found() {
    setupData("selectActiveUserByLogin");

    UserDto user = dao.selectActiveUserByLogin("not_found");
    assertThat(user).isNull();
  }

  @Test
  public void selectUsersByLogins() throws Exception {
    setupData("selectUsersByLogins");

    Collection<UserDto> users = dao.selectUsersByLogins(Arrays.asList("marius", "inactive_user", "other"));
    assertThat(users).hasSize(2);
    assertThat(users).extracting("login").containsOnly("marius", "inactive_user");
  }

  @Test
  public void selectUsersByLogins_empty_logins() throws Exception {
    // no need to access db
    Collection<UserDto> users = dao.selectUsersByLogins(Collections.<String>emptyList());
    assertThat(users).isEmpty();
  }

  @Test
  public void selectUsersByQuery_all() throws Exception {
    setupData("selectUsersByQuery");

    UserQuery query = UserQuery.builder().includeDeactivated().build();
    List<UserDto> users = dao.selectUsers(query);
    assertThat(users).hasSize(2);
  }

  @Test
  public void selectUsersByQuery_only_actives() throws Exception {
    setupData("selectUsersByQuery");

    UserQuery query = UserQuery.ALL_ACTIVES;
    List<UserDto> users = dao.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getName()).isEqualTo("Marius");
  }

  @Test
  public void selectUsersByQuery_filter_by_login() throws Exception {
    setupData("selectUsersByQuery");

    UserQuery query = UserQuery.builder().logins("marius", "john").build();
    List<UserDto> users = dao.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getName()).isEqualTo("Marius");
  }

  @Test
  public void selectUsersByQuery_search_by_login_text() throws Exception {
    setupData("selectUsersByText");

    UserQuery query = UserQuery.builder().searchText("sbr").build();
    List<UserDto> users = dao.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getLogin()).isEqualTo("sbrandhof");
  }

  @Test
  public void selectUsersByQuery_search_by_name_text() throws Exception {
    setupData("selectUsersByText");

    UserQuery query = UserQuery.builder().searchText("Simon").build();
    List<UserDto> users = dao.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getLogin()).isEqualTo("sbrandhof");
  }

  @Test
  public void selectUsersByQuery_escape_special_characters_in_like() throws Exception {
    setupData("selectUsersByText");

    UserQuery query = UserQuery.builder().searchText("%s%").build();
    // we expect really a login or name containing the 3 characters "%s%"

    List<UserDto> users = dao.selectUsers(query);
    assertThat(users).isEmpty();
  }

  @Test
  public void selectGroupByName() {
    setupData("selectGroupByName");

    GroupDto group = dao.selectGroupByName("sonar-users");
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo(1L);
    assertThat(group.getName()).isEqualTo("sonar-users");
    assertThat(group.getDescription()).isEqualTo("Sonar Users");
    assertThat(group.getCreatedAt()).isNotNull();
    assertThat(group.getUpdatedAt()).isNotNull();
  }

  @Test
  public void selectGroupByName_not_found() {
    setupData("selectGroupByName");

    GroupDto group = dao.selectGroupByName("not-found");
    assertThat(group).isNull();
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
    dao.insert(session, userDto);
    session.commit();

    UserDto user = dao.selectActiveUserByLogin("john");
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
    setupData("update_user");

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
    dao.update(session, userDto);
    session.commit();

    UserDto user = dao.getUser(1);
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
    setupData("deactivate_user");

    when(system2.now()).thenReturn(1500000000000L);

    String login = "marius";
    boolean deactivated = dao.deactivateUserByLogin(login);
    assertThat(deactivated).isTrue();

    assertThat(dao.selectActiveUserByLogin(login)).isNull();

    UserDto userDto = dao.getUser(100);
    assertThat(userDto.isActive()).isFalse();
    assertThat(userDto.getUpdatedAt()).isEqualTo(1500000000000L);

    checkTables("deactivate_user",
      "dashboards", "active_dashboards", "groups_users", "issue_filters",
      "issue_filter_favourites", "measure_filters", "measure_filter_favourites",
      "properties", "user_roles");
  }

  @Test
  public void deactivate_missing_user() {
    setupData("deactivate_user");

    String login = "does_not_exist";
    boolean deactivated = dao.deactivateUserByLogin(login);
    assertThat(deactivated).isFalse();
    assertThat(dao.selectActiveUserByLogin(login)).isNull();
  }
}
