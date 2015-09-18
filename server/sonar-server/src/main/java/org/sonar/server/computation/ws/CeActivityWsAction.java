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
package org.sonar.server.computation.ws;

import java.util.List;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeActivityQuery;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsCe;

/**
 * GET api/ce/activity
 * <p>Get the past executed tasks</p>
 */
public class CeActivityWsAction implements CeWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final CeWsTaskFormatter formatter;

  public CeActivityWsAction(UserSession userSession, DbClient dbClient, CeWsTaskFormatter formatter) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.formatter = formatter;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("activity")
      .setInternal(true)
      .setResponseExample(getClass().getResource("CeActivityWsAction/example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkGlobalPermission(UserRole.ADMIN);
    DbSession dbSession = dbClient.openSession(false);
    try {
      CeActivityQuery query = new CeActivityQuery();
      List<CeActivityDto> dtos = dbClient.ceActivityDao().selectByQuery(dbSession, query, new RowBounds(0, 100));

      WsCe.ActivityResponse.Builder wsResponseBuilder = WsCe.ActivityResponse.newBuilder();
      wsResponseBuilder.addAllTasks(formatter.formatActivity(dbSession, dtos));
      Common.Paging paging = Common.Paging.newBuilder()
        .setPageIndex(1)
        .setPageSize(dtos.size())
        .setTotal(dtos.size())
        .build();
      wsResponseBuilder.setPaging(paging);
      WsUtils.writeProtobuf(wsResponseBuilder.build(), wsRequest, wsResponse);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
