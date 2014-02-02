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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.utils.text.XmlWriter;

import javax.annotation.CheckForNull;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.2
 */
public class WsTester {

  public static class TestRequest extends Request {

    private final WebService.Controller controller;
    private final WebService.Action action;
    private String method = "GET";
    private Map<String, String> params = new HashMap<String, String>();

    private TestRequest(WebService.Controller controller, WebService.Action action) {
      this.controller = controller;
      this.action = action;
    }

    @Override
    public WebService.Action action() {
      return action;
    }

    @Override
    public String method() {
      return method;
    }

    public TestRequest setMethod(String s) {
      this.method = s;
      return this;
    }

    public TestRequest setParams(Map<String, String> m) {
      this.params = m;
      return this;
    }

    public TestRequest setParam(String key, @CheckForNull String value) {
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

    public Result execute() throws Exception {
      TestResponse response = new TestResponse();
      action.handler().handle(this, response);
      return new Result(response);
    }
  }

  public static class TestResponse implements Response {

    public class TestStream implements Response.Stream {
      private String mediaType;
      private int status;

      @CheckForNull
      public String mediaType() {
        return mediaType;
      }

      public int status() {
        return status;
      }

      @Override
      public Response.Stream setMediaType(String s) {
        this.mediaType = s;
        return this;
      }

      @Override
      public Response.Stream setStatus(int i) {
        this.status = i;
        return this;
      }

      @Override
      public OutputStream output() {
        return output;
      }
    }

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Override
    public JsonWriter newJsonWriter() {
      return JsonWriter.of(new OutputStreamWriter(output, Charsets.UTF_8));
    }

    @Override
    public XmlWriter newXmlWriter() {
      return XmlWriter.of(new OutputStreamWriter(output, Charsets.UTF_8));
    }

    @Override
    public Stream stream() {
      return new TestStream();
    }


    @Override
    public Response noContent() {
      IOUtils.closeQuietly(output);
      return this;
    }
  }


  public static class Result {
    private final TestResponse response;

    private Result(TestResponse response) {
      this.response = response;
    }

    public Result assertNoContent() {
      //FIXME
      return this;
    }

    public String outputAsString() {
      return new String(response.output.toByteArray(), Charsets.UTF_8);
    }

    public Result assertJson(String expectedJson) throws Exception {
      String json = outputAsString();
      JSONAssert.assertEquals(expectedJson, json, true);
      return this;
    }

    /**
     * Compares JSON response with JSON file available in classpath. For example if class
     * is org.foo.BarTest and filename is index.json, then file must be located
     * at src/test/resources/org/foo/BarTest/index.json.
     *
     * @param clazz                the test class
     * @param jsonResourceFilename name of the file containing the expected JSON
     */
    public Result assertJson(Class clazz, String expectedJsonFilename) throws Exception {
      String path = clazz.getSimpleName() + "/" + expectedJsonFilename;
      URL url = clazz.getResource(path);
      if (url == null) {
        throw new IllegalStateException("Cannot find " + path);
      }
      String json = outputAsString();
      JSONAssert.assertEquals(IOUtils.toString(url), json, true);
      return this;
    }
  }

  private final WebService.Context context = new WebService.Context();

  public WsTester(WebService... webServices) {
    for (WebService webService : webServices) {
      webService.define(context);
    }
  }

  public WebService.Context context() {
    return context;
  }

  @CheckForNull
  public WebService.Controller controller(String path) {
    return context.controller(path);
  }

  public TestRequest newRequest(String actionKey) {
    if (context.controllers().size() != 1) {
      throw new IllegalStateException("The method newRequest(String) requires to define one, and only one, controller");
    }
    WebService.Controller controller = context.controllers().get(0);
    WebService.Action action = controller.action(actionKey);
    if (action == null) {
      throw new IllegalArgumentException("Action not found: " + actionKey);
    }
    return new TestRequest(controller, action);
  }

  public TestRequest newRequest(String controllerPath, String actionKey) {
    WebService.Controller controller = context.controller(controllerPath);
    WebService.Action action = controller.action(actionKey);
    return new TestRequest(controller, action);
  }
}
