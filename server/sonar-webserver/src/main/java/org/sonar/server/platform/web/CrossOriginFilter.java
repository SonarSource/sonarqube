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
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CrossOriginFilter implements Filter {

  private final Map<String, String> crossOriginHeaders = new HashMap<>();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    crossOriginHeaders.put("Cross-Origin-Embedder-Policy", "require-corp");
    crossOriginHeaders.put("Cross-Origin-Opener-Policy", "same-origin");
    crossOriginHeaders.put("Cross-Origin-Resource-Policy", "same-origin");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    // Add policies to all HTTP headers
    for (Map.Entry<String, String> entry : crossOriginHeaders.entrySet()) {
      ((HttpServletResponse) response).setHeader(entry.getKey(), entry.getValue());
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // Not used
  }

}
