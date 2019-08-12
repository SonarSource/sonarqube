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
package org.sonar.server.authentication;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.ServletFilter.UrlPattern;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY;
import static org.sonar.api.web.ServletFilter.UrlPattern.Builder.staticResourcePatterns;
import static org.sonar.server.authentication.AuthenticationError.handleAuthenticationError;

@ServerSide
public class UserSessionInitializer {

  /**
   * Key of attribute to be used for displaying user login
   * in logs/access.log. The pattern to be configured
   * in property sonar.web.accessLogs.pattern is "%reqAttribute{LOGIN}"
   */
  private static final String ACCESS_LOG_LOGIN = "LOGIN";

  // SONAR-6546 these urls should be get from WebService
  private static final Set<String> SKIPPED_URLS = ImmutableSet.of(
    "/batch/index", "/batch/file",
    "/maintenance/*", "/setup/*",
    "/sessions/*", "/oauth2/callback/*",
    "/api/system/db_migration_status", "/api/system/status", "/api/system/migrate_db",
    "/api/server/version",
    "/api/users/identity_providers", "/api/l10n/index",
    "/api/authentication/login", "/api/authentication/logout", "/api/authentication/validate");

  private static final Set<String> URL_USING_PASSCODE = ImmutableSet.of(
    "/api/ce/info", "/api/ce/pause", "/api/ce/resume", "/api/system/health", "/api/system/analytics", "/api/system/migrate_es");

  private static final UrlPattern URL_PATTERN = UrlPattern.builder()
    .includes("/*")
    .excludes(staticResourcePatterns())
    .excludes(SKIPPED_URLS)
    .build();

  private static final UrlPattern PASSCODE_URLS = UrlPattern.builder()
    .includes(URL_USING_PASSCODE)
    .build();

  private final Configuration config;
  private final ThreadLocalUserSession threadLocalSession;
  private final AuthenticationEvent authenticationEvent;
  private final RequestAuthenticator requestAuthenticator;

  public UserSessionInitializer(Configuration config, ThreadLocalUserSession threadLocalSession, AuthenticationEvent authenticationEvent,
    RequestAuthenticator requestAuthenticator) {
    this.config = config;
    this.threadLocalSession = threadLocalSession;
    this.authenticationEvent = authenticationEvent;
    this.requestAuthenticator = requestAuthenticator;
  }

  public boolean initUserSession(HttpServletRequest request, HttpServletResponse response) {
    String path = request.getRequestURI().replaceFirst(request.getContextPath(), "");
    try {
      // Do not set user session when url is excluded
      if (URL_PATTERN.matches(path)) {
        loadUserSession(request, response, PASSCODE_URLS.matches(path));
      }
      return true;
    } catch (AuthenticationException e) {
      authenticationEvent.loginFailure(request, e);
      if (isWsUrl(path)) {
        response.setStatus(HTTP_UNAUTHORIZED);
        return false;
      }
      if (isNotLocalOrJwt(e.getSource())) {
        // redirect to Unauthorized error page
        handleAuthenticationError(e, request, response);
        return false;
      }
      // Web pages should redirect to the index.html file
      return true;
    }
  }

  private static boolean isNotLocalOrJwt(Source source) {
    AuthenticationEvent.Provider provider = source.getProvider();
    return provider != AuthenticationEvent.Provider.LOCAL && provider != AuthenticationEvent.Provider.JWT;
  }

  private void loadUserSession(HttpServletRequest request, HttpServletResponse response, boolean urlSupportsSystemPasscode) {
    UserSession session = requestAuthenticator.authenticate(request, response);
    if (!session.isLoggedIn() && !urlSupportsSystemPasscode && config.getBoolean(CORE_FORCE_AUTHENTICATION_PROPERTY).orElse(false)) {
      // authentication is required
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(AuthenticationEvent.Method.BASIC))
        .setMessage("User must be authenticated")
        .build();
    }
    threadLocalSession.set(session);
    request.setAttribute(ACCESS_LOG_LOGIN, defaultString(session.getLogin(), "-"));
  }

  public void removeUserSession() {
    threadLocalSession.unload();
  }

  private static boolean isWsUrl(String path) {
    return path.startsWith("/batch/") || path.startsWith("/api/");
  }
}
