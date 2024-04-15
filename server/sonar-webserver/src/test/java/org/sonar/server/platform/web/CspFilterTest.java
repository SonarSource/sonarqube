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
package org.sonar.server.platform.web;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CspFilterTest {

  private static final String TEST_CONTEXT = "/sonarqube";
  private static final String EXPECTED = "default-src 'self'; " +
    "base-uri 'none'; " +
    "connect-src 'self' http: https:; " +
    "font-src 'self' data:; " +
    "img-src * data: blob:; " +
    "object-src 'none'; " +
    "script-src 'self'; " +
    "style-src 'self' 'unsafe-inline'; " +
    "worker-src 'none'";
  private final ServletContext servletContext = mock(ServletContext.class, RETURNS_MOCKS);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final FilterChain chain = mock(FilterChain.class);
  private final CspFilter underTest = new CspFilter();
  FilterConfig config = mock(FilterConfig.class);

  @Before
  public void setUp() throws ServletException {
    when(servletContext.getContextPath()).thenReturn(TEST_CONTEXT);
  }

  @Test
  public void set_content_security_headers() throws Exception {
    doInit();
    HttpServletRequest request = newRequest("/");
    underTest.doFilter(request, response, chain);
    verify(response).setHeader("Content-Security-Policy", EXPECTED);
    verify(response).setHeader("X-Content-Security-Policy", EXPECTED);
    verify(response).setHeader("X-WebKit-CSP", EXPECTED);
    verify(chain).doFilter(request, response);
  }

  private void doInit() throws ServletException {
    underTest.init(config);
  }

  private HttpServletRequest newRequest(String path) {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getMethod()).thenReturn("GET");
    when(req.getRequestURI()).thenReturn(path);
    when(req.getContextPath()).thenReturn("");
    when(req.getServletContext()).thenReturn(this.servletContext);
    return req;
  }
}
