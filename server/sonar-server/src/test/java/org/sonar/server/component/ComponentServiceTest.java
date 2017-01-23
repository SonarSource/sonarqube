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
package org.sonar.server.component;

import java.util.Arrays;
import org.junit.Before;
import org.assertj.core.api.Fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;

public class ComponentServiceTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()),
    new ComponentIndexDefinition(new MapSettings()));
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(dbTester);
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(System2.INSTANCE, dbClient, es.client());
  private ComponentIndexer componentIndexer = new ComponentIndexer(dbClient, es.client());

  private ComponentService underTest = new ComponentService(dbClient, userSession, projectMeasuresIndexer, componentIndexer);

  @Test
  public void should_fail_silently_on_components_not_found_if_told_so() {
    String moduleKey = "sample:root:module";
    String fileKey = "sample:root:module:Foo.xoo";

    assertThat(underTest.componentUuids(dbSession, Arrays.asList(moduleKey, fileKey), true)).isEmpty();
  }

  @Test
  public void bulk_update() {
    ComponentDto project = componentDb.insertComponent(newProjectDto(dbTester.organizations().insert()).setKey("my_project"));
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

  private ComponentDto insertSampleProject() {
    return componentDb.insertComponent(newProjectDto(dbTester.organizations().insert()).setKey("sample:root"));
  }

}
