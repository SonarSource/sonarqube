/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.ce.ws;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskQuery;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Ce.ActivityResponse;
import org.sonarqube.ws.Common;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.max;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_MAX_EXECUTED_AT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_MIN_SUBMITTED_AT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_ONLY_CURRENTS;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_STATUS;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_TYPE;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ActivityAction implements CeWsAction {
  private static final int MAX_PAGE_SIZE = 1000;
  private static final String[] POSSIBLE_QUALIFIERS = new String[]{ComponentQualifiers.PROJECT, ComponentQualifiers.APP, ComponentQualifiers.VIEW};
  private static final String INVALID_QUERY_PARAM_ERROR_MESSAGE = "%s and %s must not be set at the same time";

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
                             "or project administration permission if %s is set.", PARAM_COMPONENT))
      .setResponseExample(getClass().getResource("activity-example.json"))
      .setHandler(this)
      .setChangelog(
        new Change("5.5", "it's no more possible to specify the page parameter."),
        new Change("6.1", "field \"logs\" is deprecated and its value is always false"),
        new Change("6.6", "fields \"branch\" and \"branchType\" added"),
        new Change("7.1", "field \"pullRequest\" added"),
        new Change("7.6", format("The use of module keys in parameters '%s' is deprecated", TEXT_QUERY)),
        new Change("8.8", "field \"logs\" is dropped"),
        new Change("10.0", "Remove deprecated field 'componentId'"),
        new Change("10.1", String.format("The use of module keys in parameter '%s' is removed", PARAM_COMPONENT)),
        new Change("10.1", "Warnings field will be now be filled (it was always empty in the past)."),
        new Change("10.4", "field \"infoMessages\" added to response")
      )
      .setSince("5.2");

    action.createParam(PARAM_COMPONENT)
      .setDescription("Key of the component (project) to filter on")
      .setExampleValue("projectKey")
      .setSince("8.0");

    action.createParam(TEXT_QUERY)
      .setDescription(format("Limit search to: <ul>" +
                             "<li>component names that contain the supplied string</li>" +
                             "<li>component keys that are exactly the same as the supplied string</li>" +
                             "<li>task ids that are exactly the same as the supplied string</li>" +
                             "</ul>"))
      .setExampleValue("Apache")
      .setSince("5.5");
    action.createParam(PARAM_STATUS)
      .setDescription("Comma separated list of task statuses")
      .setPossibleValues(ImmutableList.builder()
        .addAll(asList(CeActivityDto.Status.values()))
        .addAll(asList(CeQueueDto.Status.values())).build())
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
      .setExampleValue("2017-10-19T13:00:00+0200");
    action.createParam(PARAM_MAX_EXECUTED_AT)
      .setDescription("Maximum date of end of task processing (inclusive)")
      .setExampleValue("2017-10-19T13:00:00+0200");
    action.createPageSize(100, MAX_PAGE_SIZE);
    action.createPageParam()
      .setSince("9.4");
  }

  @Override
  public void handle(org.sonar.api.server.ws.Request wsRequest, Response wsResponse) throws Exception {
    ActivityResponse activityResponse = doHandle(toSearchWsRequest(wsRequest));
    writeProtobuf(activityResponse, wsRequest, wsResponse);
  }

  private ActivityResponse doHandle(Request request) {

    try (DbSession dbSession = dbClient.openSession(false)) {
      EntityDto entity = loadEntity(dbSession, request);
      checkPermission(entity);
      // if a task searched by uuid is found all other parameters are ignored
      Optional<Ce.Task> taskSearchedById = searchTaskByUuid(dbSession, request);
      if (taskSearchedById.isPresent()) {
        return buildResponse(
          singletonList(taskSearchedById.get()),
          Collections.emptyList(),
          Paging.forPageIndex(1).withPageSize(1).andTotal(1), 0);
      }

      CeTaskQuery query = buildQuery(dbSession, request, entity);

      return buildPaginatedResponse(dbSession, query, parseInt(request.getP()), parseInt(request.getPs()));
    }
  }

  @NotNull
  private ActivityResponse buildPaginatedResponse(DbSession dbSession, CeTaskQuery query, int page, int pageSize) {

    int totalQueuedTasks = countQueuedTasks(dbSession, query);
    int totalPastTasks = countPastTasks(dbSession, query);
    int totalTasks = totalQueuedTasks + totalPastTasks;
    int pastTasksEffectivePage = calculateEffectivePage(page, pageSize, totalQueuedTasks);

    validatePagingParameters(page, pageSize, totalTasks);

    List<Ce.Task> queuedTasks = loadQueuedTasks(dbSession, query, page, pageSize);
    List<Ce.Task> pastTasks = loadPastTasks(dbSession, query, pastTasksEffectivePage, pageSize);

    return buildResponse(
      queuedTasks,
      pastTasks,
      Paging.forPageIndex(page).withPageSize(pageSize).andTotal(totalTasks),
      getItemsInLastPage(pageSize, totalQueuedTasks));
  }

  private static void validatePagingParameters(int page, int pageSize, int totalResults) {
    if (page > 1) {
      int firstResultIndex = (page - 1) * pageSize + 1;
      checkArgument(firstResultIndex <= totalResults, "Can return only the first %s results. %sth result asked.", totalResults, firstResultIndex);
    }
  }

  private static int calculateEffectivePage(int page, int pageSize, int otherDatasetSize) {
    if (pageSize > 0 && page > 0) {
      int effectivePage = page - (otherDatasetSize / pageSize);
      if (getItemsInLastPage(pageSize, otherDatasetSize) > 0) {
        effectivePage--;
      }
      return max(1, effectivePage);
    }
    return page;
  }

  private static int getItemsInLastPage(int pageSize, int totalItems) {
    if (totalItems > pageSize) {
      return totalItems % pageSize;
    }
    return 0;
  }

  @CheckForNull
  private EntityDto loadEntity(DbSession dbSession, Request request) {
    String componentKey = request.getComponent();

    if (componentKey != null) {
      Optional<EntityDto> foundEntity;
      foundEntity = dbClient.entityDao().selectByKey(dbSession, componentKey);
      return checkFoundWithOptional(foundEntity, "Component '%s' does not exist", componentKey);
    } else {
      return null;
    }
  }

  private void checkPermission(@Nullable EntityDto entity) {
    // fail fast if not logged in
    userSession.checkLoggedIn();

    if (entity == null) {
      userSession.checkIsSystemAdministrator();
    } else {
      userSession.checkEntityPermission(UserRole.ADMIN, entity);
    }
  }

  private Optional<Ce.Task> searchTaskByUuid(DbSession dbSession, Request request) {
    String textQuery = request.getQ();
    if (textQuery == null) {
      return Optional.empty();
    }

    Optional<CeQueueDto> queue = dbClient.ceQueueDao().selectByUuid(dbSession, textQuery);
    if (queue.isPresent()) {
      return Optional.of(formatter.formatQueue(dbSession, queue.get()));
    }

    Optional<CeActivityDto> activity = dbClient.ceActivityDao().selectByUuid(dbSession, textQuery);
    return activity.map(ceActivityDto -> formatter.formatActivity(dbSession, ceActivityDto, null));
  }

  private CeTaskQuery buildQuery(DbSession dbSession, Request request, @Nullable EntityDto entity) {
    CeTaskQuery query = new CeTaskQuery();
    query.setType(request.getType());
    query.setOnlyCurrents(parseBoolean(request.getOnlyCurrents()));
    Date minSubmittedAt = parseStartingDateOrDateTime(request.getMinSubmittedAt());
    query.setMinSubmittedAt(minSubmittedAt == null ? null : minSubmittedAt.getTime());
    Date maxExecutedAt = parseEndingDateOrDateTime(request.getMaxExecutedAt());
    query.setMaxExecutedAt(maxExecutedAt == null ? null : maxExecutedAt.getTime());

    List<String> statuses = request.getStatus();
    if (statuses != null && !statuses.isEmpty()) {
      query.setStatuses(request.getStatus());
    }

    String componentQuery = request.getQ();
    if (entity != null) {
      query.setEntityUuid(entity.getUuid());
    } else if (componentQuery != null) {
      query.setEntityUuids(loadEntities(dbSession, componentQuery).stream()
        .map(EntityDto::getUuid)
        .toList());
    }
    return query;
  }

  private List<EntityDto> loadEntities(DbSession dbSession, String componentQuery) {
    ComponentQuery componentDtoQuery = ComponentQuery.builder()
      .setNameOrKeyQuery(componentQuery)
      .setQualifiers(POSSIBLE_QUALIFIERS)
      .build();
    List<ComponentDto> componentDtos = dbClient.componentDao().selectByQuery(dbSession, componentDtoQuery, forPage(1).andSize(CeTaskQuery.MAX_COMPONENT_UUIDS));
    return dbClient.entityDao().selectByKeys(dbSession, componentDtos.stream().map(ComponentDto::getKey).collect(toSet()));
  }

  private Integer countQueuedTasks(DbSession dbSession, CeTaskQuery query) {
    return dbClient.ceQueueDao().countByQuery(dbSession, query);
  }

  private List<Ce.Task> loadQueuedTasks(DbSession dbSession, CeTaskQuery query, int page, int pageSize) {
    List<CeQueueDto> dtos = dbClient.ceQueueDao().selectByQueryInDescOrder(dbSession, query, page, pageSize);
    return formatter.formatQueue(dbSession, dtos);
  }

  private Integer countPastTasks(DbSession dbSession, CeTaskQuery query) {
    return dbClient.ceActivityDao().countByQuery(dbSession, query);
  }

  private List<Ce.Task> loadPastTasks(DbSession dbSession, CeTaskQuery query, int page, int pageSize) {
    List<CeActivityDto> dtos = dbClient.ceActivityDao().selectByQuery(dbSession, query, forPage(page).andSize(pageSize));
    return formatter.formatActivity(dbSession, dtos);
  }

  private static ActivityResponse buildResponse(Iterable<Ce.Task> queuedTasks, Iterable<Ce.Task> pastTasks, Paging paging, int offset) {
    Ce.ActivityResponse.Builder wsResponseBuilder = Ce.ActivityResponse.newBuilder();

    Set<String> pastIds = StreamSupport
      .stream(pastTasks.spliterator(), false)
      .map(Ce.Task::getId)
      .collect(toSet());

    int nbInsertedTasks = 0;
    for (Ce.Task queuedTask : queuedTasks) {
      if (nbInsertedTasks < paging.pageSize() && !pastIds.contains(queuedTask.getId())) {
        wsResponseBuilder.addTasks(queuedTask);
        nbInsertedTasks++;
      }
    }

    int pastTasksIndex = nbInsertedTasks;
    for (Ce.Task pastTask : pastTasks) {
      if (nbInsertedTasks < paging.pageSize() && pastTasksIndex >= offset) {
        wsResponseBuilder.addTasks(pastTask);
        nbInsertedTasks++;
      }
      pastTasksIndex++;
    }

    wsResponseBuilder.setPaging(Common.Paging.newBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total()));

    return wsResponseBuilder.build();
  }

  private static Request toSearchWsRequest(org.sonar.api.server.ws.Request request) {
    Request activityWsRequest = new Request()
      .setComponent(request.param(PARAM_COMPONENT))
      .setQ(request.param(TEXT_QUERY))
      .setStatus(request.paramAsStrings(PARAM_STATUS))
      .setType(request.param(PARAM_TYPE))
      .setMinSubmittedAt(request.param(PARAM_MIN_SUBMITTED_AT))
      .setMaxExecutedAt(request.param(PARAM_MAX_EXECUTED_AT))
      .setOnlyCurrents(String.valueOf(request.paramAsBoolean(PARAM_ONLY_CURRENTS)))
      .setPs(String.valueOf(request.mandatoryParamAsInt(Param.PAGE_SIZE)))
      .setP(String.valueOf(request.mandatoryParamAsInt(Param.PAGE)));

    checkRequest(activityWsRequest.getComponent() == null || activityWsRequest.getQ() == null, INVALID_QUERY_PARAM_ERROR_MESSAGE,
      PARAM_COMPONENT, TEXT_QUERY);

    return activityWsRequest;
  }

  private static class Request {

    private String component;
    private String maxExecutedAt;
    private String minSubmittedAt;
    private String onlyCurrents;
    private String p;
    private String ps;
    private String q;
    private List<String> status;
    private String type;

    Request() {
      // Nothing to do
    }

    /**
     * Example value: "sample:src/main/xoo/sample/Sample2.xoo"
     */
    private Request setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    @CheckForNull
    private String getComponent() {
      return component;
    }

    /**
     * Example value: "2017-10-19T13:00:00+0200"
     */
    private Request setMaxExecutedAt(@Nullable String maxExecutedAt) {
      this.maxExecutedAt = maxExecutedAt;
      return this;
    }

    @CheckForNull
    private String getMaxExecutedAt() {
      return maxExecutedAt;
    }

    /**
     * Example value: "2017-10-19T13:00:00+0200"
     */
    private Request setMinSubmittedAt(@Nullable String minSubmittedAt) {
      this.minSubmittedAt = minSubmittedAt;
      return this;
    }

    @CheckForNull
    private String getMinSubmittedAt() {
      return minSubmittedAt;
    }

    /**
     * Possible values:
     * <ul>
     *   <li>"true"</li>
     *   <li>"false"</li>
     *   <li>"yes"</li>
     *   <li>"no"</li>
     * </ul>
     */
    private Request setOnlyCurrents(String onlyCurrents) {
      this.onlyCurrents = onlyCurrents;
      return this;
    }

    private String getOnlyCurrents() {
      return onlyCurrents;
    }

    /**
     * Example value: "20"
     */
    private Request setPs(String ps) {
      this.ps = ps;
      return this;
    }

    private String getPs() {
      return ps;
    }

    /**
     * Example value: "1"
     */
    private Request setP(String p) {
      this.p = p;
      return this;
    }

    private String getP() {
      return p;
    }

    /**
     * Example value: "Apache"
     */
    private Request setQ(@Nullable String q) {
      this.q = q;
      return this;
    }

    @CheckForNull
    private String getQ() {
      return q;
    }

    /**
     * Example value: "IN_PROGRESS,SUCCESS"
     * Possible values:
     * <ul>
     *   <li>"SUCCESS"</li>
     *   <li>"FAILED"</li>
     *   <li>"CANCELED"</li>
     *   <li>"PENDING"</li>
     *   <li>"IN_PROGRESS"</li>
     * </ul>
     */
    private Request setStatus(@Nullable List<String> status) {
      this.status = status;
      return this;
    }

    @CheckForNull
    private List<String> getStatus() {
      return status;
    }

    /**
     * Example value: "REPORT"
     * Possible values:
     * <ul>
     *   <li>"REPORT"</li>
     * </ul>
     */
    private Request setType(@Nullable String type) {
      this.type = type;
      return this;
    }

    @CheckForNull
    private String getType() {
      return type;
    }
  }
}
