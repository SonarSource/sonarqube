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

import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class PersistDuplicationsStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final int FILE_1_REF = 2;
  private static final int FILE_2_REF = 3;
  private static final int ROOT_REF = 1;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  DbIdsRepositoryImpl dbIdsRepository = new DbIdsRepositoryImpl();

  DbSession session = dbTester.getSession();

  DbClient dbClient = dbTester.getDbClient();

  PersistDuplicationsStep underTest;

  @Before
  public void setup() {
    dbTester.truncateTables();
    underTest = new PersistDuplicationsStep(dbClient, dbIdsRepository, treeRootHolder, reportReader);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void nothing_to_do_when_no_duplication() {
    saveDuplicationMetric();
    initReportWithProjectAndFile();

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(0);
  }

  @Test
  public void persist_duplications_on_same_file() {
    MetricDto duplicationMetric = saveDuplicationMetric();
    initReportWithProjectAndFile();

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.TextRange.newBuilder()
            .setStartLine(1)
            .setEndLine(5)
            .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
            .setRange(BatchReport.TextRange.newBuilder()
                .setStartLine(6)
                .setEndLine(10)
                .build())
            .build())
        .build();
    reportReader.putDuplications(FILE_1_REF, newArrayList(duplication));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", metric_id as \"metricId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("snapshotId")).isEqualTo(11L);
    assertThat(dto.get("metricId")).isEqualTo(duplicationMetric.getId().longValue());
    assertThat(dto.get("textValue")).isEqualTo("<duplications><g><b s=\"1\" l=\"5\" r=\"PROJECT_KEY:file\"/><b s=\"6\" l=\"5\" r=\"PROJECT_KEY:file\"/></g></duplications>");
  }

  @Test
  public void persist_duplications_on_different_files() {
    saveDuplicationMetric();
    initReportWithProjectAndFile();

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.TextRange.newBuilder()
            .setStartLine(1)
            .setEndLine(5)
            .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
            .setOtherFileRef(FILE_2_REF).setRange(BatchReport.TextRange.newBuilder()
                .setStartLine(6)
                .setEndLine(10)
                .build())
            .build())
        .build();
    reportReader.putDuplications(FILE_1_REF, newArrayList(duplication));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    Map<String, Object> dto = dbTester.selectFirst("select snapshot_id as \"snapshotId\", text_value as \"textValue\" from project_measures");
    assertThat(dto.get("snapshotId")).isEqualTo(11L);
    assertThat(dto.get("textValue")).isEqualTo("<duplications><g><b s=\"1\" l=\"5\" r=\"PROJECT_KEY:file\"/><b s=\"6\" l=\"5\" r=\"PROJECT_KEY:file2\"/></g></duplications>");
  }

  private void initReportWithProjectAndFile() {
    Component file1 = ReportComponent.builder(Component.Type.FILE, FILE_1_REF).setUuid("BCDE").setKey("PROJECT_KEY:file").build();
    Component file2 = ReportComponent.builder(Component.Type.FILE, FILE_2_REF).setUuid("CDEF").setKey("PROJECT_KEY:file2").build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, ROOT_REF).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(file1, file2).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, 1);
    dbIdsRepository.setSnapshotId(project, 10);
    dbIdsRepository.setComponentId(file1, 2);
    dbIdsRepository.setSnapshotId(file1, 11);
    dbIdsRepository.setComponentId(file2, 2);
    dbIdsRepository.setSnapshotId(file2, 12);
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
