/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nullable;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.project.Project;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.core.config.PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

@RunWith(DataProviderRunner.class)
public class BranchPersisterImplIT {
  private final static Component MAIN = builder(Component.Type.PROJECT, 1, "PROJECT_KEY").setUuid("PROJECT_UUID").setName("p1").build();
  private final static Component BRANCH1 = builder(Component.Type.PROJECT, 2, "BRANCH_KEY").setUuid("BRANCH_UUID").build();
  private final static Component PR1 = builder(Component.Type.PROJECT, 3, "develop").setUuid("PR_UUID").build();
  private static final Project PROJECT = new Project("PROJECT_UUID", MAIN.getKey(), MAIN.getName(), null, Collections.emptyList());

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private final MapSettings settings = new MapSettings();
  private final ConfigurationRepository configurationRepository = new TestSettingsRepository(new ConfigurationBridge(settings));
  private final BranchPersister underTest = new BranchPersisterImpl(dbTester.getDbClient(), treeRootHolder, analysisMetadataHolder, configurationRepository);

  @Test
  public void persist_fails_with_ISE_if_no_component_for_main_branches() {
    analysisMetadataHolder.setBranch(createBranch(BRANCH, true, "master"));
    analysisMetadataHolder.setProject(PROJECT);
    treeRootHolder.setRoot(MAIN);
    DbSession dbSession = dbTester.getSession();

    expectMissingComponentISE(() -> underTest.persist(dbSession));
  }

  @Test
  public void persist_fails_with_ISE_if_no_component_for_branches() {
    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, "foo"));
    analysisMetadataHolder.setProject(PROJECT);

    treeRootHolder.setRoot(BRANCH1);
    DbSession dbSession = dbTester.getSession();

    expectMissingComponentISE(() -> underTest.persist(dbSession));
  }

  @Test
  public void persist_fails_with_ISE_if_no_component_for_pull_request() {
    analysisMetadataHolder.setBranch(createBranch(BranchType.PULL_REQUEST, false, "12"));
    analysisMetadataHolder.setProject(PROJECT);

    treeRootHolder.setRoot(BRANCH1);
    DbSession dbSession = dbTester.getSession();

    expectMissingComponentISE(() -> underTest.persist(dbSession));
  }

  @Test
  @UseDataProvider("nullOrNotNullString")
  public void persist_creates_row_in_PROJECTS_BRANCHES_for_branch(@Nullable String mergeBranchUuid) {
    String branchName = "branch";

    // add project and branch in table PROJECTS
    ComponentDto mainComponent = ComponentTesting.newPrivateProjectDto(MAIN.getUuid()).setKey(MAIN.getKey());
    ComponentDto component = ComponentTesting.newBranchComponent(mainComponent,
      new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(BRANCH));
    dbTester.components().insertComponents(mainComponent, component);
    // set project in metadata
    treeRootHolder.setRoot(BRANCH1);
    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, branchName, mergeBranchUuid));
    analysisMetadataHolder.setProject(Project.from(mainComponent));

    underTest.persist(dbTester.getSession());

    dbTester.getSession().commit();

    assertThat(dbTester.countRowsOfTable("components")).isEqualTo(2);
    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().getBranchType()).isEqualTo(BRANCH);
    assertThat(branchDto.get().getKey()).isEqualTo(branchName);
    assertThat(branchDto.get().getMergeBranchUuid()).isEqualTo(mergeBranchUuid);
    assertThat(branchDto.get().getProjectUuid()).isEqualTo(MAIN.getUuid());
    assertThat(branchDto.get().getPullRequestData()).isNull();
  }

  @Test
  public void main_branch_is_excluded_from_branch_purge_by_default() {
    analysisMetadataHolder.setBranch(createBranch(BRANCH, true, "master"));
    analysisMetadataHolder.setProject(PROJECT);
    treeRootHolder.setRoot(MAIN);
    dbTester.components().insertPublicProject(p -> p.setKey(MAIN.getKey()).setUuid(MAIN.getUuid())).getMainBranchComponent();
    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), MAIN.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().isExcludeFromPurge()).isTrue();
  }

  @Test
  public void non_main_branch_is_excluded_from_branch_purge_if_matches_sonar_dbcleaner_keepFromPurge_property() {
    settings.setProperty(BRANCHES_TO_KEEP_WHEN_INACTIVE, "BRANCH.*");
    analysisMetadataHolder.setProject(PROJECT);
    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, "BRANCH_KEY"));
    treeRootHolder.setRoot(BRANCH1);
    ComponentDto mainComponent = dbTester.components().insertPublicProject(p -> p.setKey(MAIN.getKey()).setUuid(MAIN.getUuid())).getMainBranchComponent();
    ComponentDto component = ComponentTesting.newBranchComponent(mainComponent,
      new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(BRANCH));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), component, false);
    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().isExcludeFromPurge()).isTrue();
  }

  @Test
  public void branch_is_excluded_from_purge_when_it_matches_setting() {
    analysisMetadataHolder.setProject(PROJECT);
    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, "BRANCH_KEY"));
    treeRootHolder.setRoot(BRANCH1);
    ComponentDto mainComponent = dbTester.components().insertPublicProject(p -> p.setKey(MAIN.getKey()).setUuid(MAIN.getUuid())).getMainBranchComponent();
    ComponentDto component = ComponentTesting.newBranchComponent(mainComponent,
      new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(BRANCH));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), component, false);
    settings.setProperty(BRANCHES_TO_KEEP_WHEN_INACTIVE, "BRANCH.*");
    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().isExcludeFromPurge()).isTrue();
  }

  @Test
  public void branch_is_not_excluded_from_purge_when_it_does_not_match_setting() {
    analysisMetadataHolder.setProject(PROJECT);
    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, "BRANCH_KEY"));
    treeRootHolder.setRoot(BRANCH1);
    ComponentDto mainComponent = dbTester.components().insertPublicProject(p -> p.setKey(MAIN.getKey()).setUuid(MAIN.getUuid())).getMainBranchComponent();
    ComponentDto component = ComponentTesting.newBranchComponent(mainComponent,
      new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(BRANCH));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), component, false);
    settings.setProperty(BRANCHES_TO_KEEP_WHEN_INACTIVE, "abc.*");

    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().isExcludeFromPurge()).isFalse();
  }

  @Test
  public void pull_request_is_never_excluded_from_branch_purge_even_if_its_source_branch_name_matches_sonar_dbcleaner_keepFromPurge_property() {
    settings.setProperty(BRANCHES_TO_KEEP_WHEN_INACTIVE, "develop");
    analysisMetadataHolder.setBranch(createPullRequest(PR1.getKey(), MAIN.getUuid()));
    analysisMetadataHolder.setProject(PROJECT);

    analysisMetadataHolder.setPullRequestKey(PR1.getKey());
    treeRootHolder.setRoot(PR1);
    ComponentDto mainComponent = dbTester.components().insertPublicProject(p -> p.setKey(MAIN.getKey()).setUuid(MAIN.getUuid())).getMainBranchComponent();
    ComponentDto component = ComponentTesting.newBranchComponent(mainComponent, new BranchDto()
      .setUuid(PR1.getUuid())
      .setKey(PR1.getKey())
      .setProjectUuid(MAIN.getUuid())
      .setBranchType(PULL_REQUEST)
      .setMergeBranchUuid(MAIN.getUuid()));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), component, false);
    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), PR1.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().isExcludeFromPurge()).isFalse();
  }

  @Test
  public void non_main_branch_is_included_in_branch_purge_if_branch_name_does_not_match_sonar_dbcleaner_keepFromPurge_property() {
    settings.setProperty(BRANCHES_TO_KEEP_WHEN_INACTIVE, "foobar-.*");
    analysisMetadataHolder.setProject(PROJECT);
    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, "BRANCH_KEY"));
    treeRootHolder.setRoot(BRANCH1);
    ComponentDto mainComponent = dbTester.components().insertPublicProject(p -> p.setKey(MAIN.getKey()).setUuid(MAIN.getUuid())).getMainBranchComponent();
    ComponentDto component = ComponentTesting.newBranchComponent(mainComponent,
      new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(BRANCH));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), component, false);
    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().isExcludeFromPurge()).isFalse();
  }

  @DataProvider
  public static Object[][] nullOrNotNullString() {
    return new Object[][] {
      {null},
      {secure().nextAlphabetic(12)}
    };
  }

  @Test
  public void persist_creates_row_in_PROJECTS_BRANCHES_for_pull_request() {
    String pullRequestId = "pr-123";

    // add project and branch in table PROJECTS
    ProjectData projectData = dbTester.components().insertPrivateProject(p -> p.setKey(MAIN.getKey()).setUuid(MAIN.getUuid()));
    ComponentDto mainComponent = projectData.getMainBranchComponent();
    ComponentDto component = ComponentTesting.newBranchComponent(mainComponent,
      new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(PULL_REQUEST));
    dbTester.components().insertComponents(component);
    // set project in metadata
    treeRootHolder.setRoot(BRANCH1);
    analysisMetadataHolder.setBranch(createBranch(PULL_REQUEST, false, pullRequestId, "mergeBanchUuid"));
    analysisMetadataHolder.setProject(Project.from(projectData.getProjectDto()));
    analysisMetadataHolder.setPullRequestKey(pullRequestId);

    underTest.persist(dbTester.getSession());

    dbTester.getSession().commit();

    assertThat(dbTester.countRowsOfTable("components")).isEqualTo(2);
    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().getBranchType()).isEqualTo(PULL_REQUEST);
    assertThat(branchDto.get().getKey()).isEqualTo(pullRequestId);
    assertThat(branchDto.get().getMergeBranchUuid()).isEqualTo("mergeBanchUuid");
    assertThat(branchDto.get().getProjectUuid()).isEqualTo(projectData.projectUuid());
    assertThat(branchDto.get().getPullRequestData()).isEqualTo(DbProjectBranches.PullRequestData.newBuilder()
      .setBranch(pullRequestId)
      .setTarget("mergeBanchUuid")
      .setTitle(pullRequestId)
      .build());
  }

  private static Branch createBranch(BranchType type, boolean isMain, String name) {
    return createBranch(type, isMain, name, null);
  }

  private static Branch createPullRequest(String key, String mergeBranchUuid) {
    Branch branch = createBranch(PULL_REQUEST, false, key, mergeBranchUuid);
    when(branch.getPullRequestKey()).thenReturn(key);
    return branch;
  }

  private static Branch createBranch(BranchType type, boolean isMain, String name, @Nullable String mergeBranchUuid) {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(type);
    when(branch.getName()).thenReturn(name);
    when(branch.isMain()).thenReturn(isMain);
    when(branch.getReferenceBranchUuid()).thenReturn(mergeBranchUuid);
    when(branch.getTargetBranchName()).thenReturn(mergeBranchUuid);
    return branch;
  }

  private void expectMissingComponentISE(ThrowableAssert.ThrowingCallable callable) {
    assertThatThrownBy(callable)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Component has been deleted by end-user during analysis");
  }
}
