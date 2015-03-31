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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class PersistCoverageStepTest extends BaseStepTest {

  private static final Integer FILE_REF = 3;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  PersistCoverageStep step;

  @Before
  public void setup() throws Exception {
    reportDir = temp.newFolder();
    step = new PersistCoverageStep();
  }

  @Override
  protected ComputationStep step() throws IOException {
    return step;
  }

  @Test
  public void compute_nothing() throws Exception {
    initReport();

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    assertThat(step.getFileSourceData().getLinesList()).isEmpty();
  }

  @Test
  public void compute_coverage_from_one_line() throws Exception {
    BatchReportWriter writer = initReport();

    writer.writeFileCoverage(FILE_REF, newArrayList(BatchReport.Coverage.newBuilder()
      .setLine(1)
      .setConditions(10)
      .setUtHits(true)
      .setUtCoveredConditions(2)
      .setItHits(false)
      .setItCoveredConditions(3)
      .setOverallCoveredConditions(4)
      .build()));

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    FileSourceDb.Data data = step.getFileSourceData();
    assertThat(data.getLinesList()).hasSize(1);

    assertThat(data.getLines(0).getUtLineHits()).isEqualTo(1);
    assertThat(data.getLines(0).getUtConditions()).isEqualTo(10);
    assertThat(data.getLines(0).getUtCoveredConditions()).isEqualTo(2);
    assertThat(data.getLines(0).hasItLineHits()).isFalse();
    assertThat(data.getLines(0).getItConditions()).isEqualTo(10);
    assertThat(data.getLines(0).getItCoveredConditions()).isEqualTo(3);
    assertThat(data.getLines(0).getOverallLineHits()).isEqualTo(1);
    assertThat(data.getLines(0).getOverallConditions()).isEqualTo(10);
    assertThat(data.getLines(0).getOverallCoveredConditions()).isEqualTo(4);
  }

  @Test
  public void compute_coverage_from_lines() throws Exception {
    BatchReportWriter writer = initReport();

    writer.writeFileCoverage(FILE_REF, newArrayList(
      BatchReport.Coverage.newBuilder()
        .setLine(1)
        .setConditions(10)
        .setUtHits(true)
        .setUtCoveredConditions(1)
        .setItHits(false)
        .setItCoveredConditions(1)
        .setOverallCoveredConditions(1)
        .build(),
      BatchReport.Coverage.newBuilder()
        .setLine(2)
        .setConditions(0)
        .setUtHits(false)
        .setUtCoveredConditions(0)
        .setItHits(true)
        .setItCoveredConditions(0)
        .setOverallCoveredConditions(0)
        .build(),
      BatchReport.Coverage.newBuilder()
        .setLine(3)
        .setConditions(4)
        .setUtHits(false)
        .setUtCoveredConditions(4)
        .setItHits(true)
        .setItCoveredConditions(5)
        .setOverallCoveredConditions(5)
        .build()));

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    FileSourceDb.Data data = step.getFileSourceData();
    assertThat(data.getLinesList()).hasSize(3);

    assertThat(data.getLines(0).getUtLineHits()).isEqualTo(1);
    assertThat(data.getLines(0).hasItLineHits()).isFalse();

    assertThat(data.getLines(1).hasUtLineHits()).isFalse();
    assertThat(data.getLines(1).getItLineHits()).isEqualTo(1);

    assertThat(data.getLines(2).hasUtLineHits()).isFalse();
    assertThat(data.getLines(2).getItLineHits()).isEqualTo(1);
  }

  private BatchReportWriter initReport() {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("PROJECT_A")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("BCDE")
      .addChildRef(FILE_REF)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(FILE_REF)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    return writer;
  }

}
