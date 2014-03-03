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

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.plugins.MimeTypes;

import javax.annotation.CheckForNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class WebServiceEngineTest {

  private static class SimpleRequest extends InternalRequest {
    private String method = "GET";
    private Map<String, String> params = new HashMap<String, String>();

    @Override
    public String method() {
      return method;
    }

    public SimpleRequest setMethod(String s) {
      this.method = s;
      return this;
    }

    public SimpleRequest setParams(Map<String, String> m) {
      this.params = m;
      return this;
    }

    public SimpleRequest setParam(String key, @CheckForNull String value) {
      if (value != null) {
        params.put(key, value);
      }
      return this;
    }

    @Override
    @CheckForNull
    public String param(String key) {
      return params.get(key);
    }

  }

  WebServiceEngine engine = new WebServiceEngine(new WebService[]{new SystemWebService()});

  @Before
  public void start() {
    engine.start();
  }

  @After
  public void stop() {
    engine.stop();
  }

  @Test
  public void load_ws_definitions_at_startup() throws Exception {
    assertThat(engine.controllers()).hasSize(1);
    assertThat(engine.controllers().get(0).path()).isEqualTo("api/system");
  }

  @Test
  public void execute_request() throws Exception {
    InternalRequest request = new SimpleRequest();
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "health");

    assertThat(response.stream().outputAsString()).isEqualTo("good");
  }

  @Test
  public void no_content() throws Exception {
    InternalRequest request = new SimpleRequest();
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "alive");

    assertThat(response.stream().outputAsString()).isEmpty();
  }

  @Test
  public void bad_controller() throws Exception {
    InternalRequest request = new SimpleRequest();
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/xxx", "health");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unknown web service: api/xxx\"}]}");
  }

  @Test
  public void bad_action() throws Exception {
    InternalRequest request = new SimpleRequest();
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "xxx");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unknown action: api/system/xxx\"}]}");
  }

  @Test
  public void method_get_not_allowed() throws Exception {
    InternalRequest request = new SimpleRequest();
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "ping");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"HTTP method POST is required\"}]}");
  }

  @Test
  public void method_post_required() throws Exception {
    InternalRequest request = new SimpleRequest().setMethod("POST");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "ping");

    assertThat(response.stream().outputAsString()).isEqualTo("pong");
  }

  @Test
  public void required_parameter_is_not_set() throws Exception {
    InternalRequest request = new SimpleRequest();
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Parameter 'message' is missing\"}]}");
  }

  @Test
  public void optional_parameter_is_not_set() throws Exception {
    InternalRequest request = new SimpleRequest().setParam("message", "Hello World");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.stream().outputAsString()).isEqualTo("Hello World by -");
  }

  @Test
  public void optional_parameter_is_set() throws Exception {
    InternalRequest request = new SimpleRequest()
      .setParam("message", "Hello World")
      .setParam("author", "Marcel");
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.stream().outputAsString()).isEqualTo("Hello World by Marcel");
  }

  @Test
  public void internal_error() throws Exception {
    InternalRequest request = new SimpleRequest();
    ServletResponse response = new ServletResponse();
    engine.execute(request, response, "api/system", "fail");

    assertThat(response.stream().outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unexpected\"}]}");
    assertThat(response.stream().httpStatus()).isEqualTo(500);
    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.JSON);
  }

  static class SystemWebService implements WebService {
    @Override
    public void define(Context context) {
      NewController newController = context.newController("api/system");
      newController.newAction("health")
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
      newController.newAction("ping")
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
      newController.newAction("fail")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            throw new IllegalStateException("Unexpected");
          }
        });
      newController.newAction("alive")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          response.noContent();
        }
      });

      // parameter "message" is required but not "author"
      newController.newAction("print")
        .newParam("message", "required message")
        .newParam("author", "optional author")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) {
            try {
              IOUtils.write(
              request.mandatoryParam("message") + " by " + request.param("author", "-"), response.stream().output());
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
          }
        });
      newController.done();
    }
  }
}
