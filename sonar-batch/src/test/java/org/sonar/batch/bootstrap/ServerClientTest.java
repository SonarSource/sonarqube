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

import org.sonar.batch.util.BatchUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerClientTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private MockHttpServer server = null;
  private GlobalProperties bootstrapProps = mock(GlobalProperties.class);

  @After
  public void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void should_remove_url_ending_slash() {
    GlobalProperties settings = mock(GlobalProperties.class);
    when(settings.property("sonar.host.url")).thenReturn("http://localhost:8080/sonar/");
    ServerClient client = new ServerClient(settings, new EnvironmentInformation("Junit", "4"));

    assertThat(client.getURL()).isEqualTo("http://localhost:8080/sonar");
  }

  @Test
  public void should_request_url() throws Exception {
    startServer(null, "this is the content");
    assertThat(newServerClient().downloadString("/foo")).isEqualTo("this is the content");
  }

  @Test
  public void should_escape_html_from_url() throws Exception {
    startServer(null, "this is the content");
    assertThat(newServerClient().downloadString("/<foo>")).isEqualTo("this is the content");
  }

  @Test
  public void should_download_file() throws Exception {
    startServer(null, "this is the content");
    File file = temp.newFile();
    newServerClient().download("/foo", file);
    assertThat(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).isEqualTo("this is the content");
  }

  @Test
  public void should_fail_if_unauthorized_with_no_login_password() throws Exception {
    startServer(401, null);
    thrown.expectMessage("Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");
    newServerClient().downloadString("/foo");
  }

  @Test
  public void should_fail_if_unauthorized_with_login_password_provided() throws Exception {
    startServer(401, null);

    when(bootstrapProps.property(eq("sonar.login"))).thenReturn("login");
    when(bootstrapProps.property(eq("sonar.password"))).thenReturn("password");

    thrown.expectMessage("Not authorized. Please check the properties sonar.login and sonar.password");
    newServerClient().downloadString("/foo");
  }

  @Test
  public void should_display_json_error_when_403() throws Exception {
    startServer(403, "{\"errors\":[{\"msg\":\"Insufficient privileges\"}]}");
    thrown.expectMessage("Insufficient privileges");
    newServerClient().downloadString("/foo");
  }

  @Test
  public void should_fail_if_error() throws Exception {
    startServer(500, null);
    thrown.expectMessage("Fail to execute request [code=500, url=http://localhost:" + server.getPort() + "/foo]");
    newServerClient().downloadString("/foo");
  }

  @Test
  public void string_encode() {
    assertThat(BatchUtils.encodeForUrl("my value")).isEqualTo("my+value");
  }

  private ServerClient newServerClient() {
    when(bootstrapProps.property("sonar.host.url")).thenReturn("http://localhost:" + server.getPort());
    return new ServerClient(bootstrapProps, new EnvironmentInformation("Junit", "4"));
  }

  private void startServer(Integer responseStatus, String responseData) throws Exception {
    server = new MockHttpServer();
    server.start();
    
    if (responseStatus != null) {
      server.setMockResponseStatus(responseStatus);
    }
    if (responseData != null) {
      server.setMockResponseData(responseData);
    }
  }
}
