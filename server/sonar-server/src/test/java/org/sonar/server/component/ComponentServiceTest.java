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

package org.sonar.server.component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class ComponentServiceTest {

  System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  DbClient dbClient = dbTester.getDbClient();
  DbSession session = dbTester.getSession();
  I18n i18n = mock(I18n.class);
  ComponentService service;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    when(i18n.message(Locale.getDefault(), "qualifier.TRK", "Project")).thenReturn("Project");
    service = new ComponentService(dbClient, i18n, userSessionRule, System2.INSTANCE, new ComponentFinder(dbClient));
  }

  @Test
  public void get_by_key() {
    ComponentDto project = createProject("sample:root");
    assertThat(service.getByKey(project.getKey())).isNotNull();
  }

  @Test
  public void get_nullable_by_key() {
    ComponentDto project = createProject("sample:root");
    assertThat(service.getNullableByKey(project.getKey())).isNotNull();
    assertThat(service.getNullableByKey("unknown")).isNull();
  }

  @Test
  public void get_by_uuid() {
    ComponentDto project = createProject("sample:root");
    assertThat(service.getNonNullByUuid(project.uuid())).isNotNull();
  }

  @Test
  public void get_nullable_by_uuid() {
    ComponentDto project = createProject("sample:root");
    assertThat(service.getByUuid(project.uuid())).isPresent();
    assertThat(service.getByUuid("unknown")).isAbsent();
  }

  @Test
  public void update_project_key() {
    ComponentDto project = createProject("sample:root");
    ComponentDto file = ComponentTesting.newFileDto(project).setKey("sample:root:src/File.xoo");
    dbClient.componentDao().insert(session, file);

    session.commit();

    userSessionRule.login("john").addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    service.updateKey(project.key(), "sample2:root");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(project.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:src/File.xoo")).isNotNull();
  }

  @Test
  public void update_module_key() {
    ComponentDto project = createProject("sample:root");
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    dbClient.componentDao().insert(session, module);

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    dbClient.componentDao().insert(session, file);

    session.commit();

    userSessionRule.login("john").addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    service.updateKey(module.key(), "sample:root2:module");
    session.commit();

    // Project key has not changed
    assertThat(service.getNullableByKey(project.key())).isNotNull();

    // Check module key has been updated
    assertThat(service.getNullableByKey(module.key())).isNull();
    assertThat(service.getNullableByKey("sample:root2:module")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample:root2:module:src/File.xoo")).isNotNull();
  }

  @Test
  public void update_provisioned_project_key() {
    ComponentDto provisionedProject = ComponentTesting.newProjectDto().setKey("provisionedProject");
    dbClient.componentDao().insert(session, provisionedProject);

    session.commit();

    userSessionRule.login("john").addProjectUuidPermissions(UserRole.ADMIN, provisionedProject.uuid());
    service.updateKey(provisionedProject.key(), "provisionedProject2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(provisionedProject.key())).isNull();
    assertThat(service.getNullableByKey("provisionedProject2")).isNotNull();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_update_project_key_without_admin_permission() {
    ComponentDto project = createProject("sample:root");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, project.uuid());
    service.updateKey(project.key(), "sample2:root");
  }

  @Test
  public void check_module_keys_before_renaming() {
    ComponentDto project = createProject("sample:root");
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    dbClient.componentDao().insert(session, module);

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    dbClient.componentDao().insert(session, file);

    session.commit();

    userSessionRule.login("john").addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    Map<String, String> result = service.checkModuleKeysBeforeRenaming(project.key(), "sample", "sample2");

    assertThat(result).hasSize(2);
    assertThat(result.get("sample:root")).isEqualTo("sample2:root");
    assertThat(result.get("sample:root:module")).isEqualTo("sample2:root:module");
  }

  @Test
  public void check_module_keys_before_renaming_return_duplicate_key() {
    ComponentDto project = createProject("sample:root");
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    dbClient.componentDao().insert(session, module);

    ComponentDto module2 = ComponentTesting.newModuleDto(project).setKey("foo:module");
    dbClient.componentDao().insert(session, module2);

    session.commit();

    userSessionRule.login("john").addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    Map<String, String> result = service.checkModuleKeysBeforeRenaming(project.key(), "sample:root", "foo");

    assertThat(result).hasSize(2);
    assertThat(result.get("sample:root")).isEqualTo("foo");
    assertThat(result.get("sample:root:module")).isEqualTo("#duplicate_key#");
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_check_module_keys_before_renaming_without_admin_permission() {
    ComponentDto project = createProject("sample:root");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, project.uuid());
    service.checkModuleKeysBeforeRenaming(project.key(), "sample", "sample2");
  }

  @Test
  public void bulk_update_project_key() {
    ComponentDto project = createProject("sample:root");
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    dbClient.componentDao().insert(session, module);

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    dbClient.componentDao().insert(session, file);

    session.commit();

    userSessionRule.login("john").addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    service.bulkUpdateKey(project.key(), "sample", "sample2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(project.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root")).isNotNull();

    // Check module key has been updated
    assertThat(service.getNullableByKey(module.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:module")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:module:src/File.xoo")).isNotNull();
  }

  @Test
  public void bulk_update_provisioned_project_key() {
    ComponentDto provisionedProject = ComponentTesting.newProjectDto().setKey("provisionedProject");
    dbClient.componentDao().insert(session, provisionedProject);

    session.commit();

    userSessionRule.login("john").addProjectUuidPermissions(UserRole.ADMIN, provisionedProject.uuid());
    service.bulkUpdateKey(provisionedProject.key(), "provisionedProject", "provisionedProject2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(provisionedProject.key())).isNull();
    assertThat(service.getNullableByKey("provisionedProject2")).isNotNull();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_bulk_update_project_key_without_admin_permission() {
    ComponentDto project = createProject("sample:root");
    userSessionRule.login("john").addProjectPermissions(UserRole.USER, project.key());
    service.bulkUpdateKey("sample:root", "sample", "sample2");
  }

  @Test
  public void create_project() {
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.PROVISIONING);

    String key = service.create(NewComponent.create("struts", "Struts project"));

    ComponentDto project = service.getNullableByKey(key);
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
  }

  @Test
  public void create_new_project_with_branch() {
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.PROVISIONING);

    String key = service.create(NewComponent.create("struts", "Struts project").setBranch("origin/branch"));

    ComponentDto project = service.getNullableByKey(key);
    assertThat(project.key()).isEqualTo("struts:origin/branch");
    assertThat(project.deprecatedKey()).isEqualTo("struts:origin/branch");
  }

  @Test
  public void create_view() {
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.PROVISIONING);

    String key = service.create(NewComponent.create("all-project", "All Projects").setQualifier(Qualifiers.VIEW));

    ComponentDto project = service.getNullableByKey(key);
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
  }

  @Test
  public void fail_to_create_new_component_on_invalid_key() {
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.PROVISIONING);

    try {
      service.create(NewComponent.create("struts?parent", "Struts project"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage(
        "Malformed key for Project: struts?parent. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");
    }
  }

  @Test
  public void fail_to_create_new_component_on_invalid_branch() {
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.PROVISIONING);

    try {
      service.create(NewComponent.create("struts", "Struts project").setBranch("origin?branch"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage(
        "Malformed branch for Project: origin?branch. Allowed characters are alphanumeric, '-', '_', '.' and '/', with at least one non-digit.");
    }
  }

  @Test
  public void fail_to_create_new_component_if_key_already_exists() {
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.PROVISIONING);

    ComponentDto project = ComponentTesting.newProjectDto().setKey("struts");
    dbClient.componentDao().insert(session, project);
    session.commit();

    try {
      service.create(NewComponent.create("struts", "Struts project"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Could not create Project, key already exists: struts");
    }
  }

  @Test
  public void should_return_project_uuids() {
    ComponentDto project = createProject("sample:root");
    String moduleKey = "sample:root:module";
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey(moduleKey);
    dbClient.componentDao().insert(session, module);
    String fileKey = "sample:root:module:Foo.xoo";
    ComponentDto file = ComponentTesting.newFileDto(module).setKey(fileKey);
    dbClient.componentDao().insert(session, file);
    session.commit();

    assertThat(service.componentUuids(Arrays.asList(moduleKey, fileKey))).hasSize(2);
    assertThat(service.componentUuids(null)).isEmpty();
    assertThat(service.componentUuids(Arrays.<String>asList())).isEmpty();
  }

  @Test
  public void should_fail_on_components_not_found() {
    String moduleKey = "sample:root:module";
    String fileKey = "sample:root:module:Foo.xoo";

    try {
      service.componentUuids(Arrays.asList(moduleKey, fileKey));
      Fail.fail("Should throw NotFoundException");
    } catch (NotFoundException notFound) {
      assertThat(notFound.getMessage()).contains(moduleKey).contains(fileKey);
    }
  }

  @Test
  public void should_fail_silently_on_components_not_found_if_told_so() {
    String moduleKey = "sample:root:module";
    String fileKey = "sample:root:module:Foo.xoo";

    assertThat(service.componentUuids(session, Arrays.asList(moduleKey, fileKey), true)).isEmpty();
  }

  private ComponentDto createProject(String key) {
    ComponentDto project = ComponentTesting.newProjectDto().setKey("sample:root");
    dbClient.componentDao().insert(session, project);
    session.commit();
    return project;
  }

}
