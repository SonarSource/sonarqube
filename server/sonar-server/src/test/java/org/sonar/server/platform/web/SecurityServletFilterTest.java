/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityServletFilterTest {

  private SecurityServletFilter underTest = new SecurityServletFilter();
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);

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
    HttpServletRequest request = newRequest(httpMethod, "/");
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
    underTest.doFilter(newRequest(httpMethod, "/"), response, chain);
    verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  @Test
  public void set_security_headers() throws Exception {
    HttpServletRequest request = newRequest("GET", "/");

    underTest.doFilter(request, response, chain);

    verify(response).addHeader("X-Frame-Options", "SAMEORIGIN");
    verify(response).addHeader("X-XSS-Protection", "1; mode=block");
    verify(response).addHeader("X-Content-Type-Options", "nosniff");
  }

  @Test
  public void do_not_set_frame_protection_on_integration_resources() throws Exception {
    HttpServletRequest request = newRequest("GET", "/integration/vsts/index.html");

    underTest.doFilter(request, response, chain);

    verify(response, never()).addHeader(eq("X-Frame-Options"), anyString());
    verify(response).addHeader("X-XSS-Protection", "1; mode=block");
    verify(response).addHeader("X-Content-Type-Options", "nosniff");
  }

  @Test
  public void do_not_set_frame_protection_on_integration_resources_with_context() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/sonarqube/integration/vsts/index.html");
    when(request.getContextPath()).thenReturn("/sonarqube");

    underTest.doFilter(request, response, chain);

    verify(response, never()).addHeader(eq("X-Frame-Options"), anyString());
    verify(response).addHeader("X-XSS-Protection", "1; mode=block");
    verify(response).addHeader("X-Content-Type-Options", "nosniff");
  }

  private static HttpServletRequest newRequest(String httpMethod, String path) {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getMethod()).thenReturn(httpMethod);
    when(req.getRequestURI()).thenReturn(path);
    when(req.getContextPath()).thenReturn("");
    return req;
  }
}
