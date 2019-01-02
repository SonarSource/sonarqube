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

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.user.UserSession;

import static java.util.Optional.ofNullable;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.KEY_PARAM_MAXIMUN_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.UPDATE_ACTION;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM_MAXIMUM_LENGTH;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.NAME_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.URL_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkStateWithOptional;

public class UpdateAction implements WebhooksWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final WebhookSupport webhookSupport;

  public UpdateAction(DbClient dbClient, UserSession userSession, WebhookSupport webhookSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.webhookSupport = webhookSupport;
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
      .setMaximumLength(KEY_PARAM_MAXIMUN_LENGTH)
      .setDescription("The key of the webhook to be updated, "+
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

  }

  @Override
  public void handle(Request request, Response response) throws Exception {

    userSession.checkLoggedIn();

    String webhookKey = request.param(KEY_PARAM);
    String name = request.mandatoryParam(NAME_PARAM);
    String url = request.mandatoryParam(URL_PARAM);

    webhookSupport.checkUrlPattern(url, "Url parameter with value '%s' is not a valid url", url);

    try (DbSession dbSession = dbClient.openSession(false)) {

      Optional<WebhookDto> dtoOptional = dbClient.webhookDao().selectByUuid(dbSession, webhookKey);
      WebhookDto webhookDto = checkFoundWithOptional(dtoOptional, "No webhook with key '%s'", webhookKey);

      String organizationUuid = webhookDto.getOrganizationUuid();
      if (organizationUuid != null) {
        Optional<OrganizationDto> optionalDto = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid);
        OrganizationDto organizationDto = checkStateWithOptional(optionalDto, "the requested organization '%s' was not found", organizationUuid);
        webhookSupport.checkPermission(organizationDto);
        updateWebhook(dbSession, webhookDto, name, url);
      }

      String projectUuid = webhookDto.getProjectUuid();
      if (projectUuid != null) {
        Optional<ComponentDto> optionalDto = ofNullable(dbClient.componentDao().selectByUuid(dbSession, projectUuid).orElse(null));
        ComponentDto componentDto = checkStateWithOptional(optionalDto, "the requested project '%s' was not found", projectUuid);
        webhookSupport.checkPermission(componentDto);
        updateWebhook(dbSession, webhookDto, name, url);
      }

      dbSession.commit();
    }

    response.noContent();
  }

  private void updateWebhook(DbSession dbSession, WebhookDto webhookDto, String name, String url) {
    dbClient.webhookDao().update(dbSession, webhookDto.setName(name).setUrl(url));
  }

}
