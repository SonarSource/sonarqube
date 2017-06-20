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
package org.sonar.server.organization.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.user.UserSession;

public class SearchMyOrganizationsAction implements OrganizationsWsAction {
  private static final String ACTION = "search_my_organizations";

  private final UserSession userSession;
  private final DbClient dbClient;

  public SearchMyOrganizationsAction(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION)
      .setPost(false)
      .setDescription("List keys of the organizations for which the currently authenticated user has the System Administer permission for.")
      .setResponseExample(getClass().getResource("search_my_organization-example.json"))
      .setInternal(true)
      .setSince("6.3")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (!userSession.isLoggedIn()) {
      response.noContent();
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      json.name("organizations").beginArray();
      dbClient.organizationDao().selectByPermission(dbSession, userSession.getUserId(), OrganizationPermission.ADMINISTER.getKey())
        .forEach(dto -> json.value(dto.getKey()));
      json.endArray();
      json.endObject();
      json.close();
    }
  }
}
