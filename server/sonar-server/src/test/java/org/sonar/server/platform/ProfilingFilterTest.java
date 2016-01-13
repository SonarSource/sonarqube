/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfilingFilterTest {

  private ProfilingFilter filter;
  private FilterChain chain;

  @Before
  public void initialize() throws Exception {
    FilterConfig filterConfig = mock(FilterConfig.class);
    when(filterConfig.getInitParameter("staticDirs")).thenReturn("/static,/assets");
    ServletContext context = mock(ServletContext.class);
    when(context.getContextPath()).thenReturn("/context");
    when(filterConfig.getServletContext()).thenReturn(context);
    chain = mock(FilterChain.class);

    filter = new ProfilingFilter();
    filter.init(filterConfig);
  }

  @Test
  public void should_profile_service_call() throws Exception {
    filter.doFilter(request("POST", "/context/service/call", "param=value"), null, chain);
  }

  @Test
  public void should_profile_service() throws Exception {
    filter.doFilter(request("POST", "/context/service", null), null, chain);
  }

  @Test
  public void should_profile_context_root_as_slash2() throws Exception {
    filter.doFilter(request("POST", "/context", null), null, chain);
  }

  @Test(expected = ServletException.class)
  public void should_profile_even_when_exception() throws Exception {
    Mockito.doThrow(new ServletException()).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    filter.doFilter(request("POST", "/context/service/call", "param=value"), null, chain);
  }

  @Test
  public void should_not_profile_non_http_request() throws Exception {
    filter.doFilter(mock(ServletRequest.class), null, chain);
  }

  @Test
  public void should_not_profile_static_resource() throws Exception {
    filter.doFilter(request("GET", "/context/static/image.png", null), null, chain);
  }

  @Test
  public void should_profile_static_resource_if_no_config() throws Exception {
    FilterConfig filterConfig = mock(FilterConfig.class);
    ServletContext context = mock(ServletContext.class);
    when(context.getContextPath()).thenReturn("/context");
    when(filterConfig.getServletContext()).thenReturn(context);

    filter.init(filterConfig);
    filter.doFilter(request("GET", "/context/static/image.png", null), null, chain);
  }

  private HttpServletRequest request(String method, String path, String query) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(method);
    when(request.getRequestURI()).thenReturn(path);
    when(request.getQueryString()).thenReturn(query);
    return request;
  }
}
