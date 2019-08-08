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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.db.webhook.WebhookDeliveryDbTester;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Webhooks;
import org.sonarqube.ws.Webhooks.ListResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.DbTester.create;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.webhook.WebhookDeliveryTesting.newDto;
import static org.sonar.db.webhook.WebhookTesting.newOrganizationWebhook;
import static org.sonar.server.organization.TestDefaultOrganizationProvider.from;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.ORGANIZATION_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;

public class ListActionTest {

  private static final long NOW = 1_500_000_000L;
  private static final long BEFORE = NOW - 1_000L;

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = create();

  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = from(db);
  private WebhookSupport webhookSupport = new WebhookSupport(userSession);
  private ListAction underTest = new ListAction(dbClient, userSession, defaultOrganizationProvider, webhookSupport);

  private ComponentDbTester componentDbTester = db.components();
  private WebhookDbTester webhookDbTester = db.webhooks();
  private WebhookDeliveryDbTester webhookDeliveryDbTester = db.webhookDelivery();
  private OrganizationDbTester organizationDbTester = db.organizations();
  private WsActionTester wsActionTester = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action action = wsActionTester.getDef();

    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params())
      .extracting(Param::key, Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("organization", false),
        tuple("project", false));
    assertThat(action.changelog()).hasSize(1);
  }

  @Test
  public void list_webhooks_and_their_latest_delivery() {
    WebhookDto webhook1 = webhookDbTester.insert(newOrganizationWebhook("aaa", defaultOrganizationProvider.get().getUuid()));
    webhookDeliveryDbTester.insert(newDto("WH1-DELIVERY-1-UUID", webhook1.getUuid(), "COMPONENT_1", "TASK_1").setCreatedAt(BEFORE));
    webhookDeliveryDbTester.insert(newDto("WH1-DELIVERY-2-UUID", webhook1.getUuid(), "COMPONENT_1", "TASK_2").setCreatedAt(NOW));

    WebhookDto webhook2 = webhookDbTester.insert(newOrganizationWebhook("bbb", defaultOrganizationProvider.get().getUuid()));
    webhookDeliveryDbTester.insert(newDto("WH2-DELIVERY-1-UUID", webhook2.getUuid(), "COMPONENT_1", "TASK_1").setCreatedAt(BEFORE));
    webhookDeliveryDbTester.insert(newDto("WH2-DELIVERY-2-UUID", webhook2.getUuid(), "COMPONENT_1", "TASK_2").setCreatedAt(NOW));

    userSession.logIn().addPermission(ADMINISTER, db.getDefaultOrganization().getUuid());

    ListResponse response = wsActionTester.newRequest().executeProtobuf(ListResponse.class);

    List<Webhooks.ListResponseElement> elements = response.getWebhooksList();
    assertThat(elements.size()).isEqualTo(2);

    assertThat(elements.get(0)).extracting(Webhooks.ListResponseElement::getKey).isEqualTo(webhook1.getUuid());
    assertThat(elements.get(0)).extracting(Webhooks.ListResponseElement::getName).isEqualTo("aaa");
    assertThat(elements.get(0).getLatestDelivery()).isNotNull();
    assertThat(elements.get(0).getLatestDelivery()).extracting(Webhooks.LatestDelivery::getId).isEqualTo("WH1-DELIVERY-2-UUID");

    assertThat(elements.get(1)).extracting(Webhooks.ListResponseElement::getKey).isEqualTo(webhook2.getUuid());
    assertThat(elements.get(1)).extracting(Webhooks.ListResponseElement::getName).isEqualTo("bbb");
    assertThat(elements.get(1).getLatestDelivery()).isNotNull();
    assertThat(elements.get(1).getLatestDelivery()).extracting(Webhooks.LatestDelivery::getId).isEqualTo("WH2-DELIVERY-2-UUID");
  }

  @Test
  public void list_webhooks_when_no_delivery() {
    WebhookDto webhook1 = webhookDbTester.insert(newOrganizationWebhook("aaa", defaultOrganizationProvider.get().getUuid()));
    WebhookDto webhook2 = webhookDbTester.insert(newOrganizationWebhook("bbb", defaultOrganizationProvider.get().getUuid()));

    userSession.logIn().addPermission(ADMINISTER, db.getDefaultOrganization().getUuid());

    ListResponse response = wsActionTester.newRequest().executeProtobuf(ListResponse.class);

    List<Webhooks.ListResponseElement> elements = response.getWebhooksList();
    assertThat(elements.size()).isEqualTo(2);

    assertThat(elements.get(0)).extracting(Webhooks.ListResponseElement::getKey).isEqualTo(webhook1.getUuid());
    assertThat(elements.get(0)).extracting(Webhooks.ListResponseElement::getName).isEqualTo("aaa");
    assertThat(elements.get(0).hasLatestDelivery()).isFalse();

    assertThat(elements.get(1)).extracting(Webhooks.ListResponseElement::getKey).isEqualTo(webhook2.getUuid());
    assertThat(elements.get(1)).extracting(Webhooks.ListResponseElement::getName).isEqualTo("bbb");
    assertThat(elements.get(1).hasLatestDelivery()).isFalse();
  }

  @Test
  public void obfuscate_credentials_in_webhook_URLs() {
    String url = "http://foo:barouf@toto/bop";
    String expectedUrl = "http://***:******@toto/bop";
    WebhookDto webhook1 = webhookDbTester.insert(newOrganizationWebhook("aaa", defaultOrganizationProvider.get().getUuid(), t -> t.setUrl(url)));
    webhookDeliveryDbTester.insert(newDto("WH1-DELIVERY-1-UUID", webhook1.getUuid(), "COMPONENT_1", "TASK_1").setCreatedAt(BEFORE));
    webhookDeliveryDbTester.insert(newDto("WH1-DELIVERY-2-UUID", webhook1.getUuid(), "COMPONENT_1", "TASK_2").setCreatedAt(NOW));
    WebhookDto webhook2 = webhookDbTester.insert(newOrganizationWebhook("bbb", db.getDefaultOrganization().getUuid(), t -> t.setUrl(url)));

    userSession.logIn().addPermission(ADMINISTER, db.getDefaultOrganization().getUuid());

    ListResponse response = wsActionTester.newRequest().executeProtobuf(ListResponse.class);

    List<Webhooks.ListResponseElement> elements = response.getWebhooksList();
    assertThat(elements)
      .hasSize(2)
      .extracting(Webhooks.ListResponseElement::getUrl)
      .containsOnly(expectedUrl);
  }

  @Test
  public void list_global_webhooks() {
    WebhookDto dto1 = webhookDbTester.insertWebhook(db.getDefaultOrganization());
    WebhookDto dto2 = webhookDbTester.insertWebhook(db.getDefaultOrganization());
    userSession.logIn().addPermission(ADMINISTER, db.getDefaultOrganization().getUuid());

    ListResponse response = wsActionTester.newRequest()
      .executeProtobuf(ListResponse.class);

    assertThat(response.getWebhooksList())
      .extracting(Webhooks.ListResponseElement::getName, Webhooks.ListResponseElement::getUrl)
      .contains(tuple(dto1.getName(), dto1.getUrl()),
        tuple(dto2.getName(), dto2.getUrl()));

  }

  @Test
  public void list_project_webhooks_when_no_organization_is_provided() {
    ComponentDto project1 = componentDbTester.insertPrivateProject();
    userSession.logIn().addProjectPermission(ADMIN, project1);

    WebhookDto dto1 = webhookDbTester.insertWebhook(project1);
    WebhookDto dto2 = webhookDbTester.insertWebhook(project1);

    ListResponse response = wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, project1.getKey())
      .executeProtobuf(ListResponse.class);

    assertThat(response.getWebhooksList())
      .extracting(Webhooks.ListResponseElement::getName, Webhooks.ListResponseElement::getUrl)
      .contains(tuple(dto1.getName(), dto1.getUrl()),
        tuple(dto2.getName(), dto2.getUrl()));

  }

  @Test
  public void list_organization_webhooks() {
    OrganizationDto organizationDto = organizationDbTester.insert();
    WebhookDto dto1 = webhookDbTester.insertWebhook(organizationDto);
    WebhookDto dto2 = webhookDbTester.insertWebhook(organizationDto);
    userSession.logIn().addPermission(ADMINISTER, organizationDto.getUuid());

    ListResponse response = wsActionTester.newRequest()
      .setParam(ORGANIZATION_KEY_PARAM, organizationDto.getKey())
      .executeProtobuf(ListResponse.class);

    assertThat(response.getWebhooksList())
      .extracting(Webhooks.ListResponseElement::getName, Webhooks.ListResponseElement::getUrl)
      .contains(tuple(dto1.getName(), dto1.getUrl()),
        tuple(dto2.getName(), dto2.getUrl()));

  }

  @Test
  public void list_project_webhooks_when_organization_is_provided() {
    OrganizationDto organization = organizationDbTester.insert();
    ComponentDto project = componentDbTester.insertPrivateProject(organization);
    userSession.logIn().addProjectPermission(ADMIN, project);

    WebhookDto dto1 = webhookDbTester.insertWebhook(project);
    WebhookDto dto2 = webhookDbTester.insertWebhook(project);

    ListResponse response = wsActionTester.newRequest()
      .setParam(ORGANIZATION_KEY_PARAM, organization.getKey())
      .setParam(PROJECT_KEY_PARAM, project.getKey())
      .executeProtobuf(ListResponse.class);

    assertThat(response.getWebhooksList())
      .extracting(Webhooks.ListResponseElement::getName, Webhooks.ListResponseElement::getUrl)
      .contains(tuple(dto1.getName(), dto1.getUrl()),
        tuple(dto2.getName(), dto2.getUrl()));

  }

  @Test
  public void return_NotFoundException_if_requested_project_is_not_found() {
    userSession.logIn().setSystemAdministrator();
    expectedException.expect(NotFoundException.class);

    wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, "pipo")
      .executeProtobuf(ListResponse.class);

  }

  @Test
  public void return_NotFoundException_if_requested_organization_is_not_found() {
    userSession.logIn().setSystemAdministrator();
    expectedException.expect(NotFoundException.class);

    wsActionTester.newRequest()
      .setParam(ORGANIZATION_KEY_PARAM, "pipo")
      .executeProtobuf(ListResponse.class);

  }

  @Test
  public void fail_if_project_exists_but_does_not_belong_to_requested_organization() {
    OrganizationDto organization = organizationDbTester.insert();
    ComponentDto project = componentDbTester.insertPrivateProject();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project '%s' does not belong to organisation '%s'", project.getKey(), organization.getKey()));

    userSession.logIn().addProjectPermission(ADMIN, project);

    wsActionTester.newRequest()
      .setParam(ORGANIZATION_KEY_PARAM, organization.getKey())
      .setParam(PROJECT_KEY_PARAM, project.getKey())
      .execute();

  }

  @Test
  public void return_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    expectedException.expect(UnauthorizedException.class);

    wsActionTester.newRequest()
      .executeProtobuf(ListResponse.class);

  }

  @Test
  public void throw_ForbiddenException_if_not_organization_administrator() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    wsActionTester.newRequest()
      .executeProtobuf(ListResponse.class);
  }

  @Test
  public void throw_ForbiddenException_if_not_project_administrator() {
    ComponentDto project = componentDbTester.insertPrivateProject();

    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, project.getKey())
      .executeProtobuf(ListResponse.class);

  }

}
