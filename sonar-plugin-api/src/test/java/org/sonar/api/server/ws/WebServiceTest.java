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
package org.sonar.api.server.ws;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class WebServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  WebService.Context context = new WebService.Context();

  @Test
  public void no_web_services_by_default() {
    assertThat(context.controllers()).isEmpty();
    assertThat(context.controller("metric")).isNull();
  }

  @Test
  public void define_web_service() {
    MetricWs metricWs = new MetricWs();

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
    assertThat(showAction.responseExample()).isNotNull();
    assertThat(showAction.responseExampleFormat()).isNotEmpty();
    assertThat(showAction.responseExampleAsString()).isNotEmpty();
    assertThat(showAction.deprecatedSince()).isNull();
    // same as controller
    assertThat(showAction.since()).isEqualTo("4.2");
    assertThat(showAction.isPost()).isFalse();
    assertThat(showAction.isInternal()).isFalse();
    assertThat(showAction.path()).isEqualTo("api/metric/show");
    WebService.Action createAction = controller.action("create");
    assertThat(createAction).isNotNull();
    assertThat(createAction.key()).isEqualTo("create");
    assertThat(createAction.toString()).isEqualTo("api/metric/create");
    assertThat(createAction.deprecatedSince()).isEqualTo("5.3");
    // overrides controller version
    assertThat(createAction.since()).isEqualTo("4.1");
    assertThat(createAction.isPost()).isTrue();
    assertThat(createAction.isInternal()).isTrue();
  }

  @Test
  public void fail_if_duplicated_ws_keys() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The web service 'api/metric' is defined multiple times");

    MetricWs metricWs = new MetricWs();
    metricWs.define(context);
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/metric");
        newDefaultAction(newController, "delete");
        newController.done();
      }
    }.define(context);
  }

  @Test
  public void fail_if_no_action_handler() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("RequestHandler is not set on action rule/show");

    new WebService() {
      @Override
      public void define(Context context) {
        NewController controller = context.createController("rule");
        newDefaultAction(controller, "show")
          .setHandler(null);
        controller.done();
      }
    }.define(context);
  }

  @Test
  public void fail_if_duplicated_action_keys() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The action 'delete' is defined multiple times in the web service 'rule'");

    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("rule");
        newDefaultAction(newController, "create");
        newDefaultAction(newController, "delete");
        newDefaultAction(newController, "delete");
        newController.done();
      }
    }.define(context);
  }

  @Test
  public void fail_if_no_actions() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("At least one action must be declared in the web service 'rule'");

    new WebService() {
      @Override
      public void define(Context context) {
        context.createController("rule").done();
      }
    }.define(context);
  }

  @Test
  public void fail_if_no_controller_path() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("WS controller path must not be empty");

    new WebService() {
      @Override
      public void define(Context context) {
        context.createController(null).done();
      }
    }.define(context);
  }

  @Test
  public void controller_path_must_not_start_with_slash() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("WS controller path must not start or end with slash: /hello");

    new WebService() {
      @Override
      public void define(Context context) {
        context.createController("/hello").done();
      }
    }.define(context);
  }

  @Test
  public void controller_path_must_not_end_with_slash() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("WS controller path must not start or end with slash: hello/");

    new WebService() {
      @Override
      public void define(Context context) {
        context.createController("hello/").done();
      }
    }.define(context);
  }

  @Test
  public void handle_request() throws Exception {
    MetricWs metricWs = new MetricWs();
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
        NewAction newAction = newDefaultAction(newController, "create");
        newAction
          .createParam("key")
          .setDescription("Key of the new rule");
        newAction
          .createParam("severity")
          .setDefaultValue("MAJOR")
          .setSince("4.4")
          .setDeprecatedSince("5.3")
          .setDeprecatedKey("old-severity")
          .setPossibleValues("INFO", "MAJOR", "BLOCKER");
        newAction.addPagingParams(20);
        newAction.addFieldsParam(Arrays.asList("name", "severity"));
        newAction.addSortParams(Arrays.asList("name", "updatedAt", "severity"), "updatedAt", false);
        newController.done();
      }
    }.define(context);

    WebService.Action action = context.controller("api/rule").action("create");
    assertThat(action.params()).hasSize(7);

    WebService.Param keyParam = action.param("key");
    assertThat(keyParam.key()).isEqualTo("key");
    assertThat(keyParam.description()).isEqualTo("Key of the new rule");
    assertThat(keyParam.toString()).isEqualTo("key");

    WebService.Param severityParam = action.param("severity");
    assertThat(severityParam.key()).isEqualTo("severity");
    assertThat(severityParam.description()).isNull();
    assertThat(severityParam.deprecatedSince()).isEqualTo("5.3");
    assertThat(severityParam.since()).isEqualTo("4.4");
    assertThat(severityParam.deprecatedKey()).isEqualTo("old-severity");
    assertThat(severityParam.defaultValue()).isEqualTo("MAJOR");
    assertThat(severityParam.possibleValues()).containsOnly("INFO", "MAJOR", "BLOCKER");

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
        NewAction create = newDefaultAction(newController, "create");
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
        NewAction create = newDefaultAction(newController, "create");
        create.createParam("status")
          .setDefaultValue(null)
          .setPossibleValues(Collections.emptyList())
          .setExampleValue(null);
        create.createParam("max")
          .setPossibleValues((Object[]) null);
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
        NewAction create = newDefaultAction(newController, "create");
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
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default value must not be set on parameter 'api/rule/create?key' as it's marked as required");
    new WebService() {
      @Override
      public void define(Context context) {
        NewController controller = context.createController("api/rule");
        NewAction action = newDefaultAction(controller, "create");
        action.createParam("key").setRequired(true).setDefaultValue("abc");
        controller.done();
      }
    }.define(context);
  }

  @Test
  public void fail_if_duplicated_action_parameters() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The parameter 'key' is defined multiple times in the action 'create'");

    new WebService() {
      @Override
      public void define(Context context) {
        NewController controller = context.createController("api/rule");
        NewAction action = newDefaultAction(controller, "create");
        action.createParam("key");
        action.createParam("key");
        controller.done();
      }
    }.define(context);
  }

  @Test
  public void ws_is_internal_if_all_actions_are_internal() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        newDefaultAction(newController, "create").setInternal(true);
        newDefaultAction(newController, "update").setInternal(true);
        newController.done();
      }
    }.define(context);

    assertThat(context.controller("api/rule").isInternal()).isTrue();
  }

  @Test
  public void response_example() {
    MetricWs metricWs = new MetricWs();
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
          newDefaultAction(controller, "bar").setResponseExample(new URL("file:/does/not/exist"));
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

  @Test
  public void post_action_without_response_example() {
    WebService ws = new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        newDefaultAction(newController, "list").setPost(true).setResponseExample(null);
        newController.done();
      }
    };
    ws.define(context);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .doesNotContain("The response example is not set on action api/rule/list");
  }

  @Test
  public void fail_if_get_and_no_response_example() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        newDefaultAction(newController, "list").setResponseExample(null);
        newController.done();
      }
    }.define(context);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("The response example is not set on action api/rule/list");
  }

  @Test
  public void log_if_since_on_an_action_is_empty() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        newDefaultAction(newController, "list")
          .setSince("");
        newController.done();
      }
    }.define(context);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Since is not set on action api/rule/list");
  }

  @Test
  public void log_if_since_on_an_action_is_null() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        newDefaultAction(newController, "list")
          .setSince(null);
        newController.done();
      }
    }.define(context);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Since is not set on action api/rule/list");
  }

  @Test
  public void log_if_action_description_is_empty() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        newDefaultAction(newController, "list")
          .setDescription("");
        newController.done();
      }
    }.define(context);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Description is not set on action api/rule/list");
  }

  @Test
  public void log_if_action_description_is_null() {
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/rule");
        newDefaultAction(newController, "list")
          .setDescription(null);
        newController.done();
      }
    }.define(context);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Description is not set on action api/rule/list");
  }

  static class MetricWs implements WebService {
    boolean showCalled = false;
    boolean createCalled = false;

    @Override
    public void define(Context context) {
      NewController newController = context.createController("api/metric")
        .setDescription("Metrics")
        .setSince("3.2");

      newController.createAction("show")
        .setDescription("Show metric")
        .setSince("4.2")
        .setResponseExample(getClass().getResource("WebServiceTest/response-example.txt"))
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            show(request, response);
          }
        });

      newController.createAction("create")
        .setDescription("Create metric")
        .setSince("4.1")
        .setDeprecatedSince("5.3")
        .setPost(true)
        .setInternal(true)
        .setResponseExample(getClass().getResource("WebServiceTest/response-example.txt"))
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

  private NewAction newDefaultAction(NewController controller, String actionKey) {
    return controller.createAction(actionKey)
      .setDescription("default description")
      .setSince("5.3")
      .setResponseExample(getClass().getResource("WebServiceTest/response-example.txt"))
      .setHandler(mock(RequestHandler.class));
  }
}
