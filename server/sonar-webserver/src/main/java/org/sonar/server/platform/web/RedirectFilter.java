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
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;

public class RedirectFilter implements Filter {
  private static final String EMPTY = "";

  private static final List<Redirect> REDIRECTS = List.of(
    newSimpleRedirect("/api", "/api/webservices/list")
  );

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String path = extractPath(request);
    Predicate<Redirect> match = redirect -> redirect.test(path);
    List<Redirect> redirects = REDIRECTS.stream()
      .filter(match)
      .toList();

    switch (redirects.size()) {
      case 0:
        chain.doFilter(request, response);
        break;
      case 1:
        response.sendRedirect(redirects.get(0).apply(request));
        break;
      default:
        throw new IllegalStateException(format("Multiple redirects have been found for '%s'", path));
    }
  }

  public static Redirect newSimpleRedirect(String from, String to) {
    return new Redirect() {
      @Override
      public boolean test(String path) {
        return from.equals(path);
      }

      @Override
      public String apply(HttpServletRequest request) {
        return format("%s%s", request.getContextPath(), to);
      }
    };
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // Nothing
  }

  @Override
  public void destroy() {
    // Nothing
  }

  private interface Redirect extends Predicate<String>, Function<HttpServletRequest, String> {
    @Override
    boolean test(String path);

    @Override
    String apply(HttpServletRequest request);
  }

  private static String extractPath(HttpServletRequest request) {
    return sanitizePath(request.getRequestURI().replaceFirst(request.getContextPath(), EMPTY));
  }

  private static String sanitizePath(String path) {
    if (path.length() > 1 && path.endsWith("/")) {
      return path.substring(0, path.length() - 1);
    }
    return path;
  }
}
