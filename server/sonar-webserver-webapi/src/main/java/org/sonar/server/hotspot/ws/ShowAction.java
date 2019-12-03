/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.hotspot.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;

public class ShowAction implements HotspotsWsAction {

  private static final String PARAM_HOTSPOT_KEY = "hotspot";

  private final DbClient dbClient;
  private final UserSession userSession;

  public ShowAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("show")
      .setHandler(this)
      .setDescription("Provides the details of a Security Hotpot.")
      .setSince("8.1");

    action.createParam(PARAM_HOTSPOT_KEY)
      .setDescription("Key of the Security Hotspot")
      .setExampleValue(Uuids.UUID_EXAMPLE_03)
      .setRequired(true);
    // FIXME add response example and test it
    // action.setResponseExample()
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String hotspotKey = request.mandatoryParam(PARAM_HOTSPOT_KEY);
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto hotspot = dbClient.issueDao().selectByKey(dbSession, hotspotKey)
        .orElseThrow(() -> new NotFoundException(format("Hotspot with key '%s' does not exist", hotspotKey)));
      ComponentDto project = dbClient.componentDao().selectByUuid(dbSession, hotspot.getProjectUuid())
        .orElseThrow(() -> new NotFoundException(format("Project with uuid '%s' does not exist", hotspot.getProjectUuid())));
      userSession.checkComponentPermission(UserRole.USER, project);


    }
  }
}
