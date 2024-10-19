/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.webhook.ws;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDeliveryLiteDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Webhooks;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.webhook.ws.WebhookWsSupport.copyDtoToProtobuf;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class WebhookDeliveriesAction implements WebhooksWsAction {

  private static final String PARAM_COMPONENT = "componentKey";
  private static final String PARAM_TASK = "ceTaskId";
  private static final String PARAM_WEBHOOK = "webhook";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public WebhookDeliveriesAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("deliveries")
      .setSince("6.2")
      .setDescription("Get the recent deliveries for a specified project or Compute Engine task.<br/>" +
        "Require 'Administer' permission on the related project.<br/>" +
        "Note that additional information are returned by api/webhooks/delivery.")
      .setResponseExample(getClass().getResource("example-deliveries.json"))
      .setChangelog(
        new Change("10.7",
          "'ceTaskId' and 'componentKey' parameters are now deprecated. These parameters won't be replaced, the deliveries related to a " +
            "specific project can be obtained by fetching the webhook first, and then fetching the associated deliveries."),
        new Change("10.7",
          "'ceTaskId' response field is now deprecated."))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Key of the project")
      .setDeprecatedSince("10.7")
      .setExampleValue("my-project");

    action.createParam(PARAM_TASK)
      .setDescription("Id of the Compute Engine task")
      .setDeprecatedSince("10.7")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_WEBHOOK)
      .setSince("7.1")
      .setDescription("Key of the webhook that triggered those deliveries, " +
        "auto-generated value that can be obtained through api/webhooks/create or api/webhooks/list")
      .setExampleValue(UUID_EXAMPLE_02);

    action.addPagingParamsSince(10, MAX_PAGE_SIZE, "7.1");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    // fail-fast if not logged in
    userSession.checkLoggedIn();

    String ceTaskId = request.param(PARAM_TASK);
    String projectKey = request.param(PARAM_COMPONENT);
    String webhookUuid = request.param(PARAM_WEBHOOK);
    int page = request.mandatoryParamAsInt(PAGE);
    int pageSize = request.mandatoryParamAsInt(PAGE_SIZE);

    checkArgument(webhookUuid != null ^ (ceTaskId != null ^ projectKey != null),
      "Either '%s' or '%s' or '%s' must be provided", PARAM_TASK, PARAM_COMPONENT, PARAM_WEBHOOK);

    Data data = loadFromDatabase(webhookUuid, ceTaskId, projectKey, page, pageSize);
    data.ensureAdminPermission(userSession);
    data.writeTo(request, response);
  }

  private Data loadFromDatabase(@Nullable String webhookUuid, @Nullable String ceTaskId, @Nullable String projectKey, int page, int pageSize) {
    Map<String, ProjectDto> projectUuidMap;
    List<WebhookDeliveryLiteDto> deliveries;
    int totalElements;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (isNotBlank(webhookUuid)) {
        totalElements = dbClient.webhookDeliveryDao().countDeliveriesByWebhookUuid(dbSession, webhookUuid);
        deliveries = dbClient.webhookDeliveryDao().selectByWebhookUuid(dbSession, webhookUuid, Pagination.forPage(page).andSize(pageSize));
        projectUuidMap = getProjectsDto(dbSession, deliveries);
      } else if (projectKey != null) {
        ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
        projectUuidMap = new HashMap<>();
        projectUuidMap.put(project.getUuid(), project);
        totalElements = dbClient.webhookDeliveryDao().countDeliveriesByProjectUuid(dbSession, project.getUuid());
        deliveries = dbClient.webhookDeliveryDao().selectOrderedByProjectUuid(dbSession, project.getUuid(), Pagination.forPage(page).andSize(pageSize));
      } else {
        totalElements = dbClient.webhookDeliveryDao().countDeliveriesByCeTaskUuid(dbSession, ceTaskId);
        deliveries = dbClient.webhookDeliveryDao().selectOrderedByCeTaskUuid(dbSession, ceTaskId, Pagination.forPage(page).andSize(pageSize));
        projectUuidMap = getProjectsDto(dbSession, deliveries);
      }
    }
    return new Data(projectUuidMap, deliveries).withPagingInfo(page, pageSize, totalElements);
  }

  private Map<String, ProjectDto> getProjectsDto(DbSession dbSession, List<WebhookDeliveryLiteDto> deliveries) {
    Map<String, String> deliveredComponentUuid = deliveries
      .stream()
      .collect(Collectors.toMap(WebhookDeliveryLiteDto::getUuid, WebhookDeliveryLiteDto::getProjectUuid));

    if (!deliveredComponentUuid.isEmpty()) {
      return dbClient.projectDao().selectByUuids(dbSession, new HashSet<>(deliveredComponentUuid.values()))
        .stream()
        .collect(Collectors.toMap(ProjectDto::getUuid, Function.identity()));
    } else {
      return Collections.emptyMap();
    }
  }

  private static class Data {
    private final Map<String, ProjectDto> projectUuidMap;
    private final List<WebhookDeliveryLiteDto> deliveryDtos;

    private int pageIndex;
    private int pageSize;
    private int totalElements;

    Data(Map<String, ProjectDto> projectUuidMap, List<WebhookDeliveryLiteDto> deliveries) {
      this.deliveryDtos = deliveries;
      if (deliveries.isEmpty()) {
        this.projectUuidMap = projectUuidMap;
      } else {
        checkArgument(!projectUuidMap.isEmpty());
        this.projectUuidMap = projectUuidMap;
      }
    }

    void ensureAdminPermission(UserSession userSession) {
      if (!projectUuidMap.isEmpty()) {
        List<ProjectDto> projectsUserHasAccessTo = userSession.keepAuthorizedEntities(UserRole.ADMIN, projectUuidMap.values());
        if (projectsUserHasAccessTo.size() != projectUuidMap.size()) {
          throw new ForbiddenException("Insufficient privileges");
        }
      }
    }

    void writeTo(Request request, Response response) {
      Webhooks.DeliveriesWsResponse.Builder responseBuilder = Webhooks.DeliveriesWsResponse.newBuilder();
      Webhooks.Delivery.Builder deliveryBuilder = Webhooks.Delivery.newBuilder();
      for (WebhookDeliveryLiteDto dto : deliveryDtos) {
        ProjectDto project = projectUuidMap.get(dto.getProjectUuid());
        copyDtoToProtobuf(project, dto, deliveryBuilder);
        responseBuilder.addDeliveries(deliveryBuilder);
      }

      responseBuilder.setPaging(buildPaging(pageIndex, pageSize, totalElements));
      writeProtobuf(responseBuilder.build(), request, response);
    }

    static Common.Paging buildPaging(int pageIndex, int pageSize, int totalElements) {
      return Common.Paging.newBuilder()
        .setPageIndex(pageIndex)
        .setPageSize(pageSize)
        .setTotal(totalElements)
        .build();
    }

    public Data withPagingInfo(int pageIndex, int pageSize, int totalElements) {
      this.pageIndex = pageIndex;
      this.pageSize = pageSize;
      this.totalElements = totalElements;
      return this;
    }
  }
}
