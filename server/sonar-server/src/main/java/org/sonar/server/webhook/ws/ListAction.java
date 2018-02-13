/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.io.Resources;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Webhooks.ListWsResponse.Builder;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.LIST_ACTION;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.ORGANIZATION_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;
import static org.sonar.server.ws.KeyExamples.KEY_ORG_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkStateWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Webhooks.ListWsResponse.newBuilder;

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
      .setResponseExample(Resources.getResource(this.getClass(), "example-webhooks-search.json"))
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

  }

  @Override
  public void handle(Request request, Response response) throws Exception {

    String projectKey = request.param(PROJECT_KEY_PARAM);
    String organizationKey = request.param(ORGANIZATION_KEY_PARAM);

    userSession.checkLoggedIn();

    writeResponse(request, response, doHandle(organizationKey, projectKey));

  }

  private List<WebhookDto> doHandle(@Nullable String organizationKey, @Nullable String projectKey) {

    try (DbSession dbSession = dbClient.openSession(true)) {

      OrganizationDto organizationDto;
      if (isNotBlank(organizationKey)) {
        Optional<OrganizationDto> dtoOptional = dbClient.organizationDao().selectByKey(dbSession, organizationKey);
        organizationDto = checkFoundWithOptional(dtoOptional, "No organization with key '%s'", organizationKey);
      } else {
        organizationDto = defaultOrganizationDto(dbSession);
      }

      if (isNotBlank(projectKey)) {

        Optional<ComponentDto> optional = ofNullable(dbClient.componentDao().selectByKey(dbSession, projectKey).orNull());
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
  }

  private static void writeResponse(Request request, Response response, List<WebhookDto> webhookDtos) {

    Builder responseBuilder = newBuilder();

    webhookDtos
      .stream()
      .forEach(webhook -> responseBuilder.addWebhooksBuilder()
        .setKey(webhook.getUuid())
        .setName(webhook.getName())
        .setUrl(webhook.getUrl()));

    writeProtobuf(responseBuilder.build(), request, response);
  }

  private OrganizationDto defaultOrganizationDto(DbSession dbSession) {
    String uuid = defaultOrganizationProvider.get().getUuid();
    Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByUuid(dbSession, uuid);
    return checkStateWithOptional(organizationDto, "the default organization '%s' was not found", uuid);
  }

}
