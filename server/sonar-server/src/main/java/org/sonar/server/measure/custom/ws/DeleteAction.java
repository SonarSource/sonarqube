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
package org.sonar.server.measure.custom.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;

public class DeleteAction implements CustomMeasuresWsAction {

  private static final String ACTION = "delete";
  public static final String PARAM_ID = "id";

  private final DbClient dbClient;
  private final UserSession userSession;

  public DeleteAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setHandler(this)
      .setSince("5.2")
      .setDeprecatedSince("7.4")
      .setDescription("Delete a custom measure.<br /> Requires 'Administer System' permission or 'Administer' permission on the project.");

    action.createParam(PARAM_ID)
      .setDescription("Id")
      .setExampleValue("24")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    long id = request.mandatoryParamAsLong(PARAM_ID);

    try (DbSession dbSession = dbClient.openSession(false)) {
      CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectById(dbSession, id);
      checkArgument(customMeasure != null, "Custom measure with id '%s' does not exist", id);
      checkPermission(dbSession, customMeasure);
      dbClient.customMeasureDao().delete(dbSession, id);
      dbSession.commit();
    }

    response.noContent();
  }

  private void checkPermission(DbSession dbSession, CustomMeasureDto customMeasure) {
    userSession.checkLoggedIn();

    ComponentDto component = dbClient.componentDao().selectOrFailByUuid(dbSession, customMeasure.getComponentUuid());
    userSession.checkComponentPermission(UserRole.ADMIN, component);
  }
}
