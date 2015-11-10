/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.BatchReportReaderRule;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadReportAnalysisMetadataHolderStepTest {

  static long ANALYSIS_DATE = 123456789L;

  static String BRANCH = "origin/master";

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  ComputationStep underTest = new LoadReportAnalysisMetadataHolderStep(reportReader, analysisMetadataHolder);

  @Test
  public void set_root_component_ref() throws Exception {
    reportReader.setMetadata(
      BatchReport.Metadata.newBuilder()
        .setRootComponentRef(1)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void set_analysis_date() throws Exception {
    reportReader.setMetadata(
      BatchReport.Metadata.newBuilder()
        .setAnalysisDate(ANALYSIS_DATE)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getAnalysisDate()).isEqualTo(ANALYSIS_DATE);
  }

  @Test
  public void set_branch() throws Exception {
    reportReader.setMetadata(
      BatchReport.Metadata.newBuilder()
        .setBranch(BRANCH)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getBranch()).isEqualTo(BRANCH);
  }

  @Test
  public void set_null_branch_when_nothing_in_the_report() throws Exception {
    reportReader.setMetadata(
      BatchReport.Metadata.newBuilder()
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getBranch()).isNull();
  }

  @Test
  public void set_cross_project_duplication_to_true() throws Exception {
    reportReader.setMetadata(
      BatchReport.Metadata.newBuilder()
        .setCrossProjectDuplicationActivated(true)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(true);
  }

  @Test
  public void set_cross_project_duplication_to_false() throws Exception {
    reportReader.setMetadata(
      BatchReport.Metadata.newBuilder()
        .setCrossProjectDuplicationActivated(false)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

  @Test
  public void set_cross_project_duplication_to_false_when_nothing_in_the_report() throws Exception {
    reportReader.setMetadata(
      BatchReport.Metadata.newBuilder()
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

}
