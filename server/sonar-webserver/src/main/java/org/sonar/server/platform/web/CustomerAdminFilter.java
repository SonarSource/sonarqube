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

import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserSessionFactory;

import java.io.IOException;
import java.util.Optional;

public class CustomerAdminFilter extends HttpFilter {

  private static final UrlPattern URL_PATTERN = UrlPattern.builder()
      .includes("/api/system/info", "/api/ce/task",
          "/api/ce/task_types", "/api/ce/worker_count", "/api/ce/activity", "/api/organizations/search",
          "/api/ce/activity_status", "/api/settings/set", "/api/settings/reset", "/api/new_code_periods/set")
      .build();

  private final DbClient dbClient;
  private final ThreadLocalUserSession threadLocalUserSession;
  private final JwtHttpHandler jwtHttpHandler;
  private final UserSessionFactory userSessionFactory;

  public CustomerAdminFilter(DbClient dbClient, ThreadLocalUserSession threadLocalUserSession, JwtHttpHandler jwtHttpHandler, UserSessionFactory userSessionFactory) {
    this.dbClient = dbClient;
    this.threadLocalUserSession = threadLocalUserSession;
    this.jwtHttpHandler = jwtHttpHandler;
    this.userSessionFactory = userSessionFactory;
  }

  @Override
  public UrlPattern doGetPattern() {
    return URL_PATTERN;
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
    String path = request.getRequestURI().replaceFirst(request.getContextPath(), "");

    if (URL_PATTERN.getInclusions().contains(path)) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        OrganizationDto organization = dbClient.organizationDao().getDefaultOrganization(dbSession);
        UserSession userSession = threadLocalUserSession.get();
        if (userSession.hasPermission(OrganizationPermission.ADMINISTER_CUSTOMER, organization.getUuid())) {
          Optional<JwtHttpHandler.Token> tokenOpt = jwtHttpHandler.getToken(request, response);
          if (tokenOpt.isPresent()) {
            UserDto userDto = tokenOpt.get().getUserDto();
            userDto.setRoot(true);
            threadLocalUserSession.set(userSessionFactory.create(userDto, true /* TODO */));
          }
        }
      }
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // nothing to do
  }
}