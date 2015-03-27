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
import java.util.Arrays;

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

    assertThat(step.getFileSourceData()).isNull();
  }

  @Test
  public void compute_coverage_from_one_line() throws Exception {
    BatchReportWriter writer = initReport();

    writer.writeFileCoverage(BatchReport.Coverage.newBuilder()
      .setFileRef(FILE_REF)
      .addAllConditionsByLine(Arrays.asList(10))
      .addAllUtHitsByLine(Arrays.asList(true))
      .addAllUtCoveredConditionsByLine(Arrays.asList(2))
      .addAllItHitsByLine(Arrays.asList(false))
      .addAllItCoveredConditionsByLine(Arrays.asList(3))
      .addAllOverallCoveredConditionsByLine(Arrays.asList(4))
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    FileSourceDb.Data data = step.getFileSourceData();
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

    writer.writeFileCoverage(BatchReport.Coverage.newBuilder()
      .setFileRef(FILE_REF)
      .addAllConditionsByLine(Arrays.asList(10, 0, 4))
      .addAllUtHitsByLine(Arrays.asList(true, false, false))
      .addAllItHitsByLine(Arrays.asList(false, true, true))
      .addAllUtCoveredConditionsByLine(Arrays.asList(1, 0, 4))
      .addAllItCoveredConditionsByLine(Arrays.asList(1, 0, 5))
      .addAllOverallCoveredConditionsByLine(Arrays.asList(1, 0, 5))
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    FileSourceDb.Data data = step.getFileSourceData();
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
