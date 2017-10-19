/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.webhook.WebhookDeliveryLiteDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Webhooks;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.webhook.ws.WebhookWsSupport.copyDtoToProtobuf;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class WebhookDeliveriesAction implements WebhooksWsAction {

  private static final String PARAM_COMPONENT = "componentKey";
  private static final String PARAM_TASK = "ceTaskId";
  private static final String PARAM_BRANCH = "branch";

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
      .setResponseExample(Resources.getResource(this.getClass(), "example-deliveries.json"))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Key of the project")
      .setExampleValue("my-project");

    action.createParam(PARAM_TASK)
      .setDescription("Id of the Compute Engine task")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action
      .createParam(PARAM_BRANCH)
      .setSince("6.7")
      .setDescription("Branch key. Should only be used with '" + PARAM_COMPONENT + "' parameter")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    // fail-fast if not logged in
    userSession.checkLoggedIn();

    String ceTaskId = request.param(PARAM_TASK);
    String componentKey = request.param(PARAM_COMPONENT);
    String branchKey = request.param(PARAM_BRANCH);
    checkArgument(ceTaskId != null ^ componentKey != null, "Either '%s' or '%s' must be provided", PARAM_TASK, PARAM_COMPONENT);
    checkArgument(ceTaskId == null || branchKey == null, "'%s' should not be used with '%s'", PARAM_BRANCH, PARAM_TASK);

    Data data = loadFromDatabase(ceTaskId, componentKey, branchKey);
    data.ensureAdminPermission(userSession);
    data.writeTo(request, response);
  }

  private Data loadFromDatabase(@Nullable String ceTaskId, @Nullable String componentKey, @Nullable String branchKey) {
    ComponentDto component = null;
    BranchDto branch = null;
    List<WebhookDeliveryLiteDto> deliveries;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (componentKey != null) {
        component = loadComponent(dbSession, componentKey, branchKey);
        deliveries = dbClient.webhookDeliveryDao().selectOrderedByComponentUuid(dbSession, component.uuid());
      } else {
        deliveries = dbClient.webhookDeliveryDao().selectOrderedByCeTaskUuid(dbSession, ceTaskId);
        Optional<String> deliveredComponentUuid = deliveries
          .stream()
          .map(WebhookDeliveryLiteDto::getComponentUuid)
          .findFirst();
        if (deliveredComponentUuid.isPresent()) {
          component = componentFinder.getByUuid(dbSession, deliveredComponentUuid.get());
        }
      }
      if (component != null) {
        branch = dbClient.branchDao().selectByUuid(dbSession, component.uuid())
          .orElseThrow(() -> new IllegalStateException("Unable to find branch '" + branchKey + "' for project '" + componentKey + "'"));
      }
    }
    return new Data(component, branch, deliveries);
  }

  private ComponentDto loadComponent(DbSession dbSession, String componentKey, @Nullable String branchKey) {
    if (branchKey != null) {
      return componentFinder.getByKeyAndBranch(dbSession, componentKey, branchKey);
    }
    return componentFinder.getByKey(dbSession, componentKey);
  }

  private static class Data {
    private final ComponentDto component;
    private final List<WebhookDeliveryLiteDto> deliveryDtos;
    private final BranchDto branch;

    Data(@Nullable ComponentDto component, @Nullable BranchDto branch, List<WebhookDeliveryLiteDto> deliveries) {
      this.deliveryDtos = deliveries;
      if (deliveries.isEmpty()) {
        this.component = null;
        this.branch = null;
      } else {
        this.component = requireNonNull(component);
        this.branch = requireNonNull(branch);
      }
    }

    void ensureAdminPermission(UserSession userSession) {
      if (component != null) {
        userSession.checkComponentPermission(UserRole.ADMIN, component);
      }
    }

    void writeTo(Request request, Response response) {
      Webhooks.DeliveriesWsResponse.Builder responseBuilder = Webhooks.DeliveriesWsResponse.newBuilder();
      Webhooks.Delivery.Builder deliveryBuilder = Webhooks.Delivery.newBuilder();
      for (WebhookDeliveryLiteDto dto : deliveryDtos) {
        copyDtoToProtobuf(component, branch, dto, deliveryBuilder);
        responseBuilder.addDeliveries(deliveryBuilder);
      }
      writeProtobuf(responseBuilder.build(), request, response);
    }
  }
}
