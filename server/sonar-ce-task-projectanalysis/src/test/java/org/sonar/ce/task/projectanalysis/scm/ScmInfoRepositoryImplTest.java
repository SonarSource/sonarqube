/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.scm;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.Component.Status;
import org.sonar.ce.task.projectanalysis.component.Component.Type;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.FileStatuses;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.source.SourceLinesDiff;
import org.sonar.db.protobuf.DbFileSources.Line;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Changesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.TRACE;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

@RunWith(DataProviderRunner.class)
public class ScmInfoRepositoryImplTest {
  static final int FILE_REF = 1;
  static final FileAttributes attributes = new FileAttributes(false, "java", 3);
  static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").setFileAttributes(attributes).build();
  static final Component FILE_SAME = builder(Component.Type.FILE, FILE_REF).setStatus(Status.SAME).setKey("FILE_KEY").setUuid("FILE_UUID").setFileAttributes(attributes).build();
  static final long DATE_1 = 123456789L;
  static final long DATE_2 = 1234567810L;

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadata = new AnalysisMetadataHolderRule();

  private final FileStatuses fileStatuses = mock(FileStatuses.class);
  private final SourceLinesDiff diff = mock(SourceLinesDiff.class);
  private final ScmInfoDbLoader dbLoader = mock(ScmInfoDbLoader.class);
  private final Date analysisDate = new Date();
  private final ScmInfoRepositoryImpl underTest = new ScmInfoRepositoryImpl(reportReader, analysisMetadata, dbLoader, diff, fileStatuses);

  @Before
  public void setUp() {
    logTester.setLevel(TRACE);
    analysisMetadata.setAnalysisDate(analysisDate);
  }

  @Test
  public void return_empty_if_component_is_not_file() {
    Component c = mock(Component.class);
    when(c.getType()).thenReturn(Type.DIRECTORY);
    assertThat(underTest.getScmInfo(c)).isEmpty();
  }

  @Test
  public void load_scm_info_from_cache_when_already_loaded() {
    addChangesetInReport("john", DATE_1, "rev-1");
    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).hasSize(1);
    logTester.clear();

    underTest.getScmInfo(FILE);
    assertThat(logTester.logs(TRACE)).isEmpty();

    verifyNoInteractions(dbLoader);
    verifyNoInteractions(fileStatuses);
    verifyNoInteractions(diff);
  }

  @Test
  public void read_from_report() {
    addChangesetInReport("john", DATE_1, "rev-1");

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    Changeset changeset = scmInfo.getChangesetForLine(1);
    assertThat(changeset.getAuthor()).isEqualTo("john");
    assertThat(changeset.getDate()).isEqualTo(DATE_1);
    assertThat(changeset.getRevision()).isEqualTo("rev-1");

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from report for file 'FILE_KEY'");

    verifyNoInteractions(dbLoader);
    verifyNoInteractions(fileStatuses);
    verifyNoInteractions(diff);
  }

  @Test
  public void read_from_DB_if_no_report_and_file_unchanged() {
    createDbScmInfoWithOneLine();
    when(fileStatuses.isUnchanged(FILE_SAME)).thenReturn(true);

    // should clear revision and author
    ScmInfo scmInfo = underTest.getScmInfo(FILE_SAME).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);
    assertChangeset(scmInfo.getChangesetForLine(1), null, null, 10L);

    verify(fileStatuses).isUnchanged(FILE_SAME);
    verify(dbLoader).getScmInfo(FILE_SAME);

    verifyNoMoreInteractions(dbLoader);
    verifyNoMoreInteractions(fileStatuses);
    verifyNoInteractions(diff);
  }

  @Test
  public void read_from_DB_with_missing_lines_if_no_report_and_file_unchanged() {
    createDbScmInfoWithMissingLine();
    when(fileStatuses.isUnchanged(FILE_SAME)).thenReturn(true);

    // should clear revision and author
    ScmInfo scmInfo = underTest.getScmInfo(FILE_SAME).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(2);
    assertChangeset(scmInfo.getChangesetForLine(1), null, null, 10L);
    assertThat(scmInfo.hasChangesetForLine(2)).isFalse();

    verify(fileStatuses).isUnchanged(FILE_SAME);
    verify(dbLoader).getScmInfo(FILE_SAME);

    verifyNoMoreInteractions(dbLoader);
    verifyNoMoreInteractions(fileStatuses);
    verifyNoInteractions(diff);
  }

  @Test
  public void read_from_DB_if_no_report_and_file_unchanged_and_copyFromPrevious_is_true() {
    createDbScmInfoWithOneLine();
    when(fileStatuses.isUnchanged(FILE_SAME)).thenReturn(true);
    addFileSourceInReport(1);
    addCopyFromPrevious();

    ScmInfo scmInfo = underTest.getScmInfo(FILE_SAME).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);
    assertChangeset(scmInfo.getChangesetForLine(1), "rev1", "author1", 10L);

    verify(fileStatuses).isUnchanged(FILE_SAME);
    verify(dbLoader).getScmInfo(FILE_SAME);

    verifyNoMoreInteractions(dbLoader);
    verifyNoMoreInteractions(fileStatuses);
    verifyNoInteractions(diff);
  }

  @Test
  public void generate_scm_info_when_nothing_in_report_nor_db() {
    when(dbLoader.getScmInfo(FILE)).thenReturn(Optional.empty());
    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(3);

    for (int i = 1; i <= 3; i++) {
      assertChangeset(scmInfo.getChangesetForLine(i), null, null, analysisDate.getTime());
    }

    verify(dbLoader).getScmInfo(FILE);
    verifyNoMoreInteractions(dbLoader);
    verifyNoInteractions(fileStatuses);
    verifyNoInteractions(diff);
  }

  @Test
  public void generate_scm_info_when_nothing_in_db_and_report_is_has_no_changesets() {
    when(dbLoader.getScmInfo(FILE)).thenReturn(Optional.empty());
    addFileSourceInReport(3);
    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(3);

    for (int i = 1; i <= 3; i++) {
      assertChangeset(scmInfo.getChangesetForLine(i), null, null, analysisDate.getTime());
    }

    verify(dbLoader).getScmInfo(FILE);
    verifyNoMoreInteractions(dbLoader);
    verifyNoInteractions(fileStatuses);
    verifyNoInteractions(diff);
  }

  @Test
  public void generate_scm_info_for_new_and_changed_lines_when_report_is_empty() {
    createDbScmInfoWithOneLine();
    when(diff.computeMatchingLines(FILE)).thenReturn(new int[] {1, 0, 0});
    addFileSourceInReport(3);
    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(3);

    assertChangeset(scmInfo.getChangesetForLine(1), null, null, 10L);
    assertChangeset(scmInfo.getChangesetForLine(2), null, null, analysisDate.getTime());
    assertChangeset(scmInfo.getChangesetForLine(3), null, null, analysisDate.getTime());

    verify(dbLoader).getScmInfo(FILE);
    verify(diff).computeMatchingLines(FILE);
    verifyNoMoreInteractions(dbLoader);
    verifyNoMoreInteractions(diff);
  }

  @Test
  public void generate_scm_info_for_db_changesets_without_date_when_report_is_empty() {
    // changeset for line 1 will have no date, so won't be loaded
    createDbScmInfoWithOneLineWithoutDate();
    when(diff.computeMatchingLines(FILE)).thenReturn(new int[] {1, 0, 0});
    addFileSourceInReport(3);
    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(3);

    // a date will be generated for line 1
    assertChangeset(scmInfo.getChangesetForLine(1), null, null, analysisDate.getTime());
    assertChangeset(scmInfo.getChangesetForLine(2), null, null, analysisDate.getTime());
    assertChangeset(scmInfo.getChangesetForLine(3), null, null, analysisDate.getTime());
  }

  @Test
  public void fail_with_NPE_when_component_is_null() {
    assertThatThrownBy(() -> underTest.getScmInfo(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Component cannot be null");
  }

  @Test
  @UseDataProvider("allTypeComponentButFile")
  public void do_not_query_db_nor_report_if_component_type_is_not_FILE(Component component) {
    BatchReportReader batchReportReader = mock(BatchReportReader.class);
    ScmInfoRepositoryImpl underTest = new ScmInfoRepositoryImpl(batchReportReader, analysisMetadata, dbLoader, diff, fileStatuses);

    assertThat(underTest.getScmInfo(component)).isEmpty();

    verifyNoInteractions(batchReportReader, dbLoader);
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

  private void assertChangeset(Changeset changeset, String revision, String author, long date) {
    assertThat(changeset.getAuthor()).isEqualTo(author);
    assertThat(changeset.getRevision()).isEqualTo(revision);
    assertThat(changeset.getDate()).isEqualTo(date);
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

  private void addCopyFromPrevious() {
    reportReader.putChangesets(Changesets.newBuilder().setComponentRef(FILE_REF).setCopyFromPrevious(true).build());
  }

  private DbScmInfo createDbScmInfoWithOneLine() {
    Line line1 = Line.newBuilder().setLine(1)
      .setScmRevision("rev1")
      .setScmAuthor("author1")
      .setScmDate(10L)
      .build();
    DbScmInfo scmInfo = DbScmInfo.create(Collections.singletonList(line1), 1, "hash1").get();
    when(dbLoader.getScmInfo(FILE)).thenReturn(Optional.of(scmInfo));
    return scmInfo;
  }

  private DbScmInfo createDbScmInfoWithOneLineWithoutDate() {
    Line line1 = Line.newBuilder().setLine(1)
      .setScmRevision("rev1")
      .setScmAuthor("author1")
      .build();
    Line line2 = Line.newBuilder().setLine(2)
      .setScmRevision("rev1")
      .setScmAuthor("author1")
      .setScmDate(10L)
      .build();
    DbScmInfo scmInfo = DbScmInfo.create(List.of(line1, line2), 2, "hash1").get();
    when(dbLoader.getScmInfo(FILE)).thenReturn(Optional.of(scmInfo));
    return scmInfo;
  }

  private DbScmInfo createDbScmInfoWithMissingLine() {
    Line line1 = Line.newBuilder().setLine(1)
      .setScmRevision("rev1")
      .setScmAuthor("author1")
      .setScmDate(10L)
      .build();
    DbScmInfo scmInfo = DbScmInfo.create(Collections.singletonList(line1), 2, "hash1").get();
    when(dbLoader.getScmInfo(FILE)).thenReturn(Optional.of(scmInfo));
    return scmInfo;
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

}
