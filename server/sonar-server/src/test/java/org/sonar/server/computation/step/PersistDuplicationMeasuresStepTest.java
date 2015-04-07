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

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Range;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.measure.persistence.MetricDao;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class
  PersistDuplicationMeasuresStepTest extends BaseStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbSession session;

  DbClient dbClient;

  PersistDuplicationMeasuresStep sut;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new MeasureDao(), new MetricDao());

    reportDir = temp.newFolder();

    sut = new PersistDuplicationMeasuresStep(dbClient);
  }

  @Override
  protected ComputationStep step() throws IOException {
    return sut;
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void nothing_to_do_when_no_duplication() throws Exception {
    saveDuplicationMetric();
    initReportWithProjectAndFile();

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto("PROJECT")));

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(0);
  }

  @Test
  public void persist_duplications_on_same_file() throws Exception {
    MetricDto duplicationMetric = saveDuplicationMetric();

    BatchReportWriter writer = initReportWithProjectAndFile();

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileRef(2)
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(2, newArrayList(duplication));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto("PROJECT")));

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", metric_id as \"metricId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("snapshotId")).isEqualTo(11L);
    assertThat(dto.get("metricId")).isEqualTo(duplicationMetric.getId().longValue());
    assertThat(dto.get("textValue")).isEqualTo("<duplications><g><b s=\"1\" l=\"5\" r=\"PROJECT_KEY:file\"/><b s=\"6\" l=\"5\" r=\"PROJECT_KEY:file\"/></g></duplications>");
  }

  @Test
  public void persist_duplications_on_same_file_linked_on_a_module() throws Exception {
    saveDuplicationMetric();

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("PROJECT_KEY")
      .setSnapshotId(10L)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setSnapshotId(11L)
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setSnapshotId(12L)
      .setPath("file")
      .build());

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileRef(3)
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(3, newArrayList(duplication));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto("PROJECT")));

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("snapshotId")).isEqualTo(12L);
    assertThat(dto.get("textValue")).isEqualTo("<duplications><g><b s=\"1\" l=\"5\" r=\"MODULE_KEY:file\"/><b s=\"6\" l=\"5\" r=\"MODULE_KEY:file\"/></g></duplications>");
  }

  @Test
  public void persist_duplications_on_same_file_linked_on_a_folder() throws Exception {
    saveDuplicationMetric();

    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("PROJECT_KEY")
      .setSnapshotId(10L)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setSnapshotId(11L)
      .addChildRef(3)
      .setPath("dir")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setSnapshotId(12L)
      .setPath("file")
      .build());

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileRef(3)
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(3, newArrayList(duplication));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto("PROJECT")));

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("snapshotId")).isEqualTo(12L);
    assertThat(dto.get("textValue")).isEqualTo("<duplications><g><b s=\"1\" l=\"5\" r=\"PROJECT_KEY:file\"/><b s=\"6\" l=\"5\" r=\"PROJECT_KEY:file\"/></g></duplications>");
  }

  @Test
  public void persist_duplications_on_same_file_linked_on_sub_folder() throws Exception {
    saveDuplicationMetric();

    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("PROJECT_KEY")
      .setSnapshotId(10L)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setSnapshotId(11L)
      .addChildRef(3)
      .setPath("dir")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setSnapshotId(12L)
      .addChildRef(10)
      .setPath("dir2")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(10)
      .setType(Constants.ComponentType.FILE)
      .setSnapshotId(20L)
      .setPath("file")
      .build());

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileRef(10)
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(10, newArrayList(duplication));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto("PROJECT")));

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("textValue")).isEqualTo("<duplications><g><b s=\"1\" l=\"5\" r=\"PROJECT_KEY:file\"/><b s=\"6\" l=\"5\" r=\"PROJECT_KEY:file\"/></g></duplications>");
  }

  @Test
  public void persist_duplications_on_same_file_when_a_branch_is_used() throws Exception {
    saveDuplicationMetric();

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setBranch("origin/master")
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("PROJECT_KEY")
      .setSnapshotId(10L)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.FILE)
      .setSnapshotId(11L)
      .setPath("file")
      .build());

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileRef(2)
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(2, newArrayList(duplication));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto("PROJECT")));

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("snapshotId")).isEqualTo(11L);
    assertThat(dto.get("textValue")).isEqualTo(
      "<duplications><g><b s=\"1\" l=\"5\" r=\"PROJECT_KEY:file:origin/master\"/><b s=\"6\" l=\"5\" r=\"PROJECT_KEY:file:origin/master\"/></g></duplications>");
  }

  @Test
  public void persist_duplications_on_different_files() throws Exception {
    saveDuplicationMetric();
    BatchReportWriter writer = initReportWithProjectAndFile();

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setSnapshotId(12L)
      .setPath("file2")
      .build());

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileRef(3)
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(2, newArrayList(duplication));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto("PROJECT")));

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("snapshotId")).isEqualTo(11L);
    assertThat(dto.get("textValue")).isEqualTo("<duplications><g><b s=\"1\" l=\"5\" r=\"PROJECT_KEY:file\"/><b s=\"6\" l=\"5\" r=\"PROJECT_KEY:file2\"/></g></duplications>");
  }

  @Test
  public void persist_duplications_on_different_projects() throws Exception {
    saveDuplicationMetric();
    BatchReportWriter writer = initReportWithProjectAndFile();

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileKey("PROJECT2_KEY:file2")
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(2, newArrayList(duplication));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto("PROJECT")));

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("snapshotId")).isEqualTo(11L);
    assertThat(dto.get("textValue")).isEqualTo("<duplications><g><b s=\"1\" l=\"5\" r=\"PROJECT_KEY:file\"/><b s=\"6\" l=\"5\" r=\"PROJECT2_KEY:file2\"/></g></duplications>");
  }

  private BatchReportWriter initReportWithProjectAndFile() throws IOException {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("PROJECT_KEY")
      .setSnapshotId(10L)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.FILE)
      .setSnapshotId(11L)
      .setPath("file")
      .build());

    return writer;
  }

  private MetricDto saveDuplicationMetric() {
    MetricDto duplicationMetric = new MetricDto().setKey(CoreMetrics.DUPLICATIONS_DATA_KEY)
      .setOptimizedBestValue(false)
      .setDeleteHistoricalData(false)
      .setHidden(false);
    dbClient.metricDao().insert(session, duplicationMetric);
    session.commit();
    return duplicationMetric;
  }

}
