/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.activity.ws;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.JsonAssert.assertJson;

public class ActivitiesWsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  ActivitiesWs ws;
  ActivityService service;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    ws = tester.get(ActivitiesWs.class);
    service = tester.get(ActivityService.class);
  }

  @Test
  public void define() {
    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller(ActivitiesWs.ENDPOINT);

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(1);
    assertThat(controller.action(SearchAction.SEARCH_ACTION)).isNotNull();
  }

  @Test
  public void search() throws Exception {
    Activity activity = new Activity();
    activity.setType(Activity.Type.ANALYSIS_REPORT);
    activity.setAction("THE_ACTION");
    activity.setMessage("THE_MSG");
    activity.setData("foo", "bar");
    activity.setData("profileKey", "PROFILE_KEY");

    service.save(activity);
    WsTester.TestRequest request = tester.wsTester().newGetRequest("api/activities", "search");

    String result = request.execute().outputAsString();
    assertJson(result).isSimilarTo(
      "{" +
        "  \"total\": 1," +
        "  \"p\": 1," +
        "  \"ps\": 10," +
        "  \"logs\": [" +
        "    {" +
        "      \"type\": \"ANALYSIS_REPORT\"," +
        "      \"action\": \"THE_ACTION\"," +
        "      \"message\": \"THE_MSG\"," +
        "      \"details\": {" +
        "        \"profileKey\": \"PROFILE_KEY\"," +
        "        \"foo\": \"bar\"" +
        "      }" +
        "    }" +
        "  ]" +
        "}"
    );
  }
}
