/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.queue.CeTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoadReportAnalysisMetadataHolderStepTest {

  public static final String PROJECT_KEY = "project_key";
  static long ANALYSIS_DATE = 123456789L;

  static String BRANCH = "origin/master";

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeTask ceTask = createCeTask(PROJECT_KEY);
  private ComputationStep underTest = new LoadReportAnalysisMetadataHolderStep(ceTask, reportReader, analysisMetadataHolder);

  @Test
  public void set_root_component_ref() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setRootComponentRef(1)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void set_analysis_date() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setAnalysisDate(ANALYSIS_DATE)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getAnalysisDate()).isEqualTo(ANALYSIS_DATE);
  }

  @Test
  public void set_branch() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setBranch(BRANCH)
        .build());

    CeTask ceTask = createCeTask(PROJECT_KEY + ":" + BRANCH);
    ComputationStep underTest = new LoadReportAnalysisMetadataHolderStep(ceTask, reportReader, analysisMetadataHolder);

    underTest.execute();

    assertThat(analysisMetadataHolder.getBranch()).isEqualTo(BRANCH);
  }

  @Test
  public void set_null_branch_when_nothing_in_the_report() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getBranch()).isNull();
  }

  @Test
  public void set_cross_project_duplication_to_true() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setCrossProjectDuplicationActivated(true)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(true);
  }

  @Test
  public void set_cross_project_duplication_to_false() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setCrossProjectDuplicationActivated(false)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

  @Test
  public void set_cross_project_duplication_to_false_when_nothing_in_the_report() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

  @Test
  public void execute_fails_with_ISE_when_projectKey_in_report_is_different_from_componentKey_in_CE_task() {
    reportReader.setMetadata(
        BatchReport.Metadata.newBuilder()
            .setProjectKey("some other key")
            .build());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("ProjectKey in report (some other key) is not consistent with projectKey under which the report as been submitted (" + PROJECT_KEY + ")");

    underTest.execute();
  }

  private static BatchReport.Metadata.Builder newBatchReportBuilder() {
    return BatchReport.Metadata.newBuilder()
      .setProjectKey(PROJECT_KEY);
  }

  private CeTask createCeTask(String projectKey) {
    CeTask res = mock(CeTask.class);
    when(res.getComponentKey()).thenReturn(projectKey);
    return res;
  }

}
