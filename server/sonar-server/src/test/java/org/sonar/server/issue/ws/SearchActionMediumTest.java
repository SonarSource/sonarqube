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

package org.sonar.server.issue.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class SearchActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .setProperty("sonar.issues.use_es_backend", "true");

  IssuesWs ws;
  DbClient db;
  DbSession session;
  WsTester wsTester;

  RuleDto rule;
  ComponentDto project;
  ComponentDto file;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    ws = tester.get(IssuesWs.class);
    wsTester = tester.get(WsTester.class);
    session = db.openSession(false);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
    db.componentDao().insert(session, project);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(project));

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    file = new ComponentDto()
      .setProjectId(1L)
      .setKey("MyComponent")
      .setId(2L);
    db.componentDao().insert(session, file);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(file));

    session.commit();

    MockUserSession.set().setLogin("gandalf");
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void empty_search() throws Exception {
    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    WsTester.Result result = request.execute();

    assertThat(result).isNotNull();
    result.assertJson(this.getClass(), "empty_result.json", false);
  }

  @Test
  public void find_single_result() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setRootComponent(project)
      .setComponent(file)
      .setStatus("OPEN").setResolution("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(SearchAction.PARAM_FACETS, "true");

    WsTester.Result result = request.execute();
    // TODO Date assertion is complex du to System2
    result.assertJson(this.getClass(), "single_result.json", false);
  }

  @Test
  public void paging() throws Exception {
    for (int i=0; i<12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(SearchAction.PARAM_PAGE, "2");
    request.setParam(SearchAction.PARAM_PAGE_SIZE, "9");

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "paging.json", false);
  }

  @Test
  public void paging_with_deprecated_params() throws Exception {
    for (int i=0; i<12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(IssueFilterParameters.PAGE_INDEX, "2");
    request.setParam(IssueFilterParameters.PAGE_SIZE, "9");

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "paging.json", false);
  }

  @Test
  public void default_page_size_is_100() throws Exception {
    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "default_page_size_is_100.json", false);
  }

}
