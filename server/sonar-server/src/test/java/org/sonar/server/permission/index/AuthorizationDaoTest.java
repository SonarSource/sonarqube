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
package org.sonar.server.permission.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;

public class AuthorizationDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();
  DbSession dbSession = dbTester.getSession();

  ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  UserDbTester userDbTester = new UserDbTester(dbTester);

  ComponentDto project1;
  ComponentDto project2;
  UserDto user1;
  UserDto user2;
  GroupDto group;

  AuthorizationDao underTest = new AuthorizationDao();

  @Before
  public void setUp() throws Exception {
    project1 = componentDbTester.insertProject();
    project2 = componentDbTester.insertProject();
    user1 = userDbTester.insertUser();
    user2 = userDbTester.insertUser();
    group = userDbTester.insertGroup();
  }

  @Test
  public void select_all() {
    insertTestDataForProject1And2();

    Collection<AuthorizationDao.Dto> dtos = underTest.selectAll(dbClient, dbSession);
    assertThat(dtos).hasSize(2);

    AuthorizationDao.Dto project1Authorization = getByProjectUuid(project1.uuid(), dtos);
    assertThat(project1Authorization.getGroups()).containsOnly(ANYONE, group.getName());
    assertThat(project1Authorization.getUsers()).containsOnly(user1.getLogin());
    assertThat(project1Authorization.getUpdatedAt()).isNotNull();

    AuthorizationDao.Dto project2Authorization = getByProjectUuid(project2.uuid(), dtos);
    assertThat(project2Authorization.getGroups()).containsOnly(ANYONE);
    assertThat(project2Authorization.getUsers()).containsOnly(user1.getLogin(), user2.getLogin());
    assertThat(project2Authorization.getUpdatedAt()).isNotNull();
  }

  @Test
  public void select_by_project() throws Exception {
    insertTestDataForProject1And2();

    Collection<AuthorizationDao.Dto> dtos = underTest.selectByProjects(dbClient, dbSession, singletonList(project1.uuid()));
    assertThat(dtos).hasSize(1);
    AuthorizationDao.Dto project1Authorization = getByProjectUuid(project1.uuid(), dtos);
    assertThat(project1Authorization.getGroups()).containsOnly(ANYONE, group.getName());
    assertThat(project1Authorization.getUsers()).containsOnly(user1.getLogin());
    assertThat(project1Authorization.getUpdatedAt()).isNotNull();
  }

  @Test
  public void select_by_projects() throws Exception {
    insertTestDataForProject1And2();

    Map<String, AuthorizationDao.Dto> dtos = underTest.selectByProjects(dbClient, dbSession, asList(project1.uuid(), project2.uuid()))
      .stream()
      .collect(Collectors.uniqueIndex(AuthorizationDao.Dto::getProjectUuid, Function.identity()));
    assertThat(dtos).hasSize(2);

    AuthorizationDao.Dto project1Authorization = dtos.get(project1.uuid());
    assertThat(project1Authorization.getGroups()).containsOnly(ANYONE, group.getName());
    assertThat(project1Authorization.getUsers()).containsOnly(user1.getLogin());
    assertThat(project1Authorization.getUpdatedAt()).isNotNull();

    AuthorizationDao.Dto project2Authorization = dtos.get(project2.uuid());
    assertThat(project2Authorization.getGroups()).containsOnly(ANYONE);
    assertThat(project2Authorization.getUsers()).containsOnly(user1.getLogin(), user2.getLogin());
    assertThat(project2Authorization.getUpdatedAt()).isNotNull();
  }

  @Test
  public void select_by_projects_with_high_number_of_projects() throws Exception {
    List<String> projects = new ArrayList<>();
    for (int i = 0; i < 350; i++) {
      ComponentDto project = ComponentTesting.newProjectDto(Integer.toString(i));
      dbClient.componentDao().insert(dbSession, project);
      projects.add(project.uuid());
      GroupPermissionDto dto = new GroupPermissionDto()
        .setOrganizationUuid(group.getOrganizationUuid())
        .setGroupId(group.getId())
        .setRole(USER)
        .setResourceId(project.getId());
      dbClient.groupPermissionDao().insert(dbSession, dto);
    }
    dbSession.commit();

    Map<String, AuthorizationDao.Dto> dtos = underTest.selectByProjects(dbClient, dbSession, projects)
      .stream()
      .collect(Collectors.uniqueIndex(AuthorizationDao.Dto::getProjectUuid, Function.identity()));
    assertThat(dtos).hasSize(350);
  }

  @Test
  public void no_authorization() {
    userDbTester.insertProjectPermissionOnUser(user1, USER, project2);
    userDbTester.insertProjectPermissionOnGroup(group, USER, project2);

    Collection<AuthorizationDao.Dto> dtos = underTest.selectAll(dbClient, dbSession);

    assertThat(dtos).hasSize(2);
    AuthorizationDao.Dto project1Authorization = getByProjectUuid(project1.uuid(), dtos);
    assertThat(project1Authorization.getGroups()).isEmpty();
    assertThat(project1Authorization.getUsers()).isEmpty();
    assertThat(project1Authorization.getUpdatedAt()).isNotNull();
  }

  private static AuthorizationDao.Dto getByProjectUuid(String projectUuid, Collection<AuthorizationDao.Dto> dtos) {
    return dtos.stream().filter(dto -> dto.getProjectUuid().equals(projectUuid)).findFirst().orElseThrow(IllegalArgumentException::new);
  }

  private void insertTestDataForProject1And2() {
    // user1 can access both projects
    userDbTester.insertProjectPermissionOnUser(user1, USER, project1);
    userDbTester.insertProjectPermissionOnUser(user1, ADMIN, project1);
    userDbTester.insertProjectPermissionOnUser(user1, USER, project2);

    // user2 has user access on project2 only
    userDbTester.insertProjectPermissionOnUser(user2, USER, project2);

    // group1 has user access on project1 only
    userDbTester.insertProjectPermissionOnGroup(group, USER, project1);
    userDbTester.insertProjectPermissionOnGroup(group, ADMIN, project1);

    // Anyone group has user access on both projects
    userDbTester.insertProjectPermissionOnAnyone(USER, project1);
    userDbTester.insertProjectPermissionOnAnyone(ADMIN, project1);
    userDbTester.insertProjectPermissionOnAnyone(USER, project2);
  }
}
