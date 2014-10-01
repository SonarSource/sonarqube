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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.TestDatabase;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;

public class AnalysisReportDaoTest {
  @Rule
  public TestDatabase db = new TestDatabase();
  private AnalysisReportDao dao;
  private DbSession session;
  private System2 system2;

  @Before
  public void before() {
    this.session = db.myBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new AnalysisReportDao(system2);
  }

  @After
  public void after() throws Exception {
    session.close();
  }

  @Test
  public void insert_multiple_reports() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-09-26").getTime());

    AnalysisReportDto report = new AnalysisReportDto()
      .setProjectKey("123456789-987654321")
      .setData("data-project")
      .setStatus(PENDING);
    report.setCreatedAt(DateUtils.parseDate("2014-09-24"))
      .setUpdatedAt(DateUtils.parseDate("2014-09-25"));

    dao.insert(session, report);
    dao.insert(session, report);

    session.commit();

    db.assertDbUnit(getClass(), "insert-result.xml", "analysis_reports");
  }

  @Test
  public void update_all_to_status() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-09-26").getTime());

    db.prepareDbUnit(getClass(), "update-all-to-status-pending.xml");

    dao.cleanWithUpdateAllToPendingStatus(session);
    session.commit();

    db.assertDbUnit(getClass(), "update-all-to-status-pending-result.xml", "analysis_reports");
  }

  @Test
  public void truncate() {
    db.prepareDbUnit(getClass(), "any-analysis-reports.xml");

    dao.cleanWithTruncate(session);
    session.commit();

    db.assertDbUnit(getClass(), "truncate-result.xml", "analysis_reports");
  }

  @Test
  public void find_one_report_by_project_key() {
    db.prepareDbUnit(getClass(), "select.xml");

    final String projectKey = "123456789-987654321";
    List<AnalysisReportDto> reports = dao.findByProjectKey(session, projectKey);
    AnalysisReportDto report = reports.get(0);

    assertThat(reports).hasSize(1);
    assertThat(report.getProjectKey()).isEqualTo(projectKey);
    assertThat(report.getId()).isEqualTo(1);
  }

  @Test
  public void find_several_reports_by_project_key() {
    db.prepareDbUnit(getClass(), "select.xml");

    final String projectKey = "987654321-123456789";
    List<AnalysisReportDto> reports = dao.findByProjectKey(session, projectKey);

    assertThat(reports).hasSize(2);
  }

  @Test
  public void get_oldest_available_report() {
    db.prepareDbUnit(getClass(), "select_oldest_available_report.xml");

    final String projectKey = "123456789-987654321";
    AnalysisReportDto nextAvailableReport = dao.getNextAvailableReport(session);

    assertThat(nextAvailableReport.getId()).isEqualTo(2);
    assertThat(nextAvailableReport.getProjectKey()).isEqualTo(projectKey);
  }

  @Test
  public void get_oldest_available_report_with_working_reports_older() {
    db.prepareDbUnit(getClass(), "select_oldest_available_report_with_working_reports_older.xml");

    final String projectKey = "123456789-987654321";
    AnalysisReportDto nextAvailableReport = dao.getNextAvailableReport(session);

    assertThat(nextAvailableReport.getId()).isEqualTo(2);
    assertThat(nextAvailableReport.getProjectKey()).isEqualTo(projectKey);
  }

  @Test
  public void null_when_no_available_pending_report_because_working_report_on_the_same_project() {
    db.prepareDbUnit(getClass(), "select-with-no-available-report.xml");

    AnalysisReportDto nextAvailableReport = dao.getNextAvailableReport(session);

    assertThat(nextAvailableReport).isNull();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void doGetNullableByKey_is_not_implemented_yet() {
    dao.doGetNullableByKey(session, "ANY_STRING");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getSynchronizationParams_is_not_implemented_yet() {
    dao.getSynchronizationParams(new Date(), new HashMap<String, String>());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void doUpdate_is_not_implemented_yet() {
    dao.doUpdate(session, new AnalysisReportDto());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void tryToBookReport_is_not_implemented_yet() {
    dao.tryToBookReport(session, new AnalysisReportDto());
  }

}
