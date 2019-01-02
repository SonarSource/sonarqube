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
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

@RunWith(DataProviderRunner.class)
public class BranchPersisterImplTest {
  private final static Component MAIN = builder(PROJECT, 1).setUuid("PROJECT_UUID").setKey("PROJECT_KEY").build();
  private final static Component BRANCH = builder(PROJECT, 1).setUuid("BRANCH_UUID").setKey("BRANCH_KEY").build();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public ExpectedException exception = ExpectedException.none();

  BranchPersister underTest = new BranchPersisterImpl(dbTester.getDbClient(), treeRootHolder, analysisMetadataHolder);

  @Test
  public void persist_fails_with_ISE_if_no_component_for_main_branches() {
    analysisMetadataHolder.setBranch(createBranch(LONG, true, "master"));
    treeRootHolder.setRoot(MAIN);

    expectMissingComponentISE();

    underTest.persist(dbTester.getSession());
  }

  @Test
  public void persist_fails_with_ISE_if_no_component_for_long_branches() {
    analysisMetadataHolder.setBranch(createBranch(LONG, false, "foo"));
    treeRootHolder.setRoot(BRANCH);

    expectMissingComponentISE();

    underTest.persist(dbTester.getSession());
  }

  @Test
  public void persist_fails_with_ISE_if_no_component_for_short_branches() {
    analysisMetadataHolder.setBranch(createBranch(BranchType.SHORT, false, "foo"));
    treeRootHolder.setRoot(BRANCH);

    expectMissingComponentISE();

    underTest.persist(dbTester.getSession());
  }

  @Test
  public void persist_fails_with_ISE_if_no_component_for_pull_request() {
    analysisMetadataHolder.setBranch(createBranch(BranchType.PULL_REQUEST, false, "12"));
    treeRootHolder.setRoot(BRANCH);

    expectMissingComponentISE();

    underTest.persist(dbTester.getSession());
  }

  @Test
  @UseDataProvider("nullOrNotNullString")
  public void persist_creates_row_in_PROJECTS_BRANCHES_for_long_branch(@Nullable String mergeBranchUuid) {
    String branchName = "branch";

    // add project and branch in table PROJECTS
    ComponentDto mainComponent = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), MAIN.getUuid()).setDbKey(MAIN.getKey());
    ComponentDto component = ComponentTesting.newProjectBranch(mainComponent, new BranchDto().setUuid(BRANCH.getUuid()).setKey(BRANCH.getKey()).setBranchType(LONG));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), mainComponent, component);
    dbTester.commit();
    // set project in metadata
    treeRootHolder.setRoot(BRANCH);
    analysisMetadataHolder.setBranch(createBranch(LONG, false, branchName, mergeBranchUuid));
    analysisMetadataHolder.setProject(Project.from(mainComponent));

    underTest.persist(dbTester.getSession());

    dbTester.getSession().commit();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(2);
    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().getBranchType()).isEqualTo(LONG);
    assertThat(branchDto.get().getKey()).isEqualTo(branchName);
    assertThat(branchDto.get().getMergeBranchUuid()).isEqualTo(mergeBranchUuid);
    assertThat(branchDto.get().getProjectUuid()).isEqualTo(MAIN.getUuid());
    assertThat(branchDto.get().getPullRequestData()).isNull();
  }

  @DataProvider
  public static Object[][] nullOrNotNullString() {
    return new Object[][] {
      {null},
      {randomAlphabetic(12)}
    };
  }

  @Test
  @UseDataProvider("nullOrNotNullString")
  public void persist_creates_row_in_PROJECTS_BRANCHES_for_pull_request(@Nullable String mergeBranchUuid) {
    String pullRequestId = "pr-123";

    // add project and branch in table PROJECTS
    ComponentDto mainComponent = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), MAIN.getUuid()).setDbKey(MAIN.getKey());
    ComponentDto component = ComponentTesting.newProjectBranch(mainComponent, new BranchDto().setUuid(BRANCH.getUuid()).setKey(BRANCH.getKey()).setBranchType(PULL_REQUEST));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), mainComponent, component);
    dbTester.commit();
    // set project in metadata
    treeRootHolder.setRoot(BRANCH);
    analysisMetadataHolder.setBranch(createBranch(PULL_REQUEST, false, pullRequestId, mergeBranchUuid));
    analysisMetadataHolder.setProject(Project.from(mainComponent));
    analysisMetadataHolder.setPullRequestKey(pullRequestId);

    underTest.persist(dbTester.getSession());

    dbTester.getSession().commit();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(2);
    Optional<BranchDto> branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), BRANCH.getUuid());
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().getBranchType()).isEqualTo(PULL_REQUEST);
    assertThat(branchDto.get().getKey()).isEqualTo(pullRequestId);
    assertThat(branchDto.get().getMergeBranchUuid()).isEqualTo(mergeBranchUuid);
    assertThat(branchDto.get().getProjectUuid()).isEqualTo(MAIN.getUuid());
    assertThat(branchDto.get().getPullRequestData()).isEqualTo(DbProjectBranches.PullRequestData.newBuilder()
      .setBranch(pullRequestId)
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
    when(branch.getMergeBranchUuid()).thenReturn(Optional.ofNullable(mergeBranchUuid));
    return branch;
  }

  private void expectMissingComponentISE() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Component has been deleted by end-user during analysis");
  }
}
