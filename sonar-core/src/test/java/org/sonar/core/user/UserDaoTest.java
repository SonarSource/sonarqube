/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.user.UserQuery;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class UserDaoTest extends AbstractDaoTestCase {

  private UserDao dao;

  @Before
  public void setUp() {
    dao = new UserDao(getMyBatis());
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
    assertThat(users).onProperty("login").containsOnly("marius", "inactive_user");
  }

  @Test
  public void selectUsersByLogins_empty_logins() throws Exception {
    // no need to access db
    Collection<UserDto> users = dao.selectUsersByLogins(Collections.<String> emptyList());
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
  public void deactivate_user() {
    setupData("deactivate_user");

    String login = "marius";
    boolean deactivated = dao.deactivateUserByLogin(login);
    assertThat(deactivated).isTrue();
    assertThat(dao.selectActiveUserByLogin(login)).isNull();
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
