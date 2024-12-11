/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.http;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import org.junit.Test;
import org.sonar.api.server.http.Cookie;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JakartaHttpRequestTest {

  @Test
  public void delegate_methods() throws IOException {
    HttpServletRequest requestMock = mock(HttpServletRequest.class);
    Enumeration<String> enumeration = Collections.enumeration(Collections.emptySet());
    when(requestMock.getHeaderNames()).thenReturn(enumeration);
    when(requestMock.getRemoteAddr()).thenReturn("192.168.0.1");
    when(requestMock.getServletPath()).thenReturn("/servlet-path");
    BufferedReader bufferedReader = mock(BufferedReader.class);
    when(requestMock.getReader()).thenReturn(bufferedReader);
    jakarta.servlet.http.Cookie[] cookies = new jakarta.servlet.http.Cookie[0];
    when(requestMock.getCookies()).thenReturn(cookies);
    when(requestMock.getServerPort()).thenReturn(80);
    when(requestMock.isSecure()).thenReturn(true);
    when(requestMock.getScheme()).thenReturn("https");
    when(requestMock.getServerName()).thenReturn("hostname");
    when(requestMock.getRequestURL()).thenReturn(new StringBuffer("https://hostname:80/path"));
    when(requestMock.getRequestURI()).thenReturn("/path");
    when(requestMock.getQueryString()).thenReturn("param1=value1");
    when(requestMock.getContextPath()).thenReturn("/path");
    when(requestMock.getMethod()).thenReturn("POST");
    when(requestMock.getParameter("param1")).thenReturn("value1");
    when(requestMock.getParameterValues("param1")).thenReturn(new String[] {"value1"});
    when(requestMock.getHeader("header1")).thenReturn("hvalue1");
    Enumeration<String> headers = mock(Enumeration.class);
    when(requestMock.getHeaders("header1")).thenReturn(headers);

    JakartaHttpRequest underTest = new JakartaHttpRequest(requestMock);

    assertThat(underTest.getDelegate()).isSameAs(requestMock);
    assertThat(underTest.getServerPort()).isEqualTo(80);
    assertThat(underTest.isSecure()).isTrue();
    assertThat(underTest.getScheme()).isEqualTo("https");
    assertThat(underTest.getServerName()).isEqualTo("hostname");
    assertThat(underTest.getRequestURL()).isEqualTo("https://hostname:80/path");
    assertThat(underTest.getRequestURI()).isEqualTo("/path");
    assertThat(underTest.getQueryString()).isEqualTo("param1=value1");
    assertThat(underTest.getContextPath()).isEqualTo("/path");
    assertThat(underTest.getMethod()).isEqualTo("POST");
    assertThat(underTest.getParameter("param1")).isEqualTo("value1");
    assertThat(underTest.getParameterValues("param1")).containsExactly("value1");
    assertThat(underTest.getHeader("header1")).isEqualTo("hvalue1");
    assertThat(underTest.getHeaders("header1")).isEqualTo(headers);
    assertThat(underTest.getHeaderNames()).isEqualTo(enumeration);
    assertThat(underTest.getRemoteAddr()).isEqualTo("192.168.0.1");
    assertThat(underTest.getServletPath()).isEqualTo("/servlet-path");
    assertThat(underTest.getReader()).isEqualTo(bufferedReader);
    assertThat(underTest.getCookies()).isEqualTo(cookies);

    underTest.setAttribute("name", "value");
    verify(requestMock).setAttribute("name", "value");
  }

  @Test
  public void delegate_methods_for_cookie() {
    jakarta.servlet.http.Cookie mockCookie = new jakarta.servlet.http.Cookie("name", "value");
    mockCookie.setSecure(true);
    mockCookie.setPath("path");
    mockCookie.setHttpOnly(true);
    mockCookie.setMaxAge(100);

    Cookie cookie = new JakartaHttpRequest.JakartaCookie(mockCookie);
    assertThat(cookie.getName()).isEqualTo("name");
    assertThat(cookie.getValue()).isEqualTo("value");
    assertThat(cookie.getPath()).isEqualTo("path");
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(100);
  }

}
