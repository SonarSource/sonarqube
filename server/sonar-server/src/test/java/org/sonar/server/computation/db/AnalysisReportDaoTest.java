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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.persistence.MyBatis;
import org.sonar.test.DbTests;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

@Category(DbTests.class)
public class AnalysisReportDaoTest {

  private static final String DEFAULT_PROJECT_KEY = "123456789-987654321";

  @ClassRule
  public static DbTester db = new DbTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  AnalysisReportDao sut;
  DbSession session;
  System2 system2;

  @Before
  public void before() {
    this.session = db.myBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.sut = new AnalysisReportDao(system2);

    when(system2.now()).thenReturn(1_500_000_000_000L);
  }

  @After
  public void after() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void insert_multiple_reports() throws Exception {
    db.prepareDbUnit(getClass(), "empty.xml");

    AnalysisReportDto report1 = newDefaultAnalysisReport().setUuid("UUID_1");
    AnalysisReportDto report2 = newDefaultAnalysisReport().setUuid("UUID_2");

    sut.insert(session, report1);
    sut.insert(session, report2);
    session.commit();

    db.assertDbUnit(getClass(), "insert-result.xml", "analysis_reports");
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
    assertThat(report.getCreatedAt()).isEqualTo(1_500_000_000_001L);
    assertThat(report.getUpdatedAt()).isEqualTo(1_500_000_000_002L);
    assertThat(report.getStartedAt()).isEqualTo(1_500_000_000_003L);
    assertThat(report.getFinishedAt()).isEqualTo(1_500_000_000_004L);
    assertThat(report.getStatus()).isEqualTo(WORKING);
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

  private AnalysisReportDto newDefaultAnalysisReport() {
    return AnalysisReportDto.newForTests(1L)
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setUuid("REPORT_UUID")
      .setStatus(PENDING);
  }
}
