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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.KEY_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.SECRET_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.SECRET_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.SECRET_PARAM_MINIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.UPDATE_ACTION;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.NAME_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.URL_WEBHOOK_EXAMPLE_001;
import static org.sonarqube.ws.WsUtils.checkArgument;

public class UpdateAction implements WebhooksWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final WebhookSupport webhookSupport;
  private final ComponentFinder componentFinder;

  public UpdateAction(DbClient dbClient, UserSession userSession, WebhookSupport webhookSupport, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.webhookSupport = webhookSupport;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(UPDATE_ACTION)
      .setPost(true)
      .setDescription("Update a Webhook.<br>" +
        "Requires 'Administer' permission on the specified project, or global 'Administer' permission.")
      .setSince("7.1")
      .setHandler(this);

    action.createParam(KEY_PARAM)
      .setRequired(true)
      .setMaximumLength(KEY_PARAM_MAXIMUM_LENGTH)
      .setDescription("The key of the webhook to be updated, " +
        "auto-generated value can be obtained through api/webhooks/create or api/webhooks/list")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(NAME_PARAM)
      .setRequired(true)
      .setMaximumLength(NAME_PARAM_MAXIMUM_LENGTH)
      .setDescription("new name of the webhook")
      .setExampleValue(NAME_WEBHOOK_EXAMPLE_001);

    action.createParam(URL_PARAM)
      .setRequired(true)
      .setMaximumLength(URL_PARAM_MAXIMUM_LENGTH)
      .setDescription("new url to be called by the webhook")
      .setExampleValue(URL_WEBHOOK_EXAMPLE_001);

    action.createParam(SECRET_PARAM)
      .setRequired(false)
      .setMaximumLength(SECRET_PARAM_MAXIMUM_LENGTH)
      .setDescription("If provided, secret will be used as the key to generate the HMAC hex (lowercase) digest value in the 'X-Sonar-Webhook-HMAC-SHA256' header. " +
        "If blank, any secret previously configured will be removed. If not set, the secret will remain unchanged.")
      .setExampleValue("your_secret")
      .setSince("7.8");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String webhookKey = request.mandatoryParam(KEY_PARAM);
    String name = request.mandatoryParam(NAME_PARAM);
    String url = request.mandatoryParam(URL_PARAM);
    String secret = request.param(SECRET_PARAM);

    validateSecretLength(secret);

    webhookSupport.checkUrlPattern(url, "Url parameter with value '%s' is not a valid url", url);

    try (DbSession dbSession = dbClient.openSession(false)) {

      Optional<WebhookDto> dtoOptional = dbClient.webhookDao().selectByUuid(dbSession, webhookKey);
      WebhookDto webhookDto = checkFoundWithOptional(dtoOptional, "No webhook with key '%s'", webhookKey);

      String projectUuid = webhookDto.getProjectUuid();
      if (projectUuid != null) {
        ProjectDto projectDto = componentFinder.getProjectByUuid(dbSession, projectUuid);
        webhookSupport.checkPermission(projectDto);
        updateWebhook(dbSession, webhookDto, name, url, secret, projectDto.getKey(), projectDto.getName());
      } else {
        webhookSupport.checkPermission();
        updateWebhook(dbSession, webhookDto, name, url, secret, null, null);
      }

      dbSession.commit();
    }

    response.noContent();
  }

  private static void validateSecretLength(@Nullable String secret) {
    if (secret != null && !secret.isEmpty()) {
      checkArgument(secret.length() >= SECRET_PARAM_MINIMUM_LENGTH && secret.length() <= SECRET_PARAM_MAXIMUM_LENGTH,
        "Secret length must between %s and %s characters", SECRET_PARAM_MINIMUM_LENGTH, SECRET_PARAM_MAXIMUM_LENGTH);
    }
  }

  private void updateWebhook(DbSession dbSession, WebhookDto dto, String name, String url, @Nullable String secret,
    @Nullable String projectKey, @Nullable String projectName) {
    dto
      .setName(name)
      .setUrl(url);
    setSecret(dto, secret);
    dbClient.webhookDao().update(dbSession, dto, projectKey, projectName);
  }

  /**
   * <p>Sets the secret of the webhook. The secret is set according to the following rules:
   * <ul>
   *   <li>If the secret is null, it will remain unchanged.</li>
   *   <li>If the secret is blank (""), it will be removed.</li>
   *   <li>If the secret is not null or blank, it will be set to the provided value.</li>
   * </ul>
   * </p>
   * @param dto The webhook to update. It holds the old secret value.
   * @param secret The new secret value. It can be null or blank.
   */
  private static void setSecret(WebhookDto dto, @Nullable String secret) {
    if (secret != null) {
      if (isBlank(secret)) {
        dto.setSecret(null);
      } else {
        dto.setSecret(secret);
      }
    }
  }

}
