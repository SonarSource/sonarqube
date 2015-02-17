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
package org.sonar.api.server.ws;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
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
        .setResponseExample(getClass().getResource("/org/sonar/api/server/ws/WebServiceTest/response-example.txt"))
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
    assertThat(controller.isInternal()).isFalse();
    WebService.Action showAction = controller.action("show");
    assertThat(showAction).isNotNull();
    assertThat(showAction.key()).isEqualTo("show");
    assertThat(showAction.description()).isEqualTo("Show metric");
    assertThat(showAction.handler()).isNotNull();
    assertThat(showAction.responseExample()).isNull();
    assertThat(showAction.responseExampleFormat()).isNull();
    assertThat(showAction.responseExampleAsString()).isNull();
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
    } catch (IllegalArgumentException e) {
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
        NewAction newAction = newController.createAction("create").setHandler(mock(RequestHandler.class));
        newAction.createParam("key").setDescription("Key of the new rule");
        newAction.createParam("severity").setDefaultValue("MAJOR").setPossibleValues("INFO", "MAJOR", "BLOCKER");
        newAction.addPagingParams(20);
        newAction.addFieldsParam(Arrays.asList("name", "severity"));
        newAction.addSortParams(Arrays.asList("name", "updatedAt", "severity"), "updatedAt", false);
        newController.done();
      }
    }.define(context);

    WebService.Action action = context.controller("api/rule").action("create");
    assertThat(action.params()).hasSize(7);

    assertThat(action.param("key").key()).isEqualTo("key");
    assertThat(action.param("key").description()).isEqualTo("Key of the new rule");
    assertThat(action.param("key").toString()).isEqualTo("key");

    assertThat(action.param("severity").key()).isEqualTo("severity");
    assertThat(action.param("severity").description()).isNull();
    assertThat(action.param("severity").defaultValue()).isEqualTo("MAJOR");
    assertThat(action.param("severity").possibleValues()).containsOnly("INFO", "MAJOR", "BLOCKER");

    // predefined fields
    assertThat(action.param("p").defaultValue()).isEqualTo("1");
    assertThat(action.param("p").description()).isNotEmpty();
    assertThat(action.param("ps").defaultValue()).isEqualTo("20");
    assertThat(action.param("ps").description()).isNotEmpty();
    assertThat(action.param("f").possibleValues()).containsOnly("name", "severity");
    assertThat(action.param("s").possibleValues()).containsOnly("name", "severity", "updatedAt");
    assertThat(action.param("s").description()).isNotEmpty();
    assertThat(action.param("asc").defaultValue()).isEqualTo("false");
  }

  @Test
  public void param_metadata_as_objects() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        NewAction create = newController.createAction("create").setHandler(mock(RequestHandler.class));
        create.createParam("status")
          .setDefaultValue(RuleStatus.BETA)
          .setPossibleValues(RuleStatus.BETA, RuleStatus.READY)
          .setExampleValue(RuleStatus.BETA);
        create.createParam("max")
          .setDefaultValue(11)
          .setPossibleValues(11, 13, 17)
          .setExampleValue(17);
        newController.done();
      }
    }.define(context);

    WebService.Action action = context.controller("api/rule").action("create");
    assertThat(action.param("status").defaultValue()).isEqualTo("BETA");
    assertThat(action.param("status").possibleValues()).containsOnly("BETA", "READY");
    assertThat(action.param("status").exampleValue()).isEqualTo("BETA");
    assertThat(action.param("max").defaultValue()).isEqualTo("11");
    assertThat(action.param("max").possibleValues()).containsOnly("11", "13", "17");
    assertThat(action.param("max").exampleValue()).isEqualTo("17");
  }

  @Test
  public void param_null_metadata() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        NewAction create = newController.createAction("create").setHandler(mock(RequestHandler.class));
        create.createParam("status")
          .setDefaultValue(null)
          .setPossibleValues((Collection) null)
          .setExampleValue(null);
        create.createParam("max")
          .setPossibleValues((String[]) null);
        newController.done();
      }
    }.define(context);

    WebService.Action action = context.controller("api/rule").action("create");
    assertThat(action.param("status").defaultValue()).isNull();
    assertThat(action.param("status").possibleValues()).isNull();
    assertThat(action.param("status").exampleValue()).isNull();
    assertThat(action.param("max").possibleValues()).isNull();
  }

  @Test
  public void param_with_empty_possible_values() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        NewAction create = newController.createAction("create").setHandler(mock(RequestHandler.class));
        create.createParam("status")
          .setPossibleValues(Collections.emptyList());
        newController.done();
      }
    }.define(context);

    WebService.Action action = context.controller("api/rule").action("create");
    // no possible values -> return null but not empty
    assertThat(action.param("status").possibleValues()).isNull();
  }

  @Test
  public void fail_if_required_param_has_default_value() {
    try {
      new WebService() {
        @Override
        public void define(Context context) {
          NewController controller = context.createController("api/rule");
          NewAction action = controller.createAction("create").setHandler(mock(RequestHandler.class));
          action.createParam("key").setRequired(true).setDefaultValue("abc");
          controller.done();
        }
      }.define(context);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Default value must not be set on parameter 'api/rule/create?key' as it's marked as required");
    }
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

  @Test
  public void ws_is_internal_if_all_actions_are_internal() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        newController.createAction("create").setInternal(true).setHandler(mock(RequestHandler.class));
        newController.createAction("update").setInternal(true).setHandler(mock(RequestHandler.class));
        newController.done();
      }
    }.define(context);

    assertThat(context.controller("api/rule").isInternal()).isTrue();
  }

  @Test
  public void response_example() {
    MetricWebService metricWs = new MetricWebService();
    metricWs.define(context);
    WebService.Action action = context.controller("api/metric").action("create");

    assertThat(action.responseExampleFormat()).isEqualTo("txt");
    assertThat(action.responseExample()).isNotNull();
    assertThat(StringUtils.trim(action.responseExampleAsString())).isEqualTo("example of WS response");
  }

  @Test
  public void fail_to_open_response_example() {
    WebService ws = new WebService() {
      @Override
      public void define(Context context) {
        try {
          NewController controller = context.createController("foo");
          controller
            .createAction("bar")
            .setHandler(mock(RequestHandler.class))
            .setResponseExample(new URL("file:/does/not/exist"));
          controller.done();

        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
      }
    };
    ws.define(context);

    WebService.Action action = context.controller("foo").action("bar");
    try {
      action.responseExampleAsString();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail to load file:/does/not/exist");
    }
  }
}
