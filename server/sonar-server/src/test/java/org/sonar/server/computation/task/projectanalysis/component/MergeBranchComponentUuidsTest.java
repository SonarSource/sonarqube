/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MergeBranchComponentUuidsTest {
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public DbTester db = DbTester.create();

  private MergeBranchComponentUuids underTest;
  private Branch branch = mock(Branch.class);

  @Before
  public void setUp() {
    underTest = new MergeBranchComponentUuids(analysisMetadataHolder, db.getDbClient());
    analysisMetadataHolder.setBranch(branch);
  }

  @Test
  public void should_support_db_key() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto mergeBranch = db.components().insertProjectBranch(project, b -> b.setKey("mergeBranch"));
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setKey("branch1"));
    ComponentDto mergeBranchFile = ComponentTesting.newFileDto(mergeBranch, null, "file").setUuid("mergeFile");
    ComponentDto file = ComponentTesting.newFileDto(branch1, null, "file").setUuid("file1");

    db.components().insertComponents(mergeBranchFile, file);

    when(branch.getMergeBranchUuid()).thenReturn(Optional.of(mergeBranch.uuid()));
    assertThat(underTest.getUuid(file.getDbKey())).isEqualTo(mergeBranchFile.uuid());
  }

  @Test
  public void should_support_key() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto mergeBranch = db.components().insertProjectBranch(project, b -> b.setKey("mergeBranch"));
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setKey("branch1"));
    ComponentDto mergeBranchFile = ComponentTesting.newFileDto(mergeBranch, null, "file").setUuid("mergeFile");
    ComponentDto file = ComponentTesting.newFileDto(branch1, null, "file").setUuid("file1");

    db.components().insertComponents(mergeBranchFile, file);

    when(branch.getMergeBranchUuid()).thenReturn(Optional.of(mergeBranch.uuid()));
    assertThat(underTest.getUuid(file.getKey())).isEqualTo(mergeBranchFile.uuid());
  }

  @Test
  public void return_null_if_file_doesnt_exist() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto mergeBranch = db.components().insertProjectBranch(project, b -> b.setKey("mergeBranch"));
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setKey("branch1"));
    ComponentDto mergeBranchFile = ComponentTesting.newFileDto(mergeBranch, null, "file").setUuid("mergeFile");
    ComponentDto file = ComponentTesting.newFileDto(branch1, null, "file").setUuid("file1");

    db.components().insertComponents(mergeBranchFile, file);

    when(branch.getMergeBranchUuid()).thenReturn(Optional.of(mergeBranch.uuid()));
    assertThat(underTest.getUuid("doesnt exist")).isNull();
  }
}
