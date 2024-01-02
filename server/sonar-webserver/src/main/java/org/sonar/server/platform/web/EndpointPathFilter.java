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
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.sonar.server.platform.web.logging.EntrypointMDCStorage;

public class EndpointPathFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // nothing to do
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    String endpointPath = null;
    if (request instanceof HttpServletRequest httpRequest) {
      endpointPath = httpRequest.getRequestURI();
    }

    try (EntrypointMDCStorage entrypointMDCStorage = new EntrypointMDCStorage(endpointPath)) {
      chain.doFilter(request, response);
    }

  }

  @Override
  public void destroy() {
    // nothing to do
  }
}
