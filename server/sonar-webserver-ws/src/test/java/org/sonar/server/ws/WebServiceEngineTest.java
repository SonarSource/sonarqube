/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.function.Consumer;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.server.exceptions.BadConfigurationException;
import org.sonar.server.exceptions.BadRequestException;
import org.sonarqube.ws.MediaTypes;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class WebServiceEngineTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setup() {
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  public void load_ws_definitions_at_startup() {
    WebServiceEngine underTest = new WebServiceEngine(new WebService[] {
      newWs("api/foo/index", a -> {
      }),
      newWs("api/bar/index", a -> {
      })
    },
      new ActionInterceptor[] {});
    underTest.start();
    try {
      assertThat(underTest.controllers())
        .extracting(WebService.Controller::path)
        .containsExactlyInAnyOrder("api/foo", "api/bar");
    } finally {
      underTest.stop();
    }
  }

  @DataProvider
  public static Object[][] responseData() {
    return new Object[][] {
      {"/api/ping", "pong", 200},
      {"api/ping", "pong", 200},
      {"api/ping.json", "pong", 200},
      {"xxx/ping", "{\"errors\":[{\"msg\":\"Unknown url : xxx/ping\"}]}", 404},
      {"api/xxx", "{\"errors\":[{\"msg\":\"Unknown url : api/xxx\"}]}", 404}
    };
  }

  @Test
  @UseDataProvider("responseData")
  public void ws_returns_successful_response(String path, String output, int statusCode) {
    Request request = new TestRequest().setPath(path);

    DumbResponse response = run(request, newPingWs(a -> {
    }));

    assertThat(response.stream().outputAsString()).isEqualTo(output);
    assertThat(response.status()).isEqualTo(statusCode);
  }

  @Test
  public void bad_request_if_action_suffix_is_not_supported() {
    Request request = new TestRequest().setPath("/api/ping.bat");

    DumbResponse response = run(request, newPingWs(a -> {
    }));

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.mediaType()).isEqualTo(MediaTypes.JSON);
    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unknown action extension: bat\"}]}");
  }

  @Test
  public void test_response_with_no_content() {
    Request request = new TestRequest().setPath("api/foo");

    RequestHandler handler = (req, resp) -> resp.noContent();
    DumbResponse response = run(request, newWs("api/foo", a -> a.setHandler(handler)));

    assertThat(response.stream().outputAsString()).isEmpty();
    assertThat(response.status()).isEqualTo(204);
  }

  @Test
  public void fail_if_method_GET_is_not_allowed() {
    Request request = new TestRequest().setMethod("GET").setPath("api/foo");

    DumbResponse response = run(request, newWs("api/foo", a -> a.setPost(true)));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"HTTP method POST is required\"}]}");
    assertThat(response.status()).isEqualTo(405);
  }

  @Test
  public void fail_if_method_POST_is_not_allowed() {
    Request request = new TestRequest().setMethod("POST").setPath("api/foo");

    DumbResponse response = run(request, newWs("api/foo", a -> a.setPost(false)));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"HTTP method GET is required\"}]}");
    assertThat(response.status()).isEqualTo(405);
  }

  @DataProvider
  public static String[] verbs() {
    return new String[] {
      "PUT", "DELETE", "HEAD", "PATCH", "CONNECT", "OPTIONS", "TRACE"
    };
  }

  @Test
  @UseDataProvider("verbs")
  public void method_is_not_allowed(String verb) {
    Request request = new TestRequest().setMethod(verb).setPath("/api/ping");

    DumbResponse response = run(request, newPingWs(a -> {
    }));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"HTTP method " + verb + " is not allowed\"}]}");
    assertThat(response.status()).isEqualTo(405);
  }

  @Test
  public void method_POST_is_required() {
    Request request = new TestRequest().setMethod("POST").setPath("api/ping");

    DumbResponse response = run(request, newPingWs(a -> a.setPost(true)));

    assertThat(response.stream().outputAsString()).isEqualTo("pong");
    assertThat(response.status()).isEqualTo(200);
  }

  @Test
  public void fail_if_reading_an_undefined_parameter() {
    Request request = new TestRequest().setPath("api/foo").setParam("unknown", "Unknown");

    DumbResponse response = run(request, newWs("api/foo", a -> a.setHandler((req, resp) -> request.param("unknown"))));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"BUG - parameter \\u0027unknown\\u0027 is undefined for action \\u0027foo\\u0027\"}]}");
    assertThat(response.status()).isEqualTo(400);
  }

  @Test
  public void fail_if_request_does_not_have_required_parameter() {
    Request request = new TestRequest().setPath("api/foo").setParam("unknown", "Unknown");

    DumbResponse response = run(request, newWs("api/foo", a -> {
      a.createParam("bar").setRequired(true);
      a.setHandler((req, resp) -> request.mandatoryParam("bar"));
    }));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"The \\u0027bar\\u0027 parameter is missing\"}]}");
    assertThat(response.status()).isEqualTo(400);
  }

  @Test
  public void fail_if_request_does_not_have_required_parameter_even_if_handler_does_not_require_it() {
    Request request = new TestRequest().setPath("api/foo").setParam("unknown", "Unknown");

    DumbResponse response = run(request, newWs("api/foo", a -> {
      a.createParam("bar").setRequired(true);
      // do not use mandatoryParam("bar")
      a.setHandler((req, resp) -> request.param("bar"));
    }));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"The \\u0027bar\\u0027 parameter is missing\"}]}");
    assertThat(response.status()).isEqualTo(400);
  }

  @Test
  public void use_default_value_of_optional_parameter() {
    Request request = new TestRequest().setPath("api/print");

    DumbResponse response = run(request, newWs("api/print", a -> {
      a.createParam("message").setDefaultValue("hello");
      a.setHandler((req, resp) -> resp.stream().output().write(req.param("message").getBytes(UTF_8)));
    }));

    assertThat(response.stream().outputAsString()).isEqualTo("hello");
    assertThat(response.status()).isEqualTo(200);
  }

  @Test
  public void use_request_parameter_on_parameter_with_default_value() {
    Request request = new TestRequest().setPath("api/print").setParam("message", "bar");

    DumbResponse response = run(request, newWs("api/print", a -> {
      a.createParam("message").setDefaultValue("default_value");
      a.setHandler((req, resp) -> resp.stream().output().write(req.param("message").getBytes(UTF_8)));
    }));

    assertThat(response.stream().outputAsString()).isEqualTo("bar");
    assertThat(response.status()).isEqualTo(200);
  }

  @Test
  public void accept_parameter_value_within_defined_possible_values() {
    Request request = new TestRequest().setPath("api/foo").setParam("format", "json");

    DumbResponse response = run(request, newWs("api/foo", a -> {
      a.createParam("format").setPossibleValues("json", "xml");
      a.setHandler((req, resp) -> resp.stream().output().write(req.mandatoryParam("format").getBytes(UTF_8)));
    }));

    assertThat(response.stream().outputAsString()).isEqualTo("json");
    assertThat(response.status()).isEqualTo(200);
  }

  @Test
  public void fail_if_parameter_value_is_not_in_defined_possible_values() {
    Request request = new TestRequest().setPath("api/foo").setParam("format", "yml");

    DumbResponse response = run(request, newWs("api/foo", a -> {
      a.createParam("format").setPossibleValues("json", "xml");
      a.setHandler((req, resp) -> resp.stream().output().write(req.mandatoryParam("format").getBytes(UTF_8)));
    }));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Value of parameter \\u0027format\\u0027 (yml) must be one of: [json, xml]\"}]}");
    assertThat(response.status()).isEqualTo(400);
  }

  @Test
  public void return_500_on_internal_error() {
    Request request = new TestRequest().setPath("api/foo");

    DumbResponse response = run(request, newFailWs());

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"An error has occurred. Please contact your administrator\"}]}");
    assertThat(response.status()).isEqualTo(500);
    assertThat(response.mediaType()).isEqualTo(MediaTypes.JSON);
    assertThat(logTester.logs(Level.ERROR)).filteredOn(l -> l.contains("Fail to process request api/foo")).isNotEmpty();
  }

  @Test
  public void return_400_on_BadRequestException_with_single_message() {
    Request request = new TestRequest().setPath("api/foo");

    DumbResponse response = run(request, newWs("api/foo", a -> a.setHandler((req, resp) -> {
      throw BadRequestException.create("Bad request !");
    })));

    assertThat(response.stream().outputAsString()).isEqualTo(
      "{\"errors\":[{\"msg\":\"Bad request !\"}]}");
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.mediaType()).isEqualTo(MediaTypes.JSON);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  public void return_400_on_BadRequestException_with_multiple_messages() {
    Request request = new TestRequest().setPath("api/foo");

    DumbResponse response = run(request, newWs("api/foo", a -> a.setHandler((req, resp) -> {
      throw BadRequestException.create("one", "two", "three");
    })));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":["
      + "{\"msg\":\"one\"},"
      + "{\"msg\":\"two\"},"
      + "{\"msg\":\"three\"}"
      + "]}");
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.mediaType()).isEqualTo(MediaTypes.JSON);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  public void return_400_on_BadConfigurationException_with_single_message_and_scope() {
    Request request = new TestRequest().setPath("api/foo");

    DumbResponse response = run(request, newWs("api/foo", a -> a.setHandler((req, resp) -> {
      throw new BadConfigurationException("PROJECT", "Bad request !");
    })));

    assertThat(response.stream().outputAsString()).isEqualTo(
      "{\"scope\":\"PROJECT\",\"errors\":[{\"msg\":\"Bad request !\"}]}");
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.mediaType()).isEqualTo(MediaTypes.JSON);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  public void return_error_message_containing_character_percent() {
    Request request = new TestRequest().setPath("api/foo");

    DumbResponse response = run(request, newWs("api/foo", a -> a.setHandler((req, resp) -> {
      throw new IllegalArgumentException("this should not fail %s");
    })));

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"this should not fail %s\"}]}");
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.mediaType()).isEqualTo(MediaTypes.JSON);
  }

  @Test
  public void send_response_headers() {
    Request request = new TestRequest().setPath("api/foo");

    DumbResponse response = run(request, newWs("api/foo", a -> a.setHandler((req, resp) -> resp.setHeader("Content-Disposition", "attachment; filename=foo.zip"))));

    assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=foo.zip");
  }

  @Test
  public void support_aborted_request_when_response_is_already_committed() {
    Request request = new TestRequest().setPath("api/foo");
    Response response = mockServletResponse(true);

    run(request, response, newClientAbortWs());

    // response is committed (status is already sent), so status can't be changed
    verify(response.stream(), never()).setStatus(anyInt());

    assertThat(logTester.logs(Level.DEBUG)).contains("Request api/foo has been aborted by client");
  }

  @Test
  public void support_aborted_request_when_response_is_not_committed() {
    Request request = new TestRequest().setPath("api/foo");
    Response response = mockServletResponse(false);

    run(request, response, newClientAbortWs());

    verify(response.stream()).setStatus(299);
    assertThat(logTester.logs(Level.DEBUG)).contains("Request api/foo has been aborted by client");
  }

  @Test
  public void internal_error_when_response_is_already_committed() {
    Request request = new TestRequest().setPath("api/foo");
    Response response = mockServletResponse(true);

    run(request, response, newFailWs());

    // response is committed (status is already sent), so status can't be changed
    verify(response.stream(), never()).setStatus(anyInt());
    assertThat(logTester.logs(Level.ERROR)).contains("Fail to process request api/foo");
  }

  @Test
  public void internal_error_when_response_is_not_committed() {
    Request request = new TestRequest().setPath("api/foo");
    Response response = mockServletResponse(false);

    run(request, response, newFailWs());

    verify(response.stream()).setStatus(500);
    assertThat(logTester.logs(Level.ERROR)).contains("Fail to process request api/foo");
  }

  @Test
  public void fail_when_start_in_not_called() {
    Request request = new TestRequest().setPath("/api/ping");
    DumbResponse response = new DumbResponse();
    WebServiceEngine underTest = new WebServiceEngine(new WebService[] {
      newPingWs(a -> {
      })}, new ActionInterceptor[] {});

    underTest.execute(request, response);

    assertThat(logTester.logs(Level.ERROR)).contains("Fail to process request /api/ping");
  }

  private static WebService newWs(String path, Consumer<WebService.NewAction> consumer) {
    return context -> {
      WebService.NewController controller = context.createController(substringBeforeLast(path, "/"));
      WebService.NewAction action = createNewDefaultAction(controller, substringAfterLast(path, "/"));
      action.setHandler((request, response) -> {
      });
      consumer.accept(action);
      controller.done();
    };
  }

  private static WebService newPingWs(Consumer<WebService.NewAction> consumer) {
    return newWs("api/ping", a -> {
      a.setHandler((request, response) -> response.stream().output().write("pong".getBytes(UTF_8)));
      consumer.accept(a);
    });
  }

  private static WebService newFailWs() {
    return newWs("api/foo", a -> a.setHandler((req, resp) -> {
      throw new IllegalStateException("BOOM");
    }));
  }

  private static DumbResponse run(Request request, WebService... webServices) {
    DumbResponse response = new DumbResponse();
    return (DumbResponse) run(request, response, webServices);
  }

  private static Response run(Request request, Response response, WebService... webServices) {
    WebServiceEngine underTest = new WebServiceEngine(webServices, new ActionInterceptor[] {});
    underTest.start();
    try {
      underTest.execute(request, response);
      return response;
    } finally {
      underTest.stop();
    }
  }

  private static Response mockServletResponse(boolean committed) {
    Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);
    ServletResponse.ServletStream servletStream = mock(ServletResponse.ServletStream.class, Mockito.RETURNS_DEEP_STUBS);
    when(response.stream()).thenReturn(servletStream);
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(httpServletResponse.isCommitted()).thenReturn(committed);
    when(servletStream.response()).thenReturn(httpServletResponse);
    return response;
  }

  private static WebService newClientAbortWs() {
    return newWs("api/foo", a -> a.setHandler((req, resp) -> {
      throw new ClientAbortException();
    }));
  }

  private static WebService.NewAction createNewDefaultAction(WebService.NewController controller, String key) {
    return controller
      .createAction(key)
      .setDescription("Dummy Description")
      .setSince("5.3")
      .setResponseExample(WebServiceEngineTest.class.getResource("web-service-engine-test.txt"));
  }
}
