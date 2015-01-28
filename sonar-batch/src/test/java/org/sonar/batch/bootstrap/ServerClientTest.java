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
package org.sonar.batch.bootstrap;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerClientTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  MockHttpServer server = null;
  BootstrapProperties bootstrapProps = mock(BootstrapProperties.class);

  @After
  public void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void should_remove_url_ending_slash() throws Exception {
    BootstrapProperties settings = mock(BootstrapProperties.class);
    when(settings.property("sonar.host.url")).thenReturn("http://localhost:8080/sonar/");

    ServerClient client = new ServerClient(settings, new EnvironmentInformation("Junit", "4"));

    assertThat(client.getURL()).isEqualTo("http://localhost:8080/sonar");
  }

  @Test
  public void should_request_url() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseData("this is the content");

    assertThat(newServerClient().request("/foo")).isEqualTo("this is the content");
  }

  @Test
  public void should_escape_html_from_url() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseData("this is the content");

    assertThat(newServerClient().request("/<foo>")).isEqualTo("this is the content");
  }

  @Test
  public void should_download_file() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseData("this is the content");

    File file = temp.newFile();
    newServerClient().download("/foo", file);
    assertThat(Files.toString(file, Charsets.UTF_8)).isEqualTo("this is the content");
  }

  @Test
  public void should_fail_if_unauthorized_with_no_login_password() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseStatus(401);

    thrown.expectMessage("Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");
    newServerClient().request("/foo");
  }

  @Test
  public void should_fail_if_unauthorized_with_login_password_provided() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseStatus(401);

    when(bootstrapProps.property(eq("sonar.login"))).thenReturn("login");
    when(bootstrapProps.property(eq("sonar.password"))).thenReturn("password");

    thrown.expectMessage("Not authorized. Please check the properties sonar.login and sonar.password");
    newServerClient().request("/foo");
  }

  @Test
  public void should_display_json_error_when_403() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseStatus(403);
    server.setMockResponseData("{\"errors\":[{\"msg\":\"Insufficient privileges\"}]}");

    thrown.expectMessage("Insufficient privileges");
    newServerClient().request("/foo");
  }

  @Test
  public void should_fail_if_error() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseStatus(500);

    thrown.expectMessage("Fail to execute request [code=500, url=http://localhost:" + server.getPort() + "/foo]");
    newServerClient().request("/foo");
  }

  @Test
  public void testEncode() {
    assertThat(ServerClient.encodeForUrl("my value")).isEqualTo("my+value");
  }

  private ServerClient newServerClient() {
    when(bootstrapProps.property("sonar.host.url")).thenReturn("http://localhost:" + server.getPort());
    return new ServerClient(bootstrapProps, new EnvironmentInformation("Junit", "4"));
  }

  static class MockHttpServer {
    private Server server;
    private String responseBody;
    private String requestBody;
    private String mockResponseData;
    private int mockResponseStatus = SC_OK;

    public void start() throws Exception {
      server = new Server(0);
      server.setHandler(getMockHandler());
      server.start();
    }

    /**
     * Creates an {@link org.mortbay.jetty.handler.AbstractHandler handler} returning an arbitrary String as a response.
     *
     * @return never <code>null</code>.
     */
    public Handler getMockHandler() {
      Handler handler = new AbstractHandler() {

        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
          setResponseBody(getMockResponseData());
          setRequestBody(IOUtils.toString(baseRequest.getInputStream()));
          response.setStatus(mockResponseStatus);
          response.setContentType("text/xml;charset=utf-8");
          write(getResponseBody(), response.getOutputStream());
          baseRequest.setHandled(true);
        }
      };
      return handler;
    }

    public void stop() {
      try {
        if (server != null) {
          server.stop();
        }
      } catch (Exception e) {
        throw new IllegalStateException("Fail to stop HTTP server", e);
      }
    }

    public String getResponseBody() {
      return responseBody;
    }

    public void setResponseBody(String responseBody) {
      this.responseBody = responseBody;
    }

    public String getRequestBody() {
      return requestBody;
    }

    public void setRequestBody(String requestBody) {
      this.requestBody = requestBody;
    }

    public void setMockResponseStatus(int status) {
      this.mockResponseStatus = status;
    }

    public String getMockResponseData() {
      return mockResponseData;
    }

    public void setMockResponseData(String mockResponseData) {
      this.mockResponseData = mockResponseData;
    }

    public int getPort() {
      return server.getConnectors()[0].getLocalPort();
    }

  }

}
