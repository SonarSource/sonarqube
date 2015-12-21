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

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeActivityQuery;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentDtoFunctions;
import org.sonar.db.component.ComponentQuery;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsCe;

import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class ActivityAction implements CeWsAction {

  private static final String PARAM_COMPONENT_UUID = "componentId";
  private static final String PARAM_COMPONENT_QUERY = "componentQuery";
  private static final String PARAM_TYPE = "type";
  private static final String PARAM_STATUS = "status";
  private static final String PARAM_ONLY_CURRENTS = "onlyCurrents";
  private static final String PARAM_MIN_SUBMITTED_AT = "minSubmittedAt";
  private static final String PARAM_MAX_EXECUTED_AT = "maxExecutedAt";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final TaskFormatter formatter;

  public ActivityAction(UserSession userSession, DbClient dbClient, TaskFormatter formatter) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.formatter = formatter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("activity")
      .setDescription(format("Search for past task executions. Requires the system administration permission, " +
        "or project administration permission if %s is set.", PARAM_COMPONENT_UUID))
      .setResponseExample(getClass().getResource("activity-example.json"))
      .setHandler(this)
      .setSince("5.2");
    action.createParam(PARAM_COMPONENT_UUID)
      .setDescription("Optional id of the component (project) to filter on")
      .setExampleValue(Uuids.UUID_EXAMPLE_03);
    action.createParam(PARAM_COMPONENT_QUERY)
      .setDescription(format("Optional search by component name or key. Must not be set together with %s.", PARAM_COMPONENT_UUID))
      .setExampleValue("Apache");
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
    action.createParam(PARAM_MAX_EXECUTED_AT)
      .setDescription("Optional filter on the maximum date of end of task processing")
      .setExampleValue(DateUtils.formatDateTime(new Date()));
    action.addPagingParams(10);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      CeActivityQuery query = buildQuery(dbSession, wsRequest);
      checkPermissions(query);

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

  private CeActivityQuery buildQuery(DbSession dbSession, Request wsRequest) {
    String componentUuid = wsRequest.param(PARAM_COMPONENT_UUID);
    String componentQuery = wsRequest.param(PARAM_COMPONENT_QUERY);
    checkRequest(componentUuid == null || componentQuery == null,
      format("Only one of following parameters is accepted: %s or %s", PARAM_COMPONENT_UUID, PARAM_COMPONENT_QUERY));

    CeActivityQuery query = new CeActivityQuery();
    query.setType(wsRequest.param(PARAM_TYPE));
    query.setOnlyCurrents(wsRequest.mandatoryParamAsBoolean(PARAM_ONLY_CURRENTS));
    query.setMinSubmittedAt(toTime(wsRequest.paramAsDateTime(PARAM_MIN_SUBMITTED_AT)));
    query.setMaxExecutedAt(toTime(wsRequest.paramAsDateTime(PARAM_MAX_EXECUTED_AT)));

    String status = wsRequest.param(PARAM_STATUS);
    if (status != null) {
      query.setStatus(CeActivityDto.Status.valueOf(status));
    }

    loadComponentUuids(dbSession, wsRequest, query);
    return query;
  }

  private void loadComponentUuids(DbSession dbSession, Request wsRequest, CeActivityQuery query) {
    String componentUuid = wsRequest.param(PARAM_COMPONENT_UUID);
    String componentQuery = wsRequest.param(PARAM_COMPONENT_QUERY);
    if (componentUuid != null && componentQuery != null) {
      throw new BadRequestException(format("Only one of parameters must be set: %s or %s", PARAM_COMPONENT_UUID, PARAM_COMPONENT_QUERY));
    }

    if (componentUuid != null) {
      query.setComponentUuid(componentUuid);
    }
    if (componentQuery != null) {
      ComponentQuery componentDtoQuery = new ComponentQuery(componentQuery, null, Qualifiers.PROJECT, Qualifiers.VIEW);
      List<ComponentDto> componentDtos = dbClient.componentDao().selectByQuery(dbSession, componentDtoQuery, 0, CeActivityQuery.MAX_COMPONENT_UUIDS);
      query.setComponentUuids(Lists.transform(componentDtos, ComponentDtoFunctions.toUuid()));
    }
  }

  private void checkPermissions(CeActivityQuery query) {
    List<String> componentUuids = query.getComponentUuids();
    if (componentUuids != null && componentUuids.size() == 1) {
      if (!isAllowedOnComponentUuid(userSession, componentUuids.get(0))) {
        throw new ForbiddenException("Requires administration permission");
      }
    } else {
      userSession.checkGlobalPermission(UserRole.ADMIN);
    }
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

  public static boolean isAllowedOnComponentUuid(UserSession userSession, String componentUuid) {
    return userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN) || userSession.hasComponentUuidPermission(UserRole.ADMIN, componentUuid);
  }
}
