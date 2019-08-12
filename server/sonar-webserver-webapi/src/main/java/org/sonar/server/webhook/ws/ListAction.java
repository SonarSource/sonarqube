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
package org.sonar.server.webhook.ws;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.webhook.WebhookDeliveryLiteDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Webhooks;
import org.sonarqube.ws.Webhooks.ListResponse;
import org.sonarqube.ws.Webhooks.ListResponseElement;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.webhook.HttpUrlHelper.obfuscateCredentials;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.LIST_ACTION;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.ORGANIZATION_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;
import static org.sonar.server.ws.KeyExamples.KEY_ORG_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkStateWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements WebhooksWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final WebhookSupport webhookSupport;

  public ListAction(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider, WebhookSupport webhookSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.webhookSupport = webhookSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(LIST_ACTION)
      .setDescription("Search for global webhooks or project webhooks. Webhooks are ordered by name.<br>" +
        "Requires 'Administer' permission on the specified project, or global 'Administer' permission.")
      .setSince("7.1")
      .setResponseExample(getClass().getResource("example-webhooks-list.json"))
      .setHandler(this);

    action.createParam(ORGANIZATION_KEY_PARAM)
      .setDescription("Organization key. If no organization is provided, the default organization is used.")
      .setInternal(true)
      .setRequired(false)
      .setExampleValue(KEY_ORG_EXAMPLE_001);

    action.createParam(PROJECT_KEY_PARAM)
      .setDescription("Project key")
      .setRequired(false)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.setChangelog(new Change("7.8", "Field 'secret' added to response"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.param(PROJECT_KEY_PARAM);
    String organizationKey = request.param(ORGANIZATION_KEY_PARAM);

    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(true)) {
      List<WebhookDto> webhookDtos = doHandle(dbSession, organizationKey, projectKey);
      Map<String, WebhookDeliveryLiteDto> lastDeliveries = loadLastDeliveriesOf(dbSession, webhookDtos);
      writeResponse(request, response, webhookDtos, lastDeliveries);
    }
  }

  private Map<String, WebhookDeliveryLiteDto> loadLastDeliveriesOf(DbSession dbSession, List<WebhookDto> webhookDtos) {
    return dbClient.webhookDeliveryDao().selectLatestDeliveries(dbSession, webhookDtos);
  }

  private List<WebhookDto> doHandle(DbSession dbSession, @Nullable String organizationKey, @Nullable String projectKey) {
    OrganizationDto organizationDto;
    if (isNotBlank(organizationKey)) {
      Optional<OrganizationDto> dtoOptional = dbClient.organizationDao().selectByKey(dbSession, organizationKey);
      organizationDto = checkFoundWithOptional(dtoOptional, "No organization with key '%s'", organizationKey);
    } else {
      organizationDto = defaultOrganizationDto(dbSession);
    }

    if (isNotBlank(projectKey)) {
      Optional<ComponentDto> optional = dbClient.componentDao().selectByKey(dbSession, projectKey);
      ComponentDto componentDto = checkFoundWithOptional(optional, "project %s does not exist", projectKey);
      webhookSupport.checkPermission(componentDto);
      webhookSupport.checkThatProjectBelongsToOrganization(componentDto, organizationDto, "Project '%s' does not belong to organisation '%s'", projectKey, organizationKey);
      webhookSupport.checkPermission(componentDto);
      return dbClient.webhookDao().selectByProject(dbSession, componentDto);

    } else {
      webhookSupport.checkPermission(organizationDto);
      return dbClient.webhookDao().selectByOrganization(dbSession, organizationDto);
    }
  }

  private static void writeResponse(Request request, Response response, List<WebhookDto> webhookDtos, Map<String, WebhookDeliveryLiteDto> lastDeliveries) {
    ListResponse.Builder responseBuilder = ListResponse.newBuilder();
    webhookDtos
      .forEach(webhook -> {
        ListResponseElement.Builder responseElementBuilder = responseBuilder.addWebhooksBuilder();
        responseElementBuilder
          .setKey(webhook.getUuid())
          .setName(webhook.getName())
          .setUrl(obfuscateCredentials(webhook.getUrl()));
        if (webhook.getSecret() != null) {
          responseElementBuilder.setSecret(webhook.getSecret());
        }
        addLastDelivery(responseElementBuilder, webhook, lastDeliveries);
      });
    writeProtobuf(responseBuilder.build(), request, response);
  }

  private static void addLastDelivery(ListResponseElement.Builder responseElementBuilder, WebhookDto webhook, Map<String, WebhookDeliveryLiteDto> lastDeliveries) {
    if (lastDeliveries.containsKey(webhook.getUuid())) {
      WebhookDeliveryLiteDto delivery = lastDeliveries.get(webhook.getUuid());
      Webhooks.LatestDelivery.Builder builder = responseElementBuilder.getLatestDeliveryBuilder()
        .setId(delivery.getUuid())
        .setAt(formatDateTime(delivery.getCreatedAt()))
        .setSuccess(delivery.isSuccess());
      if (delivery.getHttpStatus() != null) {
        builder.setHttpStatus(delivery.getHttpStatus());
      }
      if (delivery.getDurationMs() != null) {
        builder.setDurationMs(delivery.getDurationMs());
      }
      builder.build();
    }
  }

  private OrganizationDto defaultOrganizationDto(DbSession dbSession) {
    String uuid = defaultOrganizationProvider.get().getUuid();
    Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByUuid(dbSession, uuid);
    return checkStateWithOptional(organizationDto, "the default organization '%s' was not found", uuid);
  }
}
