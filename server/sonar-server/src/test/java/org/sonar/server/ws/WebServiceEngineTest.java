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
package org.sonar.server.ws;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.internal.ValidatingRequest;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.Message;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebServiceEngineTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  I18n i18n = mock(I18n.class);
  WebServiceEngine engine = new WebServiceEngine(new WebService[] {new SystemWs()}, i18n, userSessionRule);

  @Before
  public void start() {
    engine.start();
  }

  @After
  public void stop() {
    engine.stop();
  }

  @Test
  public void load_ws_definitions_at_startup() {
    assertThat(engine.controllers()).hasSize(1);
    assertThat(engine.controllers().get(0).path()).isEqualTo("api/system");
  }

  @Test
  public void execute_request() {
    ValidatingRequest request = new SimpleRequest("GET");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "health");

    assertThat(response.stream().outputAsString()).isEqualTo("good");
  }

  @Test
  public void execute_request_with_format_type() {
    ValidatingRequest request = new SimpleRequest("GET");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "health.protobuf");

    assertThat(response.stream().outputAsString()).isEqualTo("good");
  }

  @Test
  public void no_content() {
    ValidatingRequest request = new SimpleRequest("GET");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "alive");

    assertThat(response.stream().outputAsString()).isEmpty();
  }

  @Test
  public void bad_controller() {
    ValidatingRequest request = new SimpleRequest("GET");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/xxx", "health");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unknown web service: api/xxx\"}]}");
  }

  @Test
  public void bad_action() {
    ValidatingRequest request = new SimpleRequest("GET");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "xxx");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unknown action: api/system/xxx\"}]}");
  }

  @Test
  public void method_get_not_allowed() {
    ValidatingRequest request = new SimpleRequest("GET");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "ping");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"HTTP method POST is required\"}]}");
  }

  @Test
  public void method_post_required() {
    ValidatingRequest request = new SimpleRequest("POST");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "ping");

    assertThat(response.stream().outputAsString()).isEqualTo("pong");
  }

  @Test
  public void unknown_parameter_is_set() {
    ValidatingRequest request = new SimpleRequest("GET").setParam("unknown", "Unknown");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "fail_with_undeclared_parameter");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"BUG - parameter 'unknown' is undefined for action 'fail_with_undeclared_parameter'\"}]}");
  }

  @Test
  public void required_parameter_is_not_set() {
    ValidatingRequest request = new SimpleRequest("GET");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"The 'message' parameter is missing\"}]}");
  }

  @Test
  public void optional_parameter_is_not_set() {
    ValidatingRequest request = new SimpleRequest("GET").setParam("message", "Hello World");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.stream().outputAsString()).isEqualTo("Hello World by -");
  }

  @Test
  public void optional_parameter_is_set() {
    ValidatingRequest request = new SimpleRequest("GET")
      .setParam("message", "Hello World")
      .setParam("author", "Marcel");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.stream().outputAsString()).isEqualTo("Hello World by Marcel");
  }

  @Test
  public void param_value_is_in_possible_values() {
    ValidatingRequest request = new SimpleRequest("GET")
      .setParam("message", "Hello World")
      .setParam("format", "json");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.stream().outputAsString()).isEqualTo("Hello World by -");
  }

  @Test
  public void param_value_is_not_in_possible_values() {
    ValidatingRequest request = new SimpleRequest("GET")
      .setParam("message", "Hello World")
      .setParam("format", "html");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Value of parameter 'format' (html) must be one of: [json, xml]\"}]}");
  }

  @Test
  public void internal_error() {
    ValidatingRequest request = new SimpleRequest("GET");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "fail");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unexpected\"}]}");
    assertThat(response.stream().httpStatus()).isEqualTo(500);
    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.JSON);
  }

  @Test
  public void bad_request_with_i18n_message() {
    userSessionRule.setLocale(Locale.ENGLISH);
    ValidatingRequest request = new SimpleRequest("GET").setParam("count", "3");
    ServletResponse response = new ServletResponse();
    when(i18n.message(Locale.ENGLISH, "bad.request.reason", "bad.request.reason", 0)).thenReturn("reason #0");

    engine.execute(request, response, "api/system", "fail_with_i18n_message");

    assertThat(response.stream().outputAsString()).isEqualTo(
      "{\"errors\":[{\"msg\":\"reason #0\"}]}"
      );
    assertThat(response.stream().httpStatus()).isEqualTo(400);
    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.JSON);
  }

  @Test
  public void bad_request_with_multiple_messages() {
    ValidatingRequest request = new SimpleRequest("GET").setParam("count", "3");
    ServletResponse response = new ServletResponse();

    engine.execute(request, response, "api/system", "fail_with_multiple_messages");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":["
      + "{\"msg\":\"Bad request reason #0\"},"
      + "{\"msg\":\"Bad request reason #1\"},"
      + "{\"msg\":\"Bad request reason #2\"}"
      + "]}");
    assertThat(response.stream().httpStatus()).isEqualTo(400);
    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.JSON);
  }

  @Test
  public void bad_request_with_multiple_i18n_messages() {
    userSessionRule.setLocale(Locale.ENGLISH);

    ValidatingRequest request = new SimpleRequest("GET").setParam("count", "3");
    ServletResponse response = new ServletResponse();
    when(i18n.message(Locale.ENGLISH, "bad.request.reason", "bad.request.reason", 0)).thenReturn("reason #0");
    when(i18n.message(Locale.ENGLISH, "bad.request.reason", "bad.request.reason", 1)).thenReturn("reason #1");
    when(i18n.message(Locale.ENGLISH, "bad.request.reason", "bad.request.reason", 2)).thenReturn("reason #2");

    engine.execute(request, response, "api/system", "fail_with_multiple_i18n_messages");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[" +
      "{\"msg\":\"reason #0\"}," +
      "{\"msg\":\"reason #1\"}," +
      "{\"msg\":\"reason #2\"}]}");
    assertThat(response.stream().httpStatus()).isEqualTo(400);
    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.JSON);
  }

  @Test
  public void should_handle_headers() {
    ServletResponse response = new ServletResponse();
    String name = "Content-Disposition";
    String value = "attachment; filename=sonarqube.zip";
    response.setHeader(name, value);
    assertThat(response.getHeaderNames()).containsExactly(name);
    assertThat(response.getHeader(name)).isEqualTo(value);
  }

  private static class SimpleRequest extends ValidatingRequest {
    private final String method;
    private Map<String, String> params = Maps.newHashMap();

    private SimpleRequest(String method) {
      this.method = method;
    }

    @Override
    public String method() {
      return method;
    }

    @Override
    public String getMediaType() {
      return MimeTypes.JSON;
    }

    @Override
    public boolean hasParam(String key) {
      return params.keySet().contains(key);
    }

    @Override
    protected String readParam(String key) {
      return params.get(key);
    }

    @Override
    protected InputStream readInputStreamParam(String key) {
      String param = readParam(key);

      return param == null ? null : IOUtils.toInputStream(param);
    }

    public SimpleRequest setParams(Map<String, String> m) {
      this.params = m;
      return this;
    }

    public SimpleRequest setParam(String key, @Nullable String value) {
      if (value != null) {
        params.put(key, value);
      }
      return this;
    }

  }

  static class SystemWs implements WebService {
    @Override
    public void define(Context context) {
      NewController newController = context.createController("api/system");
      newController.createAction("health")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            try {
              response.stream().output().write("good".getBytes());
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
          }
        });
      newController.createAction("ping")
        .setPost(true)
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            try {
              response.stream().output().write("pong".getBytes());
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
          }
        });
      newController.createAction("fail")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            throw new IllegalStateException("Unexpected");
          }
        });
      newController.createAction("fail_with_i18n_message")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            throw new BadRequestException("bad.request.reason", 0);
          }
        });
      newController.createAction("fail_with_multiple_messages")
        .createParam("count", "Number of error messages to generate")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            Errors errors = new Errors();
            for (int count = 0; count < Integer.valueOf(request.param("count")); count++) {
              errors.add(Message.of("Bad request reason #" + count));
            }
            throw new BadRequestException(errors);
          }
        });
      newController.createAction("fail_with_multiple_i18n_messages")
        .createParam("count", "Number of error messages to generate")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            Errors errors = new Errors();
            for (int count = 0; count < Integer.valueOf(request.param("count")); count++) {
              errors.add(Message.of("bad.request.reason", count));
            }
            throw new BadRequestException(errors);
          }
        });
      newController.createAction("alive")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            response.noContent();
          }
        });

      newController.createAction("fail_with_undeclared_parameter")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            response.newJsonWriter().prop("unknown", request.param("unknown"));
          }
        });

      // parameter "message" is required but not "author"
      NewAction print = newController.createAction("print");
      print.createParam("message").setDescription("required message").setRequired(true);
      print.createParam("author").setDescription("optional author").setDefaultValue("-");
      print.createParam("format").setDescription("optional format").setPossibleValues("json", "xml");
      print.setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          try {
            request.param("format");
            IOUtils.write(
              request.mandatoryParam("message") + " by " + request.param("author", "nobody"), response.stream().output());
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        }
      });
      newController.done();
    }

  }
}
