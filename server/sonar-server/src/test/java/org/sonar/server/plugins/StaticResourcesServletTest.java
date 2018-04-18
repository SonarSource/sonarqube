/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.plugins;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaticResourcesServletTest {

  @Rule
  public LogTester logTester = new LogTester();
  private Server jetty;

  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private TestSystem system = new TestSystem(pluginRepository);

  @Before
  public void setUp() throws Exception {
    jetty = new Server(0);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    ServletHolder servletHolder = new ServletHolder(new StaticResourcesServlet(system));
    context.addServlet(servletHolder, "/static/*");
    jetty.setHandler(context);
    jetty.start();
  }

  @After
  public void tearDown() throws Exception {
    if (jetty != null) {
      jetty.stop();
    }
  }

  private Response call(String path) throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder()
      .url(jetty.getURI().resolve(path).toString())
      .build();
    return client.newCall(request).execute();
  }

  @Test
  public void return_content_if_exists_in_installed_plugin() throws Exception {
    system.answer = IOUtils.toInputStream("bar");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    Response response = call("/static/myplugin/foo.txt");

    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body().string()).isEqualTo("bar");
    assertThat(system.calledResource).isEqualTo("static/foo.txt");
  }

  @Test
  public void return_content_of_folder_of_installed_plugin() throws Exception {
    system.answer = IOUtils.toInputStream("bar");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    Response response = call("/static/myplugin/foo/bar.txt");

    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body().string()).isEqualTo("bar");
    assertThat(system.calledResource).isEqualTo("static/foo/bar.txt");
  }

  @Test
  public void mime_type_is_set_on_response() throws Exception {
    system.answer = IOUtils.toInputStream("bar");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.header("Content-Type")).isEqualTo("text/css");
    assertThat(response.body().string()).isEqualTo("bar");
  }

  @Test
  public void return_404_if_resource_not_found_in_installed_plugin() throws Exception {
    system.answer = null;
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.code()).isEqualTo(404);
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void return_404_if_plugin_does_not_exist() throws Exception {
    system.answer = null;
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(false);

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.code()).isEqualTo(404);
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void return_resource_if_exists_in_requested_plugin() throws Exception {
    system.answer = IOUtils.toInputStream("bar");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);
    when(pluginRepository.getPluginInfo("myplugin")).thenReturn(new PluginInfo("myplugin"));

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body().string()).isEqualTo("bar");
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void do_not_fail_nor_log_ERROR_when_response_is_already_committed_and_plugin_does_not_exist() throws Exception {
    system.answer = null;
    system.isCommitted = true;
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(false);

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.code()).isEqualTo(200);
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).contains("Response is committed. Cannot send error response code 404");
  }

  @Test
  public void do_not_fail_nor_log_ERROR_when_sendError_throws_IOException_and_plugin_does_not_exist() throws Exception {
    system.sendErrorException = new IOException("Simulating sendError throwing IOException");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(false);

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.code()).isEqualTo(200);
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).contains("Failed to send error code 404: java.io.IOException: Simulating sendError throwing IOException");
  }

  @Test
  public void do_not_fail_nor_log_ERROR_when_response_is_already_committed_and_resource_does_not_exist_in_installed_plugin() throws Exception {
    system.isCommitted = true;
    system.answer = null;
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.code()).isEqualTo(200);
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).contains("Response is committed. Cannot send error response code 404");
  }

  @Test
  public void do_not_fail_nor_log_not_attempt_to_send_error_if_ClientAbortException_is_raised() throws Exception {
    system.answerException = new ClientAbortException("Simulating ClientAbortException");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.code()).isEqualTo(200);
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).contains(
      "Client canceled loading resource [static/foo.css] from plugin [myplugin]: org.apache.catalina.connector.ClientAbortException: Simulating ClientAbortException");
  }

  @Test
  public void do_not_fail_when_response_is_committed_after_other_error() throws Exception {
    system.isCommitted = true;
    system.answerException = new RuntimeException("Simulating a error");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    Response response = call("/static/myplugin/foo.css");

    assertThat(response.code()).isEqualTo(200);
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Unable to load resource [static/foo.css] from plugin [myplugin]");
  }

  private static class TestSystem extends StaticResourcesServlet.System {
    private final PluginRepository pluginRepository;
    @Nullable
    private InputStream answer;
    private Exception answerException = null;
    @Nullable
    private String calledResource;
    private boolean isCommitted = false;
    private IOException sendErrorException = null;

    TestSystem(PluginRepository pluginRepository) {
      this.pluginRepository = pluginRepository;
    }

    @Override
    PluginRepository getPluginRepository() {
      return pluginRepository;
    }

    @CheckForNull
    @Override
    InputStream openResourceStream(String pluginKey, String resource, PluginRepository pluginRepository) throws Exception {
      calledResource = resource;
      if (answerException != null) {
        throw answerException;
      }
      return answer;
    }

    @Override
    boolean isCommitted(HttpServletResponse response) {
      return isCommitted;
    }

    @Override
    void sendError(HttpServletResponse response, int error) throws IOException {
      if (sendErrorException != null) {
        throw sendErrorException;
      } else {
        super.sendError(response, error);
      }
    }
  }
}
