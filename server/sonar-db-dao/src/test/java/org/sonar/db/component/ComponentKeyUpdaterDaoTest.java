/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.component;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.db.component.ComponentKeyUpdaterDao.computeNewKey;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

public class ComponentKeyUpdaterDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentKeyUpdaterDao underTest = db.getDbClient().componentKeyUpdaterDao();

  @Test
  public void updateKey_changes_the_key_of_tree_of_components() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.updateKey(dbSession, "B", "struts:core");
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldUpdateKey-result.xml", "projects");
  }

  @Test
  public void updateKey_does_not_update_inactive_components() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organizationDto, "A").setDbKey("my_project"));
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "/directory").setDbKey("my_project:directory"));
    db.components().insertComponent(newFileDto(project, directory).setDbKey("my_project:directory/file"));
    ComponentDto inactiveDirectory = db.components().insertComponent(newDirectory(project, "/inactive_directory").setDbKey("my_project:inactive_directory").setEnabled(false));
    db.components().insertComponent(newFileDto(project, inactiveDirectory).setDbKey("my_project:inactive_directory/file").setEnabled(false));

    underTest.updateKey(dbSession, "A", "your_project");
    dbSession.commit();

    List<ComponentDto> result = dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, "your_project");
    assertThat(result).hasSize(5).extracting(ComponentDto::getDbKey)
      .containsOnlyOnce("your_project", "your_project:directory", "your_project:directory/file", "my_project:inactive_directory", "my_project:inactive_directory/file");
  }

  @Test
  public void updateKey_updates_branches_too() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    db.components().insertComponent(newFileDto(branch));
    db.components().insertComponent(newFileDto(branch));
    int branchComponentCount = 3;

    String oldProjectKey = project.getKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).hasSize(1);

    String oldBranchKey = branch.getDbKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).hasSize(branchComponentCount);

    String newProjectKey = "newKey";
    String newBranchKey = ComponentDto.generateBranchKey(newProjectKey, branch.getBranch());
    underTest.updateKey(dbSession, project.uuid(), newProjectKey);

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).isEmpty();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).isEmpty();

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newProjectKey)).hasSize(1);
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newBranchKey)).hasSize(branchComponentCount);
    db.select(dbSession, "select kee from projects")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));
  }

  @Test
  public void bulk_updateKey_updates_branches_too() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto module = db.components().insertComponent(prefixDbKeyWithKey(newModuleDto(branch), project.getKey()));
    db.components().insertComponent(prefixDbKeyWithKey(newFileDto(module), module.getKey()));
    db.components().insertComponent(prefixDbKeyWithKey(newFileDto(module), module.getKey()));
    int branchComponentCount = 4;

    String oldProjectKey = project.getKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).hasSize(1);

    String oldBranchKey = branch.getDbKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).hasSize(branchComponentCount);

    String newProjectKey = "newKey";
    String newBranchKey = ComponentDto.generateBranchKey(newProjectKey, branch.getBranch());
    underTest.bulkUpdateKey(dbSession, project.uuid(), oldProjectKey, newProjectKey);

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).isEmpty();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).isEmpty();

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newProjectKey)).hasSize(1);
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newBranchKey)).hasSize(branchComponentCount);
    db.select(dbSession, "select kee from projects")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));
  }

  private ComponentDto prefixDbKeyWithKey(ComponentDto componentDto, String key) {
    return componentDto.setDbKey(key + ":" + componentDto.getDbKey());
  }

  @Test
  public void updateKey_throws_IAE_if_component_with_specified_key_does_not_exist() {
    db.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Impossible to update key: a component with key \"org.struts:struts-ui\" already exists.");

    underTest.updateKey(dbSession, "B", "org.struts:struts-ui");
  }

  @Test
  public void bulk_update_key_does_not_update_inactive_components() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "A").setDbKey("my_project"));
    db.components().insertComponent(newModuleDto(project).setDbKey("my_project:module"));
    db.components().insertComponent(newModuleDto(project).setDbKey("my_project:inactive_module").setEnabled(false));

    underTest.bulkUpdateKey(dbSession, "A", "my_", "your_");

    List<ComponentDto> result = dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, "your_project");
    assertThat(result).hasSize(3).extracting(ComponentDto::getDbKey)
      .containsOnlyOnce("your_project", "your_project:module", "my_project:inactive_module");
  }

  @Test
  public void shouldBulkUpdateKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "org.apache.struts");
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldBulkUpdateKey-result.xml", "projects");
  }

  @Test
  public void shouldBulkUpdateKeyOnOnlyOneSubmodule() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.bulkUpdateKey(dbSession, "A", "struts-ui", "struts-web");
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldBulkUpdateKeyOnOnlyOneSubmodule-result.xml", "projects");
  }

  @Test
  public void shouldFailBulkUpdateKeyIfKeyAlreadyExist() {
    db.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Impossible to update key: a component with key \"foo:struts-core\" already exists.");

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "foo");
    dbSession.commit();
  }

  @Test
  public void shouldNotUpdateAllSubmodules() {
    db.prepareDbUnit(getClass(), "shouldNotUpdateAllSubmodules.xml");

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "org.apache.struts");
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldNotUpdateAllSubmodules-result.xml", "projects");
  }

  @Test
  public void updateKey_throws_IAE_when_sub_component_key_is_too_long() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = newPrivateProjectDto(organizationDto, "project-uuid").setDbKey("old-project-key");
    db.components().insertComponent(project);
    db.components().insertComponent(newFileDto(project, null).setDbKey("old-project-key:file"));
    String newLongProjectKey = Strings.repeat("a", 400);
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Component key length (405) is longer than the maximum authorized (400). '" + newLongProjectKey + ":file' was provided.");

    underTest.updateKey(dbSession, project.uuid(), newLongProjectKey);
  }

  @Test
  public void fail_when_new_key_is_invalid() {
    ComponentDto project = db.components().insertPrivateProject();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Malformed key for 'my?project?key'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    underTest.bulkUpdateKey(dbSession, project.uuid(), project.getDbKey(), "my?project?key");
  }

  @Test
  public void check_component_keys() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Map<String, Boolean> result = underTest.checkComponentKeys(dbSession, newArrayList("foo:struts", "foo:struts-core", "foo:struts-ui"));

    assertThat(result)
      .hasSize(3)
      .containsOnly(entry("foo:struts", false), entry("foo:struts-core", true), entry("foo:struts-ui", false));
  }

  @Test
  public void check_component_keys_checks_inactive_components() {
    OrganizationDto organizationDto = db.organizations().insert();
    db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("my-project"));
    db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("your-project").setEnabled(false));

    Map<String, Boolean> result = underTest.checkComponentKeys(dbSession, newArrayList("my-project", "your-project", "new-project"));

    assertThat(result)
      .hasSize(3)
      .containsOnly(entry("my-project", true), entry("your-project", true), entry("new-project", false));
  }

  @Test
  public void simulate_bulk_update_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Map<String, String> result = underTest.simulateBulkUpdateKey(dbSession, "A", "org.struts", "foo");

    assertThat(result)
      .hasSize(3)
      .containsOnly(entry("org.struts:struts", "foo:struts"), entry("org.struts:struts-core", "foo:struts-core"), entry("org.struts:struts-ui", "foo:struts-ui"));
  }

  @Test
  public void simulate_bulk_update_key_do_not_return_disable_components() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "A").setDbKey("project"));
    db.components().insertComponent(newModuleDto(project).setDbKey("project:enabled-module"));
    db.components().insertComponent(newModuleDto(project).setDbKey("project:disabled-module").setEnabled(false));

    Map<String, String> result = underTest.simulateBulkUpdateKey(dbSession, "A", "project", "new-project");

    assertThat(result)
      .hasSize(2)
      .containsOnly(entry("project", "new-project"), entry("project:enabled-module", "new-project:enabled-module"));
  }

  @Test
  public void simulate_bulk_update_key_fails_if_invalid_componentKey() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organizationDto, "A").setDbKey("project"));
    db.components().insertComponent(newModuleDto(project).setDbKey("project:enabled-module"));
    db.components().insertComponent(newModuleDto(project).setDbKey("project:disabled-module").setEnabled(false));

    thrown.expect(IllegalArgumentException.class);

    underTest.simulateBulkUpdateKey(dbSession, "A", "project", "project?");
  }

  @Test
  public void compute_new_key() {
    assertThat(computeNewKey("my_project", "my_", "your_")).isEqualTo("your_project");
    assertThat(computeNewKey("my_project", "my_", "$()_")).isEqualTo("$()_project");
  }
}
