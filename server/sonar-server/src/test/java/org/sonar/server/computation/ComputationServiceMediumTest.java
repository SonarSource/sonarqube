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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueAuthorizationDoc;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.platform.BackendCleanup;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

public class ComputationServiceMediumTest {
  private static final String DEFAULT_PROJECT_KEY = "123456789-987654321";

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private ComputationService sut;

  private DbClient db;
  private DbSession session;
  private MockUserSession userSession;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);

    sut = tester.get(ComputationService.class);

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
    sut.addAnalysisReport(DEFAULT_PROJECT_KEY);

    List<AnalysisReportDto> reports = sut.findByProjectKey(DEFAULT_PROJECT_KEY);
    AnalysisReportDto report = reports.get(0);

    assertThat(reports).hasSize(1);
    assertThat(report.getProjectKey()).isEqualTo(DEFAULT_PROJECT_KEY);
  }

  @Test
  public void create_and_book_oldest_available_analysis_report_thrice() {
    insertPermissionsForProject(DEFAULT_PROJECT_KEY);
    insertPermissionsForProject("2");
    insertPermissionsForProject("3");

    sut.addAnalysisReport(DEFAULT_PROJECT_KEY);
    sut.addAnalysisReport("2");
    sut.addAnalysisReport("3");

    AnalysisReportDto firstBookedReport = sut.findAndBookNextAvailableAnalysisReport();
    AnalysisReportDto secondBookedReport = sut.findAndBookNextAvailableAnalysisReport();
    AnalysisReportDto thirdBookedReport = sut.findAndBookNextAvailableAnalysisReport();

    assertThat(firstBookedReport.getProjectKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(firstBookedReport.getStatus()).isEqualTo(WORKING);
    assertThat(secondBookedReport.getProjectKey()).isEqualTo("2");
    assertThat(thirdBookedReport.getProjectKey()).isEqualTo("3");
  }

  @Test
  public void analyze_report() {
    insertPermissionsForProject(DEFAULT_PROJECT_KEY);
    sut.addAnalysisReport(DEFAULT_PROJECT_KEY);

    AnalysisReportDto report = sut.findAndBookNextAvailableAnalysisReport();

    sut.analyzeReport(report);

    assertThat(sut.findByProjectKey(DEFAULT_PROJECT_KEY)).isEmpty();

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
  public void search_for_available_report_when_no_report_available() {
    AnalysisReportDto nullAnalysisReport = sut.findAndBookNextAvailableAnalysisReport();

    assertThat(nullAnalysisReport).isNull();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_without_global_scan_permission() {
    ComponentDto project = new ComponentDto()
      .setKey("MyProject");
    db.componentDao().insert(session, project);
    session.commit();

    MockUserSession.set().setLogin("gandalf").addProjectPermissions(UserRole.USER, project.key());

    sut.addAnalysisReport(project.getKey());
  }

  @Test
  public void add_issues_in_index() {
    ComponentDto project = insertPermissionsForProject(DEFAULT_PROJECT_KEY);

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    ComponentDto file = ComponentTesting.newFileDto(project);
    db.componentDao().insert(session, file);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(file, project));

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);

    session.commit();

    clearIssueIndexToSimulateBatchInsertWithoutIndexing();

    sut.addAnalysisReport(DEFAULT_PROJECT_KEY);
    List<AnalysisReportDto> reports = sut.findByProjectKey(DEFAULT_PROJECT_KEY);

    sut.analyzeReport(reports.get(0));

    // Check that the issue has well be indexed in E/S
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNotNull();
  }

  @Test
  public void add_project_issue_permission_in_index() throws Exception {
    ComponentDto project = insertPermissionsForProject(DEFAULT_PROJECT_KEY);

    sut.addAnalysisReport(DEFAULT_PROJECT_KEY);
    List<AnalysisReportDto> reports = sut.findByProjectKey(DEFAULT_PROJECT_KEY);

    sut.analyzeReport(reports.get(0));

    IssueAuthorizationDoc issueAuthorizationIndex = tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.getKey());
    assertThat(issueAuthorizationIndex).isNotNull();
  }

  private void clearIssueIndexToSimulateBatchInsertWithoutIndexing() {
    tester.get(BackendCleanup.class).clearIndexType(IndexDefinition.ISSUES);
  }

  @Test
  public void index_a_lot_of_issues() throws Exception {
    ComponentDto project = insertPermissionsForProject(DEFAULT_PROJECT_KEY);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    ComponentDto file = ComponentTesting.newFileDto(project);
    db.componentDao().insert(session, file);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(file, project));

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    List<String> issueKeys = newArrayList();
    for (int i = 0; i < 2001; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
      issueKeys.add(issue.getKey());
    }
    session.commit();
    session.clearCache();

    clearIssueIndexToSimulateBatchInsertWithoutIndexing();

    sut.addAnalysisReport(DEFAULT_PROJECT_KEY);
    List<AnalysisReportDto> reports = sut.findByProjectKey(DEFAULT_PROJECT_KEY);

    sut.analyzeReport(reports.get(0));

    session.commit();
    session.clearCache();

    Result<Issue> issueIndex = tester.get(IssueIndex.class).search(IssueQuery.builder().build(), new QueryContext());
    assertThat(issueIndex.getTotal()).isEqualTo(2001);
  }
}
