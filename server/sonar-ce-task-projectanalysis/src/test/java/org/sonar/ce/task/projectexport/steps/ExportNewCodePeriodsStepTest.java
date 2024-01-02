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
package org.sonar.ce.task.projectexport.steps;

import com.google.common.collect.ImmutableList;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectExportMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.PREVIOUS_VERSION;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.SPECIFIC_ANALYSIS;

public class ExportNewCodePeriodsStepTest {

  private static final String PROJECT_UUID = "project_uuid";
  private static final String ANOTHER_PROJECT_UUID = "another_project_uuid";
  private static final ComponentDto PROJECT = new ComponentDto()
    .setUuid(PROJECT_UUID)
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setRootUuid(PROJECT_UUID)
    .setBranchUuid(PROJECT_UUID)
    .setQualifier(Qualifiers.PROJECT)
    .setName("project")
    .setKey("the_project");
  private static final ComponentDto ANOTHER_PROJECT = new ComponentDto()
    .setUuid(ANOTHER_PROJECT_UUID)
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setRootUuid(ANOTHER_PROJECT_UUID)
    .setBranchUuid(ANOTHER_PROJECT_UUID)
    .setQualifier(Qualifiers.PROJECT)
    .setName("another_project")
    .setKey("another_project");

  private static final List<BranchDto> PROJECT_BRANCHES = ImmutableList.of(
    new BranchDto().setBranchType(BranchType.PULL_REQUEST).setProjectUuid(PROJECT_UUID).setKey("pr-1").setUuid("pr-uuid-1").setMergeBranchUuid("master"),
    new BranchDto().setBranchType(BranchType.BRANCH).setProjectUuid(PROJECT_UUID).setKey("branch-1").setUuid("branch-uuid-1").setMergeBranchUuid("master")
      .setExcludeFromPurge(true),
    new BranchDto().setBranchType(BranchType.BRANCH).setProjectUuid(PROJECT_UUID).setKey("branch-2").setUuid("branch-uuid-2").setMergeBranchUuid("master")
      .setExcludeFromPurge(false));

  private static final List<BranchDto> ANOTHER_PROJECT_BRANCHES = ImmutableList.of(
    new BranchDto().setBranchType(BranchType.BRANCH).setProjectUuid(ANOTHER_PROJECT_UUID).setKey("branch-3").setUuid("branch-uuid-3").setMergeBranchUuid("master")
      .setExcludeFromPurge(true));

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public DbTester dbTester = DbTester.createWithExtensionMappers(System2.INSTANCE, ProjectExportMapper.class);

  private MutableProjectHolder projectHolder = new MutableProjectHolderImpl();
  private FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private ExportNewCodePeriodsStep underTest = new ExportNewCodePeriodsStep(dbTester.getDbClient(), projectHolder, dumpWriter);

  @Before
  public void setUp() {
    Date createdAt = new Date();
    ComponentDto projectDto = dbTester.components().insertPublicProject(PROJECT);
    PROJECT_BRANCHES.forEach(branch -> dbTester.components().insertProjectBranch(projectDto, branch).setCreatedAt(createdAt));

    ComponentDto anotherProjectDto = dbTester.components().insertPublicProject(ANOTHER_PROJECT);
    ANOTHER_PROJECT_BRANCHES.forEach(branch -> dbTester.components().insertProjectBranch(anotherProjectDto, branch).setCreatedAt(createdAt));

    dbTester.commit();
    projectHolder.setProjectDto(dbTester.components().getProjectDto(PROJECT));
  }

  @Test
  public void export_zero_new_code_periods() {
    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.NEW_CODE_PERIODS)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("0 new code periods exported");
  }

  @Test
  public void export_only_project_new_code_periods_on_branches_excluded_from_purge() {
    NewCodePeriodDto newCodePeriod1 = newDto("uuid1", PROJECT.uuid(), null, SPECIFIC_ANALYSIS, "analysis-uuid");
    NewCodePeriodDto newCodePeriod2 = newDto("uuid2", PROJECT.uuid(), "branch-uuid-1", SPECIFIC_ANALYSIS, "analysis-uuid");
    // the following new code periods are not exported
    NewCodePeriodDto newCodePeriod3 = newDto("uuid3", PROJECT.uuid(), "branch-uuid-2", SPECIFIC_ANALYSIS, "analysis-uuid");
    NewCodePeriodDto anotherProjectNewCodePeriods = newDto("uuid4", ANOTHER_PROJECT.uuid(), "branch-uuid-3", SPECIFIC_ANALYSIS, "analysis-uuid");
    NewCodePeriodDto globalNewCodePeriod = newDto("uuid5", null, null, PREVIOUS_VERSION, null);
    insertNewCodePeriods(newCodePeriod1, newCodePeriod2, newCodePeriod3, anotherProjectNewCodePeriods, globalNewCodePeriod);

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.NewCodePeriod> exportedProps = dumpWriter.getWrittenMessagesOf(DumpElement.NEW_CODE_PERIODS);
    assertThat(exportedProps).hasSize(2);
    assertThat(exportedProps).extracting(ProjectDump.NewCodePeriod::getUuid).containsOnly("uuid1", "uuid2");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("2 new code periods exported");
  }

  @Test
  public void test_exported_fields() {
    NewCodePeriodDto dto = newDto("uuid1", PROJECT.uuid(), "branch-uuid-1", SPECIFIC_ANALYSIS, "analysis-uuid");
    insertNewCodePeriods(dto);

    underTest.execute(new TestComputationStepContext());

    ProjectDump.NewCodePeriod exportedNewCodePeriod = dumpWriter.getWrittenMessagesOf(DumpElement.NEW_CODE_PERIODS).get(0);
    assertThat(exportedNewCodePeriod.getUuid()).isEqualTo(dto.getUuid());
    assertThat(exportedNewCodePeriod.getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(exportedNewCodePeriod.getBranchUuid()).isEqualTo(dto.getBranchUuid());
    assertThat(exportedNewCodePeriod.getType()).isEqualTo(dto.getType().name());
    assertThat(exportedNewCodePeriod.getValue()).isEqualTo(dto.getValue());
  }

  @Test
  public void throws_ISE_if_error() {
    dumpWriter.failIfMoreThan(1, DumpElement.NEW_CODE_PERIODS);
    insertNewCodePeriods(
      newDto("uuid1", PROJECT.uuid(), null, SPECIFIC_ANALYSIS, "analysis-uuid"),
      newDto("uuid2", PROJECT.uuid(), "branch-uuid-1", SPECIFIC_ANALYSIS, "analysis-uuid"));

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("New Code Periods Export failed after processing 1 new code periods successfully");
  }

  @Test
  public void test_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export new code periods");
  }

  private static NewCodePeriodDto newDto(String uuid, @Nullable String projectUuid, @Nullable String branchUuid, NewCodePeriodType type, @Nullable String value) {
    return new NewCodePeriodDto()
      .setUuid(uuid)
      .setProjectUuid(projectUuid)
      .setBranchUuid(branchUuid)
      .setType(type)
      .setValue(value);
  }

  private void insertNewCodePeriods(NewCodePeriodDto... dtos) {
    for (NewCodePeriodDto dto : dtos) {
      dbTester.getDbClient().newCodePeriodDao().insert(dbTester.getSession(), dto);
    }
    dbTester.commit();
  }
}
