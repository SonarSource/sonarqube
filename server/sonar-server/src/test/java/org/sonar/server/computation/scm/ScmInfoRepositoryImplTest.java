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
package org.sonar.server.computation.scm;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.snapshot.Snapshot;
import org.sonar.server.computation.source.SourceHashRepository;
import org.sonar.server.computation.source.SourceHashRepositoryImpl;
import org.sonar.server.computation.source.SourceLinesRepositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;
import static org.sonar.server.computation.component.ReportComponent.builder;

@RunWith(DataProviderRunner.class)
public class ScmInfoRepositoryImplTest {

  static final int FILE_REF = 1;
  static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").build();
  static final long DATE_1 = 123456789L;
  static final long DATE_2 = 1234567810L;

  static Snapshot BASE_PROJECT_SNAPSHOT = new Snapshot.Builder()
    .setId(1)
    .setUuid("uuid_1")
    .setCreatedAt(123456789L)
    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  ScmInfoRepositoryImpl underTest = new ScmInfoRepositoryImpl(reportReader, analysisMetadataHolder, dbClient,
    new SourceHashRepositoryImpl(new SourceLinesRepositoryImpl(reportReader)));

  @Test
  public void read_from_report() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    addChangesetInReport("john", DATE_1, "rev-1");

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from report for file 'FILE_KEY'");
  }

  @Test
  public void getScmInfo_returns_absent_if_CopyFromPrevious_is_false_and_there_is_no_changeset_in_report() {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    // put data in DB, which should not be used
    addFileSourceInDb("henry", DATE_1, "rev-1", computeSourceHash(1));
    addFileSourceInReport(1);

    assertThat(underTest.getScmInfo(FILE)).isAbsent();
  }

  @Test
  public void getScmInfo_returns_ScmInfo_from_DB_CopyFromPrevious_is_true_if_hashes_are_the_same() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    addFileSourceInDb("henry", DATE_1, "rev-1", computeSourceHash(1));
    addFileSourceInReport(1);
    addCopyFromPreviousChangesetInReport();

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from db for file 'FILE_KEY'");
  }

  @Test
  public void getScmInfo_returns_absent_when_CopyFromPrevious_is_true_but_hashes_are_not_the_same() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    addFileSourceInDb("henry", DATE_1, "rev-1", computeSourceHash(1) + "_different");
    addFileSourceInReport(1);
    addCopyFromPreviousChangesetInReport();

    assertThat(underTest.getScmInfo(FILE)).isAbsent();

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from db for file 'FILE_KEY'");
  }

  @Test
  public void read_from_report_even_if_data_in_db_exists() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    addFileSourceInDb("henry", DATE_1, "rev-1", computeSourceHash(1));
    addChangesetInReport("john", DATE_2, "rev-2");

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();

    Changeset changeset = scmInfo.getChangesetForLine(1);
    assertThat(changeset.getAuthor()).isEqualTo("john");
    assertThat(changeset.getDate()).isEqualTo(DATE_2);
    assertThat(changeset.getRevision()).isEqualTo("rev-2");
  }

  @Test
  public void read_from_db_even_if_data_in_report_exists_when_CopyFromPrevious_is_true() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    addFileSourceInDb("henry", DATE_1, "rev-1", computeSourceHash(1));
    addFileSourceInReport(1);
    addChangesetInReport("john", DATE_2, "rev-2", true);

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();

    Changeset changeset = scmInfo.getChangesetForLine(1);
    assertThat(changeset.getAuthor()).isEqualTo("henry");
    assertThat(changeset.getDate()).isEqualTo(DATE_1);
    assertThat(changeset.getRevision()).isEqualTo("rev-1");
  }

  @Test
  public void return_nothing_when_no_data_in_report_nor_db() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    assertThat(underTest.getScmInfo(FILE)).isAbsent();
  }

  @Test
  public void return_nothing_when_nothing_in_report_and_db_has_no_scm() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    addFileSourceInDb(null, null, null, "don't care");
    addFileSourceInReport(1);

    assertThat(underTest.getScmInfo(FILE)).isAbsent();
  }

  @Test
  public void fail_with_NPE_when_component_is_null() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Component cannot be bull");

    underTest.getScmInfo(null);
  }

  @DataProvider
  public static Object[][] allTypeComponentButFile() {
    Object[][] res = new Object[Component.Type.values().length - 1][1];
    int i = 0;
    for (Component.Type type : EnumSet.complementOf(EnumSet.of(Component.Type.FILE))) {
      if (type.isReportType()) {
        res[i][0] = ReportComponent.builder(type, i).build();
      } else {
        res[i][0] = ViewsComponent.builder(type, i).build();
      }
      i++;
    }
    return res;
  }

  @Test
  @UseDataProvider("allTypeComponentButFile")
  public void do_not_query_db_nor_report_if_component_type_is_not_FILE(Component component) {
    BatchReportReader batchReportReader = mock(BatchReportReader.class);
    AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
    DbClient dbClient = mock(DbClient.class);
    SourceHashRepository sourceHashRepository = mock(SourceHashRepository.class);
    ScmInfoRepositoryImpl underTest = new ScmInfoRepositoryImpl(batchReportReader, analysisMetadataHolder, dbClient, sourceHashRepository);

    assertThat(underTest.getScmInfo(component)).isAbsent();

    verifyNoMoreInteractions(batchReportReader, analysisMetadataHolder, dbClient, sourceHashRepository);
  }

  @Test
  public void load_scm_info_from_cache_when_already_read() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    addChangesetInReport("john", DATE_1, "rev-1");
    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).hasSize(1);
    logTester.clear();

    underTest.getScmInfo(FILE);
    assertThat(logTester.logs(TRACE)).isEmpty();
  }

  @Test
  public void not_read_in_db_on_first_analysis_when_CopyFromPrevious_is_true() throws Exception {
    analysisMetadataHolder.setBaseProjectSnapshot(null);
    addFileSourceInDb("henry", DATE_1, "rev-1", "don't care");
    addFileSourceInReport(1);
    addCopyFromPreviousChangesetInReport();

    assertThat(underTest.getScmInfo(FILE)).isAbsent();
    assertThat(logTester.logs(TRACE)).isEmpty();
  }

  private void addFileSourceInDb(@Nullable String author, @Nullable Long date, @Nullable String revision, String srcHash) {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    DbFileSources.Line.Builder builder = fileDataBuilder.addLinesBuilder()
      .setLine(1);
    if (author != null) {
      builder.setScmAuthor(author);
    }
    if (date != null) {
      builder.setScmDate(date);
    }
    if (revision != null) {
      builder.setScmRevision(revision);
    }
    dbTester.getDbClient().fileSourceDao().insert(new FileSourceDto()
      .setFileUuid(FILE.getUuid())
      .setProjectUuid("PROJECT_UUID")
      .setSourceData(fileDataBuilder.build())
      .setSrcHash(srcHash));
  }

  private void addCopyFromPreviousChangesetInReport() {
    reportReader.putChangesets(ScannerReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .setCopyFromPrevious(true)
      .build());
  }

  private void addChangesetInReport(String author, Long date, String revision) {
    addChangesetInReport(author, date, revision, false);
  }

  private void addChangesetInReport(String author, Long date, String revision, boolean copyFromPrevious) {
    reportReader.putChangesets(ScannerReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .setCopyFromPrevious(copyFromPrevious)
      .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
        .setAuthor(author)
        .setDate(date)
        .setRevision(revision)
        .build())
      .addChangesetIndexByLine(0)
      .build());
  }

  private void addFileSourceInReport(int lineCount) {
    reportReader.putFileSourceLines(FILE_REF, generateLines(lineCount));
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(FILE_REF)
      .setLines(lineCount)
      .build());
  }

  private static List<String> generateLines(int lineCount) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (int i = 0; i < lineCount; i++) {
      builder.add("line " + i);
    }
    return builder.build();
  }

  private static String computeSourceHash(int lineCount) {
    SourceHashComputer sourceHashComputer = new SourceHashComputer();
    Iterator<String> lines = generateLines(lineCount).iterator();
    while (lines.hasNext()) {
      sourceHashComputer.addLine(lines.next(), lines.hasNext());
    }
    return sourceHashComputer.getHash();
  }
}
