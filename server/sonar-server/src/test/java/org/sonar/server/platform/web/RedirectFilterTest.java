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
import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RedirectFilterTest {

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);

  RedirectFilter underTest = new RedirectFilter();

  @Before
  public void setUp() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
  }

  @Test
  public void send_redirect_when_url_contains_only_api() throws Exception {
    verifyRedirection("/api", null, "/sonarqube/api/webservices/list");
    verifyRedirection("/api/", null, "/sonarqube/api/webservices/list");
  }

  @Test
  public void send_redirect_when_url_contains_batch_with_jar() throws Exception {
    verifyRedirection("/batch/file.jar", null, "/sonarqube/batch/file?name=file.jar");
  }

  @Test
  public void send_redirect_when_url_contains_batch_bootstrap() throws Exception {
    verifyRedirection("/batch_bootstrap/index", null, "/sonarqube/batch/index");
    verifyRedirection("/batch_bootstrap/index/", null, "/sonarqube/batch/index");
  }

  @Test
  public void send_redirect_when_url_contains_profiles_export() throws Exception {
    verifyRedirection("/profiles/export", "format=pmd", "/sonarqube/api/qualityprofiles/export?format=pmd");
    verifyRedirection("/profiles/export/", "format=pmd", "/sonarqube/api/qualityprofiles/export?format=pmd");
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
    verifyZeroInteractions(chain);
    reset(response, chain);
  }

  private void verifyNoRedirection(String requestUrl, @Nullable String queryString) throws IOException, ServletException {
    when(request.getRequestURI()).thenReturn(requestUrl);
    when(request.getQueryString()).thenReturn(queryString);
    when(request.getParameter(anyString())).thenReturn(null);

    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyZeroInteractions(response);
    reset(response, chain);
  }
}
