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

package org.sonar.server.platform.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.server.platform.web.RoutesFilter;

public class RoutesFilterTest {

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);

  RoutesFilter underTest = new RoutesFilter();

  @Before
  public void setUp() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
  }

  @Test
  public void send_redirect_when_url_contains_batch_with_jar() throws Exception {
    when(request.getRequestURI()).thenReturn("/batch/file.jar");

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/batch/file?name=file.jar");
    verifyZeroInteractions(chain);
  }

  @Test
  public void send_redirect_when_url_contains_batch_bootstrap() throws Exception {
    when(request.getRequestURI()).thenReturn("/batch_bootstrap/index");

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/batch/index");
    verifyZeroInteractions(chain);
  }

  @Test
  public void send_redirect_when_url_contains_api_sources() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/sources");
    when(request.getQueryString()).thenReturn("resource=my.project");

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/api/sources/index?resource=my.project");
    verifyZeroInteractions(chain);
  }

  @Test
  public void does_not_redirect_and_execute_remaining_filter_on_unknown_path() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/issues/search");

    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyZeroInteractions(response);
  }
}
