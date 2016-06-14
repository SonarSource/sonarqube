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

package org.sonar.server.authentication;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.ServletFilter;
import org.sonar.server.exceptions.UnauthorizedException;

@ServerSide
public class ValidateJwtTokenFilter extends ServletFilter {

  private final JwtHttpHandler jwtHttpHandler;

  public ValidateJwtTokenFilter(JwtHttpHandler jwtHttpHandler) {
    this.jwtHttpHandler = jwtHttpHandler;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .setIncludePatterns("/*")
      .setExcludePatterns("/css/*", "/fonts/*", "/images/*", "/js/*", "/static/*")
      .build();
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    try {
      jwtHttpHandler.validateToken(request, response);
      chain.doFilter(request, response);
    } catch (UnauthorizedException e) {
      response.setStatus(e.httpCode());
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
