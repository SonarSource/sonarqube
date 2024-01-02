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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class ReferenceBranchComponentUuidsIT {
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public DbTester db = DbTester.create();

  private ReferenceBranchComponentUuids underTest;
  private Branch branch = mock(Branch.class);

  private ComponentDto branch1;
  private ComponentDto branch1File;
  private ComponentDto pr1File;
  private ComponentDto pr2File;
  private Project project;
  private ComponentDto pr1;
  private ComponentDto pr2;
  private ComponentDto branch2;
  private ComponentDto branch2File;

  @Before
  public void setUp() {
    underTest = new ReferenceBranchComponentUuids(analysisMetadataHolder, db.getDbClient());
    project = mock(Project.class);
    analysisMetadataHolder.setProject(project);
    analysisMetadataHolder.setBranch(branch);

    ComponentDto projectDto = db.components().insertPublicProject().getMainBranchComponent();
    when(project.getUuid()).thenReturn(projectDto.uuid());
    branch1 = db.components().insertProjectBranch(projectDto, b -> b.setKey("branch1"));
    branch2 = db.components().insertProjectBranch(projectDto, b -> b.setKey("branch2"));
    pr1 = db.components().insertProjectBranch(projectDto, b -> b.setKey("pr1").setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid(branch1.uuid()));
    pr2 = db.components().insertProjectBranch(projectDto, b -> b.setKey("pr2").setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid(branch1.uuid()));
    branch1File = ComponentTesting.newFileDto(branch1, null, "file").setUuid("branch1File");
    branch2File = ComponentTesting.newFileDto(branch2, null, "file").setUuid("branch2File");
    pr1File = ComponentTesting.newFileDto(pr1, null, "file").setUuid("file1");
    pr2File = ComponentTesting.newFileDto(pr2, null, "file").setUuid("file2");
    db.components().insertComponents(branch1File, pr1File, pr2File, branch2File);
  }

  @Test
  public void should_support_db_key_when_looking_for_reference_component() {
    when(branch.getReferenceBranchUuid()).thenReturn(branch1.uuid());
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getTargetBranchName()).thenReturn("notAnalyzedBranch");
    db.components().insertSnapshot(newAnalysis(branch1));
    assertThat(underTest.getComponentUuid(pr1File.getKey())).isEqualTo(branch1File.uuid());
    assertThat(underTest.hasReferenceBranchAnalysis()).isTrue();
    assertThat(underTest.getReferenceBranchName()).isEqualTo("branch1");
  }

  @Test
  public void should_support_key_when_looking_for_reference_component() {
    when(branch.getReferenceBranchUuid()).thenReturn(branch1.uuid());
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getTargetBranchName()).thenReturn("notAnalyzedBranch");
    db.components().insertSnapshot(newAnalysis(branch1));
    assertThat(underTest.getComponentUuid(pr1File.getKey())).isEqualTo(branch1File.uuid());
  }

  @Test
  public void return_null_if_file_doesnt_exist() {
    when(branch.getReferenceBranchUuid()).thenReturn(branch1.uuid());
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getTargetBranchName()).thenReturn("notAnalyzedBranch");
    db.components().insertSnapshot(newAnalysis(branch1));
    assertThat(underTest.getComponentUuid("doesnt exist")).isNull();
  }

  @Test
  public void skip_init_if_no_reference_branch_analysis() {
    when(branch.getReferenceBranchUuid()).thenReturn(branch1.uuid());
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getTargetBranchName()).thenReturn("notAnalyzedBranch");
    assertThat(underTest.getComponentUuid(pr1File.getKey())).isNull();
  }
}
