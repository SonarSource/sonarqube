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

package org.sonar.server.batch;

import org.junit.*;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.platform.BackendCleanup;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.QueryContext;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;

public class UploadReportActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .setProperty("sonar.issues.use_es_backend", "true");

  DbClient db;
  DbSession session;

  WsTester wsTester;
  BatchWs ws;
  WebService.Controller controller;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();

    db = tester.get(DbClient.class);
    session = db.openSession(false);

    ws = tester.get(BatchWs.class);
    wsTester = tester.get(WsTester.class);
    controller = wsTester.controller(BatchWs.API_ENDPOINT);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void define() throws Exception {
    WebService.Action restoreProfiles = controller.action(UploadReportAction.UPLOAD_REPORT_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.params()).hasSize(1);
  }

  @Test
  public void add_project_issue_permission_index() throws Exception {
    ComponentDto project = new ComponentDto()
      .setKey("MyProject");
    db.componentDao().insert(session, project);

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);

    session.commit();

    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.getKey())).isNull();

    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    WsTester.TestRequest request = wsTester.newGetRequest(BatchWs.API_ENDPOINT, UploadReportAction.UPLOAD_REPORT_ACTION);
    request.setParam(UploadReportAction.PARAM_PROJECT, project.key());
    request.execute();

    // Check that issue authorization index has been created
    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.getKey())).isNotNull();
  }
  
  @Test
  public void add_analysis_report_in_database() throws Exception {
    final String projectKey = "123456789-987654321";
    ComponentDto project = new ComponentDto()
        .setKey(projectKey);
    db.componentDao().insert(session, project);

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);

    session.commit();

    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    WsTester.TestRequest request = wsTester.newGetRequest(BatchWs.API_ENDPOINT, UploadReportAction.UPLOAD_REPORT_ACTION);
    request.setParam(UploadReportAction.PARAM_PROJECT, project.key());
    request.execute();

    List<AnalysisReportDto> analysisReports = db.analysisReportDao().findByProjectKey(session, projectKey);
    AnalysisReportDto analysisReport = analysisReports.get(0);

    assertThat(analysisReports).hasSize(1);
    assertThat(analysisReport.getProjectKey()).isEqualTo(projectKey);
    assertThat(analysisReport.getStatus()).isEqualTo(PENDING);
  }

  @Test(expected = ForbiddenException.class)
  public void fail_without_global_scan_permission() throws Exception {
    ComponentDto project = new ComponentDto()
      .setKey("MyProject");
    db.componentDao().insert(session, project);
    session.commit();

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    WsTester.TestRequest request = wsTester.newGetRequest(BatchWs.API_ENDPOINT, UploadReportAction.UPLOAD_REPORT_ACTION);
    request.setParam(UploadReportAction.PARAM_PROJECT, project.key());
    request.execute();
  }

  @Test
  public void index_project_issues() throws Exception {
    ComponentDto project = new ComponentDto()
      .setKey("MyProject");
    db.componentDao().insert(session, project);

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    ComponentDto resource = new ComponentDto()
      .setProjectId_unit_test_only(project.getId())
      .setKey("MyComponent");
    db.componentDao().insert(session, resource);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(resource));

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    IssueDto issue = new IssueDto()
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setRootComponent(project)
      .setComponent(resource)
      .setStatus("OPEN").setResolution("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue);

    session.commit();

    // Clear issue index to simulate that the issue has been inserted by the batch, so that it's not yet index in E/S
    tester.get(BackendCleanup.class).clearIndex(IndexDefinition.ISSUES);
    assertThat(db.issueDao().getByKey(session, issue.getKey())).isNotNull();
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNull();

    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    WsTester.TestRequest request = wsTester.newGetRequest(BatchWs.API_ENDPOINT, UploadReportAction.UPLOAD_REPORT_ACTION);
    request.setParam(UploadReportAction.PARAM_PROJECT, project.key());
    request.execute();

    // Check that the issue has well be indexed in E/S
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNotNull();
  }

  @Test
  @Ignore("To be fixed")
  public void index_a_lot_of_issues() throws Exception {
    ComponentDto project = new ComponentDto()
      .setKey("MyProject");
    db.componentDao().insert(session, project);

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    ComponentDto resource = new ComponentDto()
      .setProjectId_unit_test_only(project.getId())
      .setKey("MyComponent");
    db.componentDao().insert(session, resource);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(resource));

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    List<String> issueKeys = newArrayList();
    for (int i=0; i<2001; i++) {
      IssueDto issue = IssueTesting.newDto(rule, resource, project);
      tester.get(IssueDao.class).insert(session, issue);
      issueKeys.add(issue.getKey());
    }
    session.commit();
    session.clearCache();

    // Clear issue index to simulate that the issue has been inserted by the batch, so that it's not yet index in E/S
    tester.get(BackendCleanup.class).clearIndex(IndexDefinition.ISSUES);

    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    WsTester.TestRequest request = wsTester.newGetRequest(BatchWs.API_ENDPOINT, UploadReportAction.UPLOAD_REPORT_ACTION);
    request.setParam(UploadReportAction.PARAM_PROJECT, project.key());
    request.execute();

    session.commit();
    session.clearCache();

    // Check that the issue has well be indexed in E/S
    assertThat(tester.get(IssueIndex.class).search(IssueQuery.builder().build(), new QueryContext()).getTotal()).isEqualTo(2001);
  }

}
