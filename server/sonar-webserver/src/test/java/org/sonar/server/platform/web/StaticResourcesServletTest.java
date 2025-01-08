/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaticResourcesServletTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private Server jetty;

  private final PluginRepository pluginRepository = mock(PluginRepository.class);
  private final CoreExtensionRepository coreExtensionRepository = mock(CoreExtensionRepository.class);
  private final TestSystem system = new TestSystem(pluginRepository, coreExtensionRepository);

  @BeforeEach
  void setUp() throws Exception {
    logTester.setLevel(Level.TRACE);
    jetty = new Server(InetSocketAddress.createUnresolved("localhost", 0));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    ServletHolder servletHolder = new ServletHolder(new StaticResourcesServlet(system));
    context.addServlet(servletHolder, "/static/*");
    jetty.setHandler(context);
    jetty.start();
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (jetty != null) {
      jetty.stop();
    }
  }

  private HttpResponse<String> callAndStop(String path) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(jetty.getURI().resolve(URI.create(path)))
      .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    jetty.stop();
    return response;
  }

  @Test
  void return_content_if_exists_in_installed_plugin() throws Exception {
    system.pluginStream = IOUtils.toInputStream("bar", Charset.defaultCharset());
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.txt");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("bar");
    assertThat(system.pluginResource).isEqualTo("static/foo.txt");
  }

  @Test
  void return_content_of_folder_of_installed_plugin() throws Exception {
    system.pluginStream = IOUtils.toInputStream("bar", Charset.defaultCharset());
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo/bar.txt");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("bar");
    assertThat(system.pluginResource).isEqualTo("static/foo/bar.txt");
  }

  @Test
  void return_content_of_folder_of_installed_core_extension() throws Exception {
    system.coreExtensionStream = IOUtils.toInputStream("bar", Charset.defaultCharset());
    when(coreExtensionRepository.isInstalled("coreext")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/coreext/foo/bar.txt");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("bar");
    assertThat(system.coreExtensionResource).isEqualTo("static/foo/bar.txt");
  }

  @Test
  void return_content_of_folder_of_installed_core_extension_over_installed_plugin_in_case_of_key_conflict() throws Exception {
    system.coreExtensionStream = IOUtils.toInputStream("bar of plugin", Charset.defaultCharset());
    when(coreExtensionRepository.isInstalled("samekey")).thenReturn(true);
    system.coreExtensionStream = IOUtils.toInputStream("bar of core extension", Charset.defaultCharset());
    when(coreExtensionRepository.isInstalled("samekey")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/samekey/foo/bar.txt");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("bar of core extension");
    assertThat(system.pluginResource).isNull();
    assertThat(system.coreExtensionResource).isEqualTo("static/foo/bar.txt");
  }

  @Test
  void mime_type_is_set_on_response() throws Exception {
    system.pluginStream = IOUtils.toInputStream("bar", Charset.defaultCharset());
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    Optional<String> header = response.headers().firstValue("Content-Type");
    assertThat(header).hasValue("text/css");
    assertThat(response.body()).isEqualTo("bar");
  }

  @Test
  void return_404_if_resource_not_found_in_installed_plugin() throws Exception {
    system.pluginStream = null;
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  void return_404_if_plugin_does_not_exist() throws Exception {
    system.pluginStream = null;
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(false);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  void return_resource_if_exists_in_requested_plugin() throws Exception {
    system.pluginStream = IOUtils.toInputStream("bar", Charset.defaultCharset());
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);
    when(pluginRepository.getPluginInfo("myplugin")).thenReturn(new PluginInfo("myplugin"));

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("bar");
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  void do_not_fail_nor_log_ERROR_when_response_is_already_committed_and_plugin_does_not_exist() throws Exception {
    system.pluginStream = null;
    system.isCommitted = true;
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(false);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.TRACE)).contains("Response is committed. Cannot send error response code 404");
  }

  @Test
  void do_not_fail_nor_log_ERROR_when_sendError_throws_IOException_and_plugin_does_not_exist() throws Exception {
    system.sendErrorException = new IOException("Simulating sendError throwing IOException");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(false);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.TRACE)).contains("Failed to send error code 404: {}");
  }

  @Test
  void do_not_fail_nor_log_ERROR_when_response_is_already_committed_and_resource_does_not_exist_in_installed_plugin() throws Exception {
    system.isCommitted = true;
    system.pluginStream = null;
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.TRACE)).contains("Response is committed. Cannot send error response code 404");
  }

  @Test
  void do_not_fail_nor_log_not_attempt_to_send_error_if_ClientAbortException_is_raised() throws Exception {
    system.pluginStreamException = new ClientAbortException("Simulating ClientAbortException");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.getLogs(Level.TRACE)).extracting(LogAndArguments::getFormattedMsg).contains(
      "Client canceled loading resource [static/foo.css] from plugin [myplugin]: {}");
  }

  @Test
  void do_not_fail_when_response_is_committed_after_other_error() throws Exception {
    system.isCommitted = true;
    system.pluginStreamException = new RuntimeException("Simulating a error");
    when(pluginRepository.hasPlugin("myplugin")).thenReturn(true);

    HttpResponse<String> response = callAndStop("/static/myplugin/foo.css");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(logTester.logs(Level.ERROR)).contains("Unable to load resource [static/foo.css] from plugin [myplugin]");
  }

  private static class TestSystem extends StaticResourcesServlet.System {
    private final PluginRepository pluginRepository;
    private final CoreExtensionRepository coreExtensionRepository;
    @Nullable
    private InputStream pluginStream;
    private Exception pluginStreamException = null;
    @Nullable
    private String pluginResource;
    @Nullable
    private InputStream coreExtensionStream;
    private Exception coreExtensionStreamException = null;
    private String coreExtensionResource;
    private boolean isCommitted = false;
    private IOException sendErrorException = null;

    TestSystem(PluginRepository pluginRepository, CoreExtensionRepository coreExtensionRepository) {
      this.pluginRepository = pluginRepository;
      this.coreExtensionRepository = coreExtensionRepository;
    }

    @Override
    PluginRepository getPluginRepository() {
      return pluginRepository;
    }

    @Override
    CoreExtensionRepository getCoreExtensionRepository() {
      return this.coreExtensionRepository;
    }

    @CheckForNull
    @Override
    InputStream openPluginResourceStream(String pluginKey, String resource, PluginRepository pluginRepository) throws Exception {
      pluginResource = resource;
      if (pluginStreamException != null) {
        throw pluginStreamException;
      }
      return pluginStream;
    }

    @CheckForNull
    @Override
    InputStream openCoreExtensionResourceStream(String resource) throws Exception {
      coreExtensionResource = resource;
      if (coreExtensionStreamException != null) {
        throw coreExtensionStreamException;
      }
      return coreExtensionStream;
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
