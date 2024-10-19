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

import java.io.IOException;
import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class RedirectFilterTest {

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  private RedirectFilter underTest = new RedirectFilter();

  @Before
  public void setUp() {
    when(request.getContextPath()).thenReturn("/sonarqube");
  }

  @Test
  public void send_redirect_when_url_contains_only_api() throws Exception {
    verifyRedirection("/api", null, "/sonarqube/api/webservices/list");
    verifyRedirection("/api/", null, "/sonarqube/api/webservices/list");
  }

  @Test
  public void does_not_redirect_and_execute_remaining_filter_on_unknown_path() throws Exception {
    verifyNoRedirection("/api/issues/search", null);
  }

  private void verifyRedirection(String requestUrl, @Nullable String queryString, String expectedRedirection) throws Exception {
    when(request.getRequestURI()).thenReturn(requestUrl);
    when(request.getQueryString()).thenReturn(queryString);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect(expectedRedirection);
    verifyNoInteractions(chain);
    reset(response, chain);
  }

  private void verifyNoRedirection(String requestUrl, @Nullable String queryString) throws IOException, ServletException {
    when(request.getRequestURI()).thenReturn(requestUrl);
    when(request.getQueryString()).thenReturn(queryString);
    when(request.getParameter(anyString())).thenReturn(null);

    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyNoInteractions(response);
    reset(response, chain);
  }
}
