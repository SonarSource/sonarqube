/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.app;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RootContextServletTest {

  private RootContextServlet underTest;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private ServletConfig config;

  @BeforeEach
  void setUp() {
    underTest = new RootContextServlet();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    config = mock(ServletConfig.class);
    when(request.getRequestURI()).thenReturn("/some/page");
  }

  @ParameterizedTest
  @MethodSource("webContextParameters")
  void init_shouldSetWebContextAndRedirect(String webContext, String expectedRedirect) throws IOException {
    when(config.getInitParameter("webContext")).thenReturn(webContext);

    underTest.init(config);
    underTest.doGet(request, response);

    verify(response).sendRedirect(expectedRedirect);
  }

  @Test
  void doPost_whenCalled_shouldDelegateToDoGet() throws IOException {
    when(config.getInitParameter("webContext")).thenReturn("/sonarqube");
    underTest.init(config);

    underTest.doPost(request, response);

    verify(response).sendRedirect("/sonarqube/not-found");
  }

  @Test
  void doHead_whenCalled_shouldReturn404Status() {
    underTest.doHead(request, response);

    verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  void doHead_whenCalledWithoutInit_shouldReturn404Status() {
    RootContextServlet servlet = new RootContextServlet();

    servlet.doHead(request, response);

    verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  void doGet_whenSendRedirectThrowsIOException_shouldLogErrorAndSetInternalServerError() throws IOException {
    when(config.getInitParameter("webContext")).thenReturn("/sonarqube");
    underTest.init(config);

    doThrow(new IOException("Network error")).when(response).sendRedirect(anyString());

    underTest.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  void doPost_whenSendRedirectThrowsIOException_shouldLogErrorAndSetInternalServerError() throws IOException {
    when(config.getInitParameter("webContext")).thenReturn("/sonarqube");
    underTest.init(config);

    doThrow(new IOException("Network error")).when(response).sendRedirect(anyString());

    underTest.doPost(request, response);

    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  void constructor_shouldInitializeWithDefaultWebContext() throws IOException {
    RootContextServlet servlet = new RootContextServlet();

    servlet.doGet(request, response);

    verify(response).sendRedirect("/not-found");
  }

  @Test
  void doGet_whenAuthorizationServerMetadataOfWebContextIsRequested_shouldForwardToWebapp() throws Exception {
    RequestDispatcher dispatcher = mockWebapp("/.well-known/oauth-authorization-server/sonarqube");

    underTest.doGet(request, response);

    verify(dispatcher).forward(request, response);
    verify(response, never()).sendRedirect(anyString());
  }

  @Test
  void doHead_whenAuthorizationServerMetadataOfWebContextIsRequested_shouldForwardToWebapp() throws Exception {
    RequestDispatcher dispatcher = mockWebapp("/.well-known/oauth-authorization-server/sonarqube");

    underTest.doHead(request, response);

    verify(dispatcher).forward(request, response);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/.well-known/oauth-authorization-server",
    "/.well-known/openid-configuration/sonarqube",
    "/.well-known/oauth-authorization-server/",
    "/.well-known/oauth-authorization-server/../../robots.txt",
    "/.well-known/oauth-authorization-server/sonarqube/../../../api/system/status",
    "/.well-known/oauth-authorization-server/%2e%2e/%2e%2e/api/system/status",
    "/.well-known/oauth-authorization-server/./sonarqube",
    "/.well-known/oauth-authorization-server/.hidden",
    "/.well-known/oauth-authorization-server/sonarqube;jsessionid=1",
    "/.well-known/oauth-authorization-server/sonar%2Fqube",
    "/.well-known/oauth-authorization-server//sonarqube",
    "/.well-known/oauth-protected-resource/sonarqube/mcp"
  })
  void doGet_whenOtherWellKnownResourceIsRequested_shouldReturn404Status(String path) throws IOException {
    when(config.getInitParameter("webContext")).thenReturn("/sonarqube");
    underTest.init(config);
    when(request.getRequestURI()).thenReturn(path);

    underTest.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    verify(response, never()).sendRedirect(anyString());
    verify(request, never()).getServletContext();
  }

  @Test
  void doGet_whenIssuerPathHasSeveralSegments_shouldForwardToWebapp() throws Exception {
    RequestDispatcher dispatcher = mockWebapp("/.well-known/oauth-authorization-server/tools/sonar.qube-1");

    underTest.doGet(request, response);

    verify(dispatcher).forward(request, response);
  }

  @Test
  void doGet_whenIssuerPathDiffersFromWebContext_shouldForwardToWebapp() throws Exception {
    RequestDispatcher dispatcher = mockWebapp("/.well-known/oauth-authorization-server/public-path");

    underTest.doGet(request, response);

    verify(dispatcher).forward(request, response);
  }

  @Test
  void doGet_whenWebappIsNotReachable_shouldReturn503Status() {
    when(config.getInitParameter("webContext")).thenReturn("/sonarqube");
    underTest.init(config);
    when(request.getRequestURI()).thenReturn("/.well-known/oauth-authorization-server/sonarqube");
    ServletContext rootContext = mock(ServletContext.class);
    when(request.getServletContext()).thenReturn(rootContext);

    underTest.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
  }

  @Test
  void doGet_whenWebContextResolvesToRootContext_shouldReturn503StatusWithoutForwarding() {
    when(config.getInitParameter("webContext")).thenReturn("/sonarqube");
    underTest.init(config);
    String path = "/.well-known/oauth-authorization-server/sonarqube";
    when(request.getRequestURI()).thenReturn(path);
    ServletContext rootContext = mock(ServletContext.class);
    RequestDispatcher loopingDispatcher = mock(RequestDispatcher.class);
    when(request.getServletContext()).thenReturn(rootContext);
    when(rootContext.getContext("/sonarqube")).thenReturn(rootContext);
    when(rootContext.getContextPath()).thenReturn("");
    when(rootContext.getRequestDispatcher(path)).thenReturn(loopingDispatcher);

    underTest.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    verifyNoInteractions(loopingDispatcher);
  }

  @Test
  void doGet_whenForwardFails_shouldReturnInternalServerError() throws Exception {
    RequestDispatcher dispatcher = mockWebapp("/.well-known/oauth-authorization-server/sonarqube");
    doThrow(new ServletException("boom")).when(dispatcher).forward(request, response);

    underTest.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  private RequestDispatcher mockWebapp(String path) {
    when(config.getInitParameter("webContext")).thenReturn("/sonarqube");
    underTest.init(config);
    when(request.getRequestURI()).thenReturn(path);
    ServletContext rootContext = mock(ServletContext.class);
    ServletContext webapp = mock(ServletContext.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
    when(request.getServletContext()).thenReturn(rootContext);
    when(rootContext.getContext("/sonarqube")).thenReturn(webapp);
    when(webapp.getContextPath()).thenReturn("/sonarqube");
    when(webapp.getRequestDispatcher(path)).thenReturn(dispatcher);
    return dispatcher;
  }

  private static Stream<Arguments> webContextParameters() {
    return Stream.of(
      arguments("/sonarqube", "/sonarqube/not-found"),
      arguments(null, "/not-found"),
      arguments("", "/not-found")
    );
  }
}
