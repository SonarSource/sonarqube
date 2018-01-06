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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaticResourcesServletTest {

  @Rule
  public LogTester logTester = new LogTester();

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private ComponentContainer componentContainer = mock(ComponentContainer.class);
  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private StaticResourcesServlet underTest = new StaticResourcesServlet() {
    @Override
    protected ComponentContainer getContainer() {
      return componentContainer;
    }
  };

  @Test
  public void shouldDeterminePluginKey() {
    mockRequest("myplugin", "image.png");
    assertThat(underTest.getPluginKey(request)).isEqualTo("myplugin");

    mockRequest("myplugin", "images/image.png");
    assertThat(underTest.getPluginKey(request)).isEqualTo("myplugin");

    mockRequest("myplugin", "");
    assertThat(underTest.getPluginKey(request)).isEqualTo("myplugin");
  }

  @Test
  public void shouldDetermineResourcePath() {
    mockRequest("myplugin", "image.png");
    assertThat(underTest.getResourcePath(request)).isEqualTo("static/image.png");

    mockRequest("myplugin", "images/image.png");
    assertThat(underTest.getResourcePath(request)).isEqualTo("static/images/image.png");

    mockRequest("myplugin", "");
    assertThat(underTest.getResourcePath(request)).isEqualTo("static/");
  }

  @Test
  public void completeMimeType() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    underTest.completeContentType(response, "static/sqale/sqale.css");
    verify(response).setContentType("text/css");
  }

  @Test
  public void does_not_fail_nor_log_ERROR_when_response_is_already_committed_and_plugin_does_not_exist() throws ServletException, IOException {
    mockPluginForResourceDoesNotExist();
    mockSendErrorOnCommittedResponse();

    underTest.doGet(request, response);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).containsOnly("Response is committed. Cannot send error response code 404");
  }

  @Test
  public void does_not_fail_nor_log_ERROR_when_sendError_throws_IOException_and_plugin_does_not_exist() throws ServletException, IOException {
    mockPluginForResourceDoesNotExist();
    mockSendErrorThrowsIOException();

    underTest.doGet(request, response);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).containsOnly("Failed to send error code 404: java.io.IOException: Simulating sendError throwing IOException");
  }

  private void mockPluginForResourceDoesNotExist() {
    String pluginKey = "myplugin";
    mockRequest(pluginKey, "image.png");
    when(pluginRepository.hasPlugin(pluginKey)).thenReturn(false);
    when(componentContainer.getComponentByType(PluginRepository.class)).thenReturn(pluginRepository);
  }

  @Test
  public void does_not_fail_nor_log_ERROR_when_response_is_already_committed_and_plugin_exists_but_not_resource() throws ServletException, IOException {
    mockPluginExistsButNoResource();
    mockSendErrorOnCommittedResponse();

    underTest.doGet(request, response);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).containsOnly("Response is committed. Cannot send error response code 404");
  }

  @Test
  public void does_not_fail_nor_log_ERROR_when_sendError_throws_IOException_and_plugin_exists_but_not_resource() throws ServletException, IOException {
    mockPluginExistsButNoResource();
    mockSendErrorThrowsIOException();

    underTest.doGet(request, response);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).containsOnly("Failed to send error code 404: java.io.IOException: Simulating sendError throwing IOException");
  }

  private void mockPluginExistsButNoResource() {
    String pluginKey = "myplugin";
    mockRequest(pluginKey, "image.png");
    when(pluginRepository.hasPlugin(pluginKey)).thenReturn(true);
    when(pluginRepository.getPluginInstance(pluginKey)).thenReturn(mock(Plugin.class));
    when(componentContainer.getComponentByType(PluginRepository.class)).thenReturn(pluginRepository);
  }

  @Test
  public void does_not_fail_nor_log_not_attempt_to_send_error_if_ClientAbortException_is_raised() throws ServletException, IOException {
    String pluginKey = "myplugin";
    mockRequest(pluginKey, "foo.txt");
    when(pluginRepository.hasPlugin(pluginKey)).thenReturn(true);
    when(pluginRepository.getPluginInstance(pluginKey)).thenReturn(new TestPluginA());
    when(componentContainer.getComponentByType(PluginRepository.class)).thenReturn(pluginRepository);
    when(response.getOutputStream()).thenThrow(new ClientAbortException("Simulating ClientAbortException"));

    underTest.doGet(request, response);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).containsOnly("Client canceled loading resource [static/foo.txt] from plugin [myplugin]: org.apache.catalina.connector.ClientAbortException: Simulating ClientAbortException");
    verify(response, times(0)).sendError(anyInt());
  }

  @Test
  public void does_not_fail_when_response_is_committed_after_other_error() throws ServletException, IOException {
    mockRequest("myplugin", "image.png");
    when(componentContainer.getComponentByType(PluginRepository.class)).thenThrow(new RuntimeException("Simulating a error"));
    mockSendErrorOnCommittedResponse();

    underTest.doGet(request, response);

    assertThat(logTester.logs(LoggerLevel.ERROR)).containsOnly("Unable to load resource [static/image.png] from plugin [myplugin]");
  }

  @Test
  public void does_not_fail_when_sendError_throws_IOException_after_other_error() throws ServletException, IOException {
    mockRequest("myplugin", "image.png");
    when(componentContainer.getComponentByType(PluginRepository.class)).thenThrow(new RuntimeException("Simulating a error"));
    mockSendErrorThrowsIOException();

    underTest.doGet(request, response);

    assertThat(logTester.logs(LoggerLevel.ERROR)).containsOnly("Unable to load resource [static/image.png] from plugin [myplugin]");
  }

  private void mockSendErrorThrowsIOException() throws IOException {
    doThrow(new IOException("Simulating sendError throwing IOException")).when(response).sendError(anyInt());
  }

  private void mockSendErrorOnCommittedResponse() throws IOException {
    when(response.isCommitted()).thenReturn(true);
    doThrow(new IllegalStateException("Simulating sendError call when already committed")).when(response).sendError(anyInt());
  }

  private void mockRequest(String plugin, String resource) {
    when(request.getContextPath()).thenReturn("/");
    when(request.getServletPath()).thenReturn("static");
    when(request.getRequestURI()).thenReturn("/static/" + plugin + "/" + resource);
  }
}
