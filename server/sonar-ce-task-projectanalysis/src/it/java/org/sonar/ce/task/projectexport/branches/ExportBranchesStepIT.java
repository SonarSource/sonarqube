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
package org.sonar.ce.task.projectexport.branches;

import com.google.common.collect.ImmutableList;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectExportMapper;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExportBranchesStepIT {
  private static final String PROJECT_UUID = "PROJECT_UUID";

  @Rule
  public DbTester dbTester = DbTester.createWithExtensionMappers(System2.INSTANCE, ProjectExportMapper.class);
  @Rule
  public LogTester logTester = new LogTester();

  private final ProjectHolder projectHolder = mock(ProjectHolder.class);
  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final ExportBranchesStep underTest = new ExportBranchesStep(dumpWriter, dbTester.getDbClient(), projectHolder);
  private final List<BranchDto> branches = ImmutableList.of(
    new BranchDto()
      .setBranchType(BranchType.BRANCH)
      .setProjectUuid(PROJECT_UUID)
      .setKey("branch-1")
      .setUuid("branch-1-uuid")
      .setMergeBranchUuid("master")
      .setIsMain(false)
      .setExcludeFromPurge(true),
    new BranchDto()
      .setBranchType(BranchType.PULL_REQUEST)
      .setProjectUuid(PROJECT_UUID)
      .setKey("branch-3")
      .setUuid("branch-3-uuid")
      .setIsMain(false)
      .setMergeBranchUuid("master"),
    new BranchDto()
      .setBranchType(BranchType.BRANCH)
      .setProjectUuid(PROJECT_UUID)
      .setKey("branch-4")
      .setUuid("branch-4-uuid")
      .setIsMain(false)
      .setMergeBranchUuid("branch-1-uuid"),
    new BranchDto()
      .setBranchType(BranchType.BRANCH)
      .setProjectUuid(PROJECT_UUID)
      .setKey("branch-5")
      .setUuid("branch-5-uuid")
      .setIsMain(false)
      .setMergeBranchUuid("master")
      .setExcludeFromPurge(true));

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    Date createdAt = new Date();
    ProjectData projectData = dbTester.components().insertPublicProject(PROJECT_UUID);
    for (BranchDto branch : branches) {
      createdAt = DateUtils.addMinutes(createdAt, 10);
      dbTester.components().insertProjectBranch(projectData.getProjectDto(), branch).setCreatedAt(createdAt);
    }
    dbTester.commit();
    when(projectHolder.projectDto()).thenReturn(projectData.getProjectDto());
  }

  @Test
  public void export_branches() {
    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("3 branches exported");
    Map<String, ProjectDump.Branch> branches = dumpWriter.getWrittenMessagesOf(DumpElement.BRANCHES)
      .stream()
      .collect(toMap(ProjectDump.Branch::getUuid, Function.identity()));
    assertThat(branches).hasSize(3);
    ProjectDump.Branch mainBranch = branches.values().stream().filter(ProjectDump.Branch::getIsMain).findFirst().get();
    assertThat(mainBranch).isNotNull();
    assertThat(mainBranch.getKee()).isEqualTo(BranchDto.DEFAULT_MAIN_BRANCH_NAME);
    assertThat(mainBranch.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(mainBranch.getMergeBranchUuid()).isEmpty();
    assertThat(mainBranch.getBranchType()).isEqualTo("BRANCH");
    ProjectDump.Branch branch1 = branches.get("branch-1-uuid");
    assertThat(branch1.getKee()).isEqualTo("branch-1");
    assertThat(branch1.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(branch1.getMergeBranchUuid()).isEqualTo("master");
    assertThat(branch1.getBranchType()).isEqualTo("BRANCH");
    ProjectDump.Branch branch5 = branches.get("branch-5-uuid");
    assertThat(branch5.getKee()).isEqualTo("branch-5");
    assertThat(branch5.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(branch5.getMergeBranchUuid()).isEqualTo("master");
    assertThat(branch5.getBranchType()).isEqualTo("BRANCH");
  }

  @Test
  public void throws_ISE_if_error() {
    dumpWriter.failIfMoreThan(1, DumpElement.BRANCHES);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Branch export failed after processing 1 branch(es) successfully");
  }

  @Test
  public void getDescription_is_defined() {
    assertThat(underTest.getDescription()).isEqualTo("Export branches");
  }
}
