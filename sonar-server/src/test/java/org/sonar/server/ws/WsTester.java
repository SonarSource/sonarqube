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
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.utils.text.XmlWriter;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Map;

/**
 * @since 4.2
 */
public class WsTester {

  public static class TestRequest extends InternalRequest {

    private final String method;
    private Map<String, String> params = Maps.newHashMap();

    private TestRequest(String method) {
      this.method = method;
    }

    @Override
    public String method() {
      return method;
    }

    public TestRequest setParams(Map<String, String> m) {
      this.params = m;
      return this;
    }

    public TestRequest setParam(String key, @Nullable String value) {
      if (value != null) {
        params.put(key, value);
      }
      return this;
    }

    @Override
    protected String readParam(String key) {
      return params.get(key);
    }

    public Result execute() throws Exception {
      TestResponse response = new TestResponse();
      action().handler().handle(this, response);
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
     * @param expectedJsonFilename name of the file containing the expected JSON
     */
    public Result assertJson(Class clazz, String expectedJsonFilename) throws Exception {
      return assertJson(clazz, expectedJsonFilename, true);
    }

    public Result assertJson(Class clazz, String expectedJsonFilename, boolean strict) throws Exception {
      String path = clazz.getSimpleName() + "/" + expectedJsonFilename;
      URL url = clazz.getResource(path);
      if (url == null) {
        throw new IllegalStateException("Cannot find " + path);
      }
      String json = outputAsString();
      System.out.println("GOT " + json);
      JSONAssert.assertEquals(IOUtils.toString(url), json, strict);
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
  public WebService.Controller controller(String key) {
    return context.controller(key);
  }

  @CheckForNull
  public WebService.Action action(String controllerKey, String actionKey) {
    WebService.Controller controller = context.controller(controllerKey);
    if (controller != null) {
      return controller.action(actionKey);
    }
    return null;
  }

  public TestRequest newGetRequest(String controllerKey, String actionKey) {
    return newRequest(controllerKey, actionKey, "GET");
  }

  public TestRequest newPostRequest(String controllerKey, String actionKey) {
    return newRequest(controllerKey, actionKey, "POST");
  }

  private TestRequest newRequest(String controllerKey, String actionKey, String method) {
    TestRequest request = new TestRequest(method);
    WebService.Controller controller = context.controller(controllerKey);
    if (controller == null) {
      throw new IllegalArgumentException(
        String.format("Controller '%s' is unknown, did you forget to call NewController.done()?", controllerKey));
    }
    WebService.Action action = controller.action(actionKey);
    if (action == null) {
      throw new IllegalArgumentException(
        String.format("Action '%s' not found on controller '%s'.", actionKey, controllerKey));
    }
    request.setAction(action);
    return request;
  }
}
