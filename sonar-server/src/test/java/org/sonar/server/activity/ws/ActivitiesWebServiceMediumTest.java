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
package org.sonar.server.activity.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.activity.Activity;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.db.DbClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class ActivitiesWebServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private ActivitiesWebService ws;
  private ActivityService service;
  private DbSession session;


  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    ws = tester.get(ActivitiesWebService.class);
    service = tester.get(ActivityService.class);
    session = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void define() throws Exception {
    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller(ActivitiesWebService.API_ENDPOINT);

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(1);
    assertThat(controller.action(SearchAction.SEARCH_ACTION)).isNotNull();
  }

  @Test
  public void search_logs() throws Exception {
    service.write(session, Activity.Type.QPROFILE, "Hello World");
    session.commit();

    MockUserSession.set();

    // 1. List single Text log
    WsTester.TestRequest request = tester.wsTester().newGetRequest(ActivitiesWebService.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    WsTester.Result result = request.execute();
  }


}
