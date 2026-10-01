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
package org.sonar.server.platform.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.MappingMatch;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.LocalConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalHttpServletRequestTest {

  private final ServletContext servletContext = mock(ServletContext.class);
  private final LocalConnector.LocalRequest localRequest = mock(LocalConnector.LocalRequest.class);

  @Test
  void paths_shouldBeResolvedAgainstApiV2Servlet() {
    when(servletContext.getContextPath()).thenReturn("/sonarqube");
    when(localRequest.getPath()).thenReturn("api/v2/foo/bar");

    LocalHttpServletRequest underTest = new LocalHttpServletRequest(localRequest, servletContext);

    assertThat(underTest.getContextPath()).isEqualTo("/sonarqube");
    assertThat(underTest.getServletPath()).isEqualTo("/api/v2");
    assertThat(underTest.getPathInfo()).isEqualTo("/foo/bar");
    assertThat(underTest.getRequestURI()).isEqualTo("/sonarqube/api/v2/foo/bar");
    assertThat(underTest.getRequestURL()).hasToString("http://localhost/sonarqube/api/v2/foo/bar");
    HttpServletMapping mapping = underTest.getHttpServletMapping();
    assertThat(mapping.getMappingMatch()).isEqualTo(MappingMatch.PATH);
    assertThat(mapping.getPattern()).isEqualTo("/api/v2/*");
    assertThat(mapping.getMatchValue()).isEqualTo("foo/bar");
    assertThat(mapping.getServletName()).isEqualTo("app");
  }

  @Test
  void paths_whenNoContextPathAndLeadingSlash_shouldBeResolved() {
    when(localRequest.getPath()).thenReturn("/api/v2/foo");

    LocalHttpServletRequest underTest = new LocalHttpServletRequest(localRequest, servletContext);

    assertThat(underTest.getContextPath()).isEmpty();
    assertThat(underTest.getRequestURI()).isEqualTo("/api/v2/foo");
  }

  @Test
  void paths_whenTrailingSlash_shouldBeStripped() {
    when(localRequest.getPath()).thenReturn("api/v2/foo/");

    LocalHttpServletRequest underTest = new LocalHttpServletRequest(localRequest, servletContext);

    assertThat(underTest.getPathInfo()).isEqualTo("/foo");
    assertThat(underTest.getRequestURI()).isEqualTo("/api/v2/foo");
  }

  @Test
  void paths_whenApiV2Root_shouldKeepSlash() {
    when(localRequest.getPath()).thenReturn("api/v2/");

    assertThat(new LocalHttpServletRequest(localRequest, servletContext).getPathInfo()).isEqualTo("/");
  }

  @Test
  void parameters_shouldBeReadFromLocalRequest() {
    Map<String, String[]> params = new LinkedHashMap<>();
    params.put("q", new String[] {"a b"});
    params.put("tags", new String[] {"x", "y"});
    params.put("empty", new String[0]);
    when(localRequest.getPath()).thenReturn("api/v2/foo");
    when(localRequest.getParameterMap()).thenReturn(params);

    LocalHttpServletRequest underTest = new LocalHttpServletRequest(localRequest, servletContext);

    assertThat(underTest.getParameter("q")).isEqualTo("a b");
    assertThat(underTest.getParameter("empty")).isNull();
    assertThat(underTest.getParameter("missing")).isNull();
    assertThat(underTest.getParameterValues("tags")).containsExactly("x", "y");
    assertThat(Collections.list(underTest.getParameterNames())).containsExactly("q", "tags", "empty");
    assertThat(underTest.getParameterMap()).containsOnlyKeys("q", "tags", "empty");
    assertThat(underTest.getQueryString()).isEqualTo("q=a+b&tags=x&tags=y");
  }

  @Test
  void queryString_whenNoParameters_shouldBeNull() {
    when(localRequest.getPath()).thenReturn("api/v2/foo");

    assertThat(new LocalHttpServletRequest(localRequest, servletContext).getQueryString()).isNull();
  }

  @Test
  void headers_shouldBeReadFromLocalRequestAndAcceptShouldDefaultToMediaType() {
    when(localRequest.getPath()).thenReturn("api/v2/foo");
    when(localRequest.getMediaType()).thenReturn("application/json");
    when(localRequest.getHeader("Accept")).thenReturn(Optional.empty());
    when(localRequest.getHeader("X-Count")).thenReturn(Optional.of("3"));
    when(localRequest.getHeader("X-Missing")).thenReturn(Optional.empty());

    LocalHttpServletRequest underTest = new LocalHttpServletRequest(localRequest, servletContext);

    assertThat(underTest.getHeader("accept")).isEqualTo("application/json");
    assertThat(Collections.list(underTest.getHeaders("Accept"))).containsExactly("application/json");
    assertThat(Collections.list(underTest.getHeaderNames())).containsExactly("Accept");
    assertThat(underTest.getIntHeader("X-Count")).isEqualTo(3);
    assertThat(underTest.getIntHeader("X-Missing")).isEqualTo(-1);
    assertThat(underTest.getHeader("X-Missing")).isNull();
    assertThat(Collections.list(underTest.getHeaders("X-Missing"))).isEmpty();
    assertThat(underTest.getDateHeader("Date")).isEqualTo(-1L);
  }

  @Test
  void headerNames_whenNoMediaType_shouldBeEmpty() {
    when(localRequest.getPath()).thenReturn("api/v2/foo");
    when(localRequest.getHeader("Accept")).thenReturn(Optional.empty());

    assertThat(Collections.list(new LocalHttpServletRequest(localRequest, servletContext).getHeaderNames())).isEmpty();
  }

  @Test
  void attributes_shouldBeStored() {
    when(localRequest.getPath()).thenReturn("api/v2/foo");
    LocalHttpServletRequest underTest = new LocalHttpServletRequest(localRequest, servletContext);

    underTest.setAttribute("a", 1);
    underTest.setAttribute("b", 2);
    underTest.setAttribute("b", null);
    assertThat(underTest.getAttribute("a")).isEqualTo(1);
    assertThat(Collections.list(underTest.getAttributeNames())).containsExactly("a");

    underTest.removeAttribute("a");
    assertThat(underTest.getAttribute("a")).isNull();
  }

  @Test
  void defaults_shouldDescribeBodylessSessionlessLoopbackRequest() throws IOException {
    when(localRequest.getPath()).thenReturn("api/v2/foo");
    LocalHttpServletRequest underTest = new LocalHttpServletRequest(localRequest, servletContext);

    underTest.setCharacterEncoding("ISO-8859-1");

    assertThat(underTest.getCharacterEncoding()).isEqualTo("ISO-8859-1");
    assertThat(underTest.getContentType()).isNull();
    assertThat(underTest.getContentLength()).isEqualTo(-1);
    assertThat(underTest.getContentLengthLong()).isEqualTo(-1L);
    assertThat(underTest.getInputStream().read()).isEqualTo(-1);
    assertThat(underTest.getReader().read()).isEqualTo(-1);
    assertThat(underTest.getProtocol()).isEqualTo("HTTP/1.1");
    assertThat(underTest.getRemoteAddr()).isEqualTo("127.0.0.1");
    assertThat(underTest.getRemoteHost()).isEqualTo("localhost");
    assertThat(underTest.getRemotePort()).isZero();
    assertThat(underTest.getServerPort()).isEqualTo(80);
    assertThat(underTest.getLocalPort()).isEqualTo(80);
    assertThat(underTest.isSecure()).isFalse();
    assertThat(underTest.getLocale()).isEqualTo(Locale.ENGLISH);
    assertThat(Collections.list(underTest.getLocales())).containsExactly(Locale.ENGLISH);
    assertThat(underTest.getDispatcherType()).isEqualTo(DispatcherType.REQUEST);
    assertThat(underTest.getRequestId()).isNotEmpty();
    assertThat(underTest.getCookies()).isEmpty();
    assertThat(underTest.getUserPrincipal()).isNull();
    assertThat(underTest.getSession(false)).isNull();
    assertThat(underTest.getParts()).isEmpty();
    assertThatThrownBy(underTest::getSession).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(underTest::startAsync).isInstanceOf(IllegalStateException.class);
  }
}
