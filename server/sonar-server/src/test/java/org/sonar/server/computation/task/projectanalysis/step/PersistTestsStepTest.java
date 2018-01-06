/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.CoverageDetail;
import org.sonar.scanner.protocol.output.ScannerReport.Test.TestStatus;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.FileAttributes;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistTestsStepTest extends BaseStepTest {

  private static final String PROJECT_UUID = "PROJECT";
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final int TEST_FILE_REF_1 = 3;
  private static final int TEST_FILE_REF_2 = 4;
  private static final int MAIN_FILE_REF_1 = 5;
  private static final int MAIN_FILE_REF_2 = 6;
  private static final String TEST_FILE_UUID_1 = "TEST-FILE-1";
  private static final String TEST_FILE_UUID_2 = "TEST-FILE-2";
  private static final String MAIN_FILE_UUID_1 = "MAIN-FILE-1";
  private static final String MAIN_FILE_UUID_2 = "MAIN-FILE-2";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public LogTester log = new LogTester();

  DbClient dbClient = db.getDbClient();
  Component root;

  PersistTestsStep underTest;

  long now = 123456789L;

  @Before
  public void setup() {
    System2 system2 = mock(System2.class);
    when(system2.now()).thenReturn(now);

    underTest = new PersistTestsStep(dbClient, system2, reportReader, treeRootHolder);

    root = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("MODULE_UUID").setKey("MODULE_KEY").addChildren(
        ReportComponent.builder(Component.Type.FILE, 3).setUuid(TEST_FILE_UUID_1).setKey("TEST_FILE1_KEY").setFileAttributes(new FileAttributes(true, null, 1)).build(),
        ReportComponent.builder(Component.Type.FILE, 4).setUuid(TEST_FILE_UUID_2).setKey("TEST_FILE2_KEY").setFileAttributes(new FileAttributes(true, null, 1)).build(),
        ReportComponent.builder(Component.Type.FILE, 5).setUuid(MAIN_FILE_UUID_1).setKey("MAIN_FILE1_KEY").build(),
        ReportComponent.builder(Component.Type.FILE, 6).setUuid(MAIN_FILE_UUID_2).setKey("MAIN_FILE2_KEY").build()).build())
      .build();
    treeRootHolder.setRoot(root);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void no_test_in_database_and_batch_report() {
    underTest.execute();

    assertThat(dbClient.fileSourceDao().selectTest(db.getSession(), TEST_FILE_UUID_1)).isNull();
    assertThat(log.logs()).isEmpty();
  }

  @Test
  public void insert_several_tests_in_a_report() {
    List<ScannerReport.Test> batchTests = Arrays.asList(
      newTest(1), newTest(2));
    reportReader.putTests(TEST_FILE_REF_1, batchTests);
    List<CoverageDetail> coverageDetails = Arrays.asList(
      newCoverageDetail(1, MAIN_FILE_REF_1));
    reportReader.putCoverageDetails(TEST_FILE_REF_1, coverageDetails);

    underTest.execute();

    assertThat(db.countRowsOfTable("file_sources")).isEqualTo(1);

    FileSourceDto dto = dbClient.fileSourceDao().selectTest(db.getSession(), TEST_FILE_UUID_1);
    assertThat(dto.getCreatedAt()).isEqualTo(now);
    assertThat(dto.getUpdatedAt()).isEqualTo(now);
    assertThat(dto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(dto.getFileUuid()).isEqualTo(TEST_FILE_UUID_1);
    assertThat(dto.getTestData()).hasSize(2);

    assertThat(dto.getTestData()).extracting("name", "coveredFileCount").containsOnly(
      tuple("name#1", 1),
      tuple("name#2", 0));

    assertThat(log.logs()).isEmpty();
  }

  @Test
  public void insert_all_data_of_a_test() {
    reportReader.putTests(TEST_FILE_REF_1, Arrays.asList(newTest(1)));
    reportReader.putCoverageDetails(TEST_FILE_REF_1, Arrays.asList(newCoverageDetail(1, MAIN_FILE_REF_1)));

    underTest.execute();

    FileSourceDto dto = dbClient.fileSourceDao().selectTest(db.getSession(), TEST_FILE_UUID_1);
    assertThat(dto.getCreatedAt()).isEqualTo(now);
    assertThat(dto.getUpdatedAt()).isEqualTo(now);
    assertThat(dto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(dto.getFileUuid()).isEqualTo(TEST_FILE_UUID_1);
    assertThat(dto.getTestData()).hasSize(1);

    DbFileSources.Test test1 = dto.getTestData().get(0);
    assertThat(test1.getUuid()).isNotEmpty();
    assertThat(test1.getName()).isEqualTo("name#1");
    assertThat(test1.getMsg()).isEqualTo("message#1");
    assertThat(test1.getStacktrace()).isEqualTo("stacktrace#1");
    assertThat(test1.getStatus()).isEqualTo(DbFileSources.Test.TestStatus.FAILURE);
    assertThat(test1.getExecutionTimeMs()).isEqualTo(1_000);
    assertThat(test1.getCoveredFileCount()).isEqualTo(1);
    assertThat(test1.getCoveredFile(0).getCoveredLineList()).containsOnly(1, 2, 3);
    assertThat(test1.getCoveredFile(0).getFileUuid()).isEqualTo(MAIN_FILE_UUID_1);
  }

  @Test
  public void insert_tests_without_coverage_details() {
    List<ScannerReport.Test> batchTests = Arrays.asList(newTest(1));
    reportReader.putTests(TEST_FILE_REF_1, batchTests);

    underTest.execute();

    FileSourceDto dto = dbClient.fileSourceDao().selectTest(db.getSession(), TEST_FILE_UUID_1);
    assertThat(dto.getFileUuid()).isEqualTo(TEST_FILE_UUID_1);
    List<DbFileSources.Test> tests = dto.getTestData();
    assertThat(tests).hasSize(1);
    assertThat(tests.get(0).getCoveredFileList()).isEmpty();
    assertThat(tests.get(0).getMsg()).isEqualTo("message#1");
  }

  @Test
  public void insert_coverage_details_not_taken_into_account() {
    List<ScannerReport.Test> batchTests = Arrays.asList(newTest(1));
    reportReader.putTests(TEST_FILE_REF_1, batchTests);
    List<CoverageDetail> coverageDetails = Arrays.asList(newCoverageDetail(1, MAIN_FILE_REF_1), newCoverageDetail(2, MAIN_FILE_REF_2));
    reportReader.putCoverageDetails(TEST_FILE_REF_1, coverageDetails);
    reportReader.putCoverageDetails(TEST_FILE_REF_2, coverageDetails);

    underTest.execute();

    assertThat(log.logs(LoggerLevel.WARN)).hasSize(1);
    assertThat(log.logs(LoggerLevel.WARN).get(0)).isEqualTo("Some coverage tests are not taken into account during analysis of project 'PROJECT_KEY'");
    assertThat(log.logs(LoggerLevel.TRACE)).hasSize(2);
    assertThat(log.logs(LoggerLevel.TRACE).get(0)).isEqualTo("The following test coverages for file 'TEST_FILE1_KEY' have not been taken into account: name#2");
    assertThat(log.logs(LoggerLevel.TRACE).get(1)).startsWith("The following test coverages for file 'TEST_FILE2_KEY' have not been taken into account: ");
    assertThat(log.logs(LoggerLevel.TRACE).get(1)).contains("name#1", "name#2");
  }

  @Test
  public void aggregate_coverage_details() {
    reportReader.putTests(TEST_FILE_REF_1, Arrays.asList(newTest(1)));
    reportReader.putCoverageDetails(TEST_FILE_REF_1, Arrays.asList(
      newCoverageDetailWithLines(1, MAIN_FILE_REF_1, 1, 3),
      newCoverageDetailWithLines(1, MAIN_FILE_REF_1, 2, 4)));

    underTest.execute();

    FileSourceDto dto = dbClient.fileSourceDao().selectTest(db.getSession(), TEST_FILE_UUID_1);
    List<Integer> coveredLines = dto.getTestData().get(0).getCoveredFile(0).getCoveredLineList();
    assertThat(coveredLines).containsOnly(1, 2, 3, 4);
  }

  @Test
  public void update_existing_test() {
    // ARRANGE
    dbClient.fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(TEST_FILE_UUID_1)
      .setTestData(Arrays.asList(DbFileSources.Test.newBuilder()
        .setUuid("test-uuid-1")
        .setName("name#1")
        .setStatus(DbFileSources.Test.TestStatus.ERROR)
        .setStacktrace("old-stacktrace#1")
        .setMsg("old-message#1")
        .setExecutionTimeMs(987_654_321L)
        .build()))
      .setCreatedAt(100_000)
      .setUpdatedAt(100_000));
    db.getSession().commit();
    assertThat(dbClient.fileSourceDao().selectTest(db.getSession(), TEST_FILE_UUID_1)).isNotNull();

    ScannerReport.Test newBatchTest = newTest(1);
    reportReader.putTests(TEST_FILE_REF_1, Arrays.asList(newBatchTest));

    CoverageDetail newCoverageDetail = newCoverageDetail(1, MAIN_FILE_REF_1);
    reportReader.putCoverageDetails(TEST_FILE_REF_1, Arrays.asList(newCoverageDetail));

    // ACT
    underTest.execute();

    // ASSERT
    FileSourceDto dto = dbClient.fileSourceDao().selectTest(db.getSession(), TEST_FILE_UUID_1);
    assertThat(dto.getCreatedAt()).isEqualTo(100_000);
    assertThat(dto.getUpdatedAt()).isEqualTo(now);
    assertThat(dto.getTestData()).hasSize(1);

    DbFileSources.Test test = dto.getTestData().get(0);
    assertThat(test.getUuid()).isNotEqualTo("test-uuid-1");
    assertThat(test.getName()).isEqualTo("name#1");
    assertThat(test.getStatus()).isEqualTo(DbFileSources.Test.TestStatus.valueOf(newBatchTest.getStatus().name()));
    assertThat(test.getMsg()).isEqualTo(newBatchTest.getMsg());
    assertThat(test.getStacktrace()).isEqualTo(newBatchTest.getStacktrace());
    assertThat(test.getExecutionTimeMs()).isEqualTo(newBatchTest.getDurationInMs());
    assertThat(test.getCoveredFileCount()).isEqualTo(1);
    assertThat(test.getCoveredFile(0).getCoveredLineList()).containsOnly(1, 2, 3);
    assertThat(test.getCoveredFile(0).getFileUuid()).isEqualTo(MAIN_FILE_UUID_1);
  }

  private ScannerReport.Test newTest(int id) {
    return ScannerReport.Test.newBuilder()
      .setStatus(TestStatus.FAILURE)
      .setName("name#" + id)
      .setStacktrace("stacktrace#" + id)
      .setMsg("message#" + id)
      .setDurationInMs(1_000)
      .build();
  }

  private ScannerReport.CoverageDetail newCoverageDetail(int id, int covered_file_ref) {
    return newCoverageDetailWithLines(id, covered_file_ref, 1, 2, 3);
  }

  private ScannerReport.CoverageDetail newCoverageDetailWithLines(int id, int covered_file_ref, Integer... lines) {
    return CoverageDetail.newBuilder()
      .setTestName("name#" + id)
      .addCoveredFile(CoverageDetail.CoveredFile.newBuilder()
        .addAllCoveredLine(Arrays.asList(lines))
        .setFileRef(covered_file_ref)
        .build())
      .build();
  }
}
