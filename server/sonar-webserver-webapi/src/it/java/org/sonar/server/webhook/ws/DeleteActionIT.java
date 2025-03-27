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
package org.sonar.server.webhook.ws;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.db.webhook.WebhookDeliveryDao;
import org.sonar.db.webhook.WebhookDeliveryDbTester;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.db.DbTester.create;
import static org.sonar.db.webhook.WebhookDeliveryTesting.newDto;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.KEY_PARAM;

public class DeleteActionIT {

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = create();
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final WebhookDbTester webhookDbTester = db.webhooks();
  private final WebhookDeliveryDbTester webhookDeliveryDbTester = db.webhookDelivery();
  private final WebhookDeliveryDao deliveryDao = dbClient.webhookDeliveryDao();
  private final ComponentDbTester componentDbTester = db.components();
  private final Configuration configuration = mock(Configuration.class);
  private final NetworkInterfaceProvider networkInterfaceProvider = mock(NetworkInterfaceProvider.class);
  private final WebhookSupport webhookSupport = new WebhookSupport(userSession, configuration, networkInterfaceProvider);
  private final DeleteAction underTest = new DeleteAction(dbClient, userSession, webhookSupport);
  private final WsActionTester wsActionTester = new WsActionTester(underTest);

  @Test
  public void test_ws_definition() {

    WebService.Action action = wsActionTester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();

    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("webhook", true));

  }

  @Test
  public void delete_a_project_webhook() {

    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    webhookDeliveryDbTester.insert(newDto().setWebhookUuid(dto.getUuid()));
    webhookDeliveryDbTester.insert(newDto().setWebhookUuid(dto.getUuid()));

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    TestResponse response = wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded).isEmpty();

    int deliveriesCount = deliveryDao.countDeliveriesByWebhookUuid(dbSession, dto.getUuid());
    assertThat(deliveriesCount).isZero();

  }

  @Test
  public void delete_a_global_webhook() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();
    webhookDeliveryDbTester.insert(newDto().setWebhookUuid(dto.getUuid()));
    webhookDeliveryDbTester.insert(newDto().setWebhookUuid(dto.getUuid()));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestResponse response = wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded).isEmpty();

    int deliveriesCount = deliveryDao.countDeliveriesByWebhookUuid(dbSession, dto.getUuid());
    assertThat(deliveriesCount).isZero();
  }

  @Test
  public void fail_if_webhook_does_not_exist() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestRequest request = wsActionTester.newRequest()
      .setParam(KEY_PARAM, "inexistent-webhook-uuid");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No webhook with key 'inexistent-webhook-uuid'");
  }

  @Test
  public void fail_if_not_logged_in() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();
    userSession.anonymous();
    TestRequest request = wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid());

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_no_permission_on_webhook_scope_project() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn();
    TestRequest request = wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_no_permission_on_webhook_scope_global() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();
    userSession.logIn();
    TestRequest request = wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

}
