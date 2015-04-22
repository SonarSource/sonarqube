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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.Constants.TestStatus;
import org.sonar.batch.protocol.Constants.TestType;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.CoverageDetail;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDao;
import org.sonar.server.source.db.FileSourceDb;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistTestsStepTest extends BaseStepTest {
  private static final String PROJECT_UUID = "PROJECT";
  private static final int TEST_FILE_REF_1 = 3;
  private static final int TEST_FILE_REF_2 = 4;
  private static final int MAIN_FILE_REF_1 = 5;
  private static final int MAIN_FILE_REF_2 = 6;
  private static final String TEST_FILE_UUID_1 = "TEST-FILE-1";
  private static final String TEST_FILE_UUID_2 = "TEST-FILE-2";
  private static final String MAIN_FILE_UUID_1 = "MAIN-FILE-1";
  private static final String MAIN_FILE_UUID_2 = "MAIN-FILE-2";

  PersistTestsStep sut;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static DbTester db = new DbTester();

  File reportDir;
  DbSession session;
  DbClient dbClient;
  System2 system2;

  long now = 123456789L;

  @Before
  public void setup() throws Exception {
    db.truncateTables();
    session = db.myBatis().openSession(false);
    dbClient = new DbClient(db.database(), db.myBatis(), new FileSourceDao(db.myBatis()));
    reportDir = temp.newFolder();

    system2 = mock(System2.class);
    when(system2.now()).thenReturn(now);
    sut = new PersistTestsStep(dbClient, system2);

    initBasicReport();
  }

  @After
  public void tearDown() throws Exception {
    MyBatis.closeQuietly(session);

  }

  @Override
  protected ComputationStep step() throws IOException {
    return sut;
  }

  @Test
  public void no_test_in_database_and_batch_report() throws Exception {
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto()));

    assertThat(dbClient.fileSourceDao().selectTest(TEST_FILE_UUID_1)).isNull();
  }

  @Test
  public void insert_several_tests_in_a_report() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    List<BatchReport.Test> batchTests = Arrays.asList(
      newTest(1), newTest(2)
      );
    writer.writeTests(TEST_FILE_REF_1, batchTests);
    List<CoverageDetail> coverageDetails = Arrays.asList(
      newCoverageDetail(1, MAIN_FILE_REF_1)
      );
    writer.writeCoverageDetails(TEST_FILE_REF_1, coverageDetails);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    FileSourceDto dto = dbClient.fileSourceDao().selectTest(TEST_FILE_UUID_1);
    assertThat(dto.getCreatedAt()).isEqualTo(now);
    assertThat(dto.getUpdatedAt()).isEqualTo(now);
    assertThat(dto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(dto.getFileUuid()).isEqualTo(TEST_FILE_UUID_1);
    assertThat(dto.getTestData()).hasSize(2);

    FileSourceDb.Test test1 = dto.getTestData().get(0);
    assertThat(test1.getName()).isEqualTo("name#1");
    assertThat(test1.getCoveredFileCount()).isEqualTo(1);
    assertThat(test1.getCoveredFile(0).getFileUuid()).isEqualTo(MAIN_FILE_UUID_1);

    FileSourceDb.Test test2 = dto.getTestData().get(1);
    assertThat(test2.getName()).isEqualTo("name#2");
    assertThat(test2.getCoveredFileList()).isEmpty();
  }

  @Test
  public void insert_all_data_of_a_test() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    List<BatchReport.Test> batchTests = Arrays.asList(
      newTest(1)
    );
    writer.writeTests(TEST_FILE_REF_1, batchTests);
    List<CoverageDetail> coverageDetails = Arrays.asList(
      newCoverageDetail(1, MAIN_FILE_REF_1)
    );
    writer.writeCoverageDetails(TEST_FILE_REF_1, coverageDetails);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    FileSourceDto dto = dbClient.fileSourceDao().selectTest(TEST_FILE_UUID_1);
    assertThat(dto.getCreatedAt()).isEqualTo(now);
    assertThat(dto.getUpdatedAt()).isEqualTo(now);
    assertThat(dto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(dto.getFileUuid()).isEqualTo(TEST_FILE_UUID_1);
    assertThat(dto.getTestData()).hasSize(1);

    FileSourceDb.Test test1 = dto.getTestData().get(0);
    assertThat(test1.getName()).isEqualTo("name#1");
    assertThat(test1.getMsg()).isEqualTo("message#1");
    assertThat(test1.getStacktrace()).isEqualTo("stacktrace#1");
    assertThat(test1.getStatus()).isEqualTo(TestStatus.FAILURE);
    assertThat(test1.getType()).isEqualTo(TestType.UT);
    assertThat(test1.getExecutionTimeMs()).isEqualTo(1_000);
    assertThat(test1.getCoveredFileCount()).isEqualTo(1);
    assertThat(test1.getCoveredFile(0).getCoveredLineList()).containsExactly(1, 2, 3);
    assertThat(test1.getCoveredFile(0).getFileUuid()).isEqualTo(MAIN_FILE_UUID_1);
  }

  @Test
  public void insert_tests_without_coverage_details() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    List<BatchReport.Test> batchTests = Arrays.asList(
      newTest(1), newTest(2)
      );
    writer.writeTests(TEST_FILE_REF_1, batchTests);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    FileSourceDto dto = dbClient.fileSourceDao().selectTest(TEST_FILE_UUID_1);
    assertThat(dto.getFileUuid()).isEqualTo(TEST_FILE_UUID_1);
    List<FileSourceDb.Test> tests = dto.getTestData();
    assertThat(tests).hasSize(2);
    assertThat(tests.get(0).getCoveredFileList()).isEmpty();
  }

  @Test
  public void update_one_test() throws Exception {
    // ARRANGE
    dbClient.fileSourceDao().insert(session, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(TEST_FILE_UUID_1)
      .setTestData(Arrays.asList(newDbTest(1)))
      .setCreatedAt(100_000)
      .setUpdatedAt(100_000));
    session.commit();
    assertThat(dbClient.fileSourceDao().selectTest(TEST_FILE_UUID_1)).isNotNull();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    List<BatchReport.Test> batchTests = Arrays.asList(
      newTest(1), newTest(2)
      );
    writer.writeTests(TEST_FILE_REF_1, batchTests);
    List<CoverageDetail> coverageDetails = Arrays.asList(
      newCoverageDetail(1, MAIN_FILE_REF_1)
      );
    writer.writeCoverageDetails(TEST_FILE_REF_1, coverageDetails);

    // ACT
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    // ASSERT
    FileSourceDto dto = dbClient.fileSourceDao().selectTest(TEST_FILE_UUID_1);
    assertThat(dto.getCreatedAt()).isEqualTo(100_000);
    assertThat(dto.getUpdatedAt()).isEqualTo(now);
    assertThat(dto.getTestData()).hasSize(2);

    FileSourceDb.Test test = dto.getTestData().get(0);
    assertThat(test.getName()).isEqualTo("name#1");
    assertThat(test.getMsg()).isEqualTo("message#1");
    assertThat(test.getCoveredFileCount()).isEqualTo(1);
    assertThat(test.getCoveredFile(0).getCoveredLineList()).containsExactly(1, 2, 3);
    assertThat(test.getCoveredFile(0).getFileUuid()).isEqualTo(MAIN_FILE_UUID_1);
  }

  private FileSourceDb.Test newDbTest(int id) {
    return FileSourceDb.Test.newBuilder()
      .setName("name#" + id)
      .setType(TestType.IT)
      .setStatus(TestStatus.ERROR)
      .setStacktrace("old-stacktrace#" + id)
      .setMsg("old-message#" + id)
      .setExecutionTimeMs(123_456_789L)
      .build();
  }

  private BatchReport.Test newTest(int id) {
    return BatchReport.Test.newBuilder()
      .setType(TestType.UT)
      .setStatus(TestStatus.FAILURE)
      .setName("name#" + id)
      .setStacktrace("stacktrace#" + id)
      .setMsg("message#" + id)
      .setExecutionTimeMs(1_000)
      .build();
  }

  private BatchReport.CoverageDetail newCoverageDetail(int id, int covered_file_ref) {
    return CoverageDetail.newBuilder()
      .setTestName("name#" + id)
      .addCoveredFile(CoverageDetail.CoveredFile.newBuilder()
        .addAllCoveredLine(Arrays.asList(1, 2, 3))
        .setFileRef(covered_file_ref)
        .build()
      )
      .build();
  }

  private BatchReportWriter initBasicReport() throws IOException {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("MODULE")
      .addAllChildRef(Arrays.asList(TEST_FILE_REF_1, TEST_FILE_REF_2, MAIN_FILE_REF_1, MAIN_FILE_REF_2))
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(TEST_FILE_REF_1)
      .setIsTest(true)
      .setType(Constants.ComponentType.FILE)
      .setUuid(TEST_FILE_UUID_1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(TEST_FILE_REF_2)
      .setIsTest(true)
      .setType(Constants.ComponentType.FILE)
      .setUuid(TEST_FILE_UUID_2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(MAIN_FILE_REF_1)
      .setType(Constants.ComponentType.FILE)
      .setUuid(MAIN_FILE_UUID_1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(MAIN_FILE_REF_2)
      .setType(Constants.ComponentType.FILE)
      .setUuid(MAIN_FILE_UUID_2)
      .build());

    return writer;
  }
}
