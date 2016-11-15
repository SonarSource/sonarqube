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

import com.google.common.base.Optional;
import java.util.Arrays;
import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceIndexDao;
import org.sonar.server.component.es.ProjectMeasuresIndexDefinition;
import org.sonar.server.component.es.ProjectMeasuresIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ComponentServiceTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(dbTester);
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private I18nRule i18n = new I18nRule();
  private ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(system2, dbClient, es.client());

  private ComponentService underTest;

  @Before
  public void setUp() {
    i18n.put("qualifier.TRK", "Project");

    underTest = new ComponentService(dbClient, i18n, userSession, system2, new ComponentFinder(dbClient), projectMeasuresIndexer);
  }

  @Test
  public void get_by_key() {
    ComponentDto project = insertSampleProject();
    assertThat(underTest.getByKey(project.getKey())).isNotNull();
  }

  @Test
  public void get_nullable_by_key() {
    ComponentDto project = insertSampleProject();
    assertThat(underTest.getNullableByKey(project.getKey())).isNotNull();
    assertThat(underTest.getNullableByKey("unknown")).isNull();
  }

  @Test
  public void get_by_uuid() {
    ComponentDto project = insertSampleProject();
    assertThat(underTest.getNonNullByUuid(project.uuid())).isNotNull();
  }

  @Test
  public void get_nullable_by_uuid() {
    ComponentDto project = insertSampleProject();
    assertThat(underTest.getByUuid(project.uuid())).isPresent();
    assertThat(underTest.getByUuid("unknown")).isAbsent();
  }

  @Test
  public void create_project() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);

    String key = underTest.create(dbSession, NewComponent.create("struts", "Struts project")).getKey();

    ComponentDto project = underTest.getNullableByKey(key);
    assertThat(project.key()).isEqualTo("struts");
    assertThat(project.deprecatedKey()).isEqualTo("struts");
    assertThat(project.uuid()).isNotNull();
    assertThat(project.projectUuid()).isEqualTo(project.uuid());
    assertThat(project.moduleUuid()).isNull();
    assertThat(project.moduleUuidPath()).isEqualTo("." + project.uuid() + ".");
    assertThat(project.name()).isEqualTo("Struts project");
    assertThat(project.longName()).isEqualTo("Struts project");
    assertThat(project.scope()).isEqualTo("PRJ");
    assertThat(project.qualifier()).isEqualTo("TRK");
    assertThat(project.getCreatedAt()).isNotNull();

    assertProjectIsInIndex(project.uuid());
  }

  @Test
  public void create_new_project_with_branch() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);

    String key = underTest.create(dbSession, NewComponent.create("struts", "Struts project").setBranch("origin/branch")).getKey();

    ComponentDto project = underTest.getNullableByKey(key);
    assertThat(project.key()).isEqualTo("struts:origin/branch");
    assertThat(project.deprecatedKey()).isEqualTo("struts:origin/branch");
  }

  @Test
  public void create_view() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);

    String key = underTest.create(dbSession, NewComponent.create("all-project", "All Projects").setQualifier(Qualifiers.VIEW)).getKey();

    ComponentDto project = underTest.getNullableByKey(key);
    assertThat(project.key()).isEqualTo("all-project");
    assertThat(project.deprecatedKey()).isEqualTo("all-project");
    assertThat(project.uuid()).isNotNull();
    assertThat(project.projectUuid()).isEqualTo(project.uuid());
    assertThat(project.moduleUuid()).isNull();
    assertThat(project.moduleUuidPath()).isEqualTo("." + project.uuid() + ".");
    assertThat(project.name()).isEqualTo("All Projects");
    assertThat(project.longName()).isEqualTo("All Projects");
    assertThat(project.scope()).isEqualTo("PRJ");
    assertThat(project.qualifier()).isEqualTo("VW");
    assertThat(project.getCreatedAt()).isNotNull();

    assertIndexIsEmpty();
  }

  @Test
  public void create_developer() throws Exception {
    // No permission should be required to create a developer
    userSession.anonymous();

    String key = underTest.createDeveloper(dbTester.getSession(), NewComponent.create("DEV:jon.name@mail.com", "John").setQualifier("DEV")).getKey();
    dbTester.getSession().commit();

    ComponentDto dev = underTest.getNullableByKey(key);
    assertThat(dev.key()).isEqualTo("DEV:jon.name@mail.com");
    assertThat(dev.deprecatedKey()).isEqualTo("DEV:jon.name@mail.com");
    assertThat(dev.uuid()).isNotNull();
    assertThat(dev.projectUuid()).isEqualTo(dev.uuid());
    assertThat(dev.moduleUuid()).isNull();
    assertThat(dev.moduleUuidPath()).isEqualTo("." + dev.uuid() + ".");
    assertThat(dev.name()).isEqualTo("John");
    assertThat(dev.longName()).isEqualTo("John");
    assertThat(dev.scope()).isEqualTo("PRJ");
    assertThat(dev.qualifier()).isEqualTo("DEV");
    assertThat(dev.getCreatedAt()).isNotNull();

    assertIndexIsEmpty();
  }

  @Test
  public void fail_to_create_new_component_on_invalid_key() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed key for Project: struts?parent. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    underTest.create(dbSession, NewComponent.create("struts?parent", "Struts project"));
  }

  @Test
  public void fail_to_create_new_component_on_invalid_branch() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed branch for Project: origin?branch. Allowed characters are alphanumeric, '-', '_', '.' and '/', with at least one non-digit.");

    userSession.login("john").setGlobalPermissions(PROVISIONING);

    underTest.create(dbSession, NewComponent.create("struts", "Struts project").setBranch("origin?branch"));
  }

  @Test
  public void fail_to_create_new_component_if_key_already_exists() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Could not create Project, key already exists: struts");

    userSession.login("john").setGlobalPermissions(PROVISIONING);
    ComponentDto project = ComponentTesting.newProjectDto().setKey("struts");
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    underTest.create(dbSession, NewComponent.create("struts", "Struts project"));
  }

  @Test
  public void remove_duplicated_components_when_creating_project() throws Exception {
    String projectKey = "PROJECT_KEY";

    userSession.login("john").setGlobalPermissions(PROVISIONING);

    DbSession session = mock(DbSession.class);

    ComponentDao componentDao = mock(ComponentDao.class);
    when(componentDao.selectByKey(session, projectKey)).thenReturn(Optional.absent());

    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.componentIndexDao()).thenReturn(mock(ResourceIndexDao.class));

    doAnswer(invocation -> {
      ((ComponentDto) invocation.getArguments()[1]).setId(1L);
      return null;
    }).when(componentDao).insert(eq(session), any(ComponentDto.class));

    when(componentDao.selectComponentsHavingSameKeyOrderedById(session, projectKey)).thenReturn(newArrayList(
      ComponentTesting.newProjectDto().setId(1L).setKey(projectKey),
      ComponentTesting.newProjectDto().setId(2L).setKey(projectKey),
      ComponentTesting.newProjectDto().setId(3L).setKey(projectKey)));

    underTest = new ComponentService(dbClient, i18n, userSession, System2.INSTANCE, new ComponentFinder(dbClient), projectMeasuresIndexer);
    underTest.create(session, NewComponent.create(projectKey, projectKey));

    verify(componentDao).delete(session, 2L);
    verify(componentDao).delete(session, 3L);
  }

  @Test
  public void should_return_project_uuids() {
    ComponentDto project = insertSampleProject();
    String moduleKey = "sample:root:module";
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey(moduleKey);
    dbClient.componentDao().insert(dbSession, module);
    String fileKey = "sample:root:module:Foo.xoo";
    ComponentDto file = newFileDto(module, null).setKey(fileKey);
    dbClient.componentDao().insert(dbSession, file);
    dbSession.commit();

    assertThat(underTest.componentUuids(Arrays.asList(moduleKey, fileKey))).hasSize(2);
    assertThat(underTest.componentUuids(null)).isEmpty();
    assertThat(underTest.componentUuids(Arrays.<String>asList())).isEmpty();
  }

  @Test
  public void should_fail_on_components_not_found() {
    String moduleKey = "sample:root:module";
    String fileKey = "sample:root:module:Foo.xoo";

    try {
      underTest.componentUuids(Arrays.asList(moduleKey, fileKey));
      Fail.fail("Should throw NotFoundException");
    } catch (NotFoundException notFound) {
      assertThat(notFound.getMessage()).contains(moduleKey).contains(fileKey);
    }
  }

  @Test
  public void should_fail_silently_on_components_not_found_if_told_so() {
    String moduleKey = "sample:root:module";
    String fileKey = "sample:root:module:Foo.xoo";

    assertThat(underTest.componentUuids(dbSession, Arrays.asList(moduleKey, fileKey), true)).isEmpty();
  }

  @Test
  public void bulk_update() {
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
  }

  private void assertComponentKeyUpdated(String oldKey, String newKey) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, oldKey)).isAbsent();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newKey)).isPresent();
  }

  private void assertComponentKeyNotUpdated(String key) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, key)).isPresent();
  }

  private ComponentDto insertSampleProject() {
    return componentDb.insertComponent(newProjectDto().setKey("sample:root"));
  }

  private void assertProjectIsInIndex(String uuid) {
    assertThat(es.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES)).containsOnly(uuid);
  }

  private void assertIndexIsEmpty() {
    assertThat(es.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES)).isEmpty();
  }

}
