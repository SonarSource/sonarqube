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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.audit.model.ComponentNewValue;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.source.FileSourceDto;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.SUBVIEW;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newApplication;
import static org.sonar.db.component.ComponentTesting.newBranchComponent;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPortfolio;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;

class ComponentDaoIT {

  private static final String PROJECT_UUID = "project-uuid";
  private static final String DIR_UUID = "dir-uuid";
  private static final String FILE_1_UUID = "file-1-uuid";
  private static final String FILE_2_UUID = "file-2-uuid";
  private static final String FILE_3_UUID = "file-3-uuid";
  private static final String A_VIEW_UUID = "view-uuid";
  private static final ComponentQuery ALL_PROJECTS_COMPONENT_QUERY = ComponentQuery.builder().setQualifiers("TRK").build();

  private final System2 system2 = new AlwaysIncreasingSystem2(1000L);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final AuditPersister auditPersister = mock(AuditPersister.class);

  private final Random random = new Random();
  private final DbSession dbSession = db.getSession();
  private final ComponentDao underTest = new ComponentDao(new NoOpAuditPersister());
  private final ComponentDao underTestWithAuditPersister = new ComponentDao(auditPersister);

  private static ComponentTreeQuery.Builder newTreeQuery(String baseUuid) {
    return ComponentTreeQuery.builder()
      .setBaseUuid(baseUuid)
      .setStrategy(CHILDREN);
  }

  @Test
  void get_by_uuid() {
    ComponentDto project = db.components().insertPrivateProject(p -> p
      .setKey("org.struts:struts")
      .setName("Struts")
      .setLongName("Apache Struts")).getMainBranchComponent();
    ComponentDto anotherProject = db.components().insertPrivateProject().getMainBranchComponent();

    ComponentDto result = underTest.selectByUuid(dbSession, project.uuid()).get();
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo(project.uuid());
    assertThat(result.getUuidPath()).isEqualTo(".");
    assertThat(result.branchUuid()).isEqualTo(project.uuid());
    assertThat(result.getKey()).isEqualTo("org.struts:struts");
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isNull();
    assertThat(result.getCopyComponentUuid()).isNull();
    assertThat(result.isPrivate()).isTrue();

    assertThat(underTest.selectByUuid(dbSession, "UNKNOWN")).isEmpty();
  }

  @Test
  void get_by_uuid_on_technical_project_copy() {
    ComponentDto view = db.components().insertPublicPortfolio();
    ComponentDto project = db.components().insertPublicProject(p -> p
      .setKey("org.struts:struts")
      .setName("Struts")
      .setLongName("Apache Struts")).getMainBranchComponent();
    ComponentDto projectCopy = db.components().insertComponent(newProjectCopy(project, view));
    ComponentDto anotherProject = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto anotherProjectCopy = db.components().insertComponent(newProjectCopy(anotherProject, view));

    ComponentDto result = underTest.selectByUuid(dbSession, projectCopy.uuid()).get();
    assertThat(result.uuid()).isEqualTo(projectCopy.uuid());
    assertThat(result.branchUuid()).isEqualTo(view.uuid());
    assertThat(result.getKey()).isEqualTo(view.getKey() + project.getKey());
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isNull();
    assertThat(result.getCopyComponentUuid()).isEqualTo(project.uuid());
    assertThat(result.isPrivate()).isFalse();
  }

  @Test
  void selectByUuid_on_disabled_component() {
    ComponentDto enabledProject = db.components().insertPublicProject(p -> p.setEnabled(true)).getMainBranchComponent();
    ComponentDto disabledProject = db.components().insertPublicProject(p -> p.setEnabled(false)).getMainBranchComponent();

    ComponentDto result = underTest.selectByUuid(dbSession, disabledProject.uuid()).get();
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  void selectOrFailByUuid_fails_when_component_not_found() {
    db.components().insertPublicProject().getMainBranchComponent();

    assertThatThrownBy(() -> underTest.selectOrFailByUuid(dbSession, "unknown"))
      .isInstanceOf(RowNotFoundException.class);
  }

  @Test
  void select_by_key() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory)
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setLanguage("java")
      .setPath("src/RequestContext.java"));

    assertThat(underTest.selectByKey(dbSession, project.getKey())).isPresent();
    Optional<ComponentDto> optional = underTest.selectByKey(dbSession, file.getKey());

    ComponentDto result = optional.get();
    assertThat(result.uuid()).isEqualTo(file.uuid());
    assertThat(result.getKey()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.branchUuid()).isEqualTo(project.uuid());

    assertThat(underTest.selectByKey(dbSession, "unknown")).isEmpty();
  }

  @Test
  void select_by_key_and_branch() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch").setBranchType(BRANCH));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.uuid()));

    assertThat(underTest.selectByKeyAndBranch(dbSession, project.getKey(), DEFAULT_MAIN_BRANCH_NAME).get().uuid()).isEqualTo(project.uuid());
    assertThat(underTest.selectByKeyAndBranch(dbSession, branch.getKey(), "my_branch").get().uuid()).isEqualTo(branch.uuid());
    assertThat(underTest.selectByKeyAndBranch(dbSession, file.getKey(), "my_branch").get().uuid()).isEqualTo(file.uuid());
    assertThat(underTest.selectByKeyAndBranch(dbSession, "unknown", "my_branch")).isNotPresent();
    assertThat(underTest.selectByKeyAndBranch(dbSession, file.getKey(), "unknown")).isNotPresent();
  }

  @Test
  void select_by_key_and_pull_request() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setKey("my_PR").setBranchType(PULL_REQUEST));
    ComponentDto pullRequestNamedAsMainBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("master").setBranchType(PULL_REQUEST));
    ComponentDto pullRequestNamedAsBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("my_branch").setBranchType(PULL_REQUEST));
    ComponentDto file = db.components().insertComponent(newFileDto(pullRequest));

    assertThat(underTest.selectByKeyAndPullRequest(dbSession, project.getKey(), "my_PR").get().uuid()).isEqualTo(pullRequest.uuid());
    assertThat(underTest.selectByKeyAndPullRequest(dbSession, project.getKey(), "master").get().uuid()).isEqualTo(pullRequestNamedAsMainBranch.uuid());
    assertThat(underTest.selectByKeyAndPullRequest(dbSession, branch.getKey(), "my_branch").get().uuid()).isEqualTo(pullRequestNamedAsBranch.uuid());
    assertThat(underTest.selectByKeyAndPullRequest(dbSession, file.getKey(), "my_PR").get().uuid()).isEqualTo(file.uuid());
    assertThat(underTest.selectByKeyAndPullRequest(dbSession, "unknown", "my_branch")).isNotPresent();
    assertThat(underTest.selectByKeyAndPullRequest(dbSession, file.getKey(), "unknown")).isNotPresent();
  }

  @Test
  void get_by_key_on_disabled_component() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setEnabled(false)).getMainBranchComponent();

    ComponentDto result = underTest.selectByKey(dbSession, project.getKey()).get();

    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  void get_by_key_on_a_root_project() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    ComponentDto result = underTest.selectByKey(dbSession, project.getKey()).get();

    assertThat(result.getKey()).isEqualTo(project.getKey());
    assertThat(result.uuid()).isEqualTo(project.uuid());
    assertThat(result.getUuidPath()).isEqualTo(project.getUuidPath());
    assertThat(result.branchUuid()).isEqualTo(project.uuid());
  }

  @Test
  void selectByKeys_whenPassingKeys_shouldReturnComponentsInMainBranch() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project1);
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    List<ComponentDto> results = underTest.selectByKeys(dbSession, asList(project1.getKey(), project2.getKey()));

    assertThat(results)
      .extracting(ComponentDto::uuid, ComponentDto::getKey)
      .containsExactlyInAnyOrder(
        tuple(project1.uuid(), project1.getKey()),
        tuple(project2.uuid(), project2.getKey()));

    assertThat(underTest.selectByKeys(dbSession, singletonList("unknown"), null, null)).isEmpty();
  }

  @Test
  void selectByKeys_whenAppWithMultipleBranches_shouldReturnMainBranch() {
    ProjectDto proj = db.components().insertPrivateProject().getProjectDto();
    BranchDto projBranch = db.components().insertProjectBranch(proj);

    ProjectDto app = db.components().insertPrivateApplication().getProjectDto();
    BranchDto appBranch = db.components().insertProjectBranch(app);

    db.components().addApplicationProject(app, proj);
    db.components().addProjectBranchToApplicationBranch(appBranch, projBranch);

    ComponentDto projInApp = db.components().insertComponent(newProjectCopy(db.components().getComponentDto(proj),
      db.components().getComponentDto(app)));
    db.components().insertComponent(ComponentTesting.newProjectCopy(db.components().getComponentDto(projBranch),
      db.components().getComponentDto(appBranch)));

    List<ComponentDto> results = underTest.selectByKeys(dbSession, asList(projInApp.getKey()));

    assertThat(results)
      .extracting(ComponentDto::uuid, ComponentDto::getKey)
      .containsOnly(tuple(projInApp.uuid(), projInApp.getKey()));
  }

  @Test
  void selectByKeys_whenBranchMissingDueToCorruption_shouldNotReturnComponents() {
    // this will create an entry in the components table, but not in the project_branches table
    ComponentDto project1 = db.components().insertComponent(ComponentTesting.newPublicProjectDto());

    List<ComponentDto> results = underTest.selectByKeys(dbSession, asList(project1.getKey()));

    assertThat(results)
      .extracting(ComponentDto::uuid, ComponentDto::getKey)
      .isEmpty();
  }

  @Test
  void selectByKeys_whenPortfolio_shouldReturnIt() {
    ComponentDto portfolio = db.components().insertPrivatePortfolio();

    List<ComponentDto> results = underTest.selectByKeys(dbSession, asList(portfolio.getKey()));

    assertThat(results)
      .extracting(ComponentDto::uuid, ComponentDto::getKey)
      .containsExactlyInAnyOrder(tuple(portfolio.uuid(), portfolio.getKey()));
  }

  @Test
  void selectByKeys_whenSubPortfolio_shouldReturnIt() {
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    ComponentDto subPortfolio = db.components().insertSubView(portfolio);

    List<ComponentDto> results = underTest.selectByKeys(dbSession, asList(subPortfolio.getKey()));

    assertThat(results)
      .extracting(ComponentDto::uuid, ComponentDto::getKey)
      .containsExactlyInAnyOrder(tuple(subPortfolio.uuid(), subPortfolio.getKey()));
  }

  @Test
  void selectByKeys_whenBothBranchAndPrPassed_shouldThrowISE() {
    DbSession session = db.getSession();
    List<String> keys = List.of("key");
    assertThatThrownBy(() -> underTest.selectByKeys(session, keys, "branch", "pr"))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void selectByKeys_whenSpecifyingBranch_shouldReturnComponentsInIt() {
    String branchKey = "my_branch";
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchKey));
    ComponentDto file1 = db.components().insertComponent(newFileDto(branch, project.uuid()));
    ComponentDto file2 = db.components().insertComponent(newFileDto(branch, project.uuid()));
    ComponentDto anotherBranch = db.components().insertProjectBranch(project, b -> b.setKey("another_branch"));
    ComponentDto fileOnAnotherBranch = db.components().insertComponent(newFileDto(anotherBranch));

    assertThat(underTest.selectByKeys(dbSession, asList(branch.getKey(), file1.getKey(), file2.getKey()), branchKey, null)).extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(branch.uuid(), file1.uuid(), file2.uuid());
    assertThat(underTest.selectByKeys(dbSession, asList(file1.getKey(), file2.getKey(), fileOnAnotherBranch.getKey()), branchKey, null)).extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(file1.uuid(), file2.uuid());
    assertThat(underTest.selectByKeys(dbSession, singletonList(fileOnAnotherBranch.getKey()), branchKey, null)).isEmpty();
    assertThat(underTest.selectByKeys(dbSession, singletonList(file1.getKey()), "unknown", null)).isEmpty();
    assertThat(underTest.selectByKeys(dbSession, singletonList("unknown"), branchKey, null)).isEmpty();
    assertThat(underTest.selectByKeys(dbSession, singletonList(branch.getKey()), branchKey, null)).extracting(ComponentDto::uuid).containsExactlyInAnyOrder(branch.uuid());
  }

  @Test
  void selectByKeys_whenSpecifyingPR_shouldReturnComponentsInIt() {
    String prKey = "my_branch";
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setKey(prKey).setBranchType(PULL_REQUEST));
    ComponentDto file1 = db.components().insertComponent(newFileDto(pr));
    ComponentDto anotherBranch = db.components().insertProjectBranch(project, b -> b.setKey(prKey));
    ComponentDto fileOnAnotherBranch = db.components().insertComponent(newFileDto(anotherBranch));

    assertThat(underTest.selectByKeys(dbSession, asList(pr.getKey(), file1.getKey()), null, prKey)).extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(pr.uuid(), file1.uuid());
    assertThat(underTest.selectByKeys(dbSession, asList(file1.getKey(), fileOnAnotherBranch.getKey()), null, prKey)).extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(file1.uuid());
    assertThat(underTest.selectByKeys(dbSession, singletonList(fileOnAnotherBranch.getKey()), null, prKey)).isEmpty();
    assertThat(underTest.selectByKeys(dbSession, singletonList(file1.getKey()), null, "unknown")).isEmpty();
    assertThat(underTest.selectByKeys(dbSession, singletonList("unknown"), null, prKey)).isEmpty();
    assertThat(underTest.selectByKeys(dbSession, singletonList(pr.getKey()), null, prKey)).extracting(ComponentDto::uuid).containsExactlyInAnyOrder(pr.uuid());
  }

  @Test
  void get_by_uuids() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    List<ComponentDto> results = underTest.selectByUuids(dbSession, asList(project1.uuid(), project2.uuid()));

    assertThat(results)
      .extracting(ComponentDto::uuid, ComponentDto::getKey)
      .containsExactlyInAnyOrder(
        tuple(project1.uuid(), project1.getKey()),
        tuple(project2.uuid(), project2.getKey()));

    assertThat(underTest.selectByUuids(dbSession, singletonList("unknown"))).isEmpty();
  }

  @Test
  void get_by_uuids_on_removed_components() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setEnabled(false)).getMainBranchComponent();

    List<ComponentDto> results = underTest.selectByUuids(dbSession, asList(project1.uuid(), project2.uuid()));

    assertThat(results)
      .extracting(ComponentDto::getKey, ComponentDto::isEnabled)
      .containsExactlyInAnyOrder(
        tuple(project1.getKey(), true),
        tuple(project2.getKey(), false));
  }

  @Test
  void select_existing_uuids() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setEnabled(false)).getMainBranchComponent();

    assertThat(underTest.selectExistingUuids(dbSession, asList(project1.uuid(), project2.uuid()))).containsExactlyInAnyOrder(project1.uuid(), project2.uuid());
    assertThat(underTest.selectExistingUuids(dbSession, asList(project1.uuid(), "unknown"))).containsExactlyInAnyOrder(project1.uuid());
    assertThat(underTest.selectExistingUuids(dbSession, singletonList("unknown"))).isEmpty();
  }

  @Test
  void select_component_keys_by_qualifiers() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));

    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("TRK"))).extracting(ComponentDto::getKey).containsExactlyInAnyOrder(project.getKey());
    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("DIR"))).extracting(ComponentDto::getKey).containsExactlyInAnyOrder(directory.getKey());
    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("FIL"))).extracting(ComponentDto::getKey).containsExactlyInAnyOrder(file.getKey());
    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("unknown"))).isEmpty();
  }

  @Test
  void fail_with_IAE_select_component_keys_by_qualifiers_on_empty_qualifier() {
    Set<String> set = emptySet();
    assertThatThrownBy(() -> underTest.selectComponentsByQualifiers(dbSession, set))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Qualifiers cannot be empty");
  }

  @Test
  void find_sub_projects_by_component_keys() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto removedProject = db.components().insertPrivateProject(p -> p.setEnabled(false)).getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto removedDirectory = db.components().insertComponent(newDirectory(project, "src2").setEnabled(false));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    ComponentDto removedFile = db.components().insertComponent(newFileDto(project, directory).setEnabled(false));

    // Sub project of a file
    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, singletonList(file.uuid())))
      .extracting(ComponentDto::getKey)
      .containsExactlyInAnyOrder(project.getKey());

    // Sub project of a directory
    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, singletonList(directory.uuid())))
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(project.uuid());

    // Sub project of a project
    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, singletonList(project.uuid())))
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(project.uuid());

    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, singletonList("unknown"))).isEmpty();
    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, Collections.emptyList())).isEmpty();
  }

  @Test
  void select_enabled_files_from_project() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    FileSourceDto fileSource = db.fileSources().insertFileSource(file);

    // From root project
    assertThat(underTest.selectEnabledFilesFromProject(dbSession, project.uuid()))
      .extracting(FilePathWithHashDto::getUuid, FilePathWithHashDto::getSrcHash, FilePathWithHashDto::getPath,
        FilePathWithHashDto::getRevision)
      .containsExactlyInAnyOrder(
        tuple(file.uuid(), fileSource.getSrcHash(), file.path(), fileSource.getRevision()));

    // From directory
    assertThat(underTest.selectEnabledFilesFromProject(dbSession, directory.uuid())).isEmpty();

    assertThat(underTest.selectEnabledFilesFromProject(dbSession, "unknown")).isEmpty();
  }

  @Test
  void select_by_branch_uuid() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto removedProject = db.components().insertPrivateProject(p -> p.setEnabled(false)).getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto removedDirectory = db.components().insertComponent(newDirectory(project, "src2").setEnabled(false));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    ComponentDto removedFile = db.components().insertComponent(newFileDto(project, directory).setEnabled(false));

    // Removed components are included
    assertThat(underTest.selectByBranchUuid(project.uuid(), dbSession))
      .extracting(ComponentDto::getKey)
      .containsExactlyInAnyOrder(project.getKey(), directory.getKey(), removedDirectory.getKey(), file.getKey(), removedFile.getKey());

    assertThat(underTest.selectByBranchUuid("UNKNOWN", dbSession)).isEmpty();
  }

  @Test
  void select_uuids_by_key_from_project() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto removedProject = db.components().insertPrivateProject(p -> p.setEnabled(false)).getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto removedDirectory = db.components().insertComponent(newDirectory(project, "src2").setEnabled(false));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    ComponentDto removedFile = db.components().insertComponent(newFileDto(project, directory).setEnabled(false));

    Map<String, String> uuidsByKey = underTest.selectUuidsByKeyFromProjectKey(dbSession, project.getKey())
      .stream().collect(Collectors.toMap(KeyWithUuidDto::key, KeyWithUuidDto::uuid));

    assertThat(uuidsByKey).containsOnly(
      entry(project.getKey(), project.uuid()),
      entry(directory.getKey(), directory.uuid()),
      entry(removedDirectory.getKey(), removedDirectory.uuid()),
      entry(file.getKey(), file.uuid()),
      entry(removedFile.getKey(), removedFile.uuid()));
  }

  @Test
  void select_uuids_by_key_from_project_and_branch() {
    String branchKey = "branch1";
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchKey));
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setKey(branchKey).setBranchType(PULL_REQUEST));
    ComponentDto directory = db.components().insertComponent(newDirectory(branch, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, directory));
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project, directory));

    Map<String, String> uuidsByKey = underTest.selectUuidsByKeyFromProjectKeyAndBranch(dbSession, project.getKey(), branchKey)
      .stream().collect(Collectors.toMap(KeyWithUuidDto::key, KeyWithUuidDto::uuid));

    assertThat(uuidsByKey).containsOnly(
      entry(branch.getKey(), branch.uuid()),
      entry(directory.getKey(), directory.uuid()),
      entry(file.getKey(), file.uuid()));
  }

  @Test
  void select_uuids_by_key_from_project_and_pr() {
    String prKey = "pr1";
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(prKey).setBranchType(PULL_REQUEST));
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setKey(prKey).setBranchType(BRANCH));
    ComponentDto directory = db.components().insertComponent(newDirectory(branch, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, directory));
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project, directory));

    Map<String, String> uuidsByKey = underTest.selectUuidsByKeyFromProjectKeyAndPullRequest(dbSession, project.getKey(), prKey)
      .stream().collect(Collectors.toMap(KeyWithUuidDto::key, KeyWithUuidDto::uuid));

    assertThat(uuidsByKey).containsOnly(
      entry(branch.getKey(), branch.uuid()),
      entry(directory.getKey(), directory.uuid()),
      entry(file.getKey(), file.uuid()));
  }

  @Test
  void select_views_and_sub_views_and_applications() {
    db.components().insertPublicPortfolio("ABCD", p -> {
    });
    db.components().insertPublicPortfolio("IJKL", p -> {
    });
    ComponentDto view = db.components().insertPublicPortfolio("EFGH", p -> {
    });
    db.components().insertSubView(view, dto -> dto.setUuid("FGHI"));
    ComponentDto application = db.components().insertPublicApplication().getMainBranchComponent();

    assertThat(underTest.selectAllViewsAndSubViews(dbSession)).extracting(UuidWithBranchUuidDto::getUuid)
      .containsExactlyInAnyOrder("ABCD", "EFGH", "FGHI", "IJKL", application.uuid());
    assertThat(underTest.selectAllViewsAndSubViews(dbSession)).extracting(UuidWithBranchUuidDto::getBranchUuid)
      .containsExactlyInAnyOrder("ABCD", "EFGH", "EFGH", "IJKL", application.branchUuid());
  }

  @Test
  void selectViewKeysWithEnabledCopyOfProject_returns_empty_when_set_is_empty() {
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, emptySet()))
      .isEmpty();
  }

  @ParameterizedTest
  @MethodSource("oneOrMoreProjects")
  void selectViewKeysWithEnabledCopyOfProject_returns_empty_when_there_is_no_view(int projectCount) {
    Set<String> projectUuids = IntStream.range(0, projectCount)
      .mapToObj(i -> randomAlphabetic(5))
      .collect(toSet());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, projectUuids)).isEmpty();
  }

  static Object[][] oneOrMoreProjects() {
    return new Object[][]{
      {1},
      {1 + new Random().nextInt(10)}
    };
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_returns_root_view_with_direct_copy_of_project(String rootViewQualifier) {
    ComponentDto project = insertProject();
    ComponentDto view = insertView(rootViewQualifier);
    insertProjectCopy(view, project);

    Set<String> keys = underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project.uuid()));

    assertThat(keys).containsOnly(view.getKey());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project.uuid())))
      .isEqualTo(keys);
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_returns_root_views_with_direct_copy_of_projects(String rootViewQualifier) {
    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject();
    ComponentDto view = insertView(rootViewQualifier);
    insertProjectCopy(view, project1);
    insertProjectCopy(view, project2);
    ComponentDto view2 = insertView(rootViewQualifier);
    ComponentDto project3 = insertProject();
    insertProjectCopy(view2, project3);

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project1.uuid())))
      .containsOnly(view.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project1.uuid())))
      .containsOnly(view.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project2.uuid())))
      .containsOnly(view.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project2.uuid())))
      .containsOnly(view.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project3.uuid())))
      .containsOnly(view2.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project3.uuid())))
      .containsOnly(view2.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, of(project2.uuid(), project1.uuid())))
      .containsOnly(view.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project2.uuid(), project1.uuid())))
      .containsOnly(view.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, of(project1.uuid(), project3.uuid())))
      .containsOnly(view.getKey(), view2.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project1.uuid(), project3.uuid())))
      .containsOnly(view.getKey(), view2.getKey());
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_does_not_return_root_view_with_direct_copy_of_other_project(String rootViewQualifier) {
    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject();
    ComponentDto view1 = insertView(rootViewQualifier);
    insertProjectCopy(view1, project1);
    ComponentDto view2 = insertView(rootViewQualifier);
    insertProjectCopy(view2, project2);

    Set<String> keys = underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project2.uuid()));

    assertThat(keys).containsOnly(view2.getKey());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project2.uuid())))
      .isEqualTo(keys);
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_does_not_return_root_view_with_disabled_direct_copy_of_project(String rootViewQualifier) {
    ComponentDto project = insertProject();
    ComponentDto view1 = insertView(rootViewQualifier);
    insertProjectCopy(view1, project);
    ComponentDto view2 = insertView(rootViewQualifier);
    insertProjectCopy(view2, project, t -> t.setEnabled(false));

    Set<String> keys = underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project.uuid()));

    assertThat(keys).containsOnly(view1.getKey());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project.uuid())))
      .isEqualTo(keys);
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_does_not_return_disabled_root_view_with_direct_copy_of_project(String rootViewQualifier) {
    ComponentDto project = insertProject();
    ComponentDto view1 = insertView(rootViewQualifier, t -> t.setEnabled(false));
    insertProjectCopy(view1, project);
    ComponentDto view2 = insertView(rootViewQualifier);
    insertProjectCopy(view2, project);

    Set<String> keys = underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project.uuid()));

    assertThat(keys).containsOnly(view2.getKey());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project.uuid())))
      .isEqualTo(keys);
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_returns_root_view_with_indirect_copy_of_project(String rootViewQualifier) {
    ComponentDto project = insertProject();
    ComponentDto view = insertView(rootViewQualifier);
    ComponentDto lowestSubview = insertSubviews(view);
    insertProjectCopy(lowestSubview, project);

    Set<String> keys = underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project.uuid()));

    assertThat(keys).containsOnly(view.getKey());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project.uuid())))
      .isEqualTo(keys);
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_returns_root_views_with_indirect_copy_of_projects(String rootViewQualifier) {
    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject();
    ComponentDto view1 = insertView(rootViewQualifier);
    ComponentDto lowestSubview1 = insertSubviews(view1);
    insertProjectCopy(lowestSubview1, project1);
    insertProjectCopy(lowestSubview1, project2);
    ComponentDto view2 = insertView(rootViewQualifier);
    ComponentDto lowestSubview2 = insertSubviews(view2);
    ComponentDto project3 = insertProject();
    insertProjectCopy(lowestSubview2, project3);

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project1.uuid())))
      .containsOnly(view1.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project1.uuid())))
      .containsOnly(view1.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project2.uuid())))
      .containsOnly(view1.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project2.uuid())))
      .containsOnly(view1.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project3.uuid())))
      .containsOnly(view2.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project3.uuid())))
      .containsOnly(view2.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, of(project2.uuid(), project1.uuid())))
      .containsOnly(view1.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project2.uuid(), project1.uuid())))
      .containsOnly(view1.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, of(project1.uuid(), project3.uuid())))
      .containsOnly(view1.getKey(), view2.getKey());
    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project1.uuid(), project3.uuid())))
      .containsOnly(view1.getKey(), view2.getKey());
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_does_not_return_root_view_with_indirect_copy_of_other_project(String rootViewQualifier) {
    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject();
    ComponentDto view1 = insertView(rootViewQualifier);
    ComponentDto lowestSubview1 = insertSubviews(view1);
    insertProjectCopy(lowestSubview1, project1);
    ComponentDto view2 = insertView(rootViewQualifier);
    ComponentDto lowestSubview2 = insertSubviews(view2);
    insertProjectCopy(lowestSubview2, project2);

    Set<String> keys = underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project2.uuid()));

    assertThat(keys).containsOnly(view2.getKey());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project2.uuid())))
      .isEqualTo(keys);
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_does_not_return_root_view_with_disabled_indirect_copy_of_project(String rootViewQualifier) {
    ComponentDto project = insertProject();
    ComponentDto view1 = insertView(rootViewQualifier);
    ComponentDto lowestSubview1 = insertSubviews(view1);
    insertProjectCopy(lowestSubview1, project);
    ComponentDto view2 = insertView(rootViewQualifier);
    ComponentDto lowestSubview2 = insertSubviews(view2);
    insertProjectCopy(lowestSubview2, project, t -> t.setEnabled(false));

    Set<String> keys = underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project.uuid()));

    assertThat(keys).containsOnly(view1.getKey());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project.uuid())))
      .isEqualTo(keys);
  }

  @ParameterizedTest
  @MethodSource("portfolioOrApplicationRootViewQualifier")
  void selectViewKeysWithEnabledCopyOfProject_does_not_return_disabled_root_view_with_indirect_copy_of_project(String rootViewQualifier) {
    ComponentDto project = insertProject();
    ComponentDto view1 = insertView(rootViewQualifier, t -> t.setEnabled(false));
    ComponentDto lowestSubview1 = insertSubviews(view1);
    insertProjectCopy(lowestSubview1, project);
    ComponentDto view2 = insertView(rootViewQualifier);
    ComponentDto lowestSubview2 = insertSubviews(view2);
    insertProjectCopy(lowestSubview2, project);

    Set<String> keys = underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, singleton(project.uuid()));

    assertThat(keys).containsOnly(view2.getKey());

    assertThat(underTest.selectViewKeysWithEnabledCopyOfProject(dbSession, shuffleWithNonExistentUuids(project.uuid())))
      .isEqualTo(keys);
  }

  static Object[][] portfolioOrApplicationRootViewQualifier() {
    return new Object[][]{
      {Qualifiers.VIEW},
      {Qualifiers.APP},
    };
  }

  private ComponentDto insertSubviews(ComponentDto view) {
    ComponentDto lowestView = view;
    int subviewsCount1 = 1 + random.nextInt(5);
    for (int i = 0; i < subviewsCount1; i++) {
      lowestView = db.components().insertSubView(lowestView);
    }
    return lowestView;
  }

  private ComponentDto insertView(String rootViewQualifier) {
    return insertView(rootViewQualifier, defaults());
  }

  private ComponentDto insertView(String rootViewQualifier, Consumer<ComponentDto> dtoPopulators) {
    ComponentDbTester tester = db.components();
    if (rootViewQualifier.equals(Qualifiers.VIEW)) {
      return random.nextBoolean() ? tester.insertPublicPortfolio(dtoPopulators) : tester.insertPrivatePortfolio(dtoPopulators);
    }
    return random.nextBoolean() ? tester.insertPublicApplication(dtoPopulators).getMainBranchComponent() :
      tester.insertPrivatePortfolio(dtoPopulators);
  }

  private ComponentDto insertProject() {
    return random.nextBoolean() ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();
  }

  @SafeVarargs
  private final ComponentDto insertProjectCopy(ComponentDto view, ComponentDto project, Consumer<ComponentDto>... decorators) {
    ComponentDto component = ComponentTesting.newProjectCopy(project, view);
    Arrays.stream(decorators).forEach(decorator -> decorator.accept(component));
    return db.components().insertComponent(component);
  }

  @Test
  void select_projects_from_view() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto view = db.components().insertPublicPortfolio();
    db.components().insertComponent(newProjectCopy(project1, view));
    ComponentDto viewWithSubView = db.components().insertPublicPortfolio();
    db.components().insertComponent(newProjectCopy(project2, viewWithSubView));
    ComponentDto subView = db.components().insertSubView(viewWithSubView);
    db.components().insertComponent(newProjectCopy(project1, subView));
    ComponentDto viewWithoutProject = db.components().insertPrivatePortfolio();

    assertThat(underTest.selectProjectBranchUuidsFromView(dbSession, view.uuid(), view.uuid())).containsExactlyInAnyOrder(project1.uuid());
    assertThat(underTest.selectProjectBranchUuidsFromView(dbSession, viewWithSubView.uuid(), viewWithSubView.uuid())).containsExactlyInAnyOrder(project1.uuid(), project2.uuid());
    assertThat(underTest.selectProjectBranchUuidsFromView(dbSession, subView.uuid(), viewWithSubView.uuid())).containsExactlyInAnyOrder(project1.uuid());
    assertThat(underTest.selectProjectBranchUuidsFromView(dbSession, viewWithoutProject.uuid(), viewWithoutProject.uuid())).isEmpty();
    assertThat(underTest.selectProjectBranchUuidsFromView(dbSession, "Unknown", "Unknown")).isEmpty();
  }

  @Test
  void select_enabled_views_from_root_view() {
    ComponentDto rootPortfolio = db.components().insertPrivatePortfolio();
    ComponentDto subPortfolio = db.components().insertSubView(rootPortfolio);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertComponent(newProjectCopy(project, subPortfolio));

    assertThat(underTest.selectEnabledViewsFromRootView(dbSession, rootPortfolio.uuid()))
      .extracting(ComponentDto::uuid)
      .containsOnly(rootPortfolio.uuid(), subPortfolio.uuid());
    assertThat(underTest.selectEnabledViewsFromRootView(dbSession, project.uuid())).isEmpty();
  }

  @Test
  void select_projects_from_view_should_escape_like_sensitive_characters() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project3 = db.components().insertPrivateProject().getMainBranchComponent();

    ComponentDto view = db.components().insertPrivatePortfolio();

    //subview with uuid containing special character ( '_' ) for 'like' SQL clause
    ComponentDto subView1 = db.components().insertComponent(newSubPortfolio(view, "A_C", "A_C-key"));
    db.components().insertComponent(newProjectCopy(project1, subView1));
    db.components().insertComponent(newProjectCopy(project2, subView1));

    ComponentDto subView2 = db.components().insertComponent(newSubPortfolio(view, "ABC", "ABC-key"));
    db.components().insertComponent(newProjectCopy(project3, subView2));

    assertThat(underTest.selectProjectBranchUuidsFromView(dbSession, subView1.uuid(), view.uuid())).containsExactlyInAnyOrder(project1.uuid(), project2.uuid());
    assertThat(underTest.selectProjectBranchUuidsFromView(dbSession, subView2.uuid(), view.uuid())).containsExactlyInAnyOrder(project3.uuid());
  }

  @Test
  void selectByQuery_provisioned() {
    ComponentDto provisionedProject = db.components()
      .insertPrivateProject(p -> p.setKey("provisioned.project").setName("Provisioned Project")).getMainBranchComponent();
    ComponentDto provisionedPortfolio = db.components().insertPrivatePortfolio();

    SnapshotDto analyzedProject = db.components().insertProjectAndSnapshot(newPrivateProjectDto());
    SnapshotDto analyzedDisabledProject = db.components().insertProjectAndSnapshot(newPrivateProjectDto()
      .setEnabled(false));
    SnapshotDto analyzedPortfolio = db.components().insertProjectAndSnapshot(ComponentTesting.newPortfolio());

    Supplier<ComponentQuery.Builder> query = () -> ComponentQuery.builder().setQualifiers(PROJECT).setOnProvisionedOnly(true);
    assertThat(underTest.selectByQuery(dbSession, query.get().build(), forPage(1).andSize(10)))
      .extracting(ComponentDto::uuid)
      .containsOnly(provisionedProject.uuid());

    // pagination
    assertThat(underTest.selectByQuery(dbSession, query.get().build(), forPage(3).andSize(10))).isEmpty();

    // filter on qualifiers
    assertThat(underTest.selectByQuery(dbSession, query.get().setQualifiers("XXX").build(), forPage(1).andSize(10))).isEmpty();
    assertThat(underTest.selectByQuery(dbSession, query.get().setQualifiers(PROJECT, "XXX").build(), forPage(1).andSize(10)))
      .extracting(ComponentDto::uuid)
      .containsOnly(provisionedProject.uuid());
    assertThat(underTest.selectByQuery(dbSession, query.get().setQualifiers(PROJECT, Qualifiers.VIEW).build(), forPage(1).andSize(10)))
      .extracting(ComponentDto::uuid)
      .containsOnly(provisionedProject.uuid(), provisionedPortfolio.uuid());

    // match key
    assertThat(underTest.selectByQuery(dbSession, query.get().setNameOrKeyQuery(provisionedProject.getKey()).build(),
      forPage(1).andSize(10)))
      .extracting(ComponentDto::uuid)
      .containsExactly(provisionedProject.uuid());
    assertThat(underTest.selectByQuery(dbSession, query.get().setNameOrKeyQuery("pROvisiONed.proJEcT").setPartialMatchOnKey(true).build()
      , forPage(1).andSize(10)))
      .extracting(ComponentDto::uuid)
      .containsExactly(provisionedProject.uuid());
    assertThat(underTest.selectByQuery(dbSession, query.get().setNameOrKeyQuery("missing").setPartialMatchOnKey(true).build(),
      forPage(1).andSize(10))).isEmpty();
    assertThat(underTest.selectByQuery(dbSession,
      query.get().setNameOrKeyQuery("to be escaped '\"\\%").setPartialMatchOnKey(true).build(), forPage(1).andSize(10)))
      .isEmpty();

    // match name
    assertThat(underTest.selectByQuery(dbSession, query.get().setNameOrKeyQuery("ned proj").setPartialMatchOnKey(true).build(),
      forPage(1).andSize(10)))
      .extracting(ComponentDto::uuid)
      .containsExactly(provisionedProject.uuid());
  }

  @Test
  void selectByQuery_onProvisionedOnly_filters_projects_with_analysis_on_branch() {
    Supplier<ComponentQuery.Builder> query = () -> ComponentQuery.builder()
      .setQualifiers(PROJECT)
      .setOnProvisionedOnly(true);

    // the project does not have any analysis
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    assertThat(underTest.selectByQuery(dbSession, query.get().build(), forPage(1).andSize(10)))
      .extracting(ComponentDto::uuid)
      .containsOnly(project.uuid());

    // the project does not have analysis of main branch but only
    // analysis of non-main branches
    ComponentDto branchWithoutAnalysis = db.components().insertProjectBranch(project);
    ComponentDto branchWithAnalysis = db.components().insertProjectBranch(project);
    db.components().insertSnapshot(branchWithAnalysis);
    assertThat(underTest.selectByQuery(dbSession, query.get().build(), forPage(1).andSize(10)))
      .isEmpty();
  }

  @Test
  void selectByQuery_verify_order() {
    Date firstDate = new Date(system2.now());
    Date secondDate = new Date(system2.now());
    Date thirdDate = new Date(system2.now());

    ComponentDto project3 =
      db.components().insertPrivateProject(componentDto -> componentDto.setName("project3").setCreatedAt(thirdDate)).getMainBranchComponent();
    ComponentDto project1 =
      db.components().insertPrivateProject(componentDto -> componentDto.setName("project1").setCreatedAt(firstDate)).getMainBranchComponent();
    ComponentDto project2 =
      db.components().insertPrivateProject(componentDto -> componentDto.setName("project2").setCreatedAt(secondDate)).getMainBranchComponent();

    Supplier<ComponentQuery.Builder> query = () -> ComponentQuery.builder()
      .setQualifiers(PROJECT)
      .setOnProvisionedOnly(true);

    List<ComponentDto> results = underTest.selectByQuery(dbSession, query.get().build(), forPage(1).andSize(10));
    assertThat(results)
      .extracting(ComponentDto::uuid)
      .containsExactly(
        project1.uuid(),
        project2.uuid(),
        project3.uuid());
  }

  @Test
  void count_provisioned() {
    db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertProjectAndSnapshot(newPrivateProjectDto());
    db.components().insertProjectAndSnapshot(ComponentTesting.newPortfolio());
    Supplier<ComponentQuery.Builder> query = () -> ComponentQuery.builder().setOnProvisionedOnly(true);

    assertThat(underTest.countByQuery(dbSession, query.get().setQualifiers(PROJECT).build())).isOne();
    assertThat(underTest.countByQuery(dbSession, query.get().setQualifiers(Qualifiers.VIEW).build())).isZero();
    assertThat(underTest.countByQuery(dbSession, query.get().setQualifiers(PROJECT, Qualifiers.VIEW).build())).isOne();
  }

  @Test
  void countByQuery_throws_IAE_if_too_many_component_keys() {
    Set<String> keys = IntStream.range(0, 1_010).mapToObj(String::valueOf).collect(toSet());
    ComponentQuery.Builder query = ComponentQuery.builder()
      .setQualifiers(PROJECT)
      .setComponentKeys(keys);

    assertThatCountByQueryThrowsIAE(query, "Too many component keys in query");
  }

  @Test
  void countByQuery_throws_IAE_if_too_many_component_uuids() {
    Set<String> uuids = IntStream.range(0, 1_010).mapToObj(String::valueOf).collect(toSet());
    ComponentQuery.Builder query = ComponentQuery.builder()
      .setQualifiers(PROJECT)
      .setComponentUuids(uuids);

    assertThatCountByQueryThrowsIAE(query, "Too many component UUIDs in query");
  }

  private void assertThatCountByQueryThrowsIAE(ComponentQuery.Builder query, String expectedMessage) {
    ComponentQuery componentQuery = query.build();
    assertThatThrownBy(() -> underTest.countByQuery(dbSession, componentQuery))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(expectedMessage);
  }

  @Test
  void selectByProjectUuid() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto removedProject = db.components().insertPrivateProject(p -> p.setEnabled(false)).getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto removedDirectory = db.components().insertComponent(newDirectory(project, "src2").setEnabled(false));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    ComponentDto removedFile = db.components().insertComponent(newFileDto(project, directory).setEnabled(false));

    assertThat(underTest.selectByBranchUuid(project.uuid(), dbSession))
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(project.uuid(), directory.uuid(), removedDirectory.uuid(),
        file.uuid(),
        removedFile.uuid());
  }

  @Test
  void update() {
    ComponentDto mainBranch = db.components().insertPrivateProject("U1").getMainBranchComponent();

    underTest.update(dbSession, new ComponentUpdateDto()
      .setUuid(mainBranch.uuid())
      .setBKey("key")
      .setBCopyComponentUuid("copy")
      .setBChanged(true)
      .setBDescription("desc")
      .setBEnabled(true)
      .setBUuidPath("uuid_path")
      .setBLanguage("lang")
      .setBLongName("longName")
      .setBName("name")
      .setBPath("path")
      .setBQualifier("qualifier"), "qualifier");
    dbSession.commit();

    Map<String, Object> row = selectBColumnsForUuid(mainBranch.uuid());
    assertThat(row.get("bChanged")).isIn(true, /* for Oracle */1L, 1);
    assertThat(row)
      .containsEntry("bKey", "key")
      .containsEntry("bCopyComponentUuid", "copy")
      .containsEntry("bDescription", "desc");
    assertThat(row.get("bEnabled")).isIn(true, /* for Oracle */1L, 1);
    assertThat(row)
      .containsEntry("bUuidPath", "uuid_path")
      .containsEntry("bLanguage", "lang")
      .containsEntry("bLongName", "longName")
      .containsEntry("bName", "name")
      .containsEntry("bPath", "path")
      .containsEntry("bQualifier", "qualifier");
  }

  @Test
  void updateBEnabledToFalse() {
    ComponentDto dto1 = newPrivateProjectDto("U1");
    ComponentDto dto2 = newPrivateProjectDto("U2");
    ComponentDto dto3 = newPrivateProjectDto("U3");
    underTest.insert(dbSession, List.of(dto1, dto2, dto3), true);

    underTest.updateBEnabledToFalse(dbSession, asList("U1", "U2"));
    dbSession.commit();

    Map<String, Object> row1 = selectBColumnsForUuid("U1");
    assertThat(row1.get("bChanged")).isIn(true, /* for Oracle */1L, 1);
    assertThat(row1)
      .containsEntry("bKey", dto1.getKey())
      .containsEntry("bCopyComponentUuid", dto1.getCopyComponentUuid())
      .containsEntry("bDescription", dto1.description());
    assertThat(row1.get("bEnabled")).isIn(false, /* for Oracle */0L, 0);
    assertThat(row1)
      .containsEntry("bUuidPath", dto1.getUuidPath())
      .containsEntry("bLanguage", dto1.language())
      .containsEntry("bLongName", dto1.longName())
      .containsEntry("bName", dto1.name())
      .containsEntry("bPath", dto1.path())
      .containsEntry("bQualifier", dto1.qualifier());

    Map<String, Object> row2 = selectBColumnsForUuid("U2");
    assertThat(row2.get("bChanged")).isIn(true, /* for Oracle */1L, 1);
    assertThat(row2)
      .containsEntry("bKey", dto2.getKey())
      .containsEntry("bCopyComponentUuid", dto2.getCopyComponentUuid())
      .containsEntry("bDescription", dto2.description());
    assertThat(row2.get("bEnabled")).isIn(false, /* for Oracle */0L, 0);
    assertThat(row2)
      .containsEntry("bUuidPath", dto2.getUuidPath())
      .containsEntry("bLanguage", dto2.language())
      .containsEntry("bLongName", dto2.longName())
      .containsEntry("bName", dto2.name())
      .containsEntry("bPath", dto2.path())
      .containsEntry("bQualifier", dto2.qualifier());

    Map<String, Object> row3 = selectBColumnsForUuid("U3");
    assertThat(row3.get("bChanged")).isIn(false, /* for Oracle */0L, 0);
  }

  private Map<String, Object> selectBColumnsForUuid(String uuid) {
    return db.selectFirst(
      "select b_changed as \"bChanged\", deprecated_kee as \"bKey\", b_copy_component_uuid as \"bCopyComponentUuid\", b_description as " +
        "\"bDescription\", " +
        "b_enabled as \"bEnabled\", b_uuid_path as \"bUuidPath\", b_language as \"bLanguage\", b_long_name as \"bLongName\", b_name as " +
        "\"bName\", " +
        "b_path as \"bPath\", b_qualifier as \"bQualifier\" " +
        "from components where uuid='" + uuid + "'");
  }

  @Test
  void selectByQuery_throws_IAE_if_too_many_component_keys() {
    Set<String> keys = IntStream.range(0, 1_010).mapToObj(String::valueOf).collect(toSet());
    ComponentQuery.Builder query = ComponentQuery.builder()
      .setQualifiers(PROJECT)
      .setComponentKeys(keys);

    assertThatSelectByQueryThrowsIAE(query, "Too many component keys in query");
  }

  @Test
  void selectByQuery_throws_IAE_if_too_many_component_uuids() {
    Set<String> uuids = IntStream.range(0, 1_010).mapToObj(String::valueOf).collect(toSet());
    ComponentQuery.Builder query = ComponentQuery.builder()
      .setQualifiers(PROJECT)
      .setComponentUuids(uuids);

    assertThatSelectByQueryThrowsIAE(query, "Too many component UUIDs in query");
  }

  private void assertThatSelectByQueryThrowsIAE(ComponentQuery.Builder query, String expectedMessage) {
    ComponentQuery componentQuery = query.build();
    Pagination pagination = forPage(1).andSize(Integer.MAX_VALUE);
    assertThatThrownBy(() -> underTest.selectByQuery(dbSession, componentQuery, pagination))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(expectedMessage);
  }

  @Test
  void selectByQuery_with_paging_query_and_qualifiers() {
    db.components().insertProjectAndSnapshot(newPrivateProjectDto().setName("aaaa-name"));
    db.components().insertProjectAndSnapshot(newPortfolio());
    for (int i = 9; i >= 1; i--) {
      db.components().insertProjectAndSnapshot(newPrivateProjectDto().setName("project-" + i));
    }

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("oJect").setQualifiers(PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, forPage(2).andSize(3));
    int count = underTest.countByQuery(dbSession, query);

    assertThat(result).hasSize(3);
    assertThat(count).isEqualTo(9);
    assertThat(result).extracting(ComponentDto::name).containsExactly("project-4", "project-5", "project-6");
  }

  @Test
  void selectByQuery_should_not_return_branches() {
    ComponentDto main = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(main);

    assertThat(underTest.selectByQuery(dbSession, ALL_PROJECTS_COMPONENT_QUERY, forPage(1).andSize(2))).hasSize(1);
    assertThat(underTest.selectByQuery(dbSession, ALL_PROJECTS_COMPONENT_QUERY, forPage(1).andSize(2)).get(0).uuid()).isEqualTo(main.uuid());
  }

  @Test
  void countByQuery_should_not_include_branches() {
    ComponentDto main = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(main);

    assertThat(underTest.countByQuery(dbSession, ALL_PROJECTS_COMPONENT_QUERY)).isOne();
  }

  @Test
  void selectByQuery_name_with_special_characters() {
    db.components().insertProjectAndSnapshot(newPrivateProjectDto().setName("project-\\_%/-name"));

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("-\\_%/-").setQualifiers(PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, forPage(1).andSize(10));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("project-\\_%/-name");
  }

  @Test
  void selectByQuery_key_with_special_characters() {
    db.components().insertProjectAndSnapshot(newPrivateProjectDto().setKey("project-_%-key"));
    db.components().insertProjectAndSnapshot(newPrivateProjectDto().setKey("project-key-that-does-not-match"));

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("project-_%-key").setQualifiers(PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, forPage(1).andSize(10));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo("project-_%-key");
  }

  @Test
  void selectByQuery_on_key_partial_match_case_insensitive() {
    db.components().insertProjectAndSnapshot(newPrivateProjectDto().setKey("project-key"));

    ComponentQuery query = ComponentQuery.builder()
      .setNameOrKeyQuery("JECT-K")
      .setPartialMatchOnKey(true)
      .setQualifiers(PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, forPage(1).andSize(10));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo("project-key");
  }

  @Test
  void selectByQuery_filter_last_analysis_date() {
    long aLongTimeAgo = 1_000_000_000L;
    long recentTime = 3_000_000_000L;
    ComponentDto oldProject = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(oldProject, s -> s.setCreatedAt(aLongTimeAgo));
    ComponentDto recentProject = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(recentProject, s -> s.setCreatedAt(recentTime).setLast(true));
    db.components().insertSnapshot(recentProject, s -> s.setCreatedAt(aLongTimeAgo).setLast(false));

    // before date on main branch
    assertThat(selectProjectUuidsByQuery(q -> q.setAnalyzedBefore(recentTime)))
      .containsExactlyInAnyOrder(oldProject.uuid());
    assertThat(selectProjectUuidsByQuery(q -> q.setAnalyzedBefore(aLongTimeAgo)))
      .isEmpty();
    assertThat(selectProjectUuidsByQuery(q -> q.setAnalyzedBefore(recentTime + 1_000L)))
      .containsExactlyInAnyOrder(oldProject.uuid(), recentProject.uuid());

    // before date on any branch
    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(recentTime)))
      .containsExactlyInAnyOrder(oldProject.uuid());
    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(aLongTimeAgo)))
      .isEmpty();
    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(recentTime + 1_000L)))
      .containsExactlyInAnyOrder(oldProject.uuid(), recentProject.uuid());
  }

  @Test
  void selectByQuery_filter_last_analysis_date_on_non_main_branches() {
    long aLongTimeAgo = 1_000_000_000L;
    long recentTime = 3_000_000_000L;
    // project with only a non-main and old analyzed branch
    ProjectData oldProjectData = db.components().insertPublicProject();
    ComponentDto oldProject = oldProjectData.getMainBranchComponent();
    ComponentDto oldProjectBranch = db.components().insertProjectBranch(oldProject,
      newBranchDto(oldProjectData.projectUuid(), BRANCH).setBranchType(BRANCH));
    db.components().insertSnapshot(oldProjectBranch, s -> s.setLast(true).setCreatedAt(aLongTimeAgo));

    // project with only a old main branch and a recent non-main branch
    ProjectData recentProjectData = db.components().insertPublicProject();
    ComponentDto recentProject = recentProjectData.getMainBranchComponent();
    ComponentDto recentProjectBranch = db.components().insertProjectBranch(recentProject, newBranchDto(recentProjectData.projectUuid(),
      BRANCH).setBranchType(BRANCH));
    db.components().insertSnapshot(recentProjectBranch, s -> s.setCreatedAt(recentTime).setLast(true));
    db.components().insertSnapshot(recentProjectBranch, s -> s.setCreatedAt(aLongTimeAgo).setLast(false));

    // before date on main branch only
    assertThat(selectProjectUuidsByQuery(q -> q.setAnalyzedBefore(recentTime))).isEmpty();
    assertThat(selectProjectUuidsByQuery(q -> q.setAnalyzedBefore(aLongTimeAgo))).isEmpty();
    assertThat(selectProjectUuidsByQuery(q -> q.setAnalyzedBefore(recentTime + 1_000L))).isEmpty();

    // before date on any branch
    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(recentTime)))
      .containsExactlyInAnyOrder(oldProject.uuid());
    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(aLongTimeAgo)))
      .isEmpty();
    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(recentTime + 1_000L)))
      .containsExactlyInAnyOrder(oldProject.uuid(), recentProject.uuid());
  }

  @Test
  void selectByQuery_filter_last_analysisof_all_branches_before() {
    long aLongTimeAgo = 1_000_000_000L;
    long recentTime = 3_000_000_000L;
    // project with only a non-main and old analyzed branch
    ProjectData oldProjectData = db.components().insertPublicProject();
    ComponentDto oldProject = oldProjectData.getMainBranchComponent();
    ComponentDto oldProjectBranch = db.components().insertProjectBranch(oldProject,
      newBranchDto(oldProjectData.projectUuid(), BRANCH).setBranchType(BRANCH));
    db.components().insertSnapshot(oldProjectBranch, s -> s.setLast(true).setCreatedAt(aLongTimeAgo));

    // project with only a old main branch and a recent non-main branch
    ProjectData recentProjectData = db.components().insertPublicProject();
    ComponentDto recentProject = recentProjectData.getMainBranchComponent();
    ComponentDto recentProjectBranch = db.components().insertProjectBranch(recentProject, newBranchDto(recentProjectData.projectUuid(),
      BRANCH).setBranchType(BRANCH));
    db.components().insertSnapshot(recentProjectBranch, s -> s.setCreatedAt(recentTime).setLast(true));
    db.components().insertSnapshot(recentProjectBranch, s -> s.setCreatedAt(aLongTimeAgo).setLast(false));

    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(recentTime + 1_000L))).containsOnly(oldProject.uuid(),
      recentProject.uuid());
    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(aLongTimeAgo))).isEmpty();
    assertThat(selectProjectUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(aLongTimeAgo + 1_000L))).containsOnly(oldProject.uuid());
  }

  @Test
  void selectByQuery_filter_last_analysisof_all_branches_before_for_portfolios() {
    long aLongTimeAgo = 1_000_000_000L;
    long recentTime = 3_000_000_000L;

    // old portfolio
    ComponentDto oldPortfolio = db.components().insertPublicPortfolio();
    db.components().insertSnapshot(oldPortfolio, s -> s.setLast(true).setCreatedAt(aLongTimeAgo));

    // recent portfolio
    ComponentDto recentPortfolio = db.components().insertPublicPortfolio();
    db.components().insertSnapshot(recentPortfolio, s -> s.setCreatedAt(recentTime).setLast(true));

    assertThat(selectPortfolioUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(recentTime + 1_000_000L))).containsOnly(oldPortfolio.uuid(), recentPortfolio.uuid());
    assertThat(selectPortfolioUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(aLongTimeAgo))).isEmpty();
    assertThat(selectPortfolioUuidsByQuery(q -> q.setAllBranchesAnalyzedBefore(aLongTimeAgo + 1_000L))).containsOnly(oldPortfolio.uuid());
  }

  @Test
  void selectByQuery_filter_created_at() {
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setCreatedAt(parseDate("2018-02-01"))).getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setCreatedAt(parseDate("2018-06-01"))).getMainBranchComponent();

    assertThat(selectProjectUuidsByQuery(q -> q.setCreatedAfter(parseDate("2017-12-01"))))
      .containsExactlyInAnyOrder(project1.uuid(), project2.uuid());
    assertThat(selectProjectUuidsByQuery(q -> q.setCreatedAfter(parseDate("2018-02-20"))))
      .containsExactlyInAnyOrder(project2.uuid());

    assertThat(selectProjectUuidsByQuery(q -> q.setCreatedAfter(parseDate("2019-01-01"))))
      .isEmpty();
  }

  private List<String> selectProjectUuidsByQuery(Consumer<ComponentQuery.Builder> query) {
    return selectUuidsByQuery(PROJECT, query);
  }

  private List<String> selectPortfolioUuidsByQuery(Consumer<ComponentQuery.Builder> query) {
    return selectUuidsByQuery(VIEW, query);
  }

  private List<String> selectUuidsByQuery(String qualifier, Consumer<ComponentQuery.Builder> query) {
    ComponentQuery.Builder builder = ComponentQuery.builder().setQualifiers(qualifier);
    query.accept(builder);
    return underTest.selectByQuery(dbSession, builder.build(), forPage(1).andSize(5))
      .stream()
      .map(ComponentDto::uuid)
      .toList();
  }

  @Test
  void selectByQuery_filter_on_visibility() {
    db.components().insertPrivateProject(p -> p.setKey("private-key")).getMainBranchComponent();
    db.components().insertPublicProject(p -> p.setKey("-key")).getMainBranchComponent();

    ComponentQuery privateProjectsQuery = ComponentQuery.builder().setPrivate(true).setQualifiers(PROJECT).build();
    ComponentQuery ProjectsQuery = ComponentQuery.builder().setPrivate(false).setQualifiers(PROJECT).build();
    ComponentQuery allProjectsQuery = ComponentQuery.builder().setPrivate(null).setQualifiers(PROJECT).build();

    assertThat(underTest.selectByQuery(dbSession, privateProjectsQuery, forPage(1).andSize(10))).extracting(ComponentDto::getKey).containsExactly("private-key");
    assertThat(underTest.selectByQuery(dbSession, ProjectsQuery, forPage(1).andSize(10))).extracting(ComponentDto::getKey).containsExactly("-key");
    assertThat(underTest.selectByQuery(dbSession, allProjectsQuery, forPage(1).andSize(10))).extracting(ComponentDto::getKey).containsOnly("-key", "private-key");
  }

  @Test
  void selectByQuery_on_empty_list_of_component_key() {
    db.components().insertPrivateProject().getMainBranchComponent();
    ComponentQuery dbQuery = ComponentQuery.builder().setQualifiers(PROJECT).setComponentKeys(emptySet()).build();

    List<ComponentDto> result = underTest.selectByQuery(dbSession, dbQuery, forPage(1).andSize(10));
    int count = underTest.countByQuery(dbSession, dbQuery);

    assertThat(result).isEmpty();
    assertThat(count).isZero();
  }

  @Test
  void selectByQuery_on_component_keys() {
    ComponentDto sonarqube = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto jdk8 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto cLang = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentQuery query = ComponentQuery.builder().setQualifiers(PROJECT)
      .setComponentKeys(newHashSet(sonarqube.getKey(), jdk8.getKey())).build();

    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, forPage(1).andSize(10));

    assertThat(result).hasSize(2).extracting(ComponentDto::getKey)
      .containsExactlyInAnyOrder(sonarqube.getKey(), jdk8.getKey())
      .doesNotContain(cLang.getKey());
  }

  @Test
  void selectByQuery_on_empty_list_of_component_uuids() {
    db.components().insertPrivateProject().getMainBranchComponent();
    ComponentQuery dbQuery = ComponentQuery.builder().setQualifiers(PROJECT).setComponentUuids(emptySet()).build();

    List<ComponentDto> result = underTest.selectByQuery(dbSession, dbQuery, forPage(1).andSize(10));
    int count = underTest.countByQuery(dbSession, dbQuery);

    assertThat(result).isEmpty();
    assertThat(count).isZero();
  }

  @Test
  void selectByQuery_on_component_uuids() {
    ComponentDto sonarqube = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto jdk8 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto cLang = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentQuery query = ComponentQuery.builder().setQualifiers(PROJECT)
      .setComponentUuids(newHashSet(sonarqube.uuid(), jdk8.uuid())).build();

    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, forPage(1).andSize(10));

    assertThat(result).hasSize(2).extracting(ComponentDto::uuid)
      .containsOnlyOnce(sonarqube.uuid(), jdk8.uuid())
      .doesNotContain(cLang.uuid());
  }

  @Test
  void selectAncestors() {
    // project -> dir -> file
    ComponentDto project = newPrivateProjectDto(PROJECT_UUID);
    db.components().insertProjectAndSnapshot(project);
    ComponentDto dir = newDirectory(project, DIR_UUID, "path");
    db.components().insertComponent(dir);
    ComponentDto file = newFileDto(dir, null, FILE_1_UUID);
    db.components().insertComponent(file);
    db.commit();

    // ancestors of root
    List<ComponentDto> ancestors = underTest.selectAncestors(dbSession, project);
    assertThat(ancestors).isEmpty();

    // ancestors of dir
    ancestors = underTest.selectAncestors(dbSession, dir);
    assertThat(ancestors).extracting("uuid").containsExactly(PROJECT_UUID);

    // ancestors of file
    ancestors = underTest.selectAncestors(dbSession, file);
    assertThat(ancestors).extracting("uuid").containsExactly(PROJECT_UUID, DIR_UUID);
  }

  @Test
  void select_children() {
    ComponentDto project = newPrivateProjectDto(PROJECT_UUID);
    db.components().insertProjectAndSnapshot(project);
    ComponentDto dir = newDirectory(project, DIR_UUID, "path");
    db.components().insertComponent(dir);
    ComponentDto fileInProject = newFileDto(project, null, FILE_1_UUID).setKey("file-key-1").setName("File One");
    db.components().insertComponent(fileInProject);
    ComponentDto file1InDir = newFileDto(dir, null, FILE_2_UUID).setKey("file-key-2").setName("File Two");
    db.components().insertComponent(file1InDir);
    ComponentDto file2InDir = newFileDto(dir, null, FILE_3_UUID).setKey("file-key-3").setName("File Three");
    db.components().insertComponent(file2InDir);
    db.commit();

    // test children of root
    assertThat(underTest.selectChildren(dbSession, project.uuid(), List.of(project))).extracting("uuid").containsOnly(FILE_1_UUID,
      DIR_UUID);

    // test children of intermediate component (dir here)
    assertThat(underTest.selectChildren(dbSession, project.uuid(), List.of(dir))).extracting("uuid").containsOnly(FILE_2_UUID, FILE_3_UUID);

    // test children of leaf component (file here)
    assertThat(underTest.selectChildren(dbSession, project.uuid(), List.of(fileInProject))).isEmpty();

    // test children of 2 components
    assertThat(underTest.selectChildren(dbSession, project.uuid(), List.of(project, dir))).extracting("uuid").containsOnly(FILE_1_UUID,
      DIR_UUID, FILE_2_UUID, FILE_3_UUID);
  }

  @Test
  void select_descendants_with_children_strategy() {
    // project has 2 children: dir and file 1. Other files are part of dir.
    ComponentDto project = newPrivateProjectDto(PROJECT_UUID);
    db.components().insertProjectAndSnapshot(project);
    ComponentDto dir = newDirectory(project, DIR_UUID, "dir");
    db.components().insertComponent(dir);
    ComponentDto fileInProject = newFileDto(project, null, FILE_1_UUID).setKey("file-key-1").setName("File One");
    db.components().insertComponent(fileInProject);
    ComponentDto file1InDir = newFileDto(project, dir, FILE_2_UUID).setKey("file-key-2").setName("File Two");
    db.components().insertComponent(file1InDir);
    ComponentDto file2InDir = newFileDto(project, dir, FILE_3_UUID).setKey("file-key-3").setName("File Three");
    db.components().insertComponent(file2InDir);
    db.commit();

    // test children of root
    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).build();
    List<ComponentDto> children = underTest.selectDescendants(dbSession, query);
    assertThat(children).extracting("uuid").containsOnly(FILE_1_UUID, DIR_UUID);

    // test children of root, filtered by qualifier
    query = newTreeQuery(PROJECT_UUID).setQualifiers(asList(Qualifiers.DIRECTORY)).build();
    children = underTest.selectDescendants(dbSession, query);
    assertThat(children).extracting("uuid").containsOnly(DIR_UUID);

    // test children of intermediate component (dir here), default ordering by
    query = newTreeQuery(DIR_UUID).build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_2_UUID, FILE_3_UUID);

    // test children of leaf component (file here)
    query = newTreeQuery(FILE_1_UUID).build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test children of root, matching name
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("One").build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);

    // test children of root, matching case-insensitive name
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("OnE").build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);

    // test children of root, matching key
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("file-key-1").build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);

    // test children of root, without matching name nor key
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("does-not-exist").build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test children of intermediate component (dir here), matching name
    query = newTreeQuery(DIR_UUID).setNameOrKeyQuery("Two").build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_2_UUID);

    // test children of intermediate component (dir here), without matching name
    query = newTreeQuery(DIR_UUID).setNameOrKeyQuery("does-not-exist").build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test children of leaf component (file here)
    query = newTreeQuery(FILE_1_UUID).build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test children of leaf component (file here), matching name
    query = newTreeQuery(FILE_1_UUID).setNameOrKeyQuery("Foo").build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test filtering by scope
    query = newTreeQuery(project.uuid()).setScopes(asList(Scopes.FILE)).build();
    assertThat(underTest.selectDescendants(dbSession, query))
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(fileInProject.uuid());
    query = newTreeQuery(project.uuid()).setScopes(asList(Scopes.DIRECTORY)).build();
    assertThat(underTest.selectDescendants(dbSession, query))
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(dir.uuid());
  }

  @Test
  void select_descendants_with_leaves_strategy() {
    ComponentDto project = newPrivateProjectDto(PROJECT_UUID);
    db.components().insertProjectAndSnapshot(project);
    db.components().insertComponent(newDirectory(project, "dir-1-uuid", "dir"));
    db.components().insertComponent(newFileDto(project, null, "file-1-uuid"));
    db.components().insertComponent(newFileDto(project, null, "file-2-uuid"));
    db.commit();

    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).setStrategy(LEAVES).build();

    List<ComponentDto> result = underTest.selectDescendants(dbSession, query);
    assertThat(result).extracting("uuid").containsOnly("file-1-uuid", "file-2-uuid", "dir-1-uuid");
  }

  @Test
  void select_descendants_returns_empty_list_if_base_component_does_not_exist() {
    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).setStrategy(CHILDREN).build();

    List<ComponentDto> result = underTest.selectDescendants(dbSession, query);
    assertThat(result).isEmpty();
  }

  @Test
  void select_descendants_of_a_view_and_filter_by_name() {
    ComponentDto view = ComponentTesting.newPortfolio(A_VIEW_UUID);
    db.components().insertPortfolioAndSnapshot(view);
    // one subview
    ComponentDto subView = ComponentTesting.newSubPortfolio(view, "subview-uuid", "subview-key").setName("subview name");
    db.components().insertComponent(subView);
    // one project and its copy linked to the view
    ComponentDto project = newPrivateProjectDto(PROJECT_UUID).setName("project name");
    db.components().insertProjectAndSnapshot(project);
    db.components().insertComponent(newProjectCopy("project-copy-uuid", project, view));
    ComponentTreeQuery dbQuery = newTreeQuery(A_VIEW_UUID).setNameOrKeyQuery("name").setStrategy(CHILDREN).build();

    List<ComponentDto> components = underTest.selectDescendants(dbSession, dbQuery);
    assertThat(components).extracting("uuid").containsOnly("project-copy-uuid", "subview-uuid");
  }

  @Test
  void setPrivateForBranchUuid_updates_private_column_to_specified_value_for_all_rows_with_specified_projectUuid() {
    String uuid1 = "uuid1";
    String uuid2 = "uuid2";

    String[] uuids = {
      db.components().insertComponent(newPrivateProjectDto().setBranchUuid(uuid1).setPrivate(true)).uuid(),
      db.components().insertComponent(newPrivateProjectDto().setBranchUuid(uuid1).setPrivate(false)).uuid(),
      db.components().insertComponent(newPrivateProjectDto().setBranchUuid(uuid2).setPrivate(true)).uuid(),
      db.components().insertComponent(newPrivateProjectDto().setBranchUuid(uuid2).setPrivate(false)).uuid(),
      db.components().insertComponent(newPrivateProjectDto().setBranchUuid("foo").setPrivate(false)).uuid(),
    };

    underTest.setPrivateForBranchUuidWithoutAudit(db.getSession(), uuid1, true);

    assertThat(privateFlagOfUuid(uuids[0])).isTrue();
    assertThat(privateFlagOfUuid(uuids[1])).isTrue();
    assertThat(privateFlagOfUuid(uuids[2])).isTrue();
    assertThat(privateFlagOfUuid(uuids[3])).isFalse();
    assertThat(privateFlagOfUuid(uuids[4])).isFalse();

    underTest.setPrivateForBranchUuidWithoutAudit(db.getSession(), uuid1, false);

    assertThat(privateFlagOfUuid(uuids[0])).isFalse();
    assertThat(privateFlagOfUuid(uuids[1])).isFalse();
    assertThat(privateFlagOfUuid(uuids[2])).isTrue();
    assertThat(privateFlagOfUuid(uuids[3])).isFalse();
    assertThat(privateFlagOfUuid(uuids[4])).isFalse();

    underTest.setPrivateForBranchUuidWithoutAudit(db.getSession(), uuid2, false);

    assertThat(privateFlagOfUuid(uuids[0])).isFalse();
    assertThat(privateFlagOfUuid(uuids[1])).isFalse();
    assertThat(privateFlagOfUuid(uuids[2])).isFalse();
    assertThat(privateFlagOfUuid(uuids[3])).isFalse();
    assertThat(privateFlagOfUuid(uuids[4])).isFalse();

    underTest.setPrivateForBranchUuidWithoutAudit(db.getSession(), uuid2, true);

    assertThat(privateFlagOfUuid(uuids[0])).isFalse();
    assertThat(privateFlagOfUuid(uuids[1])).isFalse();
    assertThat(privateFlagOfUuid(uuids[2])).isTrue();
    assertThat(privateFlagOfUuid(uuids[3])).isTrue();
    assertThat(privateFlagOfUuid(uuids[4])).isFalse();
  }

  @Test
  void existAnyOfComponentsWithQualifiers() {
    ComponentDto projectDto = db.components().insertComponent(newPrivateProjectDto());

    ComponentDto view = db.components().insertComponent(newPortfolio());
    ComponentDto subview = db.components().insertComponent(newSubPortfolio(view));

    ComponentDto app = db.components().insertComponent(newApplication());

    assertThat(underTest.existAnyOfComponentsWithQualifiers(db.getSession(), emptyList(), newHashSet(APP, VIEW, SUBVIEW))).isFalse();
    assertThat(underTest.existAnyOfComponentsWithQualifiers(db.getSession(), singletonList("not-existing-component"), newHashSet(APP,
      VIEW, SUBVIEW))).isFalse();
    assertThat(underTest.existAnyOfComponentsWithQualifiers(db.getSession(), singletonList(projectDto.getKey()), newHashSet(APP, VIEW,
      SUBVIEW))).isFalse();

    assertThat(underTest.existAnyOfComponentsWithQualifiers(db.getSession(), singletonList(projectDto.getKey()), newHashSet(PROJECT))).isTrue();

    assertThat(underTest.existAnyOfComponentsWithQualifiers(db.getSession(), singletonList(view.getKey()),
      newHashSet(APP, VIEW, SUBVIEW))).isTrue();
    assertThat(underTest.existAnyOfComponentsWithQualifiers(db.getSession(), singletonList(subview.getKey()), newHashSet(APP, VIEW,
      SUBVIEW))).isTrue();
    assertThat(underTest.existAnyOfComponentsWithQualifiers(db.getSession(), singletonList(app.getKey()), newHashSet(APP, VIEW, SUBVIEW))).isTrue();

    assertThat(underTest.existAnyOfComponentsWithQualifiers(db.getSession(), newHashSet(projectDto.getKey(), view.getKey()),
      newHashSet(APP, VIEW, SUBVIEW))).isTrue();
  }

  @Test
  void selectComponentsFromBranchesThatHaveOpenIssues() {
    final ProjectDto project = db.components().insertPrivateProject(b -> b.setName("foo")).getProjectDto();

    ComponentDto branch1 = db.components().insertProjectBranch(project, ComponentTesting.newBranchDto(project.getUuid(), BRANCH).setKey(
      "branch1"));
    ComponentDto fileBranch1 = db.components().insertComponent(ComponentTesting.newFileDto(branch1));

    ComponentDto branch2 = db.components().insertProjectBranch(project, ComponentTesting.newBranchDto(project.getUuid(), BRANCH).setKey(
      "branch2"));
    ComponentDto fileBranch2 = db.components().insertComponent(ComponentTesting.newFileDto(branch2));
    RuleDto rule = db.rules().insert();
    db.issues().insert(new IssueDto().setKee("i1").setComponent(fileBranch1).setProject(branch1).setRule(rule).setStatus(STATUS_CONFIRMED));
    db.issues().insert(new IssueDto().setKee("i2").setComponent(fileBranch2).setProject(branch2).setRule(rule).setStatus(STATUS_CLOSED));
    db.issues().insert(new IssueDto().setKee("i3").setComponent(fileBranch2).setProject(branch2).setRule(rule).setStatus(STATUS_OPEN));

    List<KeyWithUuidDto> result = underTest.selectComponentsFromBranchesThatHaveOpenIssues(db.getSession(), of(branch1.uuid(),
      branch2.uuid()));

    assertThat(result).extracting(KeyWithUuidDto::uuid).contains(fileBranch2.uuid());
  }

  @Test
  void selectComponentsFromBranchesThatHaveOpenIssues_returns_nothing_if_no_open_issues_in_sibling_branches() {
    final ProjectDto project = db.components().insertPrivateProject(b -> b.setName("foo")).getProjectDto();
    ComponentDto branch1 = db.components().insertProjectBranch(project, ComponentTesting.newBranchDto(project.getUuid(), BRANCH).setKey(
      "branch1"));
    ComponentDto fileBranch1 = db.components().insertComponent(ComponentTesting.newFileDto(branch1));
    RuleDto rule = db.rules().insert();
    db.issues().insert(new IssueDto().setKee("i").setComponent(fileBranch1).setProject(branch1).setRule(rule).setStatus(STATUS_CLOSED));

    List<KeyWithUuidDto> result = underTest.selectComponentsFromBranchesThatHaveOpenIssues(db.getSession(), singleton(branch1.uuid()));

    assertThat(result).isEmpty();
  }

  @Test
  void setPrivateForBranchUuid_auditPersisterIsCalled() {
    underTestWithAuditPersister.setPrivateForBranchUuid(dbSession, "anyUuid", false, "key", APP, "appName");

    verify(auditPersister).updateComponentVisibility(any(DbSession.class), any(ComponentNewValue.class));
  }

  @Test
  void setPrivateForBranchUuidWithoutAudit_auditPersisterIsNotCalled() {
    underTestWithAuditPersister.setPrivateForBranchUuidWithoutAudit(dbSession, "anyUuid", false);

    verifyNoInteractions(auditPersister);
  }

  @Test
  void update_auditPersisterIsCalled() {
    ComponentUpdateDto app = new ComponentUpdateDto().setUuid("uuid");
    app.setBQualifier(APP);

    underTestWithAuditPersister.update(dbSession, app, APP);

    verify(auditPersister).updateComponent(any(DbSession.class), any(ComponentNewValue.class));
  }

  @Test
  void insert_auditPersisterIsCalled() {
    ComponentDto app = ComponentTesting.newApplication();

    underTestWithAuditPersister.insert(dbSession, app, true);

    verify(auditPersister).addComponent(any(DbSession.class), any(ComponentNewValue.class));
  }

  @Test
  void insert_branch_auditPersisterIsNotCalled() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    BranchDto branch = newBranchDto(projectData.projectUuid(), BRANCH);
    ComponentDto branchComponent = newBranchComponent(project, branch);

    underTestWithAuditPersister.insert(dbSession, branchComponent, false);

    verifyNoInteractions(auditPersister);
  }

  @Test
  void selectByKeyCaseInsensitive_shouldFindProject_whenCaseIsDifferent() {
    String projectKey = randomAlphabetic(5).toLowerCase();
    db.components().insertPrivateProject(c -> c.setKey(projectKey)).getMainBranchComponent();

    List<ComponentDto> result = underTest.selectByKeyCaseInsensitive(db.getSession(), projectKey.toUpperCase());

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getKey()).isEqualTo(projectKey);
  }

  @Test
  void selectByKeyCaseInsensitive_should_not_match_non_main_branch() {
    String projectKey = randomAlphabetic(5).toLowerCase();
    ProjectDto project = db.components().insertPrivateProject(c -> c.setKey(projectKey)).getProjectDto();
    BranchDto projectBranch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertFile(projectBranch);

    List<ComponentDto> result = underTest.selectByKeyCaseInsensitive(db.getSession(), file.getKey());

    assertThat(result).isEmpty();
  }

  @Test
  void selectByKeyCaseInsensitive_shouldNotFindProject_whenKeyIsDifferent() {
    String projectKey = randomAlphabetic(5).toLowerCase();
    db.components().insertPrivateProject(c -> c.setKey(projectKey)).getMainBranchComponent();

    List<ComponentDto> result = underTest.selectByKeyCaseInsensitive(db.getSession(), projectKey + randomAlphabetic(1));

    assertThat(result).isEmpty();
  }

  private boolean privateFlagOfUuid(String uuid) {
    return underTest.selectByUuid(db.getSession(), uuid).get().isPrivate();
  }

  private static Set<String> shuffleWithNonExistentUuids(String... uuids) {
    return Stream.concat(
        IntStream.range(0, 1 + new Random().nextInt(5)).mapToObj(i -> randomAlphabetic(9)),
        Arrays.stream(uuids))
      .collect(toSet());
  }

  private static <T> Consumer<T> defaults() {
    return t -> {
    };
  }
}
