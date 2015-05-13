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

package org.sonar.server.design.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.design.FileDependencyDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.design.db.FileDependencyDao;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class ShowActionTest {

  private static final String PROJECT_UUID = "PROJECT";
  private static final String DIR1_UUID = "DIR1";
  private static final String FILE1_UUID = "FILE1";
  private static final String DIR2_UUID = "DIR2";
  private static final String FILE2_UUID = "FILE2";
  private static final String UNKNOWN_UUID = "UNKNOWN";

  Long projectSnapshotId;

  @ClassRule
  public static DbTester dbTester = new DbTester();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  DbClient dbClient;

  DbSession session;

  WebService.Controller controller;

  WsTester tester;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao(), new SnapshotDao(System2.INSTANCE), new FileDependencyDao());
    session = dbClient.openSession(false);
    tester = new WsTester(new DependenciesWs(new ShowAction(dbClient, userSessionRule)));
    controller = tester.controller("api/dependencies");

    initComponents();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void return_file_dependencies() throws Exception {
    dbClient.fileDependencyDao().insert(session, new FileDependencyDto()
      .setFromComponentUuid(FILE1_UUID)
      .setToComponentUuid(FILE2_UUID)
      .setFromParentUuid(DIR1_UUID)
      .setToParentUuid(DIR2_UUID)
      .setRootProjectSnapshotId(projectSnapshotId)
      .setWeight(2)
      .setCreatedAt(1000L));
    session.commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    tester.newGetRequest("api/dependencies", "show")
      .setParam("fromParentUuid", DIR1_UUID)
      .setParam("toParentUuid", DIR2_UUID)
      .execute()
      .assertJson(getClass(), "return_file_dependencies.json");
  }

  @Test
  public void return_nothing() throws Exception {
    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    tester.newGetRequest("api/dependencies", "show")
      .setParam("fromParentUuid", DIR1_UUID)
      .setParam("toParentUuid", DIR2_UUID)
      .execute()
      .assertJson(getClass(), "return_nothing.json");
  }

  @Test(expected = ForbiddenException.class)
  public void fail_if_no_user_permission_on_project() throws Exception {
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    tester.newGetRequest("api/dependencies", "show")
      .setParam("fromParentUuid", DIR1_UUID)
      .setParam("toParentUuid", DIR2_UUID)
      .execute()
      .assertJson(getClass(), "return_nothing.json");
  }

  @Test
  public void fail_if_from_parent_uuid_does_not_exists() throws Exception {
    dbClient.fileDependencyDao().insert(session, new FileDependencyDto()
      .setFromComponentUuid(FILE1_UUID)
      .setToComponentUuid(FILE2_UUID)
      .setFromParentUuid(DIR1_UUID)
      .setToParentUuid(DIR2_UUID)
      .setRootProjectSnapshotId(projectSnapshotId)
      .setWeight(2)
      .setCreatedAt(1000L));
    session.commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    try {
      tester.newGetRequest("api/dependencies", "show")
        .setParam("fromParentUuid", UNKNOWN_UUID)
        .setParam("toParentUuid", DIR2_UUID)
        .execute()
        .assertJson(getClass(), "return_file_dependencies.json");
      failBecauseExceptionWasNotThrown(NotFoundException.class);
    } catch (NotFoundException e) {
      assertThat(e).hasMessage("Component with uuid 'UNKNOWN' not found");
    }
  }

  @Test
  public void return_nothing_if_to_parent_uuid_does_not_exists() throws Exception {
    dbClient.fileDependencyDao().insert(session, new FileDependencyDto()
      .setFromComponentUuid(FILE1_UUID)
      .setToComponentUuid(FILE2_UUID)
      .setFromParentUuid(DIR1_UUID)
      .setToParentUuid(DIR2_UUID)
      .setRootProjectSnapshotId(projectSnapshotId)
      .setWeight(2)
      .setCreatedAt(1000L));
    session.commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    tester.newGetRequest("api/dependencies", "show")
      .setParam("fromParentUuid", DIR1_UUID)
      .setParam("toParentUuid", UNKNOWN_UUID)
      .execute()
      .assertJson(getClass(), "return_nothing.json");
  }

  @Test
  public void fail_if_from_component_uuid_does_not_exists() throws Exception {
    dbClient.fileDependencyDao().insert(session, new FileDependencyDto()
      .setFromComponentUuid(UNKNOWN_UUID)
      .setToComponentUuid(FILE2_UUID)
      .setFromParentUuid(DIR1_UUID)
      .setToParentUuid(DIR2_UUID)
      .setRootProjectSnapshotId(projectSnapshotId)
      .setWeight(2)
      .setCreatedAt(1000L));
    session.commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    try {
      tester.newGetRequest("api/dependencies", "show")
        .setParam("fromParentUuid", DIR1_UUID)
        .setParam("toParentUuid", DIR2_UUID)
        .execute()
        .assertJson(getClass(), "return_file_dependencies.json");
      failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Component with uuid 'UNKNOWN' does not exists");
    }
  }

  @Test
  public void fail_if_to_component_uuid_does_not_exists() throws Exception {
    dbClient.fileDependencyDao().insert(session, new FileDependencyDto()
      .setFromComponentUuid(FILE1_UUID)
      .setToComponentUuid(UNKNOWN_UUID)
      .setFromParentUuid(DIR1_UUID)
      .setToParentUuid(DIR2_UUID)
      .setRootProjectSnapshotId(projectSnapshotId)
      .setWeight(2)
      .setCreatedAt(1000L));
    session.commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    try {
      tester.newGetRequest("api/dependencies", "show")
        .setParam("fromParentUuid",DIR1_UUID)
        .setParam("toParentUuid", DIR2_UUID)
        .execute()
        .assertJson(getClass(), "return_file_dependencies.json");
      failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Component with uuid 'UNKNOWN' does not exists");
    }
  }

  private void initComponents(){
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID);
    ComponentDto directory1 = ComponentTesting.newDirectory(project, "/src/main/java/dir1").setUuid(DIR1_UUID);
    ComponentDto file1 = ComponentTesting.newFileDto(directory1, FILE1_UUID).setLongName("src/main/java/dir1/File1.java");
    ComponentDto directory2 = ComponentTesting.newDirectory(project, "/src/main/java/dir2").setUuid(DIR2_UUID);
    ComponentDto file2 = ComponentTesting.newFileDto(directory1, FILE2_UUID).setLongName("src/main/java/dir2/File2.java");
    dbClient.componentDao().insert(session, project, directory1, directory2, file1, file2);

    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    dbClient.snapshotDao().insert(session, projectSnapshot);

    session.commit();

    projectSnapshotId = projectSnapshot.getId();
  }

}
