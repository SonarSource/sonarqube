/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.project.Project;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;

public class UpdateMainBranchStepTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public LogTester logTester = new LogTester().setLevel(TRACE);

  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  BatchReportReader batchReportReader = mock(BatchReportReader.class);

  private final UpdateMainBranchStep underTest = new UpdateMainBranchStep(batchReportReader, dbTester.getDbClient(), analysisMetadataHolder);
  private ComputationStep.Context context = mock(ComputationStep.Context.class);

  @Test
  public void update_main_branch_on_first_analysis() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder().setGitDefaultMainBranch("new_name").buildPartial();
    when(batchReportReader.readMetadata()).thenReturn(metadata);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    when(analysisMetadataHolder.getProject())
      .thenReturn(new Project(privateProject.uuid(), privateProject.getKey(), privateProject.name(), privateProject.description(), emptyList()));

    assertMainBranchName(privateProject, "master");

    underTest.execute(context);

    assertMainBranchName(privateProject, "new_name");
    assertThat(logTester.logs()).contains("GIT default main branch detected is [new_name]");
    assertThat(logTester.logs()).contains(String.format("updating project [%s] default main branch to [new_name]", privateProject.getKey()));
  }

  @Test
  public void do_not_update_main_branch_on_second_analysis() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder().setGitDefaultMainBranch("new_name").buildPartial();
    when(batchReportReader.readMetadata()).thenReturn(metadata);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    when(analysisMetadataHolder.getProject())
      .thenReturn(new Project(privateProject.uuid(), privateProject.getKey(), privateProject.name(), privateProject.description(), emptyList()));

    assertMainBranchName(privateProject, "master");

    underTest.execute(context);

    assertMainBranchName(privateProject, "master");
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void do_not_update_main_branch_if_no_git_info_found() {
    String emptyGitMainBranchInfo = "";
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder().setGitDefaultMainBranch(emptyGitMainBranchInfo).buildPartial();
    when(batchReportReader.readMetadata()).thenReturn(metadata);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    when(analysisMetadataHolder.getProject())
      .thenReturn(new Project(privateProject.uuid(), privateProject.getKey(), privateProject.name(), privateProject.description(), emptyList()));

    assertMainBranchName(privateProject, "master");

    underTest.execute(context);

    assertMainBranchName(privateProject, "master");
    assertThat(logTester.logs()).contains("GIT default main branch detected is empty");
  }

  @Test
  public void fail_on_invalid_project_key() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder().setGitDefaultMainBranch("new_name").buildPartial();
    when(batchReportReader.readMetadata()).thenReturn(metadata);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    when(analysisMetadataHolder.getProject())
      .thenReturn(new Project(privateProject.uuid(), "invalid project key", privateProject.name(), privateProject.description(), emptyList()));

    assertThatThrownBy(() -> underTest.execute(context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("root component key [invalid project key] is not a project");
    assertThat(logTester.logs()).contains("GIT default main branch detected is [new_name]");
  }

  @Test
  public void getDescription() {
    assertThat(underTest.getDescription()).isNotEmpty();
  }

  private void assertMainBranchName(ComponentDto privateProject, String expectedBranchName) {
    Collection<BranchDto> branches = dbTester.getDbClient().branchDao().selectByComponent(dbTester.getSession(), privateProject);
    assertThat(branches).isNotEmpty();
    assertThat(branches).hasSize(1);
    assertThat(branches.iterator().next().getKey()).isEqualTo(expectedBranchName);
  }
}
