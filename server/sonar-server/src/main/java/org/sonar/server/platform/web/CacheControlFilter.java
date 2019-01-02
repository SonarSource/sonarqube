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

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;

/**
 * This servlet filter sets response headers that enable cache control on some static resources
 */
public class CacheControlFilter implements Filter {

  private static final String CACHE_CONTROL_HEADER = "Cache-Control";

  /**
   * Recommended max value for max-age is 1 year
   * @see <a href="https://stackoverflow.com/questions/7071763/max-value-for-cache-control-header-in-http">stackoverflow thread</a>
   */
  private static final int ONE_YEAR_IN_SECONDS = 365 * 24 * 60 * 60;

  private static final int FIVE_MINUTES_IN_SECONDS = 5 * 60;

  private static final String MAX_AGE_TEMPLATE = "max-age=%d";

  private static final Map<String, Integer> MAX_AGE_BY_PATH = ImmutableMap.of(
    // These folders contains files that are suffixed with their content hash : the cache should never be invalidated
    "/js/", ONE_YEAR_IN_SECONDS,
    "/css/", ONE_YEAR_IN_SECONDS,
    // This folder contains static resources from plugins : the cache should be set to a small value
    "/static/", FIVE_MINUTES_IN_SECONDS,
    "/images/", FIVE_MINUTES_IN_SECONDS);

  @Override
  public void init(FilterConfig filterConfig) {
    // nothing
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
    String path = ((HttpServletRequest) req).getRequestURI().replaceFirst(((HttpServletRequest) req).getContextPath(), "");

    MAX_AGE_BY_PATH.entrySet().stream()
      .filter(m -> path.startsWith(m.getKey()))
      .map(Map.Entry::getValue)
      .findFirst()
      .ifPresent(maxAge -> ((HttpServletResponse) resp).addHeader(CACHE_CONTROL_HEADER, format(MAX_AGE_TEMPLATE, maxAge)));

    chain.doFilter(req, resp);
  }

  @Override
  public void destroy() {
    // nothing
  }
}
