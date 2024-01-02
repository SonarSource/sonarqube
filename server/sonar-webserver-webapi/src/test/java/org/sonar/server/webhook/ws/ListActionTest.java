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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.db.webhook.WebhookDeliveryDbTester;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Webhooks.ListResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.DbTester.create;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.webhook.WebhookDeliveryTesting.newDto;
import static org.sonar.db.webhook.WebhookTesting.newGlobalWebhook;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;
import static org.sonarqube.ws.Webhooks.LatestDelivery;
import static org.sonarqube.ws.Webhooks.ListResponseElement;

public class ListActionTest {

  private static final long NOW = 1_500_000_000L;
  private static final long BEFORE = NOW - 1_000L;

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = create();

  private final DbClient dbClient = db.getDbClient();
  private final Configuration configuration = mock(Configuration.class);
  private final NetworkInterfaceProvider networkInterfaceProvider = mock(NetworkInterfaceProvider.class);
  private final WebhookSupport webhookSupport = new WebhookSupport(userSession, configuration, networkInterfaceProvider);
  private final ResourceTypes resourceTypes = mock(ResourceTypes.class);
  private final ComponentFinder componentFinder = new ComponentFinder(dbClient, resourceTypes);
  private final ListAction underTest = new ListAction(dbClient, userSession, webhookSupport, componentFinder);

  private final ComponentDbTester componentDbTester = db.components();
  private final WebhookDbTester webhookDbTester = db.webhooks();
  private final WebhookDeliveryDbTester webhookDeliveryDbTester = db.webhookDelivery();
  private final WsActionTester wsActionTester = new WsActionTester(underTest);

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
        tuple("project", false));
    assertThat(action.changelog()).hasSize(2);
  }

  @Test
  public void list_webhooks_and_their_latest_delivery() {
    WebhookDto webhook1 = webhookDbTester.insert(newGlobalWebhook("aaa"), null, null);
    webhookDeliveryDbTester.insert(newDto("WH1-DELIVERY-1-UUID", webhook1.getUuid(), "COMPONENT_1", "TASK_1").setCreatedAt(BEFORE));
    webhookDeliveryDbTester.insert(newDto("WH1-DELIVERY-2-UUID", webhook1.getUuid(), "COMPONENT_1", "TASK_2").setCreatedAt(NOW));

    WebhookDto webhook2 = webhookDbTester.insert(newGlobalWebhook("bbb"), null, null);
    webhookDeliveryDbTester.insert(newDto("WH2-DELIVERY-1-UUID", webhook2.getUuid(), "COMPONENT_1", "TASK_1").setCreatedAt(BEFORE));
    webhookDeliveryDbTester.insert(newDto("WH2-DELIVERY-2-UUID", webhook2.getUuid(), "COMPONENT_1", "TASK_2").setCreatedAt(NOW));

    userSession.logIn().addPermission(ADMINISTER);

    ListResponse response = wsActionTester.newRequest().executeProtobuf(ListResponse.class);

    List<ListResponseElement> elements = response.getWebhooksList();
    assertThat(elements).hasSize(2);

    assertThat(elements.get(0)).extracting(ListResponseElement::getKey).isEqualTo(webhook1.getUuid());
    assertThat(elements.get(0)).extracting(ListResponseElement::getName).isEqualTo("aaa");
    assertThat(elements.get(0).getLatestDelivery()).isNotNull();
    assertThat(elements.get(0).getLatestDelivery()).extracting(LatestDelivery::getId).isEqualTo("WH1-DELIVERY-2-UUID");

    assertThat(elements.get(1)).extracting(ListResponseElement::getKey).isEqualTo(webhook2.getUuid());
    assertThat(elements.get(1)).extracting(ListResponseElement::getName).isEqualTo("bbb");
    assertThat(elements.get(1).getLatestDelivery()).isNotNull();
    assertThat(elements.get(1).getLatestDelivery()).extracting(LatestDelivery::getId).isEqualTo("WH2-DELIVERY-2-UUID");
  }

  @Test
  public void list_webhooks_when_no_delivery() {
    WebhookDto webhook1 = webhookDbTester.insert(newGlobalWebhook("aaa"), null, null);
    WebhookDto webhook2 = webhookDbTester.insert(newGlobalWebhook("bbb"), null, null);

    userSession.logIn().addPermission(ADMINISTER);

    ListResponse response = wsActionTester.newRequest().executeProtobuf(ListResponse.class);

    List<ListResponseElement> elements = response.getWebhooksList();
    assertThat(elements).hasSize(2);

    assertThat(elements.get(0)).extracting(ListResponseElement::getKey).isEqualTo(webhook1.getUuid());
    assertThat(elements.get(0)).extracting(ListResponseElement::getName).isEqualTo("aaa");
    assertThat(elements.get(0).hasLatestDelivery()).isFalse();

    assertThat(elements.get(1)).extracting(ListResponseElement::getKey).isEqualTo(webhook2.getUuid());
    assertThat(elements.get(1)).extracting(ListResponseElement::getName).isEqualTo("bbb");
    assertThat(elements.get(1).hasLatestDelivery()).isFalse();
  }

  @Test
  public void obfuscate_credentials_in_webhook_URLs() {
    String url = "http://foo:barouf@toto/bop";
    String expectedUrl = "http://***:******@toto/bop";
    WebhookDto webhook1 = webhookDbTester.insert(newGlobalWebhook("aaa", t -> t.setUrl(url)), null, null);
    webhookDeliveryDbTester.insert(newDto("WH1-DELIVERY-1-UUID", webhook1.getUuid(), "COMPONENT_1", "TASK_1").setCreatedAt(BEFORE));
    webhookDeliveryDbTester.insert(newDto("WH1-DELIVERY-2-UUID", webhook1.getUuid(), "COMPONENT_1", "TASK_2").setCreatedAt(NOW));
    webhookDbTester.insert(newGlobalWebhook("bbb", t -> t.setUrl(url)), null, null);

    userSession.logIn().addPermission(ADMINISTER);

    ListResponse response = wsActionTester.newRequest().executeProtobuf(ListResponse.class);

    List<ListResponseElement> elements = response.getWebhooksList();
    assertThat(elements)
      .hasSize(2)
      .extracting(ListResponseElement::getUrl)
      .containsOnly(expectedUrl);
  }

  @Test
  public void list_global_webhooks() {
    WebhookDto dto1 = webhookDbTester.insertGlobalWebhook();
    WebhookDto dto2 = webhookDbTester.insertGlobalWebhook().setSecret(null);
    // insert a project-specific webhook, that should not be returned when listing global webhooks
    webhookDbTester.insertWebhook(componentDbTester.insertPrivateProjectDto());

    userSession.logIn().addPermission(ADMINISTER);

    ListResponse response = wsActionTester.newRequest()
      .executeProtobuf(ListResponse.class);

    assertThat(response.getWebhooksList())
      .extracting(ListResponseElement::getName, ListResponseElement::getUrl)
      .containsExactlyInAnyOrder(tuple(dto1.getName(), dto1.getUrl()),
        tuple(dto2.getName(), dto2.getUrl()));
  }

  @Test
  public void list_webhooks_with_secret() {
    WebhookDto withSecret = webhookDbTester.insertGlobalWebhook();
    WebhookDto withoutSecret = newGlobalWebhook().setSecret(null);
    webhookDbTester.insert(withoutSecret, null, null);

    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);

    ListResponse response = wsActionTester.newRequest()
      .executeProtobuf(ListResponse.class);

    assertThat(response.getWebhooksList())
      .extracting(ListResponseElement::getName, ListResponseElement::getUrl, ListResponseElement::getHasSecret)
      .containsExactlyInAnyOrder(tuple(withSecret.getName(), withSecret.getUrl(), true),
        tuple(withoutSecret.getName(), withoutSecret.getUrl(), false));
  }

  @Test
  public void list_project_webhooks_when_project_key_param_is_provided() {
    ProjectDto project1 = componentDbTester.insertPrivateProjectDto();
    userSession.logIn().addProjectPermission(ADMIN, project1);

    WebhookDto dto1 = webhookDbTester.insertWebhook(project1);
    WebhookDto dto2 = webhookDbTester.insertWebhook(project1);

    ListResponse response = wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, project1.getKey())
      .executeProtobuf(ListResponse.class);

    assertThat(response.getWebhooksList())
      .extracting(ListResponseElement::getName, ListResponseElement::getUrl)
      .contains(tuple(dto1.getName(), dto1.getUrl()),
        tuple(dto2.getName(), dto2.getUrl()));

  }

  @Test
  public void list_global_webhooks_if_project_key_param_missing() {
    WebhookDto dto1 = webhookDbTester.insertGlobalWebhook();
    WebhookDto dto2 = webhookDbTester.insertGlobalWebhook();
    userSession.logIn().addPermission(ADMINISTER);

    ListResponse response = wsActionTester.newRequest()
      .executeProtobuf(ListResponse.class);

    assertThat(response.getWebhooksList())
      .extracting(ListResponseElement::getName, ListResponseElement::getUrl)
      .contains(tuple(dto1.getName(), dto1.getUrl()),
        tuple(dto2.getName(), dto2.getUrl()));

  }

  @Test
  public void return_NotFoundException_if_requested_project_is_not_found() {
    userSession.logIn().setSystemAdministrator();
    TestRequest request = wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, "pipo");

    assertThatThrownBy(() -> request.executeProtobuf(ListResponse.class))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void return_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    TestRequest request = wsActionTester.newRequest();

    assertThatThrownBy(() -> request.executeProtobuf(ListResponse.class))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void throw_ForbiddenException_if_not_administrator() {
    userSession.logIn();
    TestRequest request = wsActionTester.newRequest();

    assertThatThrownBy(() -> request.executeProtobuf(ListResponse.class))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_not_project_administrator() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    TestRequest request = wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, project.getKey());
    userSession.logIn();

    assertThatThrownBy(() -> request.executeProtobuf(ListResponse.class))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

}
