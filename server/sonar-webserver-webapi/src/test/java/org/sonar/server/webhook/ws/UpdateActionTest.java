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
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.webhook.WebhookDbTester;
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
import static org.sonar.server.organization.TestDefaultOrganizationProvider.from;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.server.ws.KeyExamples.NAME_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.URL_WEBHOOK_EXAMPLE_001;

public class UpdateActionTest {

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = create();
  private DbClient dbClient = db.getDbClient();
  private WebhookDbTester webhookDbTester = db.webhooks();
  private OrganizationDbTester organizationDbTester = db.organizations();
  private ComponentDbTester componentDbTester = db.components();

  private DefaultOrganizationProvider defaultOrganizationProvider = from(db);

  private WebhookSupport webhookSupport = new WebhookSupport(userSession);
  private UpdateAction underTest = new UpdateAction(dbClient, userSession, webhookSupport);
  private WsActionTester wsActionTester = new WsActionTester(underTest);

  @Test
  public void test_ws_definition() {
    WebService.Action action = wsActionTester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();

    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("webhook", true),
        tuple("name", true),
        tuple("url", true),
        tuple("secret", false));
  }

  @Test
  public void update_a_project_webhook_with_required_fields() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(ADMIN, project);

    TestResponse response = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded.get()).isNotNull();
    assertThat(reloaded.get().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getOrganizationUuid()).isNull();
    assertThat(reloaded.get().getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(reloaded.get().getSecret()).isNull();
  }

  @Test
  public void update_a_project_webhook_with_all_fields() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(ADMIN, project);

    TestResponse response = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", "a_new_secret")
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded.get()).isNotNull();
    assertThat(reloaded.get().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getOrganizationUuid()).isNull();
    assertThat(reloaded.get().getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(reloaded.get().getSecret()).isEqualTo("a_new_secret");
  }

  @Test
  public void update_an_organization_webhook() {
    OrganizationDto organization = organizationDbTester.insert();
    WebhookDto dto = webhookDbTester.insertWebhook(organization);
    userSession.logIn().addPermission(ADMINISTER, organization.getUuid());

    TestResponse response = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", "a_new_secret")
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded.get()).isNotNull();
    assertThat(reloaded.get().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getOrganizationUuid()).isEqualTo(dto.getOrganizationUuid());
    assertThat(reloaded.get().getProjectUuid()).isNull();
    assertThat(reloaded.get().getSecret()).isEqualTo("a_new_secret");
  }

  @Test
  public void fail_if_webhook_does_not_exist() {
    userSession.logIn().addPermission(ADMINISTER, defaultOrganizationProvider.get().getUuid());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No webhook with key 'inexistent-webhook-uuid'");

    wsActionTester.newRequest()
      .setParam("webhook", "inexistent-webhook-uuid")
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    OrganizationDto organization = organizationDbTester.insert();
    WebhookDto dto = webhookDbTester.insertWebhook(organization);
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
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
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
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
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void fail_if_url_is_not_valid() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);

    wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", "htp://www.wrong-protocol.com/")
      .execute();
  }

  @Test
  public void fail_if_credential_in_url_is_have_a_wrong_format() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);

    wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", "http://:www.wrong-protocol.com/")
      .execute();
  }

}
