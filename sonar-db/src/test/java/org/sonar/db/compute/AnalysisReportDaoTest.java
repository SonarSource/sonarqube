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

package org.sonar.db.compute;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.compute.AnalysisReportDto.Status.PENDING;
import static org.sonar.db.compute.AnalysisReportDto.Status.WORKING;

@Category(DbTests.class)
public class AnalysisReportDaoTest {

  static final String DEFAULT_PROJECT_KEY = "123456789-987654321";

  static System2 system2 = mock(System2.class);
  @Rule
  public DbTester db = DbTester.create(system2);
  DbSession session;

  AnalysisReportDao underTest = new AnalysisReportDao(system2);

  @Before
  public void setUp() {
    session = db.getSession();
  }

  @Test
  public void insert_multiple_reports() {
    db.prepareDbUnit(getClass(), "empty.xml");
    when(system2.now()).thenReturn(1_500_000_000_000L);

    AnalysisReportDto report1 = new AnalysisReportDto().setProjectKey("ProjectKey1").setProjectName("Project 1").setUuid("UUID_1").setStatus(PENDING);
    AnalysisReportDto report2 = new AnalysisReportDto().setProjectKey("ProjectKey2").setProjectName("Project 2").setUuid("UUID_2").setStatus(PENDING);

    underTest.insert(session, report1);
    underTest.insert(session, report2);
    session.commit();

    db.assertDbUnit(getClass(), "insert-result.xml", "analysis_reports");
  }

  @Test
  public void resetAllToPendingStatus() {
    db.prepareDbUnit(getClass(), "update-all-to-status-pending.xml");

    underTest.resetAllToPendingStatus(session);
    session.commit();

    db.assertDbUnit(getClass(), "update-all-to-status-pending-result.xml", "analysis_reports");
  }

  @Test
  public void truncate() {
    db.prepareDbUnit(getClass(), "any-analysis-reports.xml");

    underTest.truncate(session);
    session.commit();

    db.assertDbUnit(getClass(), "truncate-result.xml", "analysis_reports");
  }

  @Test
  public void find_one_report_by_project_key() {
    db.prepareDbUnit(getClass(), "select.xml");

    final String projectKey = "123456789-987654321";
    List<AnalysisReportDto> reports = underTest.selectByProjectKey(session, projectKey);
    AnalysisReportDto report = reports.get(0);

    assertThat(reports).hasSize(1);
    assertThat(report.getProjectKey()).isEqualTo(projectKey);
    assertThat(report.getProjectName()).isEqualTo("Project 1");
    assertThat(report.getStatus()).isEqualTo(AnalysisReportDto.Status.WORKING);
    assertThat(report.getId()).isEqualTo(1);
  }

  @Test
  public void find_several_reports_by_project_key() {
    db.prepareDbUnit(getClass(), "select.xml");

    final String projectKey = "987654321-123456789";
    List<AnalysisReportDto> reports = underTest.selectByProjectKey(session, projectKey);

    assertThat(reports).hasSize(2);
  }

  @Test
  public void pop_oldest_pending() {
    db.prepareDbUnit(getClass(), "pop_oldest_pending.xml");

    AnalysisReportDto nextAvailableReport = underTest.pop(session);

    assertThat(nextAvailableReport.getId()).isEqualTo(3);
    assertThat(nextAvailableReport.getProjectKey()).isEqualTo("P2");
  }

  @Test
  public void count_pending() {
    db.prepareDbUnit(getClass(), "pop_oldest_pending.xml");

    assertThat(underTest.countPending(db.getSession())).isEqualTo(2);
  }

  @Test
  public void pop_null_if_no_pending_reports() {
    db.prepareDbUnit(getClass(), "pop_null_if_no_pending_reports.xml");

    AnalysisReportDto nextAvailableReport = underTest.pop(session);

    assertThat(nextAvailableReport).isNull();
  }

  @Test
  public void getById_maps_all_the_fields_except_the_data() {
    db.prepareDbUnit(getClass(), "one_analysis_report.xml");

    AnalysisReportDto report = underTest.selectById(session, 1L);

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

    AnalysisReportDto report = underTest.selectById(session, 4L);

    assertThat(report).isNull();
  }

  @Test
  public void delete_one_analysis_report() {
    db.prepareDbUnit(getClass(), "one_analysis_report.xml");

    underTest.delete(session, 1);
    session.commit();

    db.assertDbUnit(getClass(), "truncate-result.xml", "analysis_reports");
  }

  @Test
  public void findAll_one_analysis_report() {
    db.prepareDbUnit(getClass(), "one_analysis_report.xml");

    List<AnalysisReportDto> reports = underTest.selectAll(session);

    assertThat(reports).hasSize(1);
  }

  @Test
  public void findAll_empty_table() {
    db.prepareDbUnit(getClass(), "empty.xml");

    List<AnalysisReportDto> reports = underTest.selectAll(session);

    assertThat(reports).isEmpty();
  }

  @Test
  public void findAll_three_analysis_reports() {
    db.prepareDbUnit(getClass(), "three_analysis_reports.xml");

    List<AnalysisReportDto> reports = underTest.selectAll(session);

    assertThat(reports).hasSize(3);
  }
}
