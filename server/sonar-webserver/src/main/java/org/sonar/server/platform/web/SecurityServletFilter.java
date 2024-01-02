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

  private static final Set<String> ALLOWED_HTTP_METHODS = Set.of("DELETE", "GET", "HEAD", "POST", "PUT");

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
    addSecurityHeaders(httpRequest, httpResponse);

    chain.doFilter(httpRequest, httpResponse);
  }

  /**
   * Adds security HTTP headers in the response. The headers are added using {@code setHeader()}, which overwrites existing headers.
   */
  public static void addSecurityHeaders(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    // Clickjacking protection
    // See https://www.owasp.org/index.php/Clickjacking_Protection_for_Java_EE
    // The protection is disabled on purpose for integration in external systems like Github (/integration/github).
    String path = httpRequest.getRequestURI().replaceFirst(httpRequest.getContextPath(), "");
    if (!path.startsWith("/integration/")) {
      httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
    }

    // If the request is secure, the Strict-Transport-Security header is added.
    if ("https".equals(httpRequest.getHeader("x-forwarded-proto"))) {
      httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains;");
    }

    // Cross-site scripting
    // See https://www.owasp.org/index.php/List_of_useful_HTTP_headers
    httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

    // MIME-sniffing
    // See https://www.owasp.org/index.php/List_of_useful_HTTP_headers
    httpResponse.setHeader("X-Content-Type-Options", "nosniff");
  }

  @Override
  public void destroy() {
    // nothing
  }
}
