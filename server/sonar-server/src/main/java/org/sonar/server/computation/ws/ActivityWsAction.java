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

import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeActivityQuery;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsCe;

public class ActivityWsAction implements CeWsAction {

  private static final String PARAM_COMPONENT_UUID = "componentId";
  private static final String PARAM_TYPE = "type";
  private static final String PARAM_STATUS = "status";
  private static final String PARAM_ONLY_CURRENTS = "onlyCurrents";
  private static final String PARAM_MIN_SUBMITTED_AT = "minSubmittedAt";
  private static final String PARAM_MAX_FINISHED_AT = "maxFinishedAt";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final TaskFormatter formatter;

  public ActivityWsAction(UserSession userSession, DbClient dbClient, TaskFormatter formatter) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.formatter = formatter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("activity")
      .setInternal(true)
      .setResponseExample(getClass().getResource("activity-example.json"))
      .setHandler(this);
    action.createParam(PARAM_COMPONENT_UUID)
      .setDescription("Optional id of the component (project) to filter on")
      .setExampleValue(Uuids.UUID_EXAMPLE_03);
    action.createParam(PARAM_STATUS)
      .setDescription("Optional filter on task status")
      .setPossibleValues(CeActivityDto.Status.values());
    action.createParam(PARAM_ONLY_CURRENTS)
      .setDescription("Optional filter on the current activities (only the most recent task by project)")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
    action.createParam(PARAM_TYPE)
      .setDescription("Optional filter on task type")
      .setExampleValue(CeTaskTypes.REPORT);
    action.createParam(PARAM_MIN_SUBMITTED_AT)
      .setDescription("Optional filter on minimum date of task submission")
      .setExampleValue(DateUtils.formatDateTime(new Date()));
    action.createParam(PARAM_MAX_FINISHED_AT)
      .setDescription("Optional filter on the maximum date of end of task processing")
      .setExampleValue(DateUtils.formatDateTime(new Date()));
    action.addPagingParams(10);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      CeActivityQuery query = readQuery(wsRequest);
      RowBounds rowBounds = readMyBatisRowBounds(wsRequest);
      List<CeActivityDto> dtos = dbClient.ceActivityDao().selectByQuery(dbSession, query, rowBounds);
      int total = dbClient.ceActivityDao().countByQuery(dbSession, query);

      WsCe.ActivityResponse.Builder wsResponseBuilder = WsCe.ActivityResponse.newBuilder();
      wsResponseBuilder.addAllTasks(formatter.formatActivity(dbSession, dtos));
      wsResponseBuilder.setPaging(Common.Paging.newBuilder()
        .setPageIndex(wsRequest.mandatoryParamAsInt(WebService.Param.PAGE))
        .setPageSize(wsRequest.mandatoryParamAsInt(WebService.Param.PAGE_SIZE))
        .setTotal(total));
      WsUtils.writeProtobuf(wsResponseBuilder.build(), wsRequest, wsResponse);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private CeActivityQuery readQuery(Request wsRequest) {
    CeActivityQuery query = new CeActivityQuery();
    query.setType(wsRequest.param(PARAM_TYPE));
    query.setOnlyCurrents(wsRequest.mandatoryParamAsBoolean(PARAM_ONLY_CURRENTS));
    query.setMinSubmittedAt(toTime(wsRequest.paramAsDateTime(PARAM_MIN_SUBMITTED_AT)));
    query.setMaxFinishedAt(toTime(wsRequest.paramAsDateTime(PARAM_MAX_FINISHED_AT)));

    String status = wsRequest.param(PARAM_STATUS);
    if (status != null) {
      query.setStatus(CeActivityDto.Status.valueOf(status));
    }

    String componentUuid = wsRequest.param(PARAM_COMPONENT_UUID);
    if (componentUuid == null) {
      userSession.checkGlobalPermission(UserRole.ADMIN);
    } else {
      userSession.checkProjectUuidPermission(UserRole.USER, componentUuid);
      query.setComponentUuid(componentUuid);
    }
    return query;
  }

  private static RowBounds readMyBatisRowBounds(Request wsRequest) {
    int pageIndex = wsRequest.mandatoryParamAsInt(WebService.Param.PAGE);
    int pageSize = wsRequest.mandatoryParamAsInt(WebService.Param.PAGE_SIZE);
    return new RowBounds((pageIndex - 1) * pageSize, pageSize);
  }

  @CheckForNull
  private static Long toTime(@Nullable Date date) {
    return date == null ? null : date.getTime();
  }
}
