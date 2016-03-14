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

package org.sonar.server.ce.ws;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskQuery;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentDtoFunctions;
import org.sonar.db.component.ComponentQuery;
import org.sonar.ce.taskprocessor.CeTaskProcessor;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.WsCe.ActivityResponse;
import org.sonarqube.ws.client.ce.ActivityWsRequest;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.sonar.api.utils.DateUtils.parseDateQuietly;
import static org.sonar.api.utils.DateUtils.parseDateTimeQuietly;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_QUERY;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MAX_EXECUTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MIN_SUBMITTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_ONLY_CURRENTS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_STATUS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_TYPE;

public class ActivityAction implements CeWsAction {
  private static final int MAX_PAGE_SIZE = 1000;

  private final UserSession userSession;
  private final DbClient dbClient;
  private final TaskFormatter formatter;
  private final Set<String> taskTypes;

  public ActivityAction(UserSession userSession, DbClient dbClient, TaskFormatter formatter, CeTaskProcessor[] taskProcessors) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.formatter = formatter;

    this.taskTypes = new LinkedHashSet<>();
    for (CeTaskProcessor taskProcessor : taskProcessors) {
      taskTypes.addAll(taskProcessor.getHandledCeTaskTypes());
    }
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("activity")
      .setDescription(format("Search for tasks.<br> " +
        "Requires the system administration permission, " +
        "or project administration permission if %s is set.", PARAM_COMPONENT_ID))
      .setResponseExample(getClass().getResource("activity-example.json"))
      .setHandler(this)
      .setSince("5.2");

    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Id of the component (project) to filter on")
      .setExampleValue(Uuids.UUID_EXAMPLE_03);
    action.createParam(PARAM_COMPONENT_QUERY)
      .setDescription(format("Limit search to: <ul>" +
        "<li>component names that contain the supplied string</li>" +
        "<li>component keys that are exactly the same as the supplied string</li>" +
        "</ul>" +
        "Must not be set together with %s.<br />" +
        "Deprecated and replaced by '%s'", PARAM_COMPONENT_ID, Param.TEXT_QUERY))
      .setExampleValue("Apache")
      .setDeprecatedSince("5.5");
    action.createParam(Param.TEXT_QUERY)
      .setDescription(format("Limit search to: <ul>" +
        "<li>component names that contain the supplied string</li>" +
        "<li>component keys that are exactly the same as the supplied string</li>" +
        "<li>task ids that are exactly the same as the supplied string</li>" +
        "</ul>" +
        "Must not be set together with %s", PARAM_COMPONENT_ID))
      .setExampleValue("Apache")
      .setSince("5.5");
    action.createParam(PARAM_STATUS)
      .setDescription("Comma separated list of task statuses")
      .setPossibleValues(ImmutableList.builder()
        .add(CeActivityDto.Status.values())
        .add(CeQueueDto.Status.values()).build())
      .setExampleValue(Joiner.on(",").join(CeQueueDto.Status.IN_PROGRESS, CeActivityDto.Status.SUCCESS))
      // activity statuses by default to be backward compatible
      // queued tasks have been added in 5.5
      .setDefaultValue(Joiner.on(",").join(CeActivityDto.Status.values()));
    action.createParam(PARAM_ONLY_CURRENTS)
      .setDescription("Filter on the last tasks (only the most recent finished task by project)")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
    action.createParam(PARAM_TYPE)
      .setDescription("Task type")
      .setExampleValue(CeTaskTypes.REPORT)
      .setPossibleValues(taskTypes);
    action.createParam(PARAM_MIN_SUBMITTED_AT)
      .setDescription("Minimum date of task submission (inclusive)")
      .setExampleValue(DateUtils.formatDateTime(new Date()));
    action.createParam(PARAM_MAX_EXECUTED_AT)
      .setDescription("Maximum date of end of task processing (inclusive)")
      .setExampleValue(DateUtils.formatDateTime(new Date()));
    action.addPagingParams(100, MAX_PAGE_SIZE);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    ActivityResponse activityResponse = doHandle(toSearchWsRequest(wsRequest));
    writeProtobuf(activityResponse, wsRequest, wsResponse);
  }

  private ActivityResponse doHandle(ActivityWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      // if a task searched by uuid is found all other parameters are ignored
      Optional<WsCe.Task> taskSearchedById = searchTaskByUuid(dbSession, request);
      if (taskSearchedById.isPresent()) {
        return buildResponse(
          singletonList(taskSearchedById.get()),
          Collections.<WsCe.Task>emptyList(),
          Paging.forPageIndex(1).withPageSize(request.getPageSize()).andTotal(1));
      }

      CeTaskQuery query = buildQuery(dbSession, request);
      checkPermissions(query);
      TaskResult queuedTasks = loadQueuedTasks(dbSession, request, query);
      TaskResult pastTasks = loadPastTasks(dbSession, request, query, queuedTasks.total);

      return buildResponse(
        queuedTasks.tasks,
        pastTasks.tasks,
        Paging.forPageIndex(request.getPage())
          .withPageSize(request.getPageSize())
          .andTotal(queuedTasks.total + pastTasks.total));

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private Optional<WsCe.Task> searchTaskByUuid(DbSession dbSession, ActivityWsRequest request) {
    String textQuery = request.getQuery();
    if (textQuery == null) {
      return Optional.absent();
    }

    Optional<CeQueueDto> queue = dbClient.ceQueueDao().selectByUuid(dbSession, textQuery);
    if (queue.isPresent()) {
      return Optional.of(formatter.formatQueue(dbSession, queue.get()));
    }

    Optional<CeActivityDto> activity = dbClient.ceActivityDao().selectByUuid(dbSession, textQuery);
    if (activity.isPresent()) {
      return Optional.of(formatter.formatActivity(dbSession, activity.get()));
    }

    return Optional.absent();
  }

  private CeTaskQuery buildQuery(DbSession dbSession, ActivityWsRequest request) {
    CeTaskQuery query = new CeTaskQuery();
    query.setType(request.getType());
    query.setOnlyCurrents(request.getOnlyCurrents());
    query.setMinSubmittedAt(parseDateTimeAsLong(request.getMinSubmittedAt()));
    query.setMaxExecutedAt(parseDateTimeAsLong(request.getMaxExecutedAt()));

    List<String> statuses = request.getStatus();
    if (statuses != null && !statuses.isEmpty()) {
      query.setStatuses(request.getStatus());
    }

    loadComponentUuids(dbSession, request, query);
    return query;
  }

  private void loadComponentUuids(DbSession dbSession, ActivityWsRequest request, CeTaskQuery query) {
    String componentUuid = request.getComponentId();
    String componentQuery = request.getQuery();

    if (componentUuid != null) {
      query.setComponentUuid(componentUuid);
    }
    if (componentQuery != null) {
      ComponentQuery componentDtoQuery = ComponentQuery.builder().setNameOrKeyQuery(componentQuery).setQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW).build();
      List<ComponentDto> componentDtos = dbClient.componentDao().selectByQuery(dbSession, componentDtoQuery, 0, CeTaskQuery.MAX_COMPONENT_UUIDS);
      query.setComponentUuids(Lists.transform(componentDtos, ComponentDtoFunctions.toUuid()));
    }
  }

  private TaskResult loadQueuedTasks(DbSession dbSession, ActivityWsRequest request, CeTaskQuery query) {
    int total = dbClient.ceQueueDao().countByQuery(dbSession, query);
    List<CeQueueDto> dtos = dbClient.ceQueueDao().selectByQueryInDescOrder(dbSession, query,
      Paging.forPageIndex(request.getPage())
        .withPageSize(request.getPageSize())
        .andTotal(total));
    Iterable<WsCe.Task> tasks = formatter.formatQueue(dbSession, dtos);
    return new TaskResult(tasks, total);
  }

  private TaskResult loadPastTasks(DbSession dbSession, ActivityWsRequest request, CeTaskQuery query, int totalQueuedTasks) {
    int total = dbClient.ceActivityDao().countByQuery(dbSession, query);
    // we have to take into account the total number of queue tasks found
    int offset = Math.max(0, offset(request.getPage(), request.getPageSize()) - totalQueuedTasks);
    List<CeActivityDto> dtos = dbClient.ceActivityDao().selectByQuery(dbSession, query, offset, request.getPageSize());
    Iterable<WsCe.Task> ceTasks = formatter.formatActivity(dbSession, dtos);

    return new TaskResult(ceTasks, total);
  }

  private void checkPermissions(CeTaskQuery query) {
    List<String> componentUuids = query.getComponentUuids();
    if (componentUuids != null && componentUuids.size() == 1) {
      if (!isAllowedOnComponentUuid(userSession, componentUuids.get(0))) {
        throw new ForbiddenException("Requires administration permission");
      }
    } else {
      userSession.checkPermission(UserRole.ADMIN);
    }
  }

  @CheckForNull
  private static Long parseDateTimeAsLong(@Nullable String dateAsString) {
    if (dateAsString == null) {
      return null;
    }

    Date date = parseDateTimeQuietly(dateAsString);
    if (date == null) {
      date = parseDateQuietly(dateAsString);
      checkRequest(date != null, "Date '%s' cannot be parsed as either a date or date+time", dateAsString);
      date = DateUtils.addDays(date, 1);
    }

    return date.getTime();
  }

  public static boolean isAllowedOnComponentUuid(UserSession userSession, String componentUuid) {
    return userSession.hasPermission(GlobalPermissions.SYSTEM_ADMIN) || userSession.hasComponentUuidPermission(UserRole.ADMIN, componentUuid);
  }

  private static ActivityResponse buildResponse(Iterable<WsCe.Task> queuedTasks, Iterable<WsCe.Task> pastTasks, Paging paging) {
    WsCe.ActivityResponse.Builder wsResponseBuilder = WsCe.ActivityResponse.newBuilder();

    int nbInsertedTasks = 0;
    for (WsCe.Task queuedTask : queuedTasks) {
      if (nbInsertedTasks < paging.pageSize()) {
        wsResponseBuilder.addTasks(queuedTask);
        nbInsertedTasks++;
      }
    }

    for (WsCe.Task pastTask : pastTasks) {
      if (nbInsertedTasks < paging.pageSize()) {
        wsResponseBuilder.addTasks(pastTask);
        nbInsertedTasks++;
      }
    }

    wsResponseBuilder.setPaging(Common.Paging.newBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total()));

    return wsResponseBuilder.build();
  }

  private static ActivityWsRequest toSearchWsRequest(Request request) {
    ActivityWsRequest activityWsRequest = new ActivityWsRequest()
      .setComponentId(request.param(PARAM_COMPONENT_ID))
      .setQuery(defaultString(request.param(Param.TEXT_QUERY), request.param(PARAM_COMPONENT_QUERY)))
      .setStatus(request.paramAsStrings(PARAM_STATUS))
      .setType(request.param(PARAM_TYPE))
      .setMinSubmittedAt(request.param(PARAM_MIN_SUBMITTED_AT))
      .setMaxExecutedAt(request.param(PARAM_MAX_EXECUTED_AT))
      .setOnlyCurrents(request.paramAsBoolean(PARAM_ONLY_CURRENTS))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE));

    checkRequest(activityWsRequest.getComponentId() == null || activityWsRequest.getQuery() == null, "%s and %s must not be set at the same time",
      PARAM_COMPONENT_ID, PARAM_COMPONENT_QUERY);
    checkRequest(activityWsRequest.getPageSize() <= MAX_PAGE_SIZE, "The '%s' parameter must be less than %d", Param.PAGE_SIZE, MAX_PAGE_SIZE);

    return activityWsRequest;
  }

  private static class TaskResult {
    private final Iterable<WsCe.Task> tasks;
    private final int total;

    private TaskResult(Iterable<WsCe.Task> tasks, int total) {
      this.tasks = tasks;
      this.total = total;
    }
  }
}
