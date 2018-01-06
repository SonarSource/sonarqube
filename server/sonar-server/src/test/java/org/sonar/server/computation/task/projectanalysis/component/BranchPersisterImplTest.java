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
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.analysis.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

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

  BranchPersister underTest = new BranchPersisterImpl(dbTester.getDbClient(), System2.INSTANCE, treeRootHolder, analysisMetadataHolder);

  @Test
  public void fail_if_no_component_for_main_branches() {
    analysisMetadataHolder.setBranch(createBranch(BranchType.LONG, true, "master"));
    treeRootHolder.setRoot(MAIN);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Project has been deleted by end-user during analysis");

    underTest.persist(dbTester.getSession());
  }

  @Test
  public void persist_secondary_branch() {
    analysisMetadataHolder.setBranch(createBranch(BranchType.LONG, false, "branch"));
    treeRootHolder.setRoot(BRANCH);

    // add main branch in project table and in metadata
    ComponentDto dto = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), MAIN.getUuid()).setDbKey(MAIN.getKey());
    analysisMetadataHolder.setProject(Project.copyOf(dto));
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), dto);

    // this should add new columns in project and project_branches
    underTest.persist(dbTester.getSession());

    dbTester.getSession().commit();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(2);
    assertThat(dbTester.countRowsOfTable("project_branches")).isEqualTo(1);

  }

  private static Branch createBranch(BranchType type, boolean isMain, String name) {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(type);
    when(branch.getName()).thenReturn(name);
    when(branch.isMain()).thenReturn(isMain);
    when(branch.getMergeBranchUuid()).thenReturn(Optional.empty());
    return branch;
  }
}
