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
package org.sonar.api.server.ws;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class WebServiceTest {

  static class MetricWebService implements WebService {
    boolean showCalled = false, createCalled = false;

    @Override
    public void define(Context context) {
      NewController newController = context.createController("api/metric")
        .setDescription("Metrics")
        .setSince("3.2");

      newController.createAction("show")
        .setDescription("Show metric")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            show(request, response);
          }
        });

      newController.createAction("create")
        .setDescription("Create metric")
        .setSince("4.1")
        .setPost(true)
        .setInternal(true)
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            create(request, response);
          }
        });

      newController.done();
    }

    void show(Request request, Response response) {
      showCalled = true;
    }

    void create(Request request, Response response) {
      createCalled = true;
    }
  }


  WebService.Context context = new WebService.Context();

  @Test
  public void no_web_services_by_default() {
    assertThat(context.controllers()).isEmpty();
    assertThat(context.controller("metric")).isNull();
  }

  @Test
  public void define_web_service() {
    MetricWebService metricWs = new MetricWebService();

    metricWs.define(context);

    WebService.Controller controller = context.controller("api/metric");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/metric");
    assertThat(controller.description()).isEqualTo("Metrics");
    assertThat(controller.since()).isEqualTo("3.2");
    assertThat(controller.actions()).hasSize(2);
    WebService.Action showAction = controller.action("show");
    assertThat(showAction).isNotNull();
    assertThat(showAction.key()).isEqualTo("show");
    assertThat(showAction.description()).isEqualTo("Show metric");
    assertThat(showAction.handler()).isNotNull();
    // same as controller
    assertThat(showAction.since()).isEqualTo("3.2");
    assertThat(showAction.isPost()).isFalse();
    assertThat(showAction.isInternal()).isFalse();
    assertThat(showAction.path()).isEqualTo("api/metric/show");
    WebService.Action createAction = controller.action("create");
    assertThat(createAction).isNotNull();
    assertThat(createAction.key()).isEqualTo("create");
    assertThat(createAction.toString()).isEqualTo("api/metric/create");
    // overrides controller version
    assertThat(createAction.since()).isEqualTo("4.1");
    assertThat(createAction.isPost()).isTrue();
    assertThat(createAction.isInternal()).isTrue();
  }

  @Test
  public void fail_if_duplicated_ws_keys() {
    MetricWebService metricWs = new MetricWebService();
    metricWs.define(context);
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          NewController newController = context.createController("api/metric");
          newController.createAction("delete");
          newController.done();
        }
      }.define(context);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The web service 'api/metric' is defined multiple times");
    }
  }

  @Test
  public void fail_if_no_action_handler() {
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          NewController controller = context.createController("rule");
          controller.createAction("show");
          controller.done();
        }
      }.define(context);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("RequestHandler is not set on action rule/show");
    }
  }

  @Test
  public void fail_if_duplicated_action_keys() {
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          NewController newController = context.createController("rule");
          newController.createAction("create");
          newController.createAction("delete");
          newController.createAction("delete");
          newController.done();
        }
      }.define(context);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The action 'delete' is defined multiple times in the web service 'rule'");
    }
  }

  @Test
  public void fail_if_no_actions() {
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          context.createController("rule").done();
        }
      }.define(context);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("At least one action must be declared in the web service 'rule'");
    }
  }

  @Test
  public void fail_if_no_controller_path() {
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          context.createController(null).done();
        }
      }.define(context);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("WS controller path must not be empty");
    }
  }

  @Test
  public void controller_path_must_not_start_with_slash() {
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          context.createController("/hello").done();
        }
      }.define(context);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("WS controller path must not start or end with slash: /hello");
    }
  }

  @Test
  public void controller_path_must_not_end_with_slash() {
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          context.createController("hello/").done();
        }
      }.define(context);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("WS controller path must not start or end with slash: hello/");
    }
  }

  @Test
  public void handle_request() throws Exception {
    MetricWebService metricWs = new MetricWebService();
    metricWs.define(context);

    assertThat(metricWs.showCalled).isFalse();
    assertThat(metricWs.createCalled).isFalse();
    context.controller("api/metric").action("show").handler().handle(mock(Request.class), mock(Response.class));
    assertThat(metricWs.showCalled).isTrue();
    assertThat(metricWs.createCalled).isFalse();
    context.controller("api/metric").action("create").handler().handle(mock(Request.class), mock(Response.class));
    assertThat(metricWs.createCalled).isTrue();
  }

  @Test
  public void action_parameters() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        NewAction create = newController.createAction("create").setHandler(mock(RequestHandler.class));
        create.createParam("key").setDescription("Key of the new rule");
        create.createParam("severity");
        newController.done();
      }
    }.define(context);

    WebService.Action action = context.controller("api/rule").action("create");
    assertThat(action.params()).hasSize(2);

    assertThat(action.param("key").key()).isEqualTo("key");
    assertThat(action.param("key").description()).isEqualTo("Key of the new rule");
    assertThat(action.param("key").toString()).isEqualTo("key");

    assertThat(action.param("severity").key()).isEqualTo("severity");
    assertThat(action.param("severity").description()).isNull();
  }

  @Test
  public void fail_if_duplicated_action_parameters() {
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          NewController controller = context.createController("api/rule");
          NewAction action = controller.createAction("create").setHandler(mock(RequestHandler.class));
          action.createParam("key");
          action.createParam("key");
          controller.done();
        }
      }.define(context);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The parameter 'key' is defined multiple times in the action 'create'");
    }
  }
}
