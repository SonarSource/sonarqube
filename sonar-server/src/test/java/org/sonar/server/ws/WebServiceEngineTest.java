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
import org.sonar.api.server.ws.*;

import static org.fest.assertions.Assertions.assertThat;

public class WebServiceEngineTest {

  WebServiceEngine engine = new WebServiceEngine(new WebService[]{new SystemWebService()});

  @Before
  public void before() {
    engine.start();
  }

  @After
  public void after() {
    engine.stop();
  }

  @Test
  public void load_ws_definitions_at_startup() throws Exception {
    assertThat(engine.controllers()).hasSize(1);
    assertThat(engine.controllers().get(0).path()).isEqualTo("api/system");
  }

  @Test
  public void execute_request() throws Exception {
    Request request = new SimpleRequest();
    SimpleResponse response = new SimpleResponse();
    engine.execute(request, response, "api/system", "health");

    assertThat(response.outputAsString()).isEqualTo("good");
    assertThat(response.status()).isEqualTo(200);
  }

  @Test
  public void bad_controller() throws Exception {
    Request request = new SimpleRequest();
    SimpleResponse response = new SimpleResponse();
    engine.execute(request, response, "api/xxx", "health");

    assertThat(response.outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unknown web service: api/xxx\"}]}");
    assertThat(response.status()).isEqualTo(400);
  }

  @Test
  public void bad_action() throws Exception {
    Request request = new SimpleRequest();
    SimpleResponse response = new SimpleResponse();
    engine.execute(request, response, "api/system", "xxx");

    assertThat(response.outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unknown action: api/system/xxx\"}]}");
    assertThat(response.status()).isEqualTo(400);
  }

  @Test
  public void method_not_allowed() throws Exception {
    Request request = new SimpleRequest();
    SimpleResponse response = new SimpleResponse();
    engine.execute(request, response, "api/system", "ping");

    assertThat(response.status()).isEqualTo(405);
    assertThat(response.outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"HTTP method POST is required\"}]}");
  }

  @Test
  public void required_parameter_is_not_set() throws Exception {
    Request request = new SimpleRequest();
    SimpleResponse response = new SimpleResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Parameter 'message' is missing\"}]}");
  }

  @Test
  public void optional_parameter_is_not_set() throws Exception {
    Request request = new SimpleRequest().setParam("message", "Hello World");
    SimpleResponse response = new SimpleResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.outputAsString()).isEqualTo("Hello World by -");
  }

  @Test
  public void optional_parameter_is_set() throws Exception {
    Request request = new SimpleRequest()
      .setParam("message", "Hello World")
      .setParam("author", "Marcel");
    SimpleResponse response = new SimpleResponse();
    engine.execute(request, response, "api/system", "print");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.outputAsString()).isEqualTo("Hello World by Marcel");
  }

  @Test
  public void internal_error() throws Exception {
    Request request = new SimpleRequest();
    SimpleResponse response = new SimpleResponse();
    engine.execute(request, response, "api/system", "fail");

    assertThat(response.status()).isEqualTo(500);
    assertThat(response.outputAsString()).isEqualTo("{\"errors\":[{\"msg\":\"Unexpected\"}]}");
  }

  static class SystemWebService implements WebService {
    @Override
    public void define(Context context) {
      NewController newController = context.newController("api/system");
      newController.newAction("health")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) throws Exception {
            response.stream().write("good".getBytes());
          }
        });
      newController.newAction("ping")
        .setPost(true)
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) throws Exception {
            response.stream().write("pong".getBytes());
          }
        });
      newController.newAction("fail")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) throws Exception {
            throw new IllegalStateException("Unexpected");
          }
        });

      // parameter "message" is required but not "author"
      newController.newAction("print")
        .newParam("message", "required message")
        .newParam("author", "optional author")
        .setHandler(new RequestHandler() {
          @Override
          public void handle(Request request, Response response) throws Exception {
            IOUtils.write(
              request.requiredParam("message") + " by " + request.param("author", "-"), response.stream());
          }
        });
      newController.done();
    }
  }
}
