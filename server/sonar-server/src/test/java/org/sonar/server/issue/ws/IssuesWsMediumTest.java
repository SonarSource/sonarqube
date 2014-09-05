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
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;
import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

public class IssuesWsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  IssuesWs ws;
  DbClient db;
  DbSession session;
  WsTester wsTester;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    ws = tester.get(IssuesWs.class);
    wsTester = tester.get(WsTester.class);
    session = db.openSession(false);
    MockUserSession.set().setLogin("gandalf").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void define() throws Exception {

    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller(IssuesWs.API_ENDPOINT);

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(15);
    assertThat(controller.action(IssuesWs.ADD_COMMENT_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.ASSIGN_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.BULK_CHANGE_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.CHANGELOG_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.CREATE_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.DELETE_COMMENT_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.DO_ACTION_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.DO_TRANSITION_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.EDIT_COMMENT_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.PLAN_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.SET_SEVERITY_ACTION)).isNotNull();
    assertThat(controller.action(IssuesWs.TRANSITIONS_ACTION)).isNotNull();

    assertThat(controller.action(IssueSearchAction.SEARCH_ACTION)).isNotNull();
    assertThat(controller.action(SearchAction.SEARCH_ACTION)).isNotNull();
  }

  @Test
  public void empty_search() throws Exception {

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    // request.setParam()
    WsTester.Result result = request.execute();

    assertThat(result).isNotNull();
    result.assertJson(this.getClass(), "empty_result.json", false);
  }

  @Test
  @Ignore("Work in progress")
  public void find_single_result() throws Exception {

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(session, project);

    ComponentDto resource = new ComponentDto()
      .setProjectId(1L)
      .setKey("MyComponent")
      .setId(2L);
    tester.get(ComponentDao.class).insert(session, resource);

    IssueDto issue = new IssueDto()
      .setIssueCreationDate(new Date())
      .setIssueUpdateDate(new Date())
      .setRule(rule)
      .setRootComponent(project)
      .setComponent(resource)
      .setStatus("OPEN").setResolution("OPEN")
      .setKee(UUID.randomUUID().toString())
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue);

    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    // request.setParam()
    WsTester.Result result = request.execute();

    assertThat(result).isNotNull();
    System.out.println("result.outputAsString() = " + result.outputAsString());
    result.assertJson(this.getClass(), "empty_result.json", false);
  }

}
