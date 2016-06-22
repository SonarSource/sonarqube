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
package org.sonar.server.qualityprofile;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;

public class QProfileProjectOperationsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;
  QProfileFactory factory;
  QProfileProjectOperations projectOperations;
  ComponentDto project;
  QualityProfileDto profile;
  static final String PROJECT_KEY = "SonarQube";
  static final String PROJECT_UUID = "ABCD";

  UserSession authorizedProfileAdminUserSession = new MockUserSession("john").setName("John").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession authorizedProjectAdminUserSession = new MockUserSession("john").setName("John").addProjectPermissions(UserRole.ADMIN, PROJECT_KEY);

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    factory = tester.get(QProfileFactory.class);
    projectOperations = tester.get(QProfileProjectOperations.class);

    project = ComponentTesting.newProjectDto(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .setName("SonarQube")
      .setLongName("SonarQube");
    db.componentDao().insert(dbSession, project);

    profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile);

    dbSession.commit();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void add_project() {
    projectOperations.addProject(profile.getKey(), project.uuid(), authorizedProfileAdminUserSession);

    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNotNull();
  }

  @Test
  public void add_project_with_only_project_admin_permission() {
    projectOperations.addProject(profile.getKey(), project.uuid(), authorizedProjectAdminUserSession);

    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNotNull();
  }

  @Test
  public void remove_project_from_project_id() {
    projectOperations.addProject(profile.getKey(), project.uuid(), authorizedProfileAdminUserSession);
    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNotNull();

    projectOperations.removeProject(profile.getKey(), project.uuid(), authorizedProfileAdminUserSession);
    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNull();
  }

  @Test
  public void remove_project_from_language() {
    projectOperations.addProject(profile.getKey(), project.uuid(), authorizedProfileAdminUserSession);
    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNotNull();

    projectOperations.removeProject(profile.getLanguage(), project.getId(), authorizedProfileAdminUserSession);
    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNull();
  }

  @Test
  public void remove_all_projects() {
    ComponentDto project1 = ComponentTesting.newProjectDto("BCDE")
      .setKey("project1")
      .setName("project1")
      .setLongName("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto("CDEF")
      .setKey("project2")
      .setName("project2")
      .setLongName("project2");
    db.componentDao().insert(dbSession, project1);
    db.componentDao().insert(dbSession, project2);

    // Create a user having user permission on the two projects and the global quality profile admin permission
    UserDto user = new UserDto().setLogin("john").setName("John").setEmail("jo@hn.com").setCreatedAt(System.currentTimeMillis()).setUpdatedAt(System.currentTimeMillis());
    db.userDao().insert(dbSession, user);
    tester.get(PermissionRepository.class).insertUserPermission(project1.getId(), user.getId(), UserRole.USER, dbSession);
    tester.get(PermissionRepository.class).insertUserPermission(project2.getId(), user.getId(), UserRole.USER, dbSession);
    UserSession userSession = userSessionRule.login("john").setUserId(user.getId().intValue()).setName("John")
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    dbSession.commit();

    projectOperations.addProject(profile.getKey(), project1.uuid(), userSession);
    projectOperations.addProject(profile.getKey(), project2.uuid(), userSession);
    assertThat(tester.get(QProfileProjectLookup.class).projects(profile.getId())).hasSize(2);

    projectOperations.removeAllProjects(profile.getKey(), userSession);
    assertThat(tester.get(QProfileProjectLookup.class).projects(profile.getId())).isEmpty();
  }
}
