/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.ws;

import org.junit.Test;
import org.sonar.api.server.ws.*;

import static org.fest.assertions.Assertions.assertThat;

public class ListingWsTest {

  ListingWs ws = new ListingWs();

  @Test
  public void define_ws() throws Exception {
    WsTester tester = new WsTester(ws);
    WebService.Controller controller = tester.controller("api/webservices");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/webservices");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("4.2");
    assertThat(controller.actions()).hasSize(1);

    WebService.Action index = controller.action("list");
    assertThat(index).isNotNull();
    assertThat(index.key()).isEqualTo("list");
    assertThat(index.handler()).isNotNull();
    assertThat(index.since()).isEqualTo("4.2");
    assertThat(index.isPost()).isFalse();
    assertThat(index.isPrivate()).isFalse();
  }

  @Test
  public void index() throws Exception {
    WsTester tester = new WsTester(ws, new MetricWebService());
    tester.newRequest("api/webservices", "list").execute().assertJson(getClass(), "list.json");
  }

  static class MetricWebService implements WebService {
    @Override
    public void define(Context context) {
      NewController newController = context.newController("api/metric")
        .setDescription("Metrics")
        .setSince("3.2");

      // action with default values
      newController.newAction("show")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
          }
        });


      // action with a lot of overridden values
      NewAction create = newController.newAction("create")
        .setDescription("Create metric")
        .setSince("4.1")
        .setPost(true)
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
          }
        });
      create.newParam("key").setDescription("Key of new metric");
      create.newParam("name");
      newController.done();
    }
  }
}
