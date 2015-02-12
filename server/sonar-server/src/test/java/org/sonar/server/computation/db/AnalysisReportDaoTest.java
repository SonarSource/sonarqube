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

package org.sonar.server.computation.db;

import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.persistence.MyBatis;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

@Category(DbTests.class)
public class AnalysisReportDaoTest {

  private static final String DEFAULT_PROJECT_KEY = "123456789-987654321";
  private static final long DEFAULT_SNAPSHOT_ID = 123L;

  @ClassRule
  public static DbTester db = new DbTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private AnalysisReportDao sut;
  private DbSession session;
  private System2 system2;

  @Before
  public void before() {
    db.truncateTables();
    this.session = db.myBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.sut = new AnalysisReportDao(system2);

    when(system2.now()).thenReturn(parseDate("2014-09-26").getTime());
    when(system2.newDate()).thenReturn(parseDate("2014-09-26"));
  }

  @After
  public void after() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void insert_multiple_reports() throws Exception {
    db.prepareDbUnit(getClass(), "empty.xml");

    AnalysisReportDto report1 = newDefaultAnalysisReport();
    AnalysisReportDto report2 = newDefaultAnalysisReport();

    sut.insert(session, report1);
    sut.insert(session, report2);
    session.commit();

    db.assertDbUnit(getClass(), "insert-result.xml", "analysis_reports");
  }

  @Test
  public void insert_report_data_do_not_throw_exception() throws Exception {
    db.prepareDbUnit(getClass(), "empty.xml");
    AnalysisReportDto report = newDefaultAnalysisReport()
      .setData(getClass().getResource("/org/sonar/server/computation/db/AnalysisReportDaoTest/zip.zip").openStream());

    sut.insert(session, report);
  }

  @Test
  public void resetAllToPendingStatus() {
    db.prepareDbUnit(getClass(), "update-all-to-status-pending.xml");

    sut.resetAllToPendingStatus(session);
    session.commit();

    db.assertDbUnit(getClass(), "update-all-to-status-pending-result.xml", "analysis_reports");
  }

  @Test
  public void truncate() {
    db.prepareDbUnit(getClass(), "any-analysis-reports.xml");

    sut.truncate(session);
    session.commit();

    db.assertDbUnit(getClass(), "truncate-result.xml", "analysis_reports");
  }

  @Test
  public void find_one_report_by_project_key() {
    db.prepareDbUnit(getClass(), "select.xml");

    final String projectKey = "123456789-987654321";
    List<AnalysisReportDto> reports = sut.selectByProjectKey(session, projectKey);
    AnalysisReportDto report = reports.get(0);

    assertThat(reports).hasSize(1);
    assertThat(report.getProjectKey()).isEqualTo(projectKey);
    assertThat(report.getId()).isEqualTo(1);
  }

  @Test
  public void find_several_reports_by_project_key() {
    db.prepareDbUnit(getClass(), "select.xml");

    final String projectKey = "987654321-123456789";
    List<AnalysisReportDto> reports = sut.selectByProjectKey(session, projectKey);

    assertThat(reports).hasSize(2);
  }

  @Test
  public void pop_oldest_pending() {
    db.prepareDbUnit(getClass(), "pop_oldest_pending.xml");

    AnalysisReportDto nextAvailableReport = sut.pop(session);

    assertThat(nextAvailableReport.getId()).isEqualTo(3);
    assertThat(nextAvailableReport.getProjectKey()).isEqualTo("P2");
  }

  @Test
  public void pop_null_if_no_pending_reports() {
    db.prepareDbUnit(getClass(), "pop_null_if_no_pending_reports.xml");

    AnalysisReportDto nextAvailableReport = sut.pop(session);

    assertThat(nextAvailableReport).isNull();
  }

  @Test
  public void getById_maps_all_the_fields_except_the_data() {
    db.prepareDbUnit(getClass(), "one_analysis_report.xml");

    AnalysisReportDto report = sut.selectById(session, 1L);

    assertThat(report.getProjectKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(report.getCreatedAt()).isEqualTo(parseDate("2014-09-24").getTime());
    assertThat(report.getUpdatedAt()).isEqualTo(parseDate("2014-09-25").getTime());
    assertThat(report.getStartedAt()).isEqualTo(parseDate("2014-09-26").getTime());
    assertThat(report.getFinishedAt()).isEqualTo(parseDate("2014-09-27").getTime());
    assertThat(report.getStatus()).isEqualTo(WORKING);
    assertThat(report.getData()).isNull();
    assertThat(report.getKey()).isEqualTo("1");
  }

  @Test
  public void getById_returns_null_when_id_not_found() {
    db.prepareDbUnit(getClass(), "select.xml");

    AnalysisReportDto report = sut.selectById(session, 4L);

    assertThat(report).isNull();
  }

  @Test
  public void delete_one_analysis_report() {
    db.prepareDbUnit(getClass(), "one_analysis_report.xml");

    sut.delete(session, 1);
    session.commit();

    db.assertDbUnit(getClass(), "truncate-result.xml", "analysis_reports");
  }

  @Test
  public void findAll_one_analysis_report() {
    db.prepareDbUnit(getClass(), "one_analysis_report.xml");

    List<AnalysisReportDto> reports = sut.selectAll(session);

    assertThat(reports).hasSize(1);
  }

  @Test
  public void findAll_empty_table() {
    db.prepareDbUnit(getClass(), "empty.xml");

    List<AnalysisReportDto> reports = sut.selectAll(session);

    assertThat(reports).isEmpty();
  }

  @Test
  public void findAll_three_analysis_reports() {
    db.prepareDbUnit(getClass(), "three_analysis_reports.xml");

    List<AnalysisReportDto> reports = sut.selectAll(session);

    assertThat(reports).hasSize(3);
  }

  @Test
  public void insert_and_then_retrieve_report_data_with_decompressed_files_medium_test() throws Exception {
    // ARRANGE
    db.prepareDbUnit(getClass(), "empty.xml");
    AnalysisReportDto report = newDefaultAnalysisReport();
    InputStream zip = getClass().getResource("/org/sonar/server/computation/db/AnalysisReportDaoTest/zip.zip").openStream();
    report.setData(zip);

    File toDir = temp.newFolder();
    sut.insert(session, report);
    session.commit();
    IOUtils.closeQuietly(zip);

    // ACT
    sut.selectAndDecompressToDir(session, 1L, toDir);

    // ASSERT
    assertThat(toDir.list()).hasSize(3);
  }

  private AnalysisReportDto newDefaultAnalysisReport() {
    return AnalysisReportDto.newForTests(1L)
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setSnapshotId(DEFAULT_SNAPSHOT_ID)
      .setData(null)
      .setStatus(PENDING);
  }
}
