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
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.webhook.WebhookDeliveryDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Webhooks;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.webhook.ws.WebhookWsSupport.copyDtoToProtobuf;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class WebhookDeliveryAction implements WebhooksWsAction {

  private static final String PARAM_ID = "deliveryId";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public WebhookDeliveryAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("delivery")
      .setSince("6.2")
      .setDescription("Get a webhook delivery by its id.<br/>" +
        "Require 'Administer System' permission.<br/>" +
        "Note that additional information are returned by api/webhooks/delivery.")
      .setResponseExample(Resources.getResource(this.getClass(), "example-delivery.json"))
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setDescription("Id of delivery")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_06);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    // fail-fast if not logged in
    userSession.checkLoggedIn();

    Data data = loadFromDatabase(request.mandatoryParam(PARAM_ID));
    data.ensureAdminPermission(userSession);
    data.writeTo(request, response);
  }

  private Data loadFromDatabase(String deliveryUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<WebhookDeliveryDto> delivery = dbClient.webhookDeliveryDao().selectByUuid(dbSession, deliveryUuid);
      checkFoundWithOptional(delivery, "Webhook delivery not found");
      ComponentDto component = componentFinder.getByUuid(dbSession, delivery.get().getComponentUuid());
      return new Data(component, delivery.get());
    }
  }

  private static class Data {
    private final ComponentDto component;
    private final WebhookDeliveryDto deliveryDto;

    Data(ComponentDto component, WebhookDeliveryDto delivery) {
      this.deliveryDto = requireNonNull(delivery);
      this.component = requireNonNull(component);
    }

    void ensureAdminPermission(UserSession userSession) {
      userSession.checkComponentPermission(UserRole.ADMIN, component);
    }

    void writeTo(Request request, Response response) {
      Webhooks.DeliveryWsResponse.Builder responseBuilder = Webhooks.DeliveryWsResponse.newBuilder();
      Webhooks.Delivery.Builder deliveryBuilder = Webhooks.Delivery.newBuilder();
      copyDtoToProtobuf(component, deliveryDto, deliveryBuilder);
      responseBuilder.setDelivery(deliveryBuilder);

      writeProtobuf(responseBuilder.build(), request, response);
    }
  }
}
