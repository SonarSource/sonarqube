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
package org.sonar.server.platform.web;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.sonar.server.platform.web.SecurityServletFilter;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityServletFilterTest {

  SecurityServletFilter underTest = new SecurityServletFilter();
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);

  @Test
  public void allow_GET_method() throws IOException, ServletException {
    assertThatMethodIsAllowed("GET");
  }

  @Test
  public void allow_HEAD_method() throws IOException, ServletException {
    assertThatMethodIsAllowed("HEAD");
  }

  @Test
  public void allow_PUT_method() throws IOException, ServletException {
    assertThatMethodIsAllowed("PUT");
  }

  @Test
  public void allow_POST_method() throws IOException, ServletException {
    assertThatMethodIsAllowed("POST");
  }

  private void assertThatMethodIsAllowed(String httpMethod) throws IOException, ServletException {
    HttpServletRequest request = newRequest(httpMethod);
    underTest.doFilter(request, response, chain);
    verify(response, never()).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void deny_OPTIONS_method() throws IOException, ServletException {
    assertThatMethodIsDenied("OPTIONS");
  }

  @Test
  public void deny_TRACE_method() throws IOException, ServletException {
    assertThatMethodIsDenied("TRACE");
  }

  private void assertThatMethodIsDenied(String httpMethod) throws IOException, ServletException {
    underTest.doFilter(newRequest(httpMethod), response, chain);
    verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  @Test
  public void set_secured_headers() throws ServletException, IOException {
    underTest.init(mock(FilterConfig.class));
    HttpServletRequest request = newRequest("GET");

    underTest.doFilter(request, response, chain);

    verify(response, times(3)).addHeader(startsWith("X-"), anyString());

    underTest.destroy();
  }

  private HttpServletRequest newRequest(String httpMethod) {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getMethod()).thenReturn(httpMethod);
    return req;
  }
}
