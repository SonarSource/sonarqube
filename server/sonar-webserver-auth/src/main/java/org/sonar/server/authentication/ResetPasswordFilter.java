/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.io.IOException;
import java.util.Set;
import org.sonar.api.impl.ws.StaticResources;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.sonar.server.authentication.AuthenticationRedirection.redirectTo;

public class ResetPasswordFilter extends HttpFilter {
  private static final String RESET_PASSWORD_PATH = "/account/reset_password";

  private static final Set<String> SKIPPED_URLS = Set.of(
    RESET_PASSWORD_PATH,
    "/batch/*", "/api/*", "/api/v2/*");

  private final ThreadLocalUserSession userSession;

  public ResetPasswordFilter(ThreadLocalUserSession userSession) {
    this.userSession = userSession;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes("/*")
      .excludes(StaticResources.patterns())
      .excludes(SKIPPED_URLS)
      .build();
  }

  @Override
  public void init() {
    // nothing to do
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
    if (userSession.hasSession() && userSession.isLoggedIn() && userSession.shouldResetPassword()) {
      redirectTo(response, request.getContextPath() + RESET_PASSWORD_PATH);
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // nothing to do
  }
}
