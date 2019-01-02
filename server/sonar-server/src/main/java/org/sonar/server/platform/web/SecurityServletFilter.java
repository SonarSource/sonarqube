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

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet filter sets response headers that enable browser protection against several classes if Web attacks.
 */
public class SecurityServletFilter implements Filter {

  private static final Set<String> ALLOWED_HTTP_METHODS = ImmutableSet.of("DELETE", "GET", "HEAD", "POST", "PUT");

  @Override
  public void init(FilterConfig filterConfig) {
    // nothing
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
    doHttpFilter((HttpServletRequest) req, (HttpServletResponse) resp, chain);
  }

  private static void doHttpFilter(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain) throws IOException, ServletException {
    // SONAR-6881 Disable OPTIONS and TRACE methods
    if (!ALLOWED_HTTP_METHODS.contains(httpRequest.getMethod())) {
      httpResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      return;
    }

    // WARNING, headers must be added before the doFilter, otherwise they won't be added when response is already committed (for instance when a WS is called)

    // Clickjacking protection
    // See https://www.owasp.org/index.php/Clickjacking_Protection_for_Java_EE
    // The protection is disabled on purpose for integration in external systems like VSTS (/integration/vsts/index.html).
    String path = httpRequest.getRequestURI().replaceFirst(httpRequest.getContextPath(), "");
    if (!path.startsWith("/integration/")) {
      httpResponse.addHeader("X-Frame-Options", "SAMEORIGIN");
    }

    // Cross-site scripting
    // See https://www.owasp.org/index.php/List_of_useful_HTTP_headers
    httpResponse.addHeader("X-XSS-Protection", "1; mode=block");

    // MIME-sniffing
    // See https://www.owasp.org/index.php/List_of_useful_HTTP_headers
    httpResponse.addHeader("X-Content-Type-Options", "nosniff");

    chain.doFilter(httpRequest, httpResponse);
  }

  @Override
  public void destroy() {
    // nothing
  }
}
