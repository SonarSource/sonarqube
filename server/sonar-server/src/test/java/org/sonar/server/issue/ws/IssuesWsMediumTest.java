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
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

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
  public void deactivate_rule() throws Exception {
    // QualityProfileDto profile = createProfile("java");
    // RuleDto rule = createRule(profile.getLanguage(), "toto");
    // createActiveRule(rule, profile);
    // session.commit();
    //
    // // 0. Assert No Active Rule for profile
    // assertThat(db.activeRuleDao().findByProfileKey(session, profile.getKey())).hasSize(1);
    //
    // // 1. Deactivate Rule
    // WsTester.TestRequest request = wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.DEACTIVATE_ACTION);
    // request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
    // request.setParam(RuleActivationActions.RULE_KEY, rule.getKey().toString());
    // request.execute();
    // session.clearCache();
    //
    // // 2. Assert ActiveRule in DAO
    // assertThat(db.activeRuleDao().findByProfileKey(session, profile.getKey())).isEmpty();
  }

}
