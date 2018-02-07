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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Webhooks;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.ACTION_CREATE;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.ORGANIZATION_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.ORGANIZATION_KEY_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM_MAXIMUN_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.ws.KeyExamples.KEY_ORG_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.NAME_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.URL_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Webhooks.CreateWsResponse.Webhook;
import static org.sonarqube.ws.Webhooks.CreateWsResponse.newBuilder;

public class CreateAction implements WebhooksWsAction {

  private static final int MAX_NUMBER_OF_WEBHOOKS = 10;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final UuidFactory uuidFactory;
  private final System2 system;

  public CreateAction(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider, UuidFactory uuidFactory, System2 system) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.uuidFactory = uuidFactory;
    this.system = system;
  }

  @Override
  public void define(WebService.NewController controller) {

    WebService.NewAction action = controller.createAction(ACTION_CREATE)
      .setPost(true)
      .setDescription("Create a Webhook.<br>" +
        "Requires the global, organization or project permission.")
      .setSince("7.1")
      .setResponseExample(getClass().getResource("example-webhook-create.json"))
      .setHandler(this);

    action.createParam(NAME_PARAM)
      .setRequired(true)
      .setMaximumLength(NAME_PARAM_MAXIMUM_LENGTH)
      .setDescription("The name of the webhook to create")
      .setExampleValue(NAME_WEBHOOK_EXAMPLE_001);

    action.createParam(URL_PARAM)
      .setRequired(true)
      .setMaximumLength(URL_PARAM_MAXIMUM_LENGTH)
      .setDescription("The url to be called by the webhook")
      .setExampleValue(URL_WEBHOOK_EXAMPLE_001);

    action.createParam(PROJECT_KEY_PARAM)
      .setRequired(false)
      .setMaximumLength(PROJECT_KEY_PARAM_MAXIMUN_LENGTH)
      .setDescription("The key of the project that will own the webhook")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(ORGANIZATION_KEY_PARAM)
      .setInternal(true)
      .setRequired(false)
      .setMaximumLength(ORGANIZATION_KEY_PARAM_MAXIMUM_LENGTH)
      .setDescription("The key of the organization that will own the webhook")
      .setExampleValue(KEY_ORG_EXAMPLE_001);

  }

  @Override
  public void handle(Request request, Response response) throws Exception {

    userSession.checkLoggedIn();

    String name = request.mandatoryParam(NAME_PARAM);
    String url = request.mandatoryParam(URL_PARAM);
    String projectKey = request.param(PROJECT_KEY_PARAM);
    String organizationKey = request.param(ORGANIZATION_KEY_PARAM);

    try (DbSession dbSession = dbClient.openSession(true)) {

      OrganizationDto organizationDto;
      if (isNotBlank(organizationKey)) {
        Optional<OrganizationDto> dtoOptional = dbClient.organizationDao().selectByKey(dbSession, organizationKey);
        organizationDto = checkFoundWithOptional(dtoOptional, "No organization with key '%s'", organizationKey);
      } else {
        organizationDto = defaultOrganizationDto(dbSession);
      }

      ComponentDto projectDto = null;
      if (isNotBlank(projectKey)) {
        com.google.common.base.Optional<ComponentDto> dtoOptional = dbClient.componentDao().selectByKey(dbSession, projectKey);
        checkFoundWithOptional(dtoOptional, "No project with key '%s'", projectKey);
        checkThatProjectBelongsToOrganization(dtoOptional.get(), organizationDto, "Project '%s' does not belong to organisation '%s'", projectKey, organizationKey);
        checkUserPermissionOn(dtoOptional.get());
        projectDto = dtoOptional.get();
      } else {
        checkUserPermissionOn(organizationDto);
      }

      checkUrlPattern(url, "Url parameter with value '%s' is not a valid url", url);

      writeResponse(request, response, doHandle(dbSession, organizationDto, projectDto, name, url));
    }

  }

  private WebhookDto doHandle(DbSession dbSession, @Nullable OrganizationDto organization, @Nullable ComponentDto project, String name, String url) {

    checkState(organization != null || project != null,
      "A webhook can not be created if not linked to an organization or a project.");

    WebhookDto dto = new WebhookDto()
      .setUuid(uuidFactory.create())
      .setName(name)
      .setUrl(url)

    if (project != null) {
      checkNumberOfWebhook(numberOfWebhookOf(dbSession, project), "Maximum number of webhook reached for project '%s'", project.getKey());
      dto.setProjectUuid(project.projectUuid());
    } else {
      checkNumberOfWebhook(numberOfWebhookOf(dbSession, organization), "Maximum number of webhook reached for organization '%s'", organization.getKey());
      dto.setOrganizationUuid(organization.getUuid());
    }

    dbClient.webhookDao().insert(dbSession, dto);

    return dto;
  }

  private static void writeResponse(Request request, Response response, WebhookDto element) {
    Webhooks.CreateWsResponse.Builder responseBuilder = newBuilder();
    responseBuilder.setWebhook(Webhook.newBuilder()
      .setKey(element.getUuid())
      .setName(element.getName())
      .setUrl(element.getUrl()));

    writeProtobuf(responseBuilder.build(), request, response);
  }

  private static void checkNumberOfWebhook(int nbOfWebhooks, String message, Object... messageArguments) {
    if (nbOfWebhooks >= MAX_NUMBER_OF_WEBHOOKS){
      throw new IllegalArgumentException(format(message, messageArguments));
    }
  }

  private int numberOfWebhookOf(DbSession dbSession, OrganizationDto organization) {
    return dbClient.webhookDao().selectByOrganizationUuid(dbSession, organization.getUuid()).size();
  }

  private int numberOfWebhookOf(DbSession dbSession, ComponentDto project) {
    return dbClient.webhookDao().selectByProjectUuid(dbSession, project.uuid()).size();
  }

  private void checkUserPermissionOn(ComponentDto componentDto) {
    userSession.checkComponentPermission(ADMIN, componentDto);
  }

  private void checkUserPermissionOn(OrganizationDto organizationDto) {
    userSession.checkPermission(ADMINISTER, organizationDto);
  }

  private static void checkThatProjectBelongsToOrganization(ComponentDto componentDto, OrganizationDto organizationDto, String message, Object... messageArguments) {
    if (!organizationDto.getUuid().equals(componentDto.getOrganizationUuid())) {
      throw new NotFoundException(format(message, messageArguments));
    }
  }

  private static void checkUrlPattern(String url, String message, Object... messageArguments) {
    if (!url.toLowerCase(ENGLISH).startsWith("http://") && !url.toLowerCase(ENGLISH).startsWith("https://")) {
      throw new IllegalArgumentException(format(message, messageArguments));
    }
    String sub = url.substring("http://".length());
    if (sub.contains(":") && !sub.substring(sub.indexOf(':')).contains("@")) {
      throw new IllegalArgumentException(format(message, messageArguments));
    }
  }

  private OrganizationDto defaultOrganizationDto(DbSession dbSession) {
    String uuid = defaultOrganizationProvider.get().getUuid();
    return dbClient.organizationDao().selectByUuid(dbSession, uuid).get();
  }
}
