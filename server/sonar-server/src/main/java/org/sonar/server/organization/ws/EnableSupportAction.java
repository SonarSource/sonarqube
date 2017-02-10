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
package org.sonar.server.organization.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.user.UserSession;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;

public class EnableSupportAction implements OrganizationsAction {
  private static final String ACTION = "enable_support";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationsWsSupport support;

  public EnableSupportAction(UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider, OrganizationsWsSupport support) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.support = support;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION)
      .setPost(true)
      .setDescription("Enable support of organizations.<br />" +
        "'Administer System' permission is required. The logged-in user will be flagged as root and will be able to manage organizations and other root users.")
      .setInternal(true)
      .setPost(true)
      .setSince("6.3")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    verifySystemAdministrator();

    try (DbSession dbSession = dbClient.openSession(false)) {
      verifyFeatureIsDisabled(dbSession);
      flagCurrentUserAsRoot(dbSession);
      enableFeature(dbSession);
      dbSession.commit();
    }
    response.noContent();
  }

  private void verifySystemAdministrator() {
    userSession.checkLoggedIn().checkOrganizationPermission(defaultOrganizationProvider.get().getUuid(), SYSTEM_ADMIN);
  }

  private void verifyFeatureIsDisabled(DbSession dbSession) {
    if (support.isFeatureEnabled(dbSession)) {
      throw new BadRequestException("Organizations are already enabled");
    }
  }

  private void flagCurrentUserAsRoot(DbSession dbSession) {
    dbClient.userDao().setRoot(dbSession, requireNonNull(userSession.getLogin()), true);
  }

  private void enableFeature(DbSession dbSession) {
    dbClient.internalPropertiesDao().save(dbSession, InternalProperties.ORGANIZATION_ENABLED, valueOf(true));
  }

}
