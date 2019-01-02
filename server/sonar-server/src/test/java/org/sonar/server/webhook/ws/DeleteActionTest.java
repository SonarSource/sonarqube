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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.db.webhook.WebhookDeliveryDao;
import org.sonar.db.webhook.WebhookDeliveryDbTester;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.DbTester.create;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.webhook.WebhookDeliveryTesting.newDto;
import static org.sonar.server.organization.TestDefaultOrganizationProvider.from;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.KEY_PARAM;

public class DeleteActionTest {

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = create();
  private DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private WebhookDbTester webhookDbTester = db.webhooks();
  private WebhookDeliveryDbTester webhookDeliveryDbTester = db.webhookDelivery();
  private final WebhookDeliveryDao deliveryDao = dbClient.webhookDeliveryDao();
  private OrganizationDbTester organizationDbTester = db.organizations();
  private ComponentDbTester componentDbTester = db.components();

  private DefaultOrganizationProvider defaultOrganizationProvider = from(db);

  private WebhookSupport webhookSupport = new WebhookSupport(userSession);
  private DeleteAction underTest = new DeleteAction(dbClient, userSession, webhookSupport);
  private WsActionTester wsActionTester = new WsActionTester(underTest);

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

    ComponentDto project = componentDbTester.insertPrivateProject();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    webhookDeliveryDbTester.insert(newDto().setWebhookUuid(dto.getUuid()));
    webhookDeliveryDbTester.insert(newDto().setWebhookUuid(dto.getUuid()));

    userSession.logIn().addProjectPermission(ADMIN, project);

    TestResponse response = wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded).isEmpty();

    int deliveriesCount = deliveryDao.countDeliveriesByWebhookUuid(dbSession, dto.getUuid());
    assertThat(deliveriesCount).isEqualTo(0);

  }

  @Test
  public void delete_an_organization_webhook() {

    OrganizationDto organization = organizationDbTester.insert();
    WebhookDto dto = webhookDbTester.insertWebhook(organization);
    webhookDeliveryDbTester.insert(newDto().setWebhookUuid(dto.getUuid()));
    webhookDeliveryDbTester.insert(newDto().setWebhookUuid(dto.getUuid()));

    userSession.logIn().addPermission(ADMINISTER, organization.getUuid());

    TestResponse response = wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded).isEmpty();

    int deliveriesCount = deliveryDao.countDeliveriesByWebhookUuid(dbSession, dto.getUuid());
    assertThat(deliveriesCount).isEqualTo(0);
  }

  @Test
  public void fail_if_webhook_does_not_exist() {

    userSession.logIn().addPermission(ADMINISTER, defaultOrganizationProvider.get().getUuid());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No webhook with key 'inexistent-webhook-uuid'");

    wsActionTester.newRequest()
      .setParam(KEY_PARAM, "inexistent-webhook-uuid")
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {

    OrganizationDto organization = organizationDbTester.insert();
    WebhookDto dto = webhookDbTester.insertWebhook(organization);
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid())
      .execute();

  }

  @Test
  public void fail_if_no_permission_on_webhook_scope_project() {

    ComponentDto project = componentDbTester.insertPrivateProject();
    WebhookDto dto = webhookDbTester.insertWebhook(project);

    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid())
      .execute();

  }

  @Test
  public void fail_if_no_permission_on_webhook_scope_organization() {

    OrganizationDto organization = organizationDbTester.insert();
    WebhookDto dto = webhookDbTester.insertWebhook(organization);

    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    wsActionTester.newRequest()
      .setParam(KEY_PARAM, dto.getUuid())
      .execute();

  }

}
