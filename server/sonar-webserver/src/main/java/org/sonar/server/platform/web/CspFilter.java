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
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CspFilter implements Filter {

  private final List<String> cspHeaders = new ArrayList<>();
  private String policies = null;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    cspHeaders.add("Content-Security-Policy");

    List<String> cspPolicies = new ArrayList<>();
    cspPolicies.add("default-src 'self'");
    cspPolicies.add("base-uri 'none'");
    cspPolicies.add("connect-src 'self' http: https:");
    cspPolicies.add("font-src 'self' data:");
    cspPolicies.add("img-src * data: blob:");
    cspPolicies.add("object-src 'none'");
    // the hash below corresponds to the window.__assetsPath script in index.html
    cspPolicies.add("script-src 'self' 'sha256-D1jaqcDDM2TM2STrzE42NNqyKR9PlptcHDe6tyaBcuM='");
    cspPolicies.add("style-src 'self' 'unsafe-inline'");
    cspPolicies.add("worker-src 'none'");
    this.policies = String.join("; ", cspPolicies).trim();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    // Add policies to all HTTP headers
    for (String header : this.cspHeaders) {
      ((HttpServletResponse) response).setHeader(header, this.policies);
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // Not used
  }

}
