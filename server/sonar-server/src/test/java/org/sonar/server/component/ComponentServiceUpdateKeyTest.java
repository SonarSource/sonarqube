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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.component.es.ProjectMeasuresIndexDefinition;
import org.sonar.server.component.es.ProjectMeasuresIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ComponentServiceUpdateKeyTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public DbTester db = DbTester.create(system2);

  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  I18nRule i18n = new I18nRule();

  ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(system2, dbClient, es.client());

  ComponentService underTest;

  @Before
  public void setUp() {
    i18n.put("qualifier.TRK", "Project");

    underTest = new ComponentService(dbClient, i18n, userSession, system2, new ComponentFinder(dbClient), projectMeasuresIndexer);
  }

  @Test
  public void update_project_key() {
    ComponentDto project = insertSampleRootProject();
    ComponentDto file = componentDb.insertComponent(ComponentTesting.newFileDto(project, null).setKey("sample:root:src/File.xoo"));
    ComponentDto inactiveFile = componentDb.insertComponent(ComponentTesting.newFileDto(project, null).setKey("sample:root:src/InactiveFile.xoo").setEnabled(false));

    dbSession.commit();

    userSession.login("john").addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    underTest.updateKey(dbSession, project.key(), "sample2:root");
    dbSession.commit();

    // Check project key has been updated
    assertThat(underTest.getNullableByKey(project.key())).isNull();
    assertThat(underTest.getNullableByKey("sample2:root")).isNotNull();

    // Check file key has been updated
    assertThat(underTest.getNullableByKey(file.key())).isNull();
    assertThat(underTest.getNullableByKey("sample2:root:src/File.xoo")).isNotNull();

    assertThat(dbClient.componentDao().selectByKey(dbSession, inactiveFile.getKey())).isPresent();

    assertProjectKeyExistsInIndex("sample2:root");
  }

  @Test
  public void update_module_key() {
    ComponentDto project = insertSampleRootProject();
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    dbClient.componentDao().insert(dbSession, module);
    ComponentDto file = ComponentTesting.newFileDto(module, null).setKey("sample:root:module:src/File.xoo");
    dbClient.componentDao().insert(dbSession, file);
    dbSession.commit();
    userSession.login("john").addProjectUuidPermissions(UserRole.ADMIN, project.uuid());

    underTest.updateKey(dbSession, module.key(), "sample:root2:module");
    dbSession.commit();

    assertThat(dbClient.componentDao().selectByKey(dbSession, project.key())).isPresent();
    assertComponentKeyHasBeenUpdated(module.key(), "sample:root2:module");
    assertComponentKeyHasBeenUpdated(file.key(), "sample:root2:module:src/File.xoo");

    assertProjectKeyExistsInIndex(project.key());
  }

  @Test
  public void update_provisioned_project_key() {
    ComponentDto provisionedProject = insertProject("provisionedProject");

    dbSession.commit();

    userSession.login("john").addProjectUuidPermissions(UserRole.ADMIN, provisionedProject.uuid());
    underTest.updateKey(dbSession, provisionedProject.key(), "provisionedProject2");
    dbSession.commit();

    assertComponentKeyHasBeenUpdated(provisionedProject.key(), "provisionedProject2");
    assertProjectKeyExistsInIndex("provisionedProject2");
  }

  @Test
  public void fail_to_update_project_key_without_admin_permission() {
    expectedException.expect(ForbiddenException.class);

    ComponentDto project = insertSampleRootProject();
    userSession.login("john").addProjectUuidPermissions(UserRole.USER, project.uuid());

    underTest.updateKey(dbSession, project.key(), "sample2:root");
  }

  @Test
  public void fail_if_old_key_and_new_key_are_the_same() {
    setGlobalAdminPermission();
    ComponentDto project = insertSampleRootProject();
    ComponentDto anotherProject = componentDb.insertProject();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Impossible to update key: a component with key \"" + anotherProject.key() + "\" already exists.");

    underTest.updateKey(dbSession, project.key(), anotherProject.key());
  }

  @Test
  public void fail_if_new_key_is_empty() {
    setGlobalAdminPermission();
    ComponentDto project = insertSampleRootProject();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed key for ''. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    underTest.updateKey(dbSession, project.key(), "");
  }

  @Test
  public void fail_if_new_key_is_not_formatted_correctly() {
    setGlobalAdminPermission();
    ComponentDto project = insertSampleRootProject();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed key for 'sample?root'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    underTest.updateKey(dbSession, project.key(), "sample?root");
  }

  @Test
  public void fail_if_update_is_not_on_module_or_project() {
    setGlobalAdminPermission();
    ComponentDto project = insertSampleRootProject();
    ComponentDto file = componentDb.insertComponent(newFileDto(project, null));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component updated must be a module or a key");

    underTest.updateKey(dbSession, file.key(), "file:key");
  }

  @Test
  public void bulk_update_key() {
    ComponentDto project = componentDb.insertComponent(newProjectDto().setKey("my_project"));
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

    assertProjectKeyExistsInIndex("your_project");
  }

  private void assertComponentKeyUpdated(String oldKey, String newKey) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, oldKey)).isAbsent();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newKey)).isPresent();
  }

  private void assertComponentKeyNotUpdated(String key) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, key)).isPresent();
  }

  private void setGlobalAdminPermission() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  private ComponentDto insertSampleRootProject() {
    return insertProject("sample:root");
  }

  private ComponentDto insertProject(String key) {
    ComponentDto project = componentDb.insertComponent(newProjectDto().setKey(key));
    projectMeasuresIndexer.index(project.uuid());
    return project;
  }

  private void assertComponentKeyHasBeenUpdated(String oldKey, String newKey) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, oldKey)).isAbsent();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newKey)).isPresent();
  }

  private void assertProjectKeyExistsInIndex(String key) {
    SearchRequestBuilder request = es.client()
      .prepareSearch(INDEX_PROJECT_MEASURES)
      .setTypes(TYPE_PROJECT_MEASURES)
      .setQuery(boolQuery().must(matchAllQuery()).filter(
        boolQuery().must(termQuery(ProjectMeasuresIndexDefinition.FIELD_KEY, key))));
    assertThat(request.get().getHits()).hasSize(1);
  }

}
