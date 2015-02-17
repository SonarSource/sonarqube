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
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndexDefinition;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

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

    boolean result = service.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setPasswordConfirmation("password")
      .setScmAccounts(newArrayList("u1", "u_1")));

    assertThat(result).isFalse();

    UserDto userDto = dbClient.userDao().selectNullableByLogin(session, "user");
    assertThat(userDto).isNotNull();
    assertThat(userDto.getId()).isNotNull();
    assertThat(userDto.getLogin()).isEqualTo("user");
    assertThat(userDto.getName()).isEqualTo("User");
    assertThat(userDto.getEmail()).isEqualTo("user@mail.com");
    assertThat(userDto.getCryptedPassword()).isNotNull();
    assertThat(userDto.getSalt()).isNotNull();
    assertThat(userDto.getScmAccountsAsList()).containsOnly("u1", "u_1");
    assertThat(userDto.getCreatedAt()).isNotNull();
    assertThat(userDto.getUpdatedAt()).isNotNull();

    UserDoc userDoc = service.getNullableByLogin("user");
    assertThat(userDoc).isNotNull();
    assertThat(userDoc.login()).isEqualTo("user");
    assertThat(userDoc.name()).isEqualTo("User");
    assertThat(userDoc.email()).isEqualTo("user@mail.com");
    assertThat(userDoc.scmAccounts()).containsOnly("u1", "u_1");
    assertThat(userDoc.createdAt()).isNotNull();
    assertThat(userDoc.updatedAt()).isNotNull();
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
  public void update_user() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    dbClient.userDao().insert(session, new UserDto()
      .setLogin("marius")
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setCryptedPassword("1234")
      .setSalt("abcd")
      .setCreatedAt(1000L)
      );
    GroupDto userGroup = new GroupDto().setName(CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);
    dbClient.groupDao().insert(session, userGroup);
    session.commit();

    service.update(UpdateUser.create("marius")
      .setName("Marius2")
      .setEmail("marius2@mail.com"));

    UserDto userDto = dbClient.userDao().selectNullableByLogin(session, "marius");
    assertThat(userDto.getName()).isEqualTo("Marius2");
    assertThat(userDto.getEmail()).isEqualTo("marius2@mail.com");
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_update_user_without_sys_admin_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.DASHBOARD_SHARING);

    service.update(UpdateUser.create("marius")
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setPasswordConfirmation("password2")
      .setScmAccounts(newArrayList("ma2")));
  }

  @Test
  public void get_nullable_by_login() throws Exception {
    createSampleUser();

    assertThat(service.getNullableByLogin("user")).isNotNull();
  }

  @Test
  public void get_by_login() throws Exception {
    createSampleUser();

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

  private void createSampleUser() {
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

    MockUserSession.set().setLogin("john");
  }

}
