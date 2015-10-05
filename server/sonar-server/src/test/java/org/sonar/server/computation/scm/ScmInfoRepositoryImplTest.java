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

package org.sonar.server.computation.scm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.source.SourceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class ScmInfoRepositoryImplTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final int FILE_REF = 1;
  static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").build();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  ScmInfoRepositoryImpl underTest = new ScmInfoRepositoryImpl(reportReader, dbClient, new SourceService(dbClient, null));

  @Test
  public void read_from_report() throws Exception {
    addChangesetInReport("john", 123456789L, "rev-1");

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from report for file 'ReportComponent{ref=1, key='FILE_KEY', type=FILE}'");
  }

  @Test
  public void read_from_db() throws Exception {
    addChangesetInDb("henry", 123456789L, "rev-1");

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from db for file 'ReportComponent{ref=1, key='FILE_KEY', type=FILE}'");
  }

  @Test
  public void read_from_report_even_if_data_in_db_exists() throws Exception {
    addChangesetInDb("henry", 123456789L, "rev-1");

    addChangesetInReport("john", 1234567810L, "rev-2");

    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();

    Changeset changeset = scmInfo.getChangesetForLine(1);
    assertThat(changeset.getAuthor()).isEqualTo("john");
    assertThat(changeset.getDate()).isEqualTo(1234567810L);
    assertThat(changeset.getRevision()).isEqualTo("rev-2");
  }

  @Test
  public void return_nothing_when_no_data_in_report_and_db() throws Exception {
    assertThat(underTest.getScmInfo(FILE)).isAbsent();
  }

  @Test
  public void return_nothing_when_nothing_in_erpot_and_db_has_no_scm() throws Exception {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    fileDataBuilder.addLinesBuilder()
      .setLine(1);
    dbTester.getDbClient().fileSourceDao().insert(new FileSourceDto()
      .setFileUuid(FILE.getUuid())
      .setProjectUuid("PROJECT_UUID")
      .setSourceData(fileDataBuilder.build()));

    assertThat(underTest.getScmInfo(FILE)).isAbsent();
  }

  @Test
  public void fail_with_NPE_when_component_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Component cannot be bull");

    underTest.getScmInfo(null);
  }

  @Test
  public void load_scm_info_from_cache_when_already_read() throws Exception {
    addChangesetInReport("john", 123456789L, "rev-1");
    ScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);

    assertThat(logTester.logs(TRACE)).hasSize(1);
    logTester.clear();

    underTest.getScmInfo(FILE);
    assertThat(logTester.logs(TRACE)).isEmpty();
  }

  private void addChangesetInDb(String author, Long date, String revision) {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    fileDataBuilder.addLinesBuilder()
      .setLine(1)
      .setScmAuthor(author)
      .setScmDate(date)
      .setScmRevision(revision);
    dbTester.getDbClient().fileSourceDao().insert(new FileSourceDto()
      .setFileUuid(FILE.getUuid())
      .setProjectUuid("PROJECT_UUID")
      .setSourceData(fileDataBuilder.build()));
  }

  private void addChangesetInReport(String author, Long date, String revision) {
    reportReader.putChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor(author)
        .setDate(date)
        .setRevision(revision)
        .build())
      .addChangesetIndexByLine(0)
      .build());
  }
}
