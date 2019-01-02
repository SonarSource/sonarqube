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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;

import static java.lang.String.format;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CacheControlFilterTest {

  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  private CacheControlFilter underTest = new CacheControlFilter();

  @Test
  public void max_age_is_set_to_one_year_on_js() throws Exception {
    HttpServletRequest request = newRequest("/js/sonar.js");

    underTest.doFilter(request, response, chain);

    verify(response).addHeader("Cache-Control", format("max-age=%s", 31_536_000));
  }

  @Test
  public void max_age_is_set_to_one_year_on_css() throws Exception {
    HttpServletRequest request = newRequest("/css/sonar.css");

    underTest.doFilter(request, response, chain);

    verify(response).addHeader("Cache-Control", format("max-age=%s", 31_536_000));
  }

  @Test
  public void max_age_is_set_to_five_minutes_on_images() throws Exception {
    HttpServletRequest request = newRequest("/images/logo.png");

    underTest.doFilter(request, response, chain);

    verify(response).addHeader("Cache-Control", format("max-age=%s", 300));
  }

  @Test
  public void max_age_is_set_to_five_minutes_on_static() throws Exception {
    HttpServletRequest request = newRequest("/static/something");

    underTest.doFilter(request, response, chain);

    verify(response).addHeader("Cache-Control", format("max-age=%s", 300));
  }

  @Test
  public void max_age_is_set_to_five_minutes_on_css_of_static() throws Exception {
    HttpServletRequest request = newRequest("/static/css/custom.css");

    underTest.doFilter(request, response, chain);

    verify(response).addHeader("Cache-Control", format("max-age=%s", 300));
  }

  @Test
  public void does_nothing_on_home() throws Exception {
    HttpServletRequest request = newRequest("/");

    underTest.doFilter(request, response, chain);

    verifyZeroInteractions(response);
  }

  @Test
  public void does_nothing_on_web_service() throws Exception {
    HttpServletRequest request = newRequest("/api/ping");

    underTest.doFilter(request, response, chain);

    verifyZeroInteractions(response);
  }

  private static HttpServletRequest newRequest(String path) {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getMethod()).thenReturn("GET");
    when(req.getRequestURI()).thenReturn(path);
    when(req.getContextPath()).thenReturn("");
    return req;
  }

}
