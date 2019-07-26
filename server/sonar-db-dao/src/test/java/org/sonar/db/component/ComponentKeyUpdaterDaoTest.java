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
package org.sonar.db.component;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentKeyUpdaterDao.RekeyedResource;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
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
    populateSomeData();

    underTest.updateKey(dbSession, "B", "struts:core");
    dbSession.commit();

    assertThat(db.select("select uuid as \"UUID\", kee as \"KEE\" from projects"))
      .extracting(t -> (String) t.get("UUID"), t -> (String) t.get("KEE"))
      .containsOnly(
        Tuple.tuple("A", "org.struts:struts"),
        Tuple.tuple("B", "struts:core"),
        Tuple.tuple("C", "struts:core:/src/org/struts"),
        Tuple.tuple("D", "struts:core:/src/org/struts/RequestContext.java"),
        Tuple.tuple("E", "org.struts:struts-ui"),
        Tuple.tuple("F", "org.struts:struts-ui:/src/org/struts"),
        Tuple.tuple("G", "org.struts:struts-ui:/src/org/struts/RequestContext.java"),
        Tuple.tuple("H", "foo:struts-core"));
  }

  @Test
  public void updateKey_updates_disabled_components() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(
      newPrivateProjectDto(organizationDto, "A")
        .setDbKey("my_project"));
    ComponentDto directory = db.components().insertComponent(
      newDirectory(project, "B")
        .setDbKey("my_project:directory"));
    db.components().insertComponent(newFileDto(project, directory).setDbKey("my_project:directory/file"));
    ComponentDto inactiveDirectory = db.components().insertComponent(newDirectory(project, "/inactive_directory").setDbKey("my_project:inactive_directory").setEnabled(false));
    db.components().insertComponent(newFileDto(project, inactiveDirectory).setDbKey("my_project:inactive_directory/file").setEnabled(false));

    underTest.updateKey(dbSession, "A", "your_project");
    dbSession.commit();

    List<ComponentDto> result = dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, "your_project");
    assertThat(result)
      .hasSize(5)
      .extracting(ComponentDto::getDbKey)
      .containsOnlyOnce("your_project", "your_project:directory", "your_project:directory/file", "your_project:inactive_directory", "your_project:inactive_directory/file");
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
  public void updateKey_updates_pull_requests_too() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST));
    db.components().insertComponent(newFileDto(pullRequest));
    db.components().insertComponent(newFileDto(pullRequest));
    int branchComponentCount = 3;

    String oldProjectKey = project.getKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).hasSize(1);

    String oldBranchKey = pullRequest.getDbKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).hasSize(branchComponentCount);

    String newProjectKey = "newKey";
    String newBranchKey = ComponentDto.generatePullRequestKey(newProjectKey, pullRequest.getPullRequest());
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
    ComponentDto file1 = db.components().insertComponent(prefixDbKeyWithKey(newFileDto(module), module.getKey()));
    ComponentDto file2 = db.components().insertComponent(prefixDbKeyWithKey(newFileDto(module), module.getKey()));
    int branchComponentCount = 4;

    String oldProjectKey = project.getKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).hasSize(1);

    String oldBranchKey = branch.getDbKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).hasSize(branchComponentCount);

    String newProjectKey = "newKey";
    Set<RekeyedResource> rekeyedResources = underTest.bulkUpdateKey(dbSession, project.uuid(), oldProjectKey, newProjectKey, a -> true);

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).isEmpty();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).isEmpty();

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newProjectKey)).hasSize(1);
    String newBranchKey = ComponentDto.generateBranchKey(newProjectKey, branch.getBranch());
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newBranchKey)).hasSize(branchComponentCount);
    db.select(dbSession, "select kee from projects")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));

    assertThat(rekeyedResources)
      .extracting(t -> t.getResource().getUuid())
      .containsOnly(project.uuid(), branch.uuid(), module.uuid(), file1.uuid(), file2.uuid());
    assertThat(rekeyedResources)
      .extracting(t -> t.getResource().getKey())
      .allMatch(t -> t.startsWith(newProjectKey));
    assertThat(rekeyedResources)
      .extracting(RekeyedResource::getOldKey)
      .allMatch(t -> t.startsWith(oldProjectKey));
  }

  @Test
  public void bulk_updateKey_on_branch_containing_slash() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch/with/slash"));
    String newKey = "newKey";

    underTest.bulkUpdateKey(dbSession, project.uuid(), project.getKey(), newKey, t -> true);

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newKey)).hasSize(1);
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, ComponentDto.generateBranchKey(newKey, branch.getBranch()))).hasSize(1);
  }

  @Test
  public void bulk_updateKey_updates_pull_requests_too() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST));
    ComponentDto module = db.components().insertComponent(prefixDbKeyWithKey(newModuleDto(pullRequest), project.getKey()));
    ComponentDto file1 = db.components().insertComponent(prefixDbKeyWithKey(newFileDto(module), module.getKey()));
    ComponentDto file2 = db.components().insertComponent(prefixDbKeyWithKey(newFileDto(module), module.getKey()));
    int branchComponentCount = 4;

    String oldProjectKey = project.getKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).hasSize(1);

    String oldPullRequestKey = pullRequest.getDbKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldPullRequestKey)).hasSize(branchComponentCount);

    String newProjectKey = "newKey";
    String newPullRequestKey = ComponentDto.generatePullRequestKey(newProjectKey, pullRequest.getPullRequest());
    Set<RekeyedResource> rekeyedResources = underTest.bulkUpdateKey(dbSession, project.uuid(), oldProjectKey, newProjectKey, t -> true);

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldProjectKey)).isEmpty();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldPullRequestKey)).isEmpty();

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newProjectKey)).hasSize(1);
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newPullRequestKey)).hasSize(branchComponentCount);
    db.select(dbSession, "select kee from projects")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));

    assertThat(rekeyedResources)
      .extracting(t -> t.getResource().getUuid())
      .containsOnly(project.uuid(), pullRequest.uuid(), module.uuid(), file1.uuid(), file2.uuid());
    assertThat(rekeyedResources)
      .extracting(t -> t.getResource().getKey())
      .allMatch(t -> t.startsWith(newProjectKey));
    assertThat(rekeyedResources)
      .extracting(RekeyedResource::getOldKey)
      .allMatch(t -> t.startsWith(oldProjectKey));
  }

  private ComponentDto prefixDbKeyWithKey(ComponentDto componentDto, String key) {
    return componentDto.setDbKey(key + ":" + componentDto.getDbKey());
  }

  @Test
  public void updateKey_throws_IAE_if_component_with_specified_key_does_not_exist() {
    populateSomeData();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Impossible to update key: a component with key \"org.struts:struts-ui\" already exists.");

    underTest.updateKey(dbSession, "B", "org.struts:struts-ui");
  }

  @Test
  public void bulk_update_key_updates_disabled_components() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "A").setDbKey("my_project"));
    db.components().insertComponent(newModuleDto(project).setDbKey("my_project:module"));
    db.components().insertComponent(newModuleDto(project).setDbKey("my_project:inactive_module").setEnabled(false));

    Set<RekeyedResource> rekeyedResources = underTest.bulkUpdateKey(dbSession, "A", "my_", "your_", doNotReturnAnyRekeyedResource());

    List<ComponentDto> result = dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, "your_project");
    assertThat(result)
      .hasSize(3)
      .extracting(ComponentDto::getDbKey)
      .containsOnlyOnce("your_project", "your_project:module", "your_project:inactive_module");
  }

  @Test
  public void shouldBulkUpdateKey() {
    populateSomeData();

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "org.apache.struts", doNotReturnAnyRekeyedResource());
    dbSession.commit();

    assertThat(db.select("select uuid as \"UUID\", kee as \"KEE\" from projects"))
      .extracting(t -> (String) t.get("UUID"), t -> (String) t.get("KEE"))
      .containsOnly(
        Tuple.tuple("A", "org.apache.struts:struts"),
        Tuple.tuple("B", "org.apache.struts:struts-core"),
        Tuple.tuple("C", "org.apache.struts:struts-core:/src/org/struts"),
        Tuple.tuple("D", "org.apache.struts:struts-core:/src/org/struts/RequestContext.java"),
        Tuple.tuple("E", "org.apache.struts:struts-ui"),
        Tuple.tuple("F", "org.apache.struts:struts-ui:/src/org/struts"),
        Tuple.tuple("G", "org.apache.struts:struts-ui:/src/org/struts/RequestContext.java"),
        Tuple.tuple("H", "foo:struts-core"));
  }

  @Test
  public void shouldBulkUpdateKeyOnOnlyOneSubmodule() {
    populateSomeData();

    underTest.bulkUpdateKey(dbSession, "A", "struts-ui", "struts-web", doNotReturnAnyRekeyedResource());
    dbSession.commit();

    assertThat(db.select("select uuid as \"UUID\", kee as \"KEE\" from projects"))
      .extracting(t -> (String) t.get("UUID"), t -> (String) t.get("KEE"))
      .containsOnly(
        Tuple.tuple("A", "org.struts:struts"),
        Tuple.tuple("B", "org.struts:struts-core"),
        Tuple.tuple("C", "org.struts:struts-core:/src/org/struts"),
        Tuple.tuple("D", "org.struts:struts-core:/src/org/struts/RequestContext.java"),
        Tuple.tuple("E", "org.struts:struts-web"),
        Tuple.tuple("F", "org.struts:struts-web:/src/org/struts"),
        Tuple.tuple("G", "org.struts:struts-web:/src/org/struts/RequestContext.java"),
        Tuple.tuple("H", "foo:struts-core"));
  }

  @Test
  public void shouldFailBulkUpdateKeyIfKeyAlreadyExist() {
    populateSomeData();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Impossible to update key: a component with key \"foo:struts-core\" already exists.");

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "foo", doNotReturnAnyRekeyedResource());
    dbSession.commit();
  }

  @Test
  public void shouldNotUpdateAllSubmodules() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization, t1 -> t1.setDbKey("org.struts:struts").setUuid("A"));
    ComponentDto module1 = db.components().insertComponent(newModuleDto(project1).setDbKey("org.struts:struts-core").setUuid("B"));
    ComponentDto directory1 = db.components().insertComponent(newDirectory(module1, "/src/org/struts").setUuid("C"));
    db.components().insertComponent(ComponentTesting.newFileDto(module1, directory1).setDbKey("org.struts:struts-core:/src/org/struts/RequestContext.java").setUuid("D"));
    ComponentDto module2 = db.components().insertComponent(newModuleDto(project1).setDbKey("foo:struts-ui").setUuid("E"));
    ComponentDto directory2 = db.components().insertComponent(newDirectory(module2, "/src/org/struts").setUuid("F"));
    db.components().insertComponent(ComponentTesting.newFileDto(module2, directory2).setDbKey("foo:struts-ui:/src/org/struts/RequestContext.java").setUuid("G"));
    ComponentDto project2 = db.components().insertPublicProject(organization, t1 -> t1.setDbKey("foo:struts-core").setUuid("H"));

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "org.apache.struts", doNotReturnAnyRekeyedResource());
    dbSession.commit();

    assertThat(db.select("select uuid as \"UUID\", kee as \"KEE\" from projects"))
      .extracting(t -> (String) t.get("UUID"), t -> (String) t.get("KEE"))
      .containsOnly(
        Tuple.tuple("A", "org.apache.struts:struts"),
        Tuple.tuple("B", "org.apache.struts:struts-core"),
        Tuple.tuple("C", "org.apache.struts:struts-core:/src/org/struts"),
        Tuple.tuple("D", "org.apache.struts:struts-core:/src/org/struts/RequestContext.java"),
        Tuple.tuple("E", "foo:struts-ui"),
        Tuple.tuple("F", "foo:struts-ui:/src/org/struts"),
        Tuple.tuple("G", "foo:struts-ui:/src/org/struts/RequestContext.java"),
        Tuple.tuple("H", "foo:struts-core"));
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
    thrown.expectMessage("Malformed key for '  '. Project key cannot be empty nor contain whitespaces.");

    underTest.bulkUpdateKey(dbSession, project.uuid(), project.getDbKey(), "  ", doNotReturnAnyRekeyedResource());
  }

  @Test
  public void check_component_keys() {
    populateSomeData();

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
    populateSomeData();

    Map<String, String> result = underTest.simulateBulkUpdateKey(dbSession, "A", "org.struts", "foo");

    assertThat(result)
      .hasSize(3)
      .containsOnly(entry("org.struts:struts", "foo:struts"), entry("org.struts:struts-core", "foo:struts-core"), entry("org.struts:struts-ui", "foo:struts-ui"));
  }

  @Test
  public void simulate_bulk_update_key_does_not_return_disable_components() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "A").setDbKey("project"));
    db.components().insertComponent(newModuleDto(project).setDbKey("project:enabled-module"));
    db.components().insertComponent(newModuleDto(project).setDbKey("project:disabled-module").setEnabled(false));
    db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "D").setDbKey("other-project"));

    Map<String, String> result = underTest.simulateBulkUpdateKey(dbSession, "A", "project", "new-project");

    assertThat(result).containsOnly(
      entry("project", "new-project"),
      entry("project:enabled-module", "new-project:enabled-module"));
  }

  @Test
  public void simulate_bulk_update_key_fails_if_invalid_componentKey() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organizationDto, "A").setDbKey("project"));
    db.components().insertComponent(newModuleDto(project).setDbKey("project:enabled-module"));
    db.components().insertComponent(newModuleDto(project).setDbKey("project:disabled-module").setEnabled(false));

    thrown.expect(IllegalArgumentException.class);

    underTest.simulateBulkUpdateKey(dbSession, "A", "project", "  ");
  }

  @Test
  public void compute_new_key() {
    assertThat(computeNewKey("my_project", "my_", "your_")).isEqualTo("your_project");
    assertThat(computeNewKey("my_project", "my_", "$()_")).isEqualTo("$()_project");
  }

  private Predicate<RekeyedResource> doNotReturnAnyRekeyedResource() {
    return a -> false;
  }

  private void populateSomeData() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization, t -> t.setDbKey("org.struts:struts").setUuid("A"));
    ComponentDto module1 = db.components().insertComponent(newModuleDto(project1).setDbKey("org.struts:struts-core").setUuid("B"));
    ComponentDto directory1 = db.components().insertComponent(newDirectory(module1, "/src/org/struts").setUuid("C"));
    db.components().insertComponent(ComponentTesting.newFileDto(module1, directory1).setDbKey("org.struts:struts-core:/src/org/struts/RequestContext.java").setUuid("D"));
    ComponentDto module2 = db.components().insertComponent(newModuleDto(project1).setDbKey("org.struts:struts-ui").setUuid("E"));
    ComponentDto directory2 = db.components().insertComponent(newDirectory(module2, "/src/org/struts").setUuid("F"));
    db.components().insertComponent(ComponentTesting.newFileDto(module2, directory2).setDbKey("org.struts:struts-ui:/src/org/struts/RequestContext.java").setUuid("G"));
    ComponentDto project2 = db.components().insertPublicProject(organization, t -> t.setDbKey("foo:struts-core").setUuid("H"));
  }
}
