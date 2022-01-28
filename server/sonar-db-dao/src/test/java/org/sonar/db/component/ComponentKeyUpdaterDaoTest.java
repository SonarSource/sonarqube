/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.function.Predicate;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentKeyNewValue;
import org.sonar.db.component.ComponentKeyUpdaterDao.RekeyedResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentDto.BRANCH_KEY_SEPARATOR;
import static org.sonar.db.component.ComponentDto.generateBranchKey;
import static org.sonar.db.component.ComponentKeyUpdaterDao.computeNewKey;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

public class ComponentKeyUpdaterDaoTest {


  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private AuditPersister auditPersister = mock(AuditPersister.class);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentKeyUpdaterDao underTest = db.getDbClient().componentKeyUpdaterDao();
  private ComponentKeyUpdaterDao underTestWithAuditPersister = new ComponentKeyUpdaterDao(auditPersister);

  @Test
  public void updateKey_changes_the_key_of_tree_of_components() {
    populateSomeData();

    underTest.updateKey(dbSession, "B", "struts:core");
    dbSession.commit();

    assertThat(db.select("select uuid as \"UUID\", kee as \"KEE\" from components"))
      .extracting(t -> t.get("UUID"), t -> t.get("KEE"))
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
    ComponentDto project = db.components().insertComponent(
      newPrivateProjectDto("A")
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
  public void update_application_branch_key() {
    ComponentDto app = db.components().insertPublicProject();
    ComponentDto appBranch = db.components().insertProjectBranch(app);
    ComponentDto appBranchProj1 = appBranch.copy()
      .setDbKey(appBranch.getDbKey().replace(BRANCH_KEY_SEPARATOR, "") + "appBranchProj1:BRANCH:1").setUuid("appBranchProj1").setScope(Qualifiers.FILE);
    ComponentDto appBranchProj2 = appBranch.copy()
      .setDbKey(appBranch.getDbKey().replace(BRANCH_KEY_SEPARATOR, "") + "appBranchProj2:BRANCH:2").setUuid("appBranchProj2").setScope(Qualifiers.FILE);
    db.components().insertComponent(appBranchProj1);
    db.components().insertComponent(appBranchProj2);
    int branchComponentCount = 3;

    String oldBranchKey = appBranch.getDbKey();
    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).hasSize(branchComponentCount);

    String newBranchName = "newKey";
    String newAppBranchKey = ComponentDto.generateBranchKey(app.getDbKey(), newBranchName);
    String newAppBranchFragment = app.getDbKey() + newBranchName;
    underTest.updateApplicationBranchKey(dbSession, appBranch.uuid(), app.getDbKey(), newBranchName);

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, oldBranchKey)).isEmpty();

    assertThat(dbClient.componentDao().selectAllComponentsFromProjectKey(dbSession, newAppBranchKey)).hasSize(branchComponentCount);

    List<Map<String, Object>> result = db.select(dbSession, String.format("select kee from components where root_uuid = '%s' and scope != 'PRJ'", appBranch.uuid()));

    assertThat(result).hasSize(2);
    result.forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newAppBranchFragment)));
  }

  @Test
  public void update_application_branch_key_will_fail_if_newKey_exist() {
    ComponentDto app = db.components().insertPublicProject();
    ComponentDto appBranch = db.components().insertProjectBranch(app);
    db.components().insertProjectBranch(app, b -> b.setKey("newName"));

    assertThatThrownBy(() -> underTest.updateApplicationBranchKey(dbSession, appBranch.uuid(), app.getDbKey(), "newName"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Impossible to update key: a component with key \"%s\" already exists.", generateBranchKey(app.getDbKey(), "newName")));
  }

  @Test
  public void updateApplicationBranchKey_callsAuditPersister() {
    ComponentDto app = db.components().insertPublicProject();
    ComponentDto appBranch = db.components().insertProjectBranch(app);
    db.components().insertProjectBranch(app, b -> b.setKey("newName"));

    underTestWithAuditPersister.updateApplicationBranchKey(dbSession, appBranch.uuid(), app.getDbKey(), "newName2");

    verify(auditPersister, times(1))
      .componentKeyBranchUpdate(any(DbSession.class), any(ComponentKeyNewValue.class), anyString());
  }

  @Test
  public void updateKey_updates_branches_too() {
    ComponentDto project = db.components().insertPublicProject();
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
    db.select(dbSession, "select kee from components")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));
  }

  @Test
  public void updateKey_updates_pull_requests_too() {
    ComponentDto project = db.components().insertPublicProject();
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
    db.select(dbSession, "select kee from components")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));
  }

  private ComponentDto prefixDbKeyWithKey(ComponentDto componentDto, String key) {
    return componentDto.setDbKey(key + ":" + componentDto.getDbKey());
  }

  @Test
  public void updateKey_throws_IAE_if_component_with_specified_key_does_not_exist() {
    populateSomeData();

    assertThatThrownBy(() -> underTest.updateKey(dbSession, "B", "org.struts:struts-ui"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Impossible to update key: a component with key \"org.struts:struts-ui\" already exists.");
  }

  @Test
  public void updateKey_throws_IAE_when_sub_component_key_is_too_long() {
    ComponentDto project = newPrivateProjectDto("project-uuid").setDbKey("old-project-key");
    db.components().insertComponent(project);
    db.components().insertComponent(newFileDto(project, null).setDbKey("old-project-key:file"));
    String newLongProjectKey = Strings.repeat("a", 400);

    assertThatThrownBy(() -> underTest.updateKey(dbSession, project.uuid(), newLongProjectKey))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Component key length (405) is longer than the maximum authorized (400). '" + newLongProjectKey + ":file' was provided.");
  }

  @Test
  public void compute_new_key() {
    assertThat(computeNewKey("my_project", "my_", "your_")).isEqualTo("your_project");
    assertThat(computeNewKey("my_project", "my_", "$()_")).isEqualTo("$()_project");
  }

  @Test
  public void updateKey_callsAuditPersister() {
    db.components().insertComponent(newPrivateProjectDto("A").setDbKey("my_project"));

    underTestWithAuditPersister.updateKey(dbSession, "A", "your_project");

    verify(auditPersister, times(1))
      .componentKeyUpdate(any(DbSession.class), any(ComponentKeyNewValue.class), anyString());
  }


  private Predicate<RekeyedResource> doNotReturnAnyRekeyedResource() {
    return a -> false;
  }

  private void populateSomeData() {
    ComponentDto project1 = db.components().insertPrivateProject(t -> t.setDbKey("org.struts:struts").setUuid("A"));
    ComponentDto module1 = db.components().insertComponent(newModuleDto(project1).setDbKey("org.struts:struts-core").setUuid("B"));
    ComponentDto directory1 = db.components().insertComponent(newDirectory(module1, "/src/org/struts").setUuid("C"));
    db.components().insertComponent(ComponentTesting.newFileDto(module1, directory1).setDbKey("org.struts:struts-core:/src/org/struts/RequestContext.java").setUuid("D"));
    ComponentDto module2 = db.components().insertComponent(newModuleDto(project1).setDbKey("org.struts:struts-ui").setUuid("E"));
    ComponentDto directory2 = db.components().insertComponent(newDirectory(module2, "/src/org/struts").setUuid("F"));
    db.components().insertComponent(ComponentTesting.newFileDto(module2, directory2).setDbKey("org.struts:struts-ui:/src/org/struts/RequestContext.java").setUuid("G"));
    ComponentDto project2 = db.components().insertPublicProject(t -> t.setDbKey("foo:struts-core").setUuid("H"));
  }
}
