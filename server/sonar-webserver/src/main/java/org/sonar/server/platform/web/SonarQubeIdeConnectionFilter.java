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
package org.sonar.server.platform.web;

import java.io.IOException;
import java.util.Locale;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.utils.System2;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.ws.ServletRequest;

import static java.util.concurrent.TimeUnit.HOURS;

public class SonarQubeIdeConnectionFilter extends HttpFilter {
  private static final UrlPattern URL_PATTERN = UrlPattern.builder()
    .includes("/api/*")
    .excludes("/api/v2/*")
    .build();
  private final DbClient dbClient;
  private final ThreadLocalUserSession userSession;
  private final System2 system2;

  public SonarQubeIdeConnectionFilter(DbClient dbClient, ThreadLocalUserSession userSession, System2 system2) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system2 = system2;
  }

  @Override
  public UrlPattern doGetPattern() {
    return URL_PATTERN;
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
    if (isComingFromSonarQubeIde(request)) {
      update();
    }
    chain.doFilter(request, response);
  }

  private static boolean isComingFromSonarQubeIde(HttpRequest request) {
    ServletRequest wsRequest = new ServletRequest(request);
    return wsRequest.header("User-Agent")
      .map(agent -> agent.toLowerCase(Locale.ENGLISH))
      .filter(agent -> agent.contains("sonarlint") || agent.contains("sonarqube for ide"))
      .isPresent();
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
