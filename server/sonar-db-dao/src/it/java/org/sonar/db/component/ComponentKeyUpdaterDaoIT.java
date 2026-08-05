/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentKeyNewValue;
import org.sonar.db.project.ProjectDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    // real file component keys share the project key prefix
    db.components().insertComponent(newFileDto(branch, mainBranch.uuid()).setKey(projectData.projectKey() + ":file1"));
    db.components().insertComponent(newFileDto(branch, mainBranch.uuid()).setKey(projectData.projectKey() + ":file2"));
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
    // real file component keys share the project key prefix
    db.components().insertComponent(newFileDto(pullRequest).setKey(projectData.projectKey() + ":file1"));
    db.components().insertComponent(newFileDto(pullRequest).setKey(projectData.projectKey() + ":file2"));
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

  @Test
  void updateKey_realigns_projects_kee_that_drifted_from_components_kee() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("drift_project"));
    String projectUuid = projectData.projectUuid();
    db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("drift_project:file"));
    // simulate a prior partial rename that left projects.kee out of sync with the components' key
    db.executeUpdateSql("update projects set kee = 'drift_project_STALE' where uuid = '" + projectUuid + "'");
    db.commit();

    underTest.updateKey(dbSession, projectUuid, "drift_project", "fixed_project");
    dbSession.commit();

    // projects row is updated by UUID, so it is realigned regardless of its stale key value
    assertThat(db.selectFirst(dbSession, "select kee as \"KEE\" from projects where uuid = '" + projectUuid + "'"))
      .containsEntry("KEE", "fixed_project");
    assertThat(dbClient.componentDao().selectByKey(dbSession, "fixed_project")).isPresent();
  }

  @Test
  void updateKey_allows_renaming_back_to_a_key_still_held_only_by_the_same_project() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("expected_key"));
    String projectUuid = projectData.projectUuid();
    db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("expected_key:file"));
    db.executeUpdateSql("update projects set kee = 'expected_keyexpected_key' where uuid = '" + projectUuid + "'");
    db.commit();

    // real recovery flow: the API resolves 'from' by projects.kee (the stale doubled key) and passes it as oldKey.
    // components already hold the target key, so the rename must not be rejected as "already exists", must not crash
    // on the prefix mismatch, and must realign the projects row.
    underTest.updateKey(dbSession, projectUuid, "expected_keyexpected_key", "expected_key");
    dbSession.commit();

    assertThat(db.selectFirst(dbSession, "select kee as \"KEE\" from projects where uuid = '" + projectUuid + "'"))
      .containsEntry("KEE", "expected_key");
    assertThat(dbClient.componentDao().selectByKey(dbSession, "expected_key")).isPresent();
  }

  @Test
  void updateKey_throws_ISE_when_realign_only_path_would_introduce_new_drift() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("expected_key"));
    String projectUuid = projectData.projectUuid();
    db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("expected_key:file"));
    // projects.kee drifted; the caller resolves 'from' by the stale key and passes it as oldKey. oldKey prefixes
    // no component, so this is the realign-only path - but the target key is held by no component either, so
    // completing it would move projects.kee onto a key the components do not hold, re-introducing drift.
    db.executeUpdateSql("update projects set kee = 'stale_key' where uuid = '" + projectUuid + "'");
    db.commit();

    assertThatThrownBy(() -> underTest.updateKey(dbSession, projectUuid, "stale_key", "totally_new_key"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Key update aborted: no component holds key [totally_new_key]; renaming would leave projects.kee out of sync");
  }

  @Test
  void updateKey_callsAuditPersister_on_realign_only_path() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("expected_key"));
    String projectUuid = projectData.projectUuid();
    String rootUuid = projectData.getMainBranchComponent().uuid();
    db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("expected_key:file"));
    // projects.kee drifted; components already hold the target key, so this exercises the realign-only path
    db.executeUpdateSql("update projects set kee = 'stale_key' where uuid = '" + projectUuid + "'");
    db.commit();

    ArgumentCaptor<ComponentKeyNewValue> captor = ArgumentCaptor.forClass(ComponentKeyNewValue.class);
    underTestWithAuditPersister.updateKey(dbSession, projectUuid, "stale_key", "expected_key");

    verify(auditPersister, times(1)).componentKeyUpdate(any(DbSession.class), captor.capture(), anyString());
    assertThat(captor.getValue())
      .extracting(ComponentKeyNewValue::getComponentUuid, ComponentKeyNewValue::getOldKey, ComponentKeyNewValue::getNewKey)
      .containsExactly(rootUuid, "stale_key", "expected_key");
  }

  @Test
  void updateKey_skips_components_not_sharing_the_old_prefix_instead_of_failing() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("proj"));
    db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("proj:file"));
    // a component whose key is shorter than oldKey and does not share its prefix (mixed/partial drift). Without a
    // per-resource guard the prefix substitution would throw StringIndexOutOfBoundsException on this key.
    db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("p"));
    dbSession.commit();

    underTest.updateKey(dbSession, projectData.projectUuid(), "proj", "proj2");
    dbSession.commit();

    // prefixed components are renamed; the odd one is left untouched; no exception is thrown
    assertThat(dbClient.componentDao().selectByKey(dbSession, "proj2")).isPresent();
    assertThat(dbClient.componentDao().selectByKey(dbSession, "proj2:file")).isPresent();
    assertThat(dbClient.componentDao().selectByKey(dbSession, "p")).isPresent();
  }

  @Test
  void updateKey_rewrites_deprecated_key_that_shares_the_old_prefix() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("proj"));
    ComponentDto file = db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("proj:file"));
    // deprecated_kee is not settable via the component builder, so set it directly
    db.executeUpdateSql("update components set deprecated_kee = 'proj:legacyFile' where uuid = '" + file.uuid() + "'");
    db.commit();

    underTest.updateKey(dbSession, projectData.projectUuid(), "proj", "proj2");
    dbSession.commit();

    assertThat(db.selectFirst(dbSession, "select deprecated_kee as \"DK\" from components where uuid = '" + file.uuid() + "'"))
      .containsEntry("DK", "proj2:legacyFile");
  }

  @Test
  void updateKey_leaves_deprecated_key_untouched_when_it_does_not_share_the_old_prefix() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("proj"));
    ComponentDto file = db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()).setKey("proj:file"));
    // deprecated key with an unrelated prefix must not be prefix-substituted (would corrupt or crash on substring)
    db.executeUpdateSql("update components set deprecated_kee = 'legacy_unrelated' where uuid = '" + file.uuid() + "'");
    db.commit();

    underTest.updateKey(dbSession, projectData.projectUuid(), "proj", "proj2");
    dbSession.commit();

    assertThat(db.selectFirst(dbSession, "select deprecated_kee as \"DK\" from components where uuid = '" + file.uuid() + "'"))
      .containsEntry("DK", "legacy_unrelated");
    // the (prefixed) key itself is still renamed
    assertThat(dbClient.componentDao().selectByKey(dbSession, "proj2:file")).isPresent();
  }

  @Test
  void checkExistentKey_with_null_project_uuid_throws_on_global_conflict() {
    ComponentKeyUpdaterMapper mapper = mock(ComponentKeyUpdaterMapper.class);
    when(mapper.countComponentsByKey("dup")).thenReturn(1);

    assertThatThrownBy(() -> ComponentKeyUpdaterDao.checkExistentKey(mapper, "dup", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Impossible to update key: a component with key \"dup\" already exists.");
    verify(mapper).countComponentsByKey("dup");
    verify(mapper, never()).countComponentsByKeyOutsideProject(anyString(), anyString());
  }

  @Test
  void checkExistentKey_with_null_project_uuid_passes_when_no_global_conflict() {
    ComponentKeyUpdaterMapper mapper = mock(ComponentKeyUpdaterMapper.class);
    when(mapper.countComponentsByKey("free")).thenReturn(0);

    assertThatCode(() -> ComponentKeyUpdaterDao.checkExistentKey(mapper, "free", null))
      .doesNotThrowAnyException();
    verify(mapper).countComponentsByKey("free");
    verify(mapper, never()).countComponentsByKeyOutsideProject(anyString(), anyString());
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
