/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.custommeasure.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.custommeasure.db.CustomMeasureDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

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
      .setDescription("Delete a custom measure.<br /> Requires 'Administer System' permission or 'Administer' permission on the project.");

    action.createParam(PARAM_ID)
      .setDescription("Id")
      .setExampleValue("24")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    long id = request.mandatoryParamAsLong(PARAM_ID);

    DbSession dbSession = dbClient.openSession(false);
    try {
      CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectById(dbSession, id);
      checkPermissions(dbSession, customMeasure);
      dbClient.customMeasureDao().delete(dbSession, id);
      dbSession.commit();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }

    response.noContent();
  }

  private void checkPermissions(DbSession dbSession, CustomMeasureDto customMeasure) {
    if (userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN)) {
      return;
    }

    ComponentDto component = dbClient.componentDao().selectById(customMeasure.getComponentId(), dbSession);
    userSession.checkLoggedIn().checkProjectUuidPermission(UserRole.ADMIN, component.projectUuid());
  }
}
