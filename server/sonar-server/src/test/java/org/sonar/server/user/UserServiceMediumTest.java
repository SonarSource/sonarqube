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

package org.sonar.server.user;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.db.UserDao;
import org.sonar.server.user.index.UserIndexDefinition;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class UserServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  EsClient esClient;
  DbClient dbClient;
  DbSession session;

  UserService service;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    dbClient = tester.get(DbClient.class);
    esClient = tester.get(EsClient.class);
    session = dbClient.openSession(false);
    service = tester.get(UserService.class);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void create_user() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    GroupDto userGroup = new GroupDto().setName(CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);
    dbClient.groupDao().insert(session, userGroup);
    session.commit();

    service.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setPasswordConfirmation("password")
      .setScmAccounts(newArrayList("u1", "u_1")));

    assertThat(tester.get(UserDao.class).selectNullableByLogin(session, "user")).isNotNull();
    assertThat(esClient.prepareGet(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, "user").get().isExists()).isTrue();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_create_user_without_sys_admin_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.DASHBOARD_SHARING);

    service.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setPasswordConfirmation("password")
      .setScmAccounts(newArrayList("u1", "u_1")));
  }

  @Test
  public void get_nullable_by_login() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    GroupDto userGroup = new GroupDto().setName(CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);
    dbClient.groupDao().insert(session, userGroup);
    session.commit();

    service.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setPasswordConfirmation("password")
      .setScmAccounts(newArrayList("u1", "u_1")));

    assertThat(service.getNullableByLogin("user")).isNotNull();
  }

  @Test
  public void get_by_login() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    GroupDto userGroup = new GroupDto().setName(CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);
    dbClient.groupDao().insert(session, userGroup);
    session.commit();

    service.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setPasswordConfirmation("password")
      .setScmAccounts(newArrayList("u1", "u_1")));

    assertThat(service.getByLogin("user")).isNotNull();
  }

  @Test
  public void index() throws Exception {
    UserDto userDto = new UserDto().setLogin("user").setEmail("user@mail.com").setCreatedAt(System.currentTimeMillis()).setUpdatedAt(System.currentTimeMillis());
    dbClient.userDao().insert(session, userDto);
    session.commit();
    assertThat(esClient.prepareGet(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, "user").get().isExists()).isFalse();

    service.index();
    assertThat(esClient.prepareGet(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, "user").get().isExists()).isTrue();
  }

}
