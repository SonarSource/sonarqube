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

import static java.lang.String.format;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RoutesFilter implements Filter {

  private static final String EMPTY = "";
  private static final String BATCH_WS = "/batch";
  private static final String API_SOURCES_WS = "/api/sources";

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String path = request.getRequestURI().replaceFirst(request.getContextPath(), EMPTY);
    if (path.startsWith(BATCH_WS + "/") && path.endsWith(".jar")) {
      // Scanner is still using /batch/file.jar url
      response.sendRedirect(format("%s%s/file?name=%s", request.getContextPath(), BATCH_WS, path.replace(BATCH_WS + "/", EMPTY)));
    } else if ("/batch_bootstrap/index".equals(path)) {
      // Scanner is still using /batch_bootstrap url
      response.sendRedirect(format("%s%s/index", request.getContextPath(), BATCH_WS));
    } else if (API_SOURCES_WS.equals(path)) {
      // SONAR-7852 /api/sources?resource url is still used
      response.sendRedirect(format("%s%s/index?%s", request.getContextPath(), API_SOURCES_WS, request.getQueryString()));
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing
  }

  @Override
  public void destroy() {
    // Nothing
  }
}
