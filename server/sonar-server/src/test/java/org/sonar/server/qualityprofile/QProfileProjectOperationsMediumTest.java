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

package org.sonar.server.qualityprofile;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;

public class QProfileProjectOperationsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession dbSession;
  QProfileFactory factory;
  QProfileProjectOperations projectOperations;
  ComponentDto project;
  QualityProfileDto profile;
  static final String PROJECT_KEY = "SonarQube";
  static final String PROJECT_UUID = "ABCD";

  UserSession authorizedProfileAdminUserSession = MockUserSession.create().setLogin("john").setName("John").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession authorizedProjectAdminUserSession = MockUserSession.create().setLogin("john").setName("John").addProjectPermissions(UserRole.ADMIN, PROJECT_KEY);

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    factory = tester.get(QProfileFactory.class);
    projectOperations = tester.get(QProfileProjectOperations.class);

    project = new ComponentDto()
      .setUuid(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .setName("SonarQube")
      .setLongName("SonarQube")
      .setQualifier("TRK")
      .setScope("PRJ")
      .setEnabled(true);
    db.componentDao().insert(dbSession, project);

    profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile);

    dbSession.commit();
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void add_project() throws Exception {
    projectOperations.addProject(profile.getKey(), project.uuid(), authorizedProfileAdminUserSession);

    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNotNull();
  }

  @Test
  public void add_project_with_only_project_admin_permission() throws Exception {
    projectOperations.addProject(profile.getKey(), project.uuid(), authorizedProjectAdminUserSession);

    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNotNull();
  }

  @Test
  public void remove_project_from_project_id() throws Exception {
    projectOperations.addProject(profile.getKey(), project.uuid(), authorizedProfileAdminUserSession);
    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNotNull();

    projectOperations.removeProject(profile.getKey(), project.uuid(), authorizedProfileAdminUserSession);
    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNull();
  }

  @Test
  public void remove_project_from_language() throws Exception {
    projectOperations.addProject(profile.getKey(), project.uuid(), authorizedProfileAdminUserSession);
    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNotNull();

    projectOperations.removeProject(profile.getLanguage(), project.getId(), authorizedProfileAdminUserSession);
    assertThat(factory.getByProjectAndLanguage(PROJECT_KEY, profile.getLanguage())).isNull();
  }

  @Test
  public void remove_all_projects() throws Exception {
    ComponentDto project1 = new ComponentDto()
      .setUuid("BCDE")
      .setKey("project1")
      .setName("project1")
      .setLongName("project1")
      .setQualifier("TRK")
      .setScope("PRJ")
      .setEnabled(true);
    ComponentDto project2 = new ComponentDto()
      .setUuid("CDEF")
      .setKey("project2")
      .setName("project2")
      .setLongName("project2")
      .setQualifier("TRK")
      .setScope("PRJ")
      .setEnabled(true);
    db.componentDao().insert(dbSession, project1);
    db.componentDao().insert(dbSession, project2);

    // Create a user having user permission on the two projects and the global quality profile admin permission
    UserDto user = new UserDto().setLogin("john").setName("John").setEmail("jo@hn.com").setCreatedAt(System.currentTimeMillis()).setUpdatedAt(System.currentTimeMillis());
    db.userDao().insert(dbSession, user);
    tester.get(PermissionFacade.class).insertUserPermission(project1.getId(), user.getId(), UserRole.USER, dbSession);
    tester.get(PermissionFacade.class).insertUserPermission(project2.getId(), user.getId(), UserRole.USER, dbSession);
    UserSession userSession = MockUserSession.set().setUserId(user.getId().intValue()).setLogin("john").setName("John")
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    dbSession.commit();

    projectOperations.addProject(profile.getKey(), project1.uuid(), userSession);
    projectOperations.addProject(profile.getKey(), project2.uuid(), userSession);
    assertThat(tester.get(QProfileProjectLookup.class).projects(profile.getId())).hasSize(2);

    projectOperations.removeAllProjects(profile.getKey(), userSession);
    assertThat(tester.get(QProfileProjectLookup.class).projects(profile.getId())).isEmpty();
  }
}
