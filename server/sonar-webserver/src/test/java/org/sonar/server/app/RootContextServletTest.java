/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

  private static Stream<Arguments> webContextParameters() {
    return Stream.of(
      arguments("/sonarqube", "/sonarqube/not-found"),
      arguments(null, "/not-found"),
      arguments("", "/not-found")
    );
  }
}
