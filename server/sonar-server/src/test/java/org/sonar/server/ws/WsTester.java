/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.Maps;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.internal.PartImpl;
import org.sonar.api.server.ws.internal.ValidatingRequest;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.utils.text.XmlWriter;
import org.sonar.server.ws.WsTester.TestResponse.TestStream;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.ws.RequestVerifier.verifyRequest;

/**
 * @since 4.2
 * @deprecated use {@link WsActionTester} in preference to this class
 */
@Deprecated
public class WsTester {

  public static class TestRequest extends ValidatingRequest {

    private final String method;
    private String path;
    private String mediaType = MediaTypes.JSON;

    private Map<String, String> params = Maps.newHashMap();
    private Map<String, String> headers = Maps.newHashMap();
    private final Map<String, Part> parts = Maps.newHashMap();

    private TestRequest(String method) {
      this.method = method;
    }

    @Override
    public String method() {
      return method;
    }

    @Override
    public String getMediaType() {
      return mediaType;
    }

    public TestRequest setMediaType(String s) {
      this.mediaType = s;
      return this;
    }

    @Override
    public boolean hasParam(String key) {
      return params.keySet().contains(key);
    }

    @Override
    public Optional<String> header(String name) {
      return Optional.ofNullable(headers.get(name));
    }

    public TestRequest setHeader(String name, String value) {
      this.headers.put(requireNonNull(name), requireNonNull(value));
      return this;
    }

    @Override
    public String getPath() {
      return path;
    }

    public TestRequest setPath(String path) {
      this.path = path;
      return this;
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

    @Override
    protected List<String> readMultiParam(String key) {
      String value = params.get(key);
      return value == null ? emptyList() : singletonList(value);
    }

    @Override
    public Map<String, String[]> getParams() {
      return params.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new String[] {e.getValue()}));
    }

    @Override
    protected InputStream readInputStreamParam(String key) {
      String param = readParam(key);

      return param == null ? null : IOUtils.toInputStream(param);
    }

    @Override
    protected Part readPart(String key) {
      return parts.get(key);
    }

    public TestRequest setPart(String key, InputStream input, String fileName) {
      parts.put(key, new PartImpl(input, fileName));
      return this;
    }

    public Result execute() throws Exception {
      TestResponse response = new TestResponse();
      verifyRequest(action(), this);
      action().handler().handle(this, response);
      return new Result(response);
    }
  }

  public static class TestResponse implements Response {

    private TestStream stream;

    private Map<String, String> headers = Maps.newHashMap();

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
      return JsonWriter.of(new OutputStreamWriter(output, StandardCharsets.UTF_8));
    }

    @Override
    public XmlWriter newXmlWriter() {
      return XmlWriter.of(new OutputStreamWriter(output, StandardCharsets.UTF_8));
    }

    @Override
    public Stream stream() {
      if (stream == null) {
        stream = new TestStream();
      }
      return stream;
    }

    @Override
    public Response noContent() {
      stream().setStatus(HttpURLConnection.HTTP_NO_CONTENT);
      IOUtils.closeQuietly(output);
      return this;
    }

    public String outputAsString() {
      return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public Response setHeader(String name, String value) {
      headers.put(name, value);
      return this;
    }

    @Override
    public Collection<String> getHeaderNames() {
      return headers.keySet();
    }

    @Override
    public String getHeader(String name) {
      return headers.get(name);
    }
  }

  public static class Result {
    private final TestResponse response;

    private Result(TestResponse response) {
      this.response = response;
    }

    public Result assertNoContent() {
      return assertStatus(HttpURLConnection.HTTP_NO_CONTENT);
    }

    public String outputAsString() {
      return new String(response.output.toByteArray(), StandardCharsets.UTF_8);
    }

    public byte[] output() {
      return response.output.toByteArray();
    }

    public Result assertJson(String expectedJson) {
      String json = outputAsString();
      JsonAssert.assertJson(json).isSimilarTo(expectedJson);
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
    public Result assertJson(Class clazz, String expectedJsonFilename) {
      String path = clazz.getSimpleName() + "/" + expectedJsonFilename;
      URL url = clazz.getResource(path);
      if (url == null) {
        throw new IllegalStateException("Cannot find " + path);
      }
      String json = outputAsString();
      JsonAssert.assertJson(json).isSimilarTo(url);
      return this;
    }

    public Result assertNotModified() {
      return assertStatus(HttpURLConnection.HTTP_NOT_MODIFIED);
    }

    public Result assertStatus(int httpStatus) {
      assertThat(((TestStream) response.stream()).status()).isEqualTo(httpStatus);
      return this;
    }

    public Result assertHeader(String name, String value) {
      assertThat(response.getHeader(name)).isEqualTo(value);
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
