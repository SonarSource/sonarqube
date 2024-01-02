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
import java.util.Locale;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.utils.System2;
import org.sonar.api.web.ServletFilter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.ws.ServletRequest;

import static java.util.concurrent.TimeUnit.HOURS;

public class SonarLintConnectionFilter extends ServletFilter {
  private static final UrlPattern URL_PATTERN = UrlPattern.builder()
    .includes("/api/*")
    .build();
  private final DbClient dbClient;
  private final ThreadLocalUserSession userSession;
  private final System2 system2;

  public SonarLintConnectionFilter(DbClient dbClient, ThreadLocalUserSession userSession, System2 system2) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system2 = system2;
  }

  @Override
  public UrlPattern doGetPattern() {
    return URL_PATTERN;
  }

  @Override
  public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    ServletRequest wsRequest = new ServletRequest(request);

    Optional<String> agent = wsRequest.header("User-Agent");
    if (agent.isPresent() && agent.get().toLowerCase(Locale.ENGLISH).contains("sonarlint")) {
      update();
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }

  public void update() {
    if (shouldUpdate()) {
      try (DbSession session = dbClient.openSession(false)) {
        dbClient.userDao().updateSonarlintLastConnectionDate(session, userSession.getLogin());
        session.commit();
      }
    }
  }

  private boolean shouldUpdate() {
    if (!userSession.hasSession() || !userSession.isLoggedIn()) {
      return false;
    }
    long now = system2.now();
    Long lastUpdate = userSession.getLastSonarlintConnectionDate();
    return (lastUpdate == null || lastUpdate < now - HOURS.toMillis(1L));
  }
}
