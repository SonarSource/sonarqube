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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
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
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURE;

public class ComponentServiceTest {

  private System2 system2 = System2.INSTANCE;

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
  private I18nRule i18n = new I18nRule();
  private ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(system2, dbClient, es.client());
  private ComponentIndexer componentIndexer = new ComponentIndexer(dbClient, es.client());
  private OrganizationDto organization;

  private ComponentService underTest;

  @Before
  public void setUp() {
    i18n.put("qualifier.TRK", "Project");

    underTest = new ComponentService(dbClient, i18n, userSession, system2, new ComponentFinder(dbClient), projectMeasuresIndexer, componentIndexer);
    organization = dbTester.organizations().insert();
  }

  @Test
  public void get_by_key() {
    ComponentDto project = insertSampleProject();
    assertThat(underTest.getByKey(project.getKey())).isNotNull();
  }

  @Test
  public void create_project() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);

    String key = underTest.create(
      dbSession,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey("struts")
        .setName("Struts project")
        .build())
      .getKey();

    ComponentDto project = dbClient.componentDao().selectOrFailByKey(dbSession, key);
    assertThat(project.getOrganizationUuid()).isEqualTo(organization.getUuid());
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

    String key = underTest.create(
      dbSession,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey("struts")
        .setName("Struts project")
        .setBranch("origin/branch")
        .build())
      .getKey();

    ComponentDto project = dbClient.componentDao().selectOrFailByKey(dbSession, key);
    assertThat(project.getOrganizationUuid()).isEqualTo(organization.getUuid());
    assertThat(project.key()).isEqualTo("struts:origin/branch");
    assertThat(project.deprecatedKey()).isEqualTo("struts:origin/branch");
  }

  @Test
  public void create_view() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);

    String key = underTest.create(
      dbSession,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey("all-project")
        .setName("All Projects")
        .setQualifier(Qualifiers.VIEW)
        .build())
      .getKey();

    ComponentDto project = dbClient.componentDao().selectOrFailByKey(dbSession, key);
    assertThat(project.getOrganizationUuid()).isEqualTo(organization.getUuid());
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

    String key = underTest.createDeveloper(
      dbSession,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey("DEV:jon.name@mail.com")
        .setName("John")
        .setQualifier("DEV")
        .build())
      .getKey();
    dbTester.getSession().commit();

    ComponentDto dev = dbClient.componentDao().selectOrFailByKey(dbSession, key);
    assertThat(dev.getOrganizationUuid()).isEqualTo(organization.getUuid());
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

    underTest.create(
      dbSession,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey("struts?parent")
        .setName("Struts project")
        .build());
  }

  @Test
  public void fail_to_create_new_component_on_invalid_branch() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed branch for Project: origin?branch. Allowed characters are alphanumeric, '-', '_', '.' and '/', with at least one non-digit.");

    userSession.login("john").setGlobalPermissions(PROVISIONING);

    underTest.create(
      dbSession,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey("struts")
        .setName("Struts project")
        .setBranch("origin?branch")
        .build());
  }

  @Test
  public void fail_to_create_new_component_if_key_already_exists() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Could not create Project, key already exists: struts");

    userSession.login("john").setGlobalPermissions(PROVISIONING);
    ComponentDto project = ComponentTesting.newProjectDto(dbTester.organizations().insert()).setKey("struts");
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    underTest.create(
      dbSession,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey("struts")
        .setName("Struts project")
        .build());
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

    OrganizationDto organizationDto = dbTester.organizations().insert();
    when(componentDao.selectComponentsHavingSameKeyOrderedById(session, projectKey)).thenReturn(newArrayList(
      ComponentTesting.newProjectDto(organizationDto).setId(1L).setKey(projectKey),
      ComponentTesting.newProjectDto(organizationDto).setId(2L).setKey(projectKey),
      ComponentTesting.newProjectDto(organizationDto).setId(3L).setKey(projectKey)));

    underTest = new ComponentService(dbClient, i18n, userSession, System2.INSTANCE, new ComponentFinder(dbClient), projectMeasuresIndexer, componentIndexer);
    underTest.create(
      session,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey(projectKey)
        .setName(projectKey)
        .build());

    verify(componentDao).delete(session, 2L);
    verify(componentDao).delete(session, 3L);
  }

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

  private void assertProjectIsInIndex(String uuid) {
    assertThat(es.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(uuid);
  }

  private void assertIndexIsEmpty() {
    assertThat(es.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).isEmpty();
  }

}
