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

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_03;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ID;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;

public class DeleteAction implements OrganizationsAction {
  private static final String ACTION = "delete";

  private final OrganizationsWsSupport wsSupport;
  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public DeleteAction(OrganizationsWsSupport wsSupport, UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.wsSupport = wsSupport;
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription(
        format("Delete an organization.<br/>" +
          "The '%s' or '%s' must be provided.<br/>" +
          "Require 'Administer System' permission.",
          PARAM_ID, PARAM_KEY))
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setRequired(false)
      .setDescription("Organization id")
      .setExampleValue(UUID_EXAMPLE_03);

    action.createParam(PARAM_KEY)
      .setRequired(false)
      .setDescription("Organization key")
      .setExampleValue("foo-company");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);

    String uuid = request.param(PARAM_ID);
    String key = request.param(PARAM_KEY);
    wsSupport.checkKeyOrId(uuid, key);
    preventDeletionOfDefaultOrganization(uuid, key, defaultOrganizationProvider.get());

    try (DbSession dbSession = dbClient.openSession(false)) {
      if (uuid != null) {
        dbClient.organizationDao().deleteByUuid(dbSession, uuid);
      } else {
        dbClient.organizationDao().deleteByKey(dbSession, key);
      }
      dbSession.commit();

      response.noContent();
    }
  }

  private static void preventDeletionOfDefaultOrganization(@Nullable String uuid, @Nullable String key, DefaultOrganization defaultOrganization) {
    checkArgument(uuid == null || !defaultOrganization.getUuid().equals(uuid), "Default Organization can't be deleted");
    checkArgument(key == null || !defaultOrganization.getKey().equals(key), "Default Organization can't be deleted");
  }
}
