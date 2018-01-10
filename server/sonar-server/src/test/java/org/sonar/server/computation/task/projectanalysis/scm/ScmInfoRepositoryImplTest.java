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
package org.sonar.server.computation.task.projectanalysis.scm;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.EnumSet;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.log.LogTester;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

@RunWith(DataProviderRunner.class)
public class ScmInfoRepositoryImplTest {
  static final int FILE_REF = 1;
  static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").build();
  static final long DATE_1 = 123456789L;
  static final long DATE_2 = 1234567810L;

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private ScmInfoDbLoader dbLoader = mock(ScmInfoDbLoader.class);

  private ScmInfoRepositoryImpl underTest = new ScmInfoRepositoryImpl(reportReader, dbLoader);

  @Test
  public void read_from_report() {
    addChangesetInReport("john", DATE_1, "rev-1");

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from report for file 'FILE_KEY'");
  }

  @Test
  public void getScmInfo_returns_absent_if_CopyFromPrevious_is_false_and_there_is_no_changeset_in_report() {
    addFileSourceInReport(1);

    assertThat(underTest.getScmInfo(FILE)).isAbsent();
    verifyZeroInteractions(dbLoader);
  }

  @Test
  public void read_from_report_even_if_data_in_db_exists() {
    addChangesetInReport("john", DATE_2, "rev-2");

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();

    Changeset changeset = scmInfo.getChangesetForLine(1);
    assertThat(changeset.getAuthor()).isEqualTo("john");
    assertThat(changeset.getDate()).isEqualTo(DATE_2);
    assertThat(changeset.getRevision()).isEqualTo("rev-2");
    verifyZeroInteractions(dbLoader);
  }

  @Test
  public void read_from_db_even_if_data_in_report_exists_when_CopyFromPrevious_is_true() {
    ScmInfo info = mock(ScmInfo.class);
    when(dbLoader.getScmInfoFromDb(FILE)).thenReturn(info);

    addFileSourceInReport(1);
    addChangesetInReport("john", DATE_2, "rev-2", true);

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo).isEqualTo(info);
  }

  @Test
  public void return_nothing_when_no_data_in_report_nor_db() {
    assertThat(underTest.getScmInfo(FILE)).isAbsent();
  }

  @Test
  public void return_nothing_when_nothing_in_report_and_db_has_no_scm() {
    addFileSourceInReport(1);
    assertThat(underTest.getScmInfo(FILE)).isAbsent();
  }

  @Test
  public void fail_with_NPE_when_component_is_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Component cannot be null");

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
    ScmInfoRepositoryImpl underTest = new ScmInfoRepositoryImpl(batchReportReader, dbLoader);

    assertThat(underTest.getScmInfo(component)).isAbsent();

    verifyZeroInteractions(batchReportReader, dbLoader);
  }

  @Test
  public void load_scm_info_from_cache_when_already_read() {
    addChangesetInReport("john", DATE_1, "rev-1");
    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).hasSize(1);
    logTester.clear();

    underTest.getScmInfo(FILE);
    assertThat(logTester.logs(TRACE)).isEmpty();
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
}
