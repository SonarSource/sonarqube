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
package org.sonar.db.permission;

import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserPermissionDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserWithPermissionDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  UserDbTester userDb = new UserDbTester(dbTester);
  PermissionDbTester permissionDb = new PermissionDbTester(dbTester);
  ComponentDbTester componentDb = new ComponentDbTester(dbTester);
  DbSession session = dbTester.getSession();

  PermissionDao underTest = new PermissionDao();

  @Test
  public void count_logins_by_query() {
    UserDto user1 = userDb.insertUser(newUserDto());
    UserDto user2 = userDb.insertUser(newUserDto());
    UserDto user3 = userDb.insertUser(newUserDto());
    UserDto user4 = userDb.insertUser(newUserDto());
    ComponentDto project = componentDb.insertComponent(newProjectDto());

    permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user1.getId());
    permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user2.getId());
    permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user3.getId());
    permissionDb.addGlobalPermissionToUser(PROVISIONING, user3.getId());
    permissionDb.addProjectPermissionToUser(USER, user4.getId(), project.getId());

    assertThat(countUsersByQuery(PermissionQuery.builder().build())).isEqualTo(4);
    assertThat(countUsersByQuery(PermissionQuery.builder().setPermission(PROVISIONING).build())).isEqualTo(1);
    assertThat(countUsersByQuery(PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project.uuid()).build())).isEqualTo(1);
  }

  @Test
  public void select_user_permissions_by_logins_with_global_permissions() {
    UserDto firstUser = userDb.insertUser(newUserDto());
    permissionDb.addGlobalPermissionToUser(ADMIN, firstUser.getId());

    UserDto secondUser = userDb.insertUser(newUserDto());
    permissionDb.addGlobalPermissionToUser(USER, secondUser.getId());

    UserDto thirdUser = userDb.insertUser(newUserDto());
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    permissionDb.addProjectPermissionToUser(ADMIN, thirdUser.getId(), project.getId());

    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, (asList(firstUser.getLogin(), secondUser.getLogin(), thirdUser.getLogin())), null))
      .extracting(UserPermissionDto::getUserId, UserPermissionDto::getPermission, UserPermissionDto::getComponentId)
      .containsOnly(
        tuple(firstUser.getId(), ADMIN, null),
        tuple(secondUser.getId(), USER, null));

    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, singletonList(thirdUser.getLogin()), null)).isEmpty();
    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, singletonList("unknown"), null)).isEmpty();
    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, Collections.emptyList(), null)).isEmpty();
  }

  @Test
  public void select_user_permissions_by_logins_with_project_permissions() {
    UserDto firstUser = userDb.insertUser(newUserDto());
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    permissionDb.addProjectPermissionToUser(ADMIN, firstUser.getId(), project.getId());

    UserDto secondUser = userDb.insertUser(newUserDto());
    permissionDb.addProjectPermissionToUser(USER, secondUser.getId(), project.getId());

    UserDto thirdUser = userDb.insertUser(newUserDto());
    ComponentDto anotherProject = componentDb.insertComponent(newProjectDto());
    permissionDb.addProjectPermissionToUser(ADMIN, thirdUser.getId(), anotherProject.getId());

    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, (asList(firstUser.getLogin(), secondUser.getLogin(), thirdUser.getLogin())), project.getId()))
      .extracting(UserPermissionDto::getUserId, UserPermissionDto::getPermission, UserPermissionDto::getComponentId)
      .containsOnly(
        tuple(firstUser.getId(), ADMIN, project.getId()),
        tuple(secondUser.getId(), USER, project.getId()));

    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, (asList(firstUser.getLogin(), secondUser.getLogin(), thirdUser.getLogin())), anotherProject.getId()))
      .extracting(UserPermissionDto::getUserId, UserPermissionDto::getPermission, UserPermissionDto::getComponentId)
      .containsOnly(
        tuple(thirdUser.getId(), ADMIN, anotherProject.getId()));

    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, singletonList(thirdUser.getLogin()), project.getId())).isEmpty();
    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, singletonList("unknown"), project.getId())).isEmpty();
    assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, Collections.emptyList(), project.getId())).isEmpty();
  }

  private int countUsersByQuery(PermissionQuery query) {
    return underTest.countUsersByQuery(session, query);
  }

}
