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
package org.sonar.server.organization.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.server.ws.KeyExamples.KEY_ORG_EXAMPLE_002;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public class DeleteAction implements OrganizationsWsAction {
  private static final String ACTION = "delete";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationFlags organizationFlags;
  private final OrganizationDeleter organizationDeleter;

  public DeleteAction(UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider,
    OrganizationFlags organizationFlags, OrganizationDeleter organizationDeleter) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.organizationFlags = organizationFlags;
    this.organizationDeleter = organizationDeleter;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Delete an organization.<br/>" +
        "Require 'Administer System' permission on the specified organization. Organization support must be enabled.")
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_ORGANIZATION)
      .setRequired(true)
      .setDescription("Organization key")
      .setDeprecatedKey(PARAM_KEY, "6.4")
      .setExampleValue(KEY_ORG_EXAMPLE_002);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      organizationFlags.checkEnabled(dbSession);

      String key = request.mandatoryParam(PARAM_ORGANIZATION);
      preventDeletionOfDefaultOrganization(key, defaultOrganizationProvider.get());

      OrganizationDto organization = checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, key), "Organization with key '%s' not found", key);
      userSession.checkPermission(ADMINISTER, organization);

      organizationDeleter.delete(dbSession, organization);

      response.noContent();
    }
  }

  private static void preventDeletionOfDefaultOrganization(String key, DefaultOrganization defaultOrganization) {
    checkArgument(!defaultOrganization.getKey().equals(key), "Default Organization can't be deleted");
  }
}
