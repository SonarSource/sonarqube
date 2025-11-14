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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class SourceBranchComponentUuidsIT {

  private static final String BRANCH_KEY = "branch1";
  private static final String PR_KEY = "pr1";

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public DbTester db = DbTester.create();

  private SourceBranchComponentUuids underTest;
  private final Branch branch = mock(Branch.class);
  private ComponentDto branch1;
  private ComponentDto branch1File;
  private ComponentDto pr1File;

  @Before
  public void setup() {
    underTest = new SourceBranchComponentUuids(analysisMetadataHolder, db.getDbClient());
    Project project = mock(Project.class);
    analysisMetadataHolder.setProject(project);
    analysisMetadataHolder.setBranch(branch);

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    when(project.getUuid()).thenReturn(projectData.projectUuid());
    branch1 = db.components().insertProjectBranch(mainBranch, b -> b.setKey(BRANCH_KEY));
    ComponentDto pr1branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey(PR_KEY)
      .setBranchType(BranchType.PULL_REQUEST)
      .setPullRequestData(DbProjectBranches.PullRequestData.newBuilder().setBranch(BRANCH_KEY).build())
      .setMergeBranchUuid(mainBranch.uuid()));
    branch1File = ComponentTesting.newFileDto(branch1, null, "file").setUuid("branch1File");
    pr1File = ComponentTesting.newFileDto(pr1branch, null, "file").setUuid("file1");
    db.components().insertComponents(branch1File, pr1File);
  }

  @Test
  public void should_support_db_key_when_looking_for_source_branch_component() {
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getName()).thenReturn(BRANCH_KEY);
    when(branch.getPullRequestKey()).thenReturn(PR_KEY);
    db.components().insertSnapshot(newAnalysis(branch1));

    assertThat(underTest.getSourceBranchComponentUuid(pr1File.getKey())).isEqualTo(branch1File.uuid());
    assertThat(underTest.hasSourceBranchAnalysis()).isTrue();
  }

  @Test
  public void should_support_key_when_looking_for_source_branch_component() {
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getName()).thenReturn(BRANCH_KEY);
    when(branch.getPullRequestKey()).thenReturn(PR_KEY);
    db.components().insertSnapshot(newAnalysis(branch1));

    assertThat(underTest.getSourceBranchComponentUuid(pr1File.getKey())).isEqualTo(branch1File.uuid());
  }

  @Test
  public void return_null_if_file_doesnt_exist() {
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getName()).thenReturn(BRANCH_KEY);
    when(branch.getPullRequestKey()).thenReturn(PR_KEY);
    db.components().insertSnapshot(newAnalysis(branch1));

    assertThat(underTest.getSourceBranchComponentUuid("doesnt exist")).isNull();
  }

  @Test
  public void skip_init_if_not_a_pull_request() {
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    when(branch.getName()).thenReturn(BRANCH_KEY);

    assertThat(underTest.getSourceBranchComponentUuid(pr1File.getKey())).isNull();
  }

  @Test
  public void skip_init_if_no_source_branch_analysis() {
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getName()).thenReturn(BRANCH_KEY);

    assertThat(underTest.getSourceBranchComponentUuid(pr1File.getKey())).isNull();
  }
}
