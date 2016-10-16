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
package org.sonar.server.ws;

import com.google.common.io.Resources;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class WebServicesWsTest {

  private WebServicesWs ws = new WebServicesWs();

  @Test
  public void define_ws() {
    WsTester tester = new WsTester(ws);
    WebService.Controller controller = tester.controller("api/webservices");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/webservices");
    assertThat(controller.since()).isEqualTo("4.2");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(2);

    WebService.Action index = controller.action("list");
    assertThat(index).isNotNull();
    assertThat(index.key()).isEqualTo("list");
    assertThat(index.handler()).isNotNull();
    assertThat(index.since()).isEqualTo("4.2");
    assertThat(index.isPost()).isFalse();
    assertThat(index.isInternal()).isFalse();

    assertThat(controller.action("response_example")).isNotNull();
  }

  @Test
  public void list() throws Exception {
    WsTester tester = new WsTester(ws, new MetricWs());

    String response = tester.newGetRequest("api/webservices", "list").execute().outputAsString();

    JsonAssert.assertJson(response).isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  public void list_including_internals() throws Exception {
    WsTester tester = new WsTester(ws, new MetricWs());
    tester.newGetRequest("api/webservices", "list")
      .setParam("include_internals", "true")
      .execute()
      .assertJson(getClass(), "list_including_internals.json");
  }

  @Test
  public void response_example() throws Exception {
    WsTester tester = new WsTester(ws, new MetricWs());
    tester
      .newGetRequest("api/webservices", "response_example")
      .setParam("controller", "api/metric")
      .setParam("action", "create")
      .execute().assertJson(getClass(), "response_example.json");
  }

  static class MetricWs implements WebService {
    @Override
    public void define(Context context) {
      NewController newController = context
        .createController("api/metric")
        .setDescription("Metrics")
        .setSince("3.2");

      // action with default values
      newController.createAction("show")
        .setSince("3.2")
        .setDescription("Show Description")
        .setResponseExample(getClass().getResource("web-services-ws-test.txt"))
        .setHandler((request, response) -> {});

      // action with a lot of overridden values
      NewAction create = newController.createAction("create")
        .setDescription("Create metric")
        .setSince("4.1")
        .setDeprecatedSince("5.3")
        .setPost(true)
        .setResponseExample(Resources.getResource(getClass(), "WebServicesWsTest/metrics_example.json"))
        .setHandler((request, response) -> {});
      create
        .createParam("severity")
        .setDescription("Severity")
        .setSince("4.4")
        .setDeprecatedSince("5.2")
        .setRequired(false)
        .setPossibleValues("BLOCKER", "INFO")
        .setExampleValue("INFO")
        .setDefaultValue("BLOCKER");
      create.createParam("name");
      create.createParam("internal").setInternal(true);

      newController.createAction("internal_action")
        .setDescription("Internal Action Description")
        .setResponseExample(getClass().getResource("web-services-ws-test.txt"))
        .setSince("5.3")
        .setInternal(true)
        .setHandler((request, response) -> {});

      newController.done();
    }
  }
}
