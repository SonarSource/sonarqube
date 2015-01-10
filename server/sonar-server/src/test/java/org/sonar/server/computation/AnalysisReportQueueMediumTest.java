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

package org.sonar.server.computation;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.SUCCESS;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

public class AnalysisReportQueueMediumTest {
  private static final String DEFAULT_PROJECT_KEY = "123456789-987654321";

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private AnalysisReportQueue sut;

  private DbClient db;
  private DbSession session;
  private MockUserSession userSession;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);

    sut = tester.get(AnalysisReportQueue.class);

    UserDto connectedUser = new UserDto().setLogin("gandalf").setName("Gandalf");
    db.userDao().insert(session, connectedUser);

    userSession = MockUserSession.set()
      .setLogin(connectedUser.getLogin())
      .setUserId(connectedUser.getId().intValue())
      .setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    session.commit();
  }

  @After
  public void after() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void create_analysis_report_and_retrieve_it() {
    insertPermissionsForProject(DEFAULT_PROJECT_KEY);
    sut.add(DEFAULT_PROJECT_KEY, 123L, defaultReportData());

    List<AnalysisReportDto> reports = sut.findByProjectKey(DEFAULT_PROJECT_KEY);
    AnalysisReportDto report = reports.get(0);

    assertThat(reports).hasSize(1);
    assertThat(report.getProjectKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(report.getSnapshotId()).isEqualTo(123L);
    assertThat(report.getCreatedAt()).isNotNull();
  }

  private ComponentDto insertPermissionsForProject(String projectKey) {
    ComponentDto project = new ComponentDto().setKey(projectKey);
    db.componentDao().insert(session, project);

    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    userSession.addProjectPermissions(UserRole.USER, project.key());

    session.commit();

    return project;
  }

  @Test
  public void create_and_book_oldest_available_analysis_report_thrice() {
    insertPermissionsForProject(DEFAULT_PROJECT_KEY);
    insertPermissionsForProject("2");
    insertPermissionsForProject("3");

    sut.add(DEFAULT_PROJECT_KEY, 123L, defaultReportData());
    sut.add("2", 123L, defaultReportData());
    sut.add("3", 123L, defaultReportData());

    AnalysisReportDto firstBookedReport = sut.pop();
    AnalysisReportDto secondBookedReport = sut.pop();
    AnalysisReportDto thirdBookedReport = sut.pop();

    assertThat(firstBookedReport.getProjectKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(firstBookedReport.getStatus()).isEqualTo(WORKING);
    assertThat(firstBookedReport.getStartedAt()).isNotNull();
    assertThat(secondBookedReport.getProjectKey()).isEqualTo("2");
    assertThat(thirdBookedReport.getProjectKey()).isEqualTo("3");
  }

  @Test
  public void search_for_available_report_when_no_report_available() {
    AnalysisReportDto nullAnalysisReport = sut.pop();

    assertThat(nullAnalysisReport).isNull();
  }

  @Test
  public void all() {
    insertPermissionsForProject(DEFAULT_PROJECT_KEY);

    sut.add(DEFAULT_PROJECT_KEY, 123L, defaultReportData());
    sut.add(DEFAULT_PROJECT_KEY, 123L, defaultReportData());
    sut.add(DEFAULT_PROJECT_KEY, 123L, defaultReportData());

    List<AnalysisReportDto> reports = sut.all();

    assertThat(reports).hasSize(3);
  }

  @Test
  public void remove_remove_from_queue() {
    insertPermissionsForProject(DEFAULT_PROJECT_KEY);
    sut.add(DEFAULT_PROJECT_KEY, 123L, defaultReportData());
    AnalysisReportDto report = sut.pop();
    report.setStatus(SUCCESS);

    sut.remove(report);

    assertThat(sut.all()).isEmpty();
  }

  @Test(expected = ForbiddenException.class)
  public void cannot_add_report_when_not_the_right_rights() {
    ComponentDto project = new ComponentDto()
      .setKey("MyProject");
    db.componentDao().insert(session, project);
    session.commit();

    MockUserSession.set().setLogin("gandalf").addProjectPermissions(UserRole.USER, project.key());

    sut.add(project.getKey(), 123L, defaultReportData());
  }

  private InputStream defaultReportData() {
    return IOUtils.toInputStream("default-project");
  }
}
