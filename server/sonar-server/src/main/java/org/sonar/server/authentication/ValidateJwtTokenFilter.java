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

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY;
import static org.sonar.api.web.ServletFilter.UrlPattern.Builder.staticResourcePatterns;
import static org.sonar.server.authentication.AuthLoginAction.AUTH_LOGIN_URL;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.ServletFilter;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.UserSession;

@ServerSide
public class ValidateJwtTokenFilter extends ServletFilter {

  // SONAR-6546 these urls should be get from WebService
  private static final Set<String> SKIPPED_URLS = ImmutableSet.of(
    "/batch/index", "/batch/file", "/batch_bootstrap/index",
    "/maintenance/*",
    "/setup/*",
    "/sessions/*",
    "/api/system/db_migration_status", "/api/system/status", "/api/system/migrate_db",
    "/api/server/*",
    AUTH_LOGIN_URL
  );

  private final Settings settings;
  private final JwtHttpHandler jwtHttpHandler;
  private final UserSession userSession;

  public ValidateJwtTokenFilter(Settings settings, JwtHttpHandler jwtHttpHandler, UserSession userSession) {
    this.settings = settings;
    this.jwtHttpHandler = jwtHttpHandler;
    this.userSession = userSession;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes("/*")
      .excludes(staticResourcePatterns())
      .excludes(SKIPPED_URLS)
      .build();
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String path = request.getRequestURI().replaceFirst(request.getContextPath(), "");

    try {
      if (isDeprecatedBatchWs(path)) {
        chain.doFilter(request, response);
        return;
      }

      jwtHttpHandler.validateToken(request, response);
      // TODO handle basic authentication
      if (!userSession.isLoggedIn() && settings.getBoolean(CORE_FORCE_AUTHENTICATION_PROPERTY)) {
        throw new UnauthorizedException("User must be authenticated");
      }
      chain.doFilter(request, response);
    } catch (UnauthorizedException e) {
      jwtHttpHandler.removeToken(response);
      response.setStatus(HTTP_UNAUTHORIZED);

      if (isWsUrl(path)) {
        return;
      }
      // WS should stop here. Rails page should continue in order to deal with redirection
      chain.doFilter(request, response);
    }
  }

  // Scanner is still using deprecated /batch/<File name>.jar WS
  private static boolean isDeprecatedBatchWs(String path){
    return path.startsWith("/batch/") && path.endsWith(".jar");
  }

  private static boolean isWsUrl(String path){
    return path.startsWith("/batch/") || path.startsWith("/api/");
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
