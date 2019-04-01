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

public class MergeAndTargetBranchComponentUuidsTest {
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public DbTester db = DbTester.create();

  private MergeAndTargetBranchComponentUuids underTest;
  private Branch branch = mock(Branch.class);

  private ComponentDto longBranch1;
  private ComponentDto longBranch1File;
  private ComponentDto shortBranch1File;
  private ComponentDto shortBranch2File;
  private Project project;
  private ComponentDto shortBranch1;
  private ComponentDto shortBranch2;
  private ComponentDto longBranch2;
  private ComponentDto longBranch2File;

  @Before
  public void setUp() {
    underTest = new MergeAndTargetBranchComponentUuids(analysisMetadataHolder, db.getDbClient());
    project = mock(Project.class);
    analysisMetadataHolder.setProject(project);
    analysisMetadataHolder.setBranch(branch);

    ComponentDto projectDto = db.components().insertMainBranch();
    when(project.getUuid()).thenReturn(projectDto.uuid());
    longBranch1 = db.components().insertProjectBranch(projectDto, b -> b.setKey("longBranch1"));
    longBranch2 = db.components().insertProjectBranch(projectDto, b -> b.setKey("longBranch2"));
    shortBranch1 = db.components().insertProjectBranch(projectDto, b -> b.setKey("shortBranch1").setBranchType(BranchType.SHORT).setMergeBranchUuid(longBranch1.uuid()));
    shortBranch2 = db.components().insertProjectBranch(projectDto, b -> b.setKey("shortBranch2").setBranchType(BranchType.SHORT).setMergeBranchUuid(longBranch1.uuid()));
    longBranch1File = ComponentTesting.newFileDto(longBranch1, null, "file").setUuid("long1File");
    longBranch2File = ComponentTesting.newFileDto(longBranch2, null, "file").setUuid("long2File");
    shortBranch1File = ComponentTesting.newFileDto(shortBranch1, null, "file").setUuid("file1");
    shortBranch2File = ComponentTesting.newFileDto(shortBranch2, null, "file").setUuid("file2");
    db.components().insertComponents(longBranch1File, shortBranch1File, shortBranch2File, longBranch2File);
  }

  @Test
  public void should_support_db_key_when_looking_for_merge_component() {
    when(branch.getMergeBranchUuid()).thenReturn(longBranch1.uuid());
    when(branch.getType()).thenReturn(BranchType.SHORT);
    when(branch.getTargetBranchName()).thenReturn("notAnalyzedBranch");
    db.components().insertSnapshot(newAnalysis(longBranch1));
    assertThat(underTest.getMergeBranchComponentUuid(shortBranch1File.getDbKey())).isEqualTo(longBranch1File.uuid());
    assertThat(underTest.getTargetBranchComponentUuid(shortBranch1File.getDbKey())).isNull();
    assertThat(underTest.hasMergeBranchAnalysis()).isTrue();
    assertThat(underTest.hasTargetBranchAnalysis()).isFalse();
    assertThat(underTest.areTargetAndMergeBranchesDifferent()).isTrue();
    assertThat(underTest.getMergeBranchName()).isEqualTo("longBranch1");
  }

  @Test
  public void should_support_db_key_when_looking_for_target_component() {
    when(branch.getMergeBranchUuid()).thenReturn(longBranch1.uuid());
    when(branch.getTargetBranchName()).thenReturn("shortBranch2");
    when(branch.getType()).thenReturn(BranchType.SHORT);
    db.components().insertSnapshot(newAnalysis(longBranch1));
    db.components().insertSnapshot(newAnalysis(shortBranch2));
    assertThat(underTest.getMergeBranchComponentUuid(shortBranch1File.getDbKey())).isEqualTo(longBranch1File.uuid());
    assertThat(underTest.getTargetBranchComponentUuid(shortBranch1File.getDbKey())).isEqualTo(shortBranch2File.uuid());
    assertThat(underTest.hasMergeBranchAnalysis()).isTrue();
    assertThat(underTest.hasTargetBranchAnalysis()).isTrue();
    assertThat(underTest.areTargetAndMergeBranchesDifferent()).isTrue();
  }

  @Test
  public void should_support_key_when_looking_for_merge_component() {
    when(branch.getMergeBranchUuid()).thenReturn(longBranch1.uuid());
    when(branch.getType()).thenReturn(BranchType.SHORT);
    when(branch.getTargetBranchName()).thenReturn("notAnalyzedBranch");
    db.components().insertSnapshot(newAnalysis(longBranch1));
    assertThat(underTest.getMergeBranchComponentUuid(shortBranch1File.getKey())).isEqualTo(longBranch1File.uuid());
  }

  @Test
  public void return_null_if_file_doesnt_exist() {
    when(branch.getMergeBranchUuid()).thenReturn(longBranch1.uuid());
    when(branch.getType()).thenReturn(BranchType.SHORT);
    when(branch.getTargetBranchName()).thenReturn("notAnalyzedBranch");
    db.components().insertSnapshot(newAnalysis(longBranch1));
    assertThat(underTest.getMergeBranchComponentUuid("doesnt exist")).isNull();
  }

  @Test
  public void skip_init_if_no_merge_branch_analysis() {
    when(branch.getMergeBranchUuid()).thenReturn(longBranch1.uuid());
    when(branch.getType()).thenReturn(BranchType.SHORT);
    when(branch.getTargetBranchName()).thenReturn("notAnalyzedBranch");
    assertThat(underTest.getMergeBranchComponentUuid(shortBranch1File.getDbKey())).isNull();
  }

  @Test
  public void should_skip_target_components_init_on_long_branches() {
    when(branch.getMergeBranchUuid()).thenReturn(longBranch1.uuid());
    when(branch.getType()).thenReturn(BranchType.LONG);
    when(branch.getTargetBranchName()).thenThrow(new IllegalStateException("Unsupported on long branches"));
    db.components().insertSnapshot(newAnalysis(longBranch1));

    assertThat(underTest.getMergeBranchComponentUuid(longBranch2File.getDbKey())).isEqualTo(longBranch1File.uuid());
    assertThat(underTest.getTargetBranchComponentUuid(longBranch2File.getDbKey())).isNull();
    assertThat(underTest.hasMergeBranchAnalysis()).isTrue();
    assertThat(underTest.hasTargetBranchAnalysis()).isFalse();
    assertThat(underTest.areTargetAndMergeBranchesDifferent()).isTrue();
    assertThat(underTest.getMergeBranchName()).isEqualTo("longBranch1");
  }
}
