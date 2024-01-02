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
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectExportMapper;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;

public class ExportBranchesStepTest {
  private static final String PROJECT_UUID = "PROJECT_UUID";
  private static final ComponentDto PROJECT = new ComponentDto()
    // no id yet
    .setScope(Scopes.PROJECT)
    .setQualifier(Qualifiers.PROJECT)
    .setKey("the_project")
    .setName("The Project")
    .setDescription("The project description")
    .setEnabled(true)
    .setUuid(PROJECT_UUID)
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setRootUuid(PROJECT_UUID)
    .setModuleUuid(null)
    .setModuleUuidPath("." + PROJECT_UUID + ".")
    .setBranchUuid(PROJECT_UUID);

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
      .setExcludeFromPurge(true),
    new BranchDto()
      .setBranchType(BranchType.PULL_REQUEST)
      .setProjectUuid(PROJECT_UUID)
      .setKey("branch-3")
      .setUuid("branch-3-uuid")
      .setMergeBranchUuid("master"),
    new BranchDto()
      .setBranchType(BranchType.BRANCH)
      .setProjectUuid(PROJECT_UUID)
      .setKey("branch-4")
      .setUuid("branch-4-uuid")
      .setMergeBranchUuid("branch-1-uuid"),
    new BranchDto()
      .setBranchType(BranchType.BRANCH)
      .setProjectUuid(PROJECT_UUID)
      .setKey("branch-5")
      .setUuid("branch-5-uuid")
      .setMergeBranchUuid("master")
      .setExcludeFromPurge(true));

  @Before
  public void setUp() {
    Date createdAt = new Date();
    ComponentDto projectDto = dbTester.components().insertPublicProject(PROJECT).setCreatedAt(createdAt);
    for (BranchDto branch : branches) {
      createdAt = DateUtils.addMinutes(createdAt, 10);
      dbTester.components().insertProjectBranch(PROJECT, branch).setCreatedAt(createdAt);
    }
    dbTester.commit();
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDto(projectDto));
  }

  @Test
  public void export_branches() {
    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("3 branches exported");
    Map<String, ProjectDump.Branch> branches = dumpWriter.getWrittenMessagesOf(DumpElement.BRANCHES)
      .stream()
      .collect(toMap(ProjectDump.Branch::getUuid, Function.identity()));
    assertThat(branches).hasSize(3);
    ProjectDump.Branch mainBranch = branches.get(PROJECT_UUID);
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
