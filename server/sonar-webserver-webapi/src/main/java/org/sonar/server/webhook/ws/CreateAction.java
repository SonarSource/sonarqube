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

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.ACTION_CREATE;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.SECRET_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.SECRET_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.SECRET_PARAM_MINIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.NAME_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.URL_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Webhooks.CreateWsResponse.Webhook;
import static org.sonarqube.ws.Webhooks.CreateWsResponse.newBuilder;

public class CreateAction implements WebhooksWsAction {

  private static final int MAX_NUMBER_OF_WEBHOOKS = 10;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final UuidFactory uuidFactory;
  private final WebhookSupport webhookSupport;
  private final ComponentFinder componentFinder;

  public CreateAction(DbClient dbClient, UserSession userSession, UuidFactory uuidFactory, WebhookSupport webhookSupport, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.uuidFactory = uuidFactory;
    this.webhookSupport = webhookSupport;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_CREATE)
      .setPost(true)
      .setDescription("Create a Webhook.<br>" +
        "Requires 'Administer' permission on the specified project, or global 'Administer' permission.")
      .setSince("7.1")
      .setChangelog(new Change("10.6", "The minimum length of parameter '" + SECRET_PARAM + "' increased to 16."))
      .setResponseExample(getClass().getResource("example-webhook-create.json"))
      .setHandler(this);

    action.createParam(NAME_PARAM)
      .setRequired(true)
      .setMaximumLength(NAME_PARAM_MAXIMUM_LENGTH)
      .setDescription("Name displayed in the administration console of webhooks")
      .setExampleValue(NAME_WEBHOOK_EXAMPLE_001);

    action.createParam(URL_PARAM)
      .setRequired(true)
      .setMaximumLength(URL_PARAM_MAXIMUM_LENGTH)
      .setDescription("Server endpoint that will receive the webhook payload, for example 'http://my_server/foo'." +
        " If HTTP Basic authentication is used, HTTPS is recommended to avoid man in the middle attacks." +
        " Example: 'https://myLogin:myPassword@my_server/foo'")
      .setExampleValue(URL_WEBHOOK_EXAMPLE_001);

    action.createParam(PROJECT_KEY_PARAM)
      .setRequired(false)
      .setMaximumLength(PROJECT_KEY_PARAM_MAXIMUM_LENGTH)
      .setDescription("The key of the project that will own the webhook")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(SECRET_PARAM)
      .setRequired(false)
      .setMinimumLength(SECRET_PARAM_MINIMUM_LENGTH)
      .setMaximumLength(SECRET_PARAM_MAXIMUM_LENGTH)
      .setDescription("If provided, secret will be used as the key to generate the HMAC hex (lowercase) digest value in the 'X-Sonar-Webhook-HMAC-SHA256' header")
      .setExampleValue("your_secret")
      .setSince("7.8");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String name = request.mandatoryParam(NAME_PARAM);
    String url = request.mandatoryParam(URL_PARAM);
    String projectKey = request.param(PROJECT_KEY_PARAM);
    String secret = request.param(SECRET_PARAM);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto projectDto = null;
      if (isNotBlank(projectKey)) {
        projectDto = componentFinder.getProjectByKey(dbSession, projectKey);
        webhookSupport.checkPermission(projectDto);
      } else {
        webhookSupport.checkPermission();
      }

      webhookSupport.checkUrlPattern(url, "Url parameter with value '%s' is not a valid url", url);
      WebhookDto dto = doHandle(dbSession, projectDto, name, url, secret);
      String projectName = projectDto == null ? null : projectDto.getName();
      dbClient.webhookDao().insert(dbSession, dto, projectKey, projectName);
      dbSession.commit();
      writeResponse(request, response, dto);
    }

  }

  private WebhookDto doHandle(DbSession dbSession, @Nullable ProjectDto project, String name, String url, @Nullable String secret) {
    WebhookDto dto = new WebhookDto()
      .setUuid(uuidFactory.create())
      .setName(name)
      .setUrl(url)
      .setSecret(secret);

    if (project != null) {
      checkNumberOfWebhook(numberOfWebhookOf(dbSession, project), project.getKey());
      dto.setProjectUuid(project.getUuid());
    } else {
      checkNumberOfGlobalWebhooks(dbSession);
    }

    return dto;
  }

  private static void writeResponse(Request request, Response response, WebhookDto dto) {
    Webhook.Builder webhookBuilder = Webhook.newBuilder();
    webhookBuilder
      .setKey(dto.getUuid())
      .setName(dto.getName())
      .setUrl(dto.getUrl())
      .setHasSecret(dto.getSecret() != null);
    writeProtobuf(newBuilder().setWebhook(webhookBuilder).build(), request, response);
  }

  private static void checkNumberOfWebhook(int nbOfWebhooks, String projectKey) {
    if (nbOfWebhooks >= MAX_NUMBER_OF_WEBHOOKS) {
      throw new IllegalArgumentException(format("Maximum number of webhook reached for project '%s'", projectKey));
    }
  }

  private int numberOfWebhookOf(DbSession dbSession, ProjectDto projectDto) {
    return dbClient.webhookDao().selectByProject(dbSession, projectDto).size();
  }

  private void checkNumberOfGlobalWebhooks(DbSession dbSession) {
    int globalWebhooksCount = dbClient.webhookDao().selectGlobalWebhooks(dbSession).size();
    if (globalWebhooksCount >= MAX_NUMBER_OF_WEBHOOKS) {
      throw new IllegalArgumentException("Maximum number of global webhooks reached");
    }
  }
}
