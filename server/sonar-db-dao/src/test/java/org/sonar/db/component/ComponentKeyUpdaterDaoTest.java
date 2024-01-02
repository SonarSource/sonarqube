/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentKeyNewValue;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentKeyUpdaterDao.computeNewKey;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

public class ComponentKeyUpdaterDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final ComponentKeyUpdaterDao underTest = db.getDbClient().componentKeyUpdaterDao();
  private final ComponentKeyUpdaterDao underTestWithAuditPersister = new ComponentKeyUpdaterDao(auditPersister);

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
        .setKey("my_project"));
    ComponentDto directory = db.components().insertComponent(
      newDirectory(project, "B")
        .setKey("my_project:directory"));
    db.components().insertComponent(newFileDto(project, directory).setKey("my_project:directory/file"));
    ComponentDto inactiveDirectory = db.components().insertComponent(newDirectory(project, "/inactive_directory").setKey("my_project:inactive_directory").setEnabled(false));
    db.components().insertComponent(newFileDto(project, inactiveDirectory).setKey("my_project:inactive_directory/file").setEnabled(false));

    underTest.updateKey(dbSession, "A", "your_project");
    dbSession.commit();

    List<ComponentDto> result = dbClient.componentDao().selectByBranchUuid("A", dbSession);
    assertThat(result)
      .hasSize(5)
      .extracting(ComponentDto::getKey)
      .containsOnlyOnce("your_project", "your_project:directory", "your_project:directory/file", "your_project:inactive_directory", "your_project:inactive_directory/file");
  }

  @Test
  public void updateKey_updates_branches_too() {
    ComponentDto project = db.components().insertPublicProject();
    String branchName = randomAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));
    db.components().insertComponent(newFileDto(branch));
    db.components().insertComponent(newFileDto(branch));
    int prComponentCount = 3;

    String oldProjectKey = project.getKey();
    assertThat(dbClient.componentDao().selectByBranchUuid(project.uuid(), dbSession)).hasSize(1);
    assertThat(dbClient.componentDao().selectByBranchUuid(branch.uuid(), dbSession)).hasSize(prComponentCount);

    String newProjectKey = "newKey";
    underTest.updateKey(dbSession, project.uuid(), newProjectKey);

    assertThat(dbClient.componentDao().selectByKey(dbSession, oldProjectKey)).isEmpty();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newProjectKey)).isPresent();
    assertThat(dbClient.componentDao().selectByKeyAndBranch(dbSession, newProjectKey, branchName)).isPresent();
    assertThat(dbClient.componentDao().selectByBranchUuid(project.uuid(), dbSession)).hasSize(1);
    assertThat(dbClient.componentDao().selectByBranchUuid(branch.uuid(), dbSession)).hasSize(prComponentCount);

    db.select(dbSession, "select kee from components")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));
  }

  @Test
  public void updateKey_updates_pull_requests_too() {
    ComponentDto project = db.components().insertPublicProject();
    String pullRequestKey1 = randomAlphanumeric(100);
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST).setKey(pullRequestKey1));
    db.components().insertComponent(newFileDto(pullRequest));
    db.components().insertComponent(newFileDto(pullRequest));
    int prComponentCount = 3;

    String oldProjectKey = project.getKey();
    assertThat(dbClient.componentDao().selectByBranchUuid(project.uuid(), dbSession)).hasSize(1);
    assertThat(dbClient.componentDao().selectByBranchUuid(pullRequest.uuid(), dbSession)).hasSize(prComponentCount);

    String newProjectKey = "newKey";
    underTest.updateKey(dbSession, project.uuid(), newProjectKey);

    assertThat(dbClient.componentDao().selectByKey(dbSession, oldProjectKey)).isEmpty();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newProjectKey)).isPresent();
    assertThat(dbClient.componentDao().selectByKeyAndPullRequest(dbSession, newProjectKey, pullRequestKey1)).isPresent();

    assertThat(dbClient.componentDao().selectByBranchUuid(project.uuid(), dbSession)).hasSize(1);
    assertThat(dbClient.componentDao().selectByBranchUuid(pullRequest.uuid(), dbSession)).hasSize(prComponentCount);

    db.select(dbSession, "select kee from components")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));
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
    ComponentDto project = newPrivateProjectDto("project-uuid").setKey("old-project-key");
    db.components().insertComponent(project);
    db.components().insertComponent(newFileDto(project, null).setKey("old-project-key:file"));
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
    db.components().insertComponent(newPrivateProjectDto("A").setKey("my_project"));

    underTestWithAuditPersister.updateKey(dbSession, "A", "your_project");

    verify(auditPersister, times(1))
      .componentKeyUpdate(any(DbSession.class), any(ComponentKeyNewValue.class), anyString());
  }

  private void populateSomeData() {
    ComponentDto project1 = db.components().insertPrivateProject(t -> t.setKey("org.struts:struts").setUuid("A"));
    ComponentDto module1 = db.components().insertComponent(newModuleDto(project1).setKey("org.struts:struts-core").setUuid("B"));
    ComponentDto directory1 = db.components().insertComponent(newDirectory(module1, "/src/org/struts").setUuid("C"));
    db.components().insertComponent(ComponentTesting.newFileDto(module1, directory1).setKey("org.struts:struts-core:/src/org/struts/RequestContext.java").setUuid("D"));
    ComponentDto module2 = db.components().insertComponent(newModuleDto(project1).setKey("org.struts:struts-ui").setUuid("E"));
    ComponentDto directory2 = db.components().insertComponent(newDirectory(module2, "/src/org/struts").setUuid("F"));
    db.components().insertComponent(ComponentTesting.newFileDto(module2, directory2).setKey("org.struts:struts-ui:/src/org/struts/RequestContext.java").setUuid("G"));
    ComponentDto project2 = db.components().insertPublicProject(t -> t.setKey("foo:struts-core").setUuid("H"));
  }
}
