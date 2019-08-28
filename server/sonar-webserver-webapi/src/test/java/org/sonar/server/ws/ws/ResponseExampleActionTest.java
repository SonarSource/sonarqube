/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.TestRequest;

public class ResponseExampleActionTest {
  private WebService.Context context = new WebService.Context();
  private ResponseExampleAction underTest = new ResponseExampleAction();
  private WebService.Action action;

  @Before
  public void setUp() throws Exception {
    WebService.NewController newController = context.createController("api/ws");
    underTest.define(newController);
    newController.done();
    action = Iterables.get(context.controller("api/ws").actions(), 0);
    underTest.setContext(context);
  }

  @Test
  public void response_example() {
    MetricWs metricWs = new MetricWs();
    metricWs.define(context);

    newRequest()
      .setParam("controller", "api/metric")
      .setParam("action", "create")
      .execute()
      .assertJson(getClass(), "response_example.json");
  }

  public TestRequest newRequest() {
    TestRequest request = new TestRequest();
    request.setAction(action);
    return request;
  }

}
