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
package org.sonar.ce.task.projectanalysis.component;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.project.Project;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.core.config.PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

@RunWith(DataProviderRunner.class)
public class BranchPersisterImplTest {
  private final static Component MAIN = builder(PROJECT, 1).setUuid("PROJECT_UUID").setKey("PROJECT_KEY").build();
  private final static Component BRANCH1 = builder(PROJECT, 1).setUuid("BRANCH_UUID").setKey("BRANCH_KEY").build();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private Configuration configuration = mock(Configuration.class);

  private BranchPersister underTest = new BranchPersisterImpl(dbTester.getDbClient(), treeRootHolder, analysisMetadataHolder, configuration);

  @Test
  public void persist_fails_with_ISE_if_no_component_for_main_branches() {
    analysisMetadataHolder.setBranch(createBranch(BRANCH, true, "master"));
    treeRootHolder.setRoot(MAIN);

    expectMissingComponentISE();

    underTest.persist(dbTester.getSession());
  }

  @Test
  public void persist_fails_with_ISE_if_no_component_for_branches() {
    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, "foo"));
    treeRootHolder.setRoot(BRANCH1);

    expectMissingComponentISE();

    underTest.persist(dbTester.getSession());
  }

  @Test
  public void persist_fails_with_ISE_if_no_component_for_pull_request() {
    analysisMetadataHolder.setBranch(createBranch(BranchType.PULL_REQUEST, false, "12"));
    treeRootHolder.setRoot(BRANCH1);

    expectMissingComponentISE();

    underTest.persist(dbTester.getSession());
  }

  @Test
  @UseDataProvider("nullOrNotNullString")
  public void persist_creates_row_in_PROJECTS_BRANCHES_for_branch(@Nullable String mergeBranchUuid) {
    String branchName = "branch";

    // add project and branch in table PROJECTS
    ComponentDto mainComponent = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), MAIN.getUuid()).setDbKey(MAIN.getKey());
    ComponentDto component = ComponentTesting.newProjectBranch(mainComponent, new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(BRANCH));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), mainComponent, component);
    dbTester.commit();
    // set project in metadata
    treeRootHolder.setRoot(BRANCH1);
    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, branchName, mergeBranchUuid));
    analysisMetadataHolder.setProject(Project.from(mainComponent));

    underTest.persist(dbTester.getSession());

    dbTester.getSession().commit();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(2);
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
    treeRootHolder.setRoot(MAIN);
    dbTester.components().insertMainBranch(p -> p.setDbKey(MAIN.getDbKey()).setUuid(MAIN.getUuid()));
    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), MAIN.getUuid());
    assertThat(branchDto.get().isExcludeFromPurge()).isTrue();
  }

  @Test
  public void non_main_branch_is_excluded_from_branch_purge_if_matches_sonar_dbcleaner_keepFromPurge_property() {
    when(configuration.getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE)).thenReturn(new String[] {"BRANCH.*"});

    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, "BRANCH_KEY"));
    treeRootHolder.setRoot(BRANCH1);
    ComponentDto mainComponent = dbTester.components().insertMainBranch(p -> p.setDbKey(MAIN.getDbKey()).setUuid(MAIN.getUuid()));
    ComponentDto component = ComponentTesting.newProjectBranch(mainComponent, new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(BRANCH));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), component);
    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto.get().isExcludeFromPurge()).isTrue();
  }

  @Test
  public void non_main_branch_is_included_in_branch_purge_if_branch_name_does_not_match_sonar_dbcleaner_keepFromPurge_property() {
    when(configuration.getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE)).thenReturn(new String[] {"foobar-.*"});

    analysisMetadataHolder.setBranch(createBranch(BRANCH, false, "BRANCH_KEY"));
    treeRootHolder.setRoot(BRANCH1);
    ComponentDto mainComponent = dbTester.components().insertMainBranch(p -> p.setDbKey(MAIN.getDbKey()).setUuid(MAIN.getUuid()));
    ComponentDto component = ComponentTesting.newProjectBranch(mainComponent, new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(BRANCH));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), component);
    dbTester.commit();

    underTest.persist(dbTester.getSession());

    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto.get().isExcludeFromPurge()).isFalse();
  }

  @DataProvider
  public static Object[][] nullOrNotNullString() {
    return new Object[][] {
      {null},
      {randomAlphabetic(12)}
    };
  }

  @Test
  public void persist_creates_row_in_PROJECTS_BRANCHES_for_pull_request() {
    String pullRequestId = "pr-123";

    // add project and branch in table PROJECTS
    ComponentDto mainComponent = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), MAIN.getUuid()).setDbKey(MAIN.getKey());
    ComponentDto component = ComponentTesting.newProjectBranch(mainComponent, new BranchDto().setUuid(BRANCH1.getUuid()).setKey(BRANCH1.getKey()).setBranchType(PULL_REQUEST));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), mainComponent, component);
    dbTester.commit();
    // set project in metadata
    treeRootHolder.setRoot(BRANCH1);
    analysisMetadataHolder.setBranch(createBranch(PULL_REQUEST, false, pullRequestId, "mergeBanchUuid"));
    analysisMetadataHolder.setProject(Project.from(mainComponent));
    analysisMetadataHolder.setPullRequestKey(pullRequestId);

    underTest.persist(dbTester.getSession());

    dbTester.getSession().commit();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(2);
    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH1.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().getBranchType()).isEqualTo(PULL_REQUEST);
    assertThat(branchDto.get().getKey()).isEqualTo(pullRequestId);
    assertThat(branchDto.get().getMergeBranchUuid()).isEqualTo("mergeBanchUuid");
    assertThat(branchDto.get().getProjectUuid()).isEqualTo(MAIN.getUuid());
    assertThat(branchDto.get().getPullRequestData()).isEqualTo(DbProjectBranches.PullRequestData.newBuilder()
      .setBranch(pullRequestId)
      .setTarget("mergeBanchUuid")
      .setTitle(pullRequestId)
      .build());
  }

  private static Branch createBranch(BranchType type, boolean isMain, String name) {
    return createBranch(type, isMain, name, null);
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

  private void expectMissingComponentISE() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Component has been deleted by end-user during analysis");
  }
}
