/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.ui.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

public class OrganizationAction implements NavigationWsAction {

  private static final String ACTION_NAME = "organization";
  private static final String PARAM_ORGANIZATION = "organization";

  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final UserSession userSession;

  public OrganizationAction(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider, UserSession userSession) {
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction projectNavigation = context.createAction(ACTION_NAME)
      .setDescription("Get information concerning organization navigation for the current user")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("organization-example.json"))
      .setSince("6.3");

    projectNavigation.createParam(PARAM_ORGANIZATION)
      .setRequired(true)
      .setDescription("the organization key")
      .setExampleValue("my-org");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String organizationKey = request.mandatoryParam(PARAM_ORGANIZATION);
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = checkFoundWithOptional(
        dbClient.organizationDao().selectByKey(dbSession, organizationKey),
        "No organization with key '%s'", organizationKey);

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      writeOrganization(json, organization);
      json.endObject()
        .close();
    }
  }

  private void writeOrganization(JsonWriter json, OrganizationDto organization) {
    String organizationUuid = organization.getUuid();
    json.name("organization")
      .beginObject()
      .prop("canAdmin", userSession.hasOrganizationPermission(organizationUuid, SYSTEM_ADMIN))
      .prop("canProvisionProjects", userSession.hasOrganizationPermission(organizationUuid, GlobalPermissions.PROVISIONING))
      .prop("canDelete", organization.isGuarded() ? userSession.isSystemAdministrator() : userSession.hasOrganizationPermission(organizationUuid, SYSTEM_ADMIN))
      .prop("isDefault", organization.getKey().equals(defaultOrganizationProvider.get().getKey()))
      .endObject();
  }
}
