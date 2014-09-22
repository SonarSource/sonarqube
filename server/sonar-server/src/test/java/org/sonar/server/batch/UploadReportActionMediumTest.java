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

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

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
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
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

  @Test(expected = ForbiddenException.class)
  public void fail_without_global_scan_permission() throws Exception {
    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
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
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
    db.componentDao().insert(session, project);

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    ComponentDto resource = new ComponentDto()
      .setProjectId(1L)
      .setKey("MyComponent")
      .setId(2L);
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
    clearIssueIndex();
    assertThat(db.issueDao().getByKey(session, issue.getKey())).isNotNull();
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNull();

    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    WsTester.TestRequest request = wsTester.newGetRequest(BatchWs.API_ENDPOINT, UploadReportAction.UPLOAD_REPORT_ACTION);
    request.setParam(UploadReportAction.PARAM_PROJECT, project.key());
    request.execute();

    // Check that the issue has well be indexed in E/S
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNotNull();
  }

  private void clearIssueIndex(){
    tester.get(SearchClient.class).prepareDeleteByQuery(tester.get(SearchClient.class).admin().cluster().prepareState().get()
      .getState().getMetaData().concreteIndices(new String[]{IndexDefinition.ISSUES.getIndexName()})).setTypes(new String[]{IndexDefinition.ISSUES.getIndexType()})
      .setQuery(QueryBuilders.matchAllQuery())
      .get();
  }

}
