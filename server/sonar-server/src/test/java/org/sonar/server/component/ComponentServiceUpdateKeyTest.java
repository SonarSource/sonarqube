/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class ComponentServiceUpdateKeyTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ProjectIndexer projectIndexer = mock(ProjectIndexer.class);
  private ComponentService underTest = new ComponentService(dbClient, userSession, projectIndexer);

  @Test
  public void update_project_key() {
    ComponentDto project = insertSampleRootProject();
    ComponentDto file = componentDb.insertComponent(ComponentTesting.newFileDto(project, null).setKey("sample:root:src/File.xoo"));
    ComponentDto inactiveFile = componentDb.insertComponent(ComponentTesting.newFileDto(project, null).setKey("sample:root:src/InactiveFile.xoo").setEnabled(false));

    dbSession.commit();

    logInAsProjectAdministrator(project);
    underTest.updateKey(dbSession, project, "sample2:root");
    dbSession.commit();

    // Check project key has been updated
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, project.key())).isAbsent();
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, "sample2:root")).isNotNull();

    // Check file key has been updated
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, file.key())).isAbsent();
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, "sample2:root:src/File.xoo")).isNotNull();

    assertThat(dbClient.componentDao().selectByKey(dbSession, inactiveFile.getKey())).isPresent();

    verify(projectIndexer).indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
  }

  @Test
  public void update_module_key() {
    ComponentDto project = insertSampleRootProject();
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    dbClient.componentDao().insert(dbSession, module);
    ComponentDto file = ComponentTesting.newFileDto(module, null).setKey("sample:root:module:src/File.xoo");
    dbClient.componentDao().insert(dbSession, file);
    dbSession.commit();
    logInAsProjectAdministrator(project);

    underTest.updateKey(dbSession, module, "sample:root2:module");
    dbSession.commit();

    assertThat(dbClient.componentDao().selectByKey(dbSession, project.key())).isPresent();
    assertComponentKeyHasBeenUpdated(module.key(), "sample:root2:module");
    assertComponentKeyHasBeenUpdated(file.key(), "sample:root2:module:src/File.xoo");

    verify(projectIndexer).indexProject(module.uuid(), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
  }

  @Test
  public void update_provisioned_project_key() {
    ComponentDto provisionedProject = insertProject("provisionedProject");

    dbSession.commit();

    logInAsProjectAdministrator(provisionedProject);
    underTest.updateKey(dbSession, provisionedProject, "provisionedProject2");
    dbSession.commit();

    assertComponentKeyHasBeenUpdated(provisionedProject.key(), "provisionedProject2");
    verify(projectIndexer).indexProject(provisionedProject.uuid(), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
  }

  @Test
  public void fail_to_update_project_key_without_admin_permission() {
    expectedException.expect(ForbiddenException.class);

    ComponentDto project = insertSampleRootProject();
    userSession.logIn("john").addProjectPermission(UserRole.USER, project);

    underTest.updateKey(dbSession, project, "sample2:root");
  }

  @Test
  public void fail_if_old_key_and_new_key_are_the_same() {
    ComponentDto project = insertSampleRootProject();
    ComponentDto anotherProject = componentDb.insertPrivateProject();
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Impossible to update key: a component with key \"" + anotherProject.key() + "\" already exists.");

    underTest.updateKey(dbSession, project, anotherProject.key());
  }

  @Test
  public void fail_if_new_key_is_empty() {
    ComponentDto project = insertSampleRootProject();
    logInAsProjectAdministrator(project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed key for ''. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    underTest.updateKey(dbSession, project, "");
  }

  @Test
  public void fail_if_new_key_is_not_formatted_correctly() {
    ComponentDto project = insertSampleRootProject();
    logInAsProjectAdministrator(project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed key for 'sample?root'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    underTest.updateKey(dbSession, project, "sample?root");
  }

  @Test
  public void fail_if_update_is_not_on_module_or_project() {
    ComponentDto project = insertSampleRootProject();
    ComponentDto file = componentDb.insertComponent(newFileDto(project, null));
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component updated must be a module or a key");

    underTest.updateKey(dbSession, file, "file:key");
  }

  @Test
  public void bulk_update_key() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey("my_project"));
    ComponentDto module = componentDb.insertComponent(newModuleDto(project).setKey("my_project:root:module"));
    ComponentDto inactiveModule = componentDb.insertComponent(newModuleDto(project).setKey("my_project:root:inactive_module").setEnabled(false));
    ComponentDto file = componentDb.insertComponent(newFileDto(module, null).setKey("my_project:root:module:src/File.xoo"));
    ComponentDto inactiveFile = componentDb.insertComponent(newFileDto(module, null).setKey("my_project:root:module:src/InactiveFile.xoo").setEnabled(false));

    underTest.bulkUpdateKey(dbSession, project.uuid(), "my_", "your_");

    assertComponentKeyUpdated(project.key(), "your_project");
    assertComponentKeyUpdated(module.key(), "your_project:root:module");
    assertComponentKeyUpdated(file.key(), "your_project:root:module:src/File.xoo");
    assertComponentKeyNotUpdated(inactiveModule.key());
    assertComponentKeyNotUpdated(inactiveFile.key());
  }

  private void assertComponentKeyUpdated(String oldKey, String newKey) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, oldKey)).isAbsent();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newKey)).isPresent();
  }

  private void assertComponentKeyNotUpdated(String key) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, key)).isPresent();
  }

  private ComponentDto insertSampleRootProject() {
    return insertProject("sample:root");
  }

  private ComponentDto insertProject(String key) {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey(key));
    return project;
  }

  private void assertComponentKeyHasBeenUpdated(String oldKey, String newKey) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, oldKey)).isAbsent();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newKey)).isPresent();
  }

  private void logInAsProjectAdministrator(ComponentDto provisionedProject) {
    userSession.logIn("john").addProjectPermission(UserRole.ADMIN, provisionedProject);
  }
}
