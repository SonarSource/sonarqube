/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class ComponentServiceTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(dbTester);
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);

  private ComponentService underTest = new ComponentService(dbClient, userSession, projectIndexers, projectLifeCycleListeners);

  @Test
  public void bulk_update() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert()).setDbKey("my_project"));
    ComponentDto module = componentDb.insertComponent(newModuleDto(project).setDbKey("my_project:root:module"));
    ComponentDto inactiveModule = componentDb.insertComponent(newModuleDto(project).setDbKey("my_project:root:inactive_module").setEnabled(false));
    ComponentDto file = componentDb.insertComponent(newFileDto(module, null).setDbKey("my_project:root:module:src/File.xoo"));
    ComponentDto inactiveFile = componentDb.insertComponent(newFileDto(module, null).setDbKey("my_project:root:module:src/InactiveFile.xoo").setEnabled(false));

    underTest.bulkUpdateKey(dbSession, project, "my_", "your_");

    assertComponentKeyUpdated(project.getDbKey(), "your_project");
    assertComponentKeyUpdated(module.getDbKey(), "your_project:root:module");
    assertComponentKeyUpdated(file.getDbKey(), "your_project:root:module:src/File.xoo");
    assertComponentKeyUpdated(inactiveModule.getDbKey(), "your_project:root:inactive_module");
    assertComponentKeyUpdated(inactiveFile.getDbKey(), "your_project:root:module:src/InactiveFile.xoo");
  }

  private void assertComponentKeyUpdated(String oldKey, String newKey) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, oldKey)).isEmpty();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newKey)).isPresent();
  }

}
