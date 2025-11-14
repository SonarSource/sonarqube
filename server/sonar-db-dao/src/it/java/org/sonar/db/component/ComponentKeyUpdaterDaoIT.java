/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentKeyNewValue;
import org.sonar.db.project.ProjectDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
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

class ComponentKeyUpdaterDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final ComponentKeyUpdaterDao underTest = db.getDbClient().componentKeyUpdaterDao();
  private final ComponentKeyUpdaterDao underTestWithAuditPersister = new ComponentKeyUpdaterDao(auditPersister);

  @Test
  void updateKey_changes_the_key_of_tree_of_components() {
    ProjectData projectData = populateSomeData();

    underTest.updateKey(dbSession, projectData.getProjectDto().getUuid(), "org.struts:struts", "struts:core");
    dbSession.commit();

    assertThat(db.select("select uuid as \"UUID\", kee as \"KEE\" from components"))
      .extracting(t -> t.get("UUID"), t -> t.get("KEE"))
      .containsOnly(
        Tuple.tuple("A", "struts:core"),
        Tuple.tuple("B", "struts:core:/src/org/struts"),
        Tuple.tuple("C", "struts:core:/src/org/struts/RequestContext.java"),
        Tuple.tuple("D", "foo:struts-core"));
  }

  @Test
  void updateKey_updates_disabled_components() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("my_project"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(
      newDirectory(mainBranch, "B")
        .setKey("my_project:directory"));
    db.components().insertComponent(newFileDto(mainBranch, directory).setKey("my_project:directory/file"));
    ComponentDto inactiveDirectory = db.components().insertComponent(newDirectory(mainBranch, "/inactive_directory").setKey("my_project" +
      ":inactive_directory").setEnabled(false));
    db.components().insertComponent(newFileDto(mainBranch, inactiveDirectory).setKey("my_project:inactive_directory/file").setEnabled(false));

    underTest.updateKey(dbSession, projectData.projectUuid(), "my_project", "your_project");
    dbSession.commit();

    List<ComponentDto> result = dbClient.componentDao().selectByBranchUuid(mainBranch.uuid(), dbSession);
    assertThat(result)
      .hasSize(5)
      .extracting(ComponentDto::getKey)
      .containsOnlyOnce("your_project", "your_project:directory", "your_project:directory/file", "your_project:inactive_directory",
        "your_project:inactive_directory/file");
  }

  @Test
  void updateKey_updates_branches_too() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey(branchName));
    db.components().insertComponent(newFileDto(branch, mainBranch.uuid()));
    db.components().insertComponent(newFileDto(branch, mainBranch.uuid()));
    int prComponentCount = 3;

    String oldProjectKey = mainBranch.getKey();
    assertThat(dbClient.componentDao().selectByBranchUuid(mainBranch.uuid(), dbSession)).hasSize(1);
    assertThat(dbClient.componentDao().selectByBranchUuid(branch.uuid(), dbSession)).hasSize(prComponentCount);

    String newProjectKey = "newKey";
    underTest.updateKey(dbSession, projectData.projectUuid(), projectData.projectKey(), newProjectKey);

    assertThat(dbClient.componentDao().selectByKey(dbSession, oldProjectKey)).isEmpty();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newProjectKey)).isPresent();
    assertThat(dbClient.componentDao().selectByKeyAndBranch(dbSession, newProjectKey, branchName)).isPresent();
    assertThat(dbClient.componentDao().selectByBranchUuid(mainBranch.uuid(), dbSession)).hasSize(1);
    assertThat(dbClient.componentDao().selectByBranchUuid(branch.uuid(), dbSession)).hasSize(prComponentCount);

    db.select(dbSession, "select kee from components")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));
  }

  @Test
  void updateKey_updates_pull_requests_too() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    String pullRequestKey1 = secure().nextAlphanumeric(100);
    ComponentDto pullRequest = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(PULL_REQUEST).setKey(pullRequestKey1));
    db.components().insertComponent(newFileDto(pullRequest));
    db.components().insertComponent(newFileDto(pullRequest));
    int prComponentCount = 3;

    String oldProjectKey = mainBranch.getKey();
    assertThat(dbClient.componentDao().selectByBranchUuid(mainBranch.uuid(), dbSession)).hasSize(1);
    assertThat(dbClient.componentDao().selectByBranchUuid(pullRequest.uuid(), dbSession)).hasSize(prComponentCount);

    String newProjectKey = "newKey";
    underTest.updateKey(dbSession, projectData.projectUuid(), projectData.projectKey(), newProjectKey);

    assertThat(dbClient.componentDao().selectByKey(dbSession, oldProjectKey)).isEmpty();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newProjectKey)).isPresent();
    assertThat(dbClient.componentDao().selectByKeyAndPullRequest(dbSession, newProjectKey, pullRequestKey1)).isPresent();

    assertThat(dbClient.componentDao().selectByBranchUuid(mainBranch.uuid(), dbSession)).hasSize(1);
    assertThat(dbClient.componentDao().selectByBranchUuid(pullRequest.uuid(), dbSession)).hasSize(prComponentCount);

    db.select(dbSession, "select kee from components")
      .forEach(map -> map.values().forEach(k -> assertThat(k.toString()).startsWith(newProjectKey)));
  }

  @Test
  void updateKey_throws_IAE_if_component_with_specified_key_does_not_exist() {
    populateSomeData();

    assertThatThrownBy(() -> underTest.updateKey(dbSession, "A", "org.struts:struts", "foo:struts-core"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Impossible to update key: a component with key \"foo:struts-core\" already exists.");
  }

  @Test
  void updateKey_throws_IAE_when_sub_component_key_is_too_long() {
    ProjectData projectData = db.components().insertPrivateProject("project-uuid", p -> p.setKey("old-project-key"));
    ProjectDto project = projectData.getProjectDto();
    db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("old-project-key:file"));
    String newLongProjectKey = Strings.repeat("a", 400);
    String projectUuid = project.getUuid();
    String projectKey = project.getKey();
    assertThatThrownBy(() -> underTest.updateKey(dbSession, projectUuid, projectKey, newLongProjectKey))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Component key length (405) is longer than the maximum authorized (400). '" + newLongProjectKey + ":file' was provided.");
  }

  @Test
  void compute_new_key() {
    assertThat(computeNewKey("my_project", "my_", "your_")).isEqualTo("your_project");
    assertThat(computeNewKey("my_project", "my_", "$()_")).isEqualTo("$()_project");
  }

  @Test
  void updateKey_callsAuditPersister() {
    db.components().insertPrivateProject("A", p -> p.setKey("my_project"));

    underTestWithAuditPersister.updateKey(dbSession, "A", "my_project", "your_project");

    verify(auditPersister, times(1)).componentKeyUpdate(any(DbSession.class), any(ComponentKeyNewValue.class), anyString());
  }

  private ProjectData populateSomeData() {
    ProjectData projectData = db.components().insertPrivateProject(t -> t.setKey("org.struts:struts").setUuid("A").setBranchUuid("A"));
    ComponentDto mainBranch1 = projectData.getMainBranchComponent();
    ComponentDto directory1 = db.components().insertComponent(newDirectory(mainBranch1, "/src/org/struts").setUuid("B"));
    db.components().insertComponent(ComponentTesting.newFileDto(mainBranch1, directory1).setKey("org.struts:struts:/src/org/struts" +
      "/RequestContext.java").setUuid("C"));
    ComponentDto project2 = db.components().insertPublicProject(t -> t.setKey("foo:struts-core").setUuid("D")).getMainBranchComponent();
    return projectData;
  }
}
