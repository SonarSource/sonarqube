/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.ws.ws;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.TestRequest;

import static org.sonar.test.JsonAssert.assertJson;

public class ListActionTest {
  private WebService.Context context = new WebService.Context();
  private ListAction underTest = new ListAction();
  private WebService.Action action;

  @Before
  public void setUp() throws Exception {
    WebService.NewController newController = context.createController("api/webservices")
      .setDescription("Get information on the web api supported on this instance.")
      .setSince("4.2");

    for (WebServicesWsAction wsWsAction : Arrays.asList(underTest, new ResponseExampleAction(), new ChangeLogAction())) {
      wsWsAction.define(newController);
      wsWsAction.setContext(context);
    }
    newController.done();
    action = context.controller("api/webservices").action("list");
  }

  @Test
  public void list() {
    new MetricWs().define(context);

    String response = newRequest().execute().getInput();

    assertJson(response).withStrictArrayOrder().isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  public void list_including_internals() {
    MetricWs metricWs = new MetricWs();
    metricWs.define(context);

    newRequest()
      .setParam("include_internals", "true")
      .execute()
      .assertJson(getClass(), "list_including_internals.json");
  }

  public TestRequest newRequest() {
    TestRequest request = new TestRequest();
    request.setAction(action);
    return request;
  }

  class ChangeLogAction implements WebServicesWsAction {

    @Override
    public void define(WebService.NewController controller) {
      WebService.NewAction action = controller
        .createAction("action")
        .setHandler(this);
      action.setChangelog(new Change("1.0", "Initial"), new Change("2.0", "Second"), new Change("10.0", "Ten"));
    }

    @Override
    public void handle(Request request, Response response) throws Exception {

    }

    @Override
    public void setContext(WebService.Context context) {

    }
  }
}
