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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Webhooks.CreateWsResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.DbTester.create;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.organization.TestDefaultOrganizationProvider.from;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.ORGANIZATION_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM;
import static org.sonar.server.ws.KeyExamples.NAME_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.URL_WEBHOOK_EXAMPLE_001;

public class CreateActionTest {

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
  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private WebhookSupport webhookSupport = new WebhookSupport(userSession);
  private org.sonar.server.webhook.ws.CreateAction underTest = new CreateAction(dbClient, userSession, defaultOrganizationProvider, uuidFactory, webhookSupport);
  private WsActionTester wsActionTester = new WsActionTester(underTest);

  @Test
  public void test_ws_definition() {
    WebService.Action action = wsActionTester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNotEmpty();

    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("organization", false),
        tuple("project", false),
        tuple("name", true),
        tuple("url", true),
        tuple("secret", false));

  }

  @Test
  public void create_a_webhook_with_400_length_project_key() {
    String longProjectKey = generateStringWithLength(400);
    ComponentDto project = componentDbTester.insertPrivateProject(componentDto -> componentDto.setDbKey(longProjectKey));

    userSession.logIn().addProjectPermission(ADMIN, project);

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("project", longProjectKey)
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", "a_secret")
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getSecret()).isEqualTo("a_secret");
  }

  @Test
  public void create_a_webhook_with_secret() {
    userSession.logIn().addPermission(ADMINISTER, defaultOrganizationProvider.get().getUuid());

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", "a_secret")
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getSecret()).isEqualTo("a_secret");
  }

  @Test
  public void create_a_webhook_on_default_organization() {
    userSession.logIn().addPermission(ADMINISTER, defaultOrganizationProvider.get().getUuid());

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().hasSecret()).isFalse();
  }

  @Test
  public void create_a_webhook_on_specific_organization() {
    OrganizationDto organization = organizationDbTester.insert();
    userSession.logIn().addPermission(ADMINISTER, organization.getUuid());

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().hasSecret()).isFalse();
  }

  @Test
  public void create_a_webhook_on_project() {
    ComponentDto project = componentDbTester.insertPrivateProject();

    userSession.logIn().addProjectPermission(ADMIN, project);

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().hasSecret()).isFalse();
  }

  @Test
  public void create_a_webhook_on_a_project_belonging_to_an_organization() {
    OrganizationDto organization = organizationDbTester.insert();
    ComponentDto project = componentDbTester.insertPrivateProject(organization);

    userSession.logIn().addProjectPermission(ADMIN, project);

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", project.getKey())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().hasSecret()).isFalse();
  }

  @Test
  public void fail_if_project_does_not_belong_to_requested_organization() {
    OrganizationDto organization = organizationDbTester.insert();
    ComponentDto project = componentDbTester.insertPrivateProject();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project '%s' does not belong to organisation '%s'", project.getKey(), organization.getKey()));

    userSession.logIn().addProjectPermission(ADMIN, project);

    wsActionTester.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", project.getKey())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void fail_if_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No project with key 'inexistent-project-uuid'");

    userSession.logIn();

    wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, "inexistent-project-uuid")
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void fail_if_crossing_maximum_quantity_of_webhooks_on_this_project() {
    ComponentDto project = componentDbTester.insertPrivateProject();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Maximum number of webhook reached for project '%s'", project.getKey()));

    for (int i = 0; i < 10; i++) {
      webhookDbTester.insertWebhook(project);
    }
    userSession.logIn().addProjectPermission(ADMIN, project);

    wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, project.getKey())
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void fail_if_crossing_maximum_quantity_of_webhooks_on_this_organization() {
    OrganizationDto organization = organizationDbTester.insert();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Maximum number of webhook reached for organization '%s'", organization.getKey()));

    for (int i = 0; i < 10; i++) {
      webhookDbTester.insertWebhook(organization);
    }
    userSession.logIn().addPermission(ADMINISTER, organization.getUuid());

    wsActionTester.newRequest()
      .setParam(ORGANIZATION_KEY_PARAM, organization.getKey())
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void fail_if_organization_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'inexistent-organization-uuid'");

    userSession.logIn();

    wsActionTester.newRequest()
      .setParam(ORGANIZATION_KEY_PARAM, "inexistent-organization-uuid")
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void fail_if_url_is_not_valid() {
    userSession.logIn().addPermission(ADMINISTER, defaultOrganizationProvider.get().getUuid());

    expectedException.expect(IllegalArgumentException.class);

    wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, "htp://www.wrong-protocol.com/")
      .execute();
  }

  @Test
  public void fail_if_credential_in_url_is_have_a_wrong_format() {
    userSession.logIn().addPermission(ADMINISTER, defaultOrganizationProvider.get().getUuid());

    expectedException.expect(IllegalArgumentException.class);

    wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, "http://:www.wrong-protocol.com/")
      .execute();
  }

  @Test
  public void return_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    expectedException.expect(UnauthorizedException.class);

    wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_no_organization_provided_and_user_is_not_system_administrator() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_organization_provided_but_user_is_not_organization_administrator() {
    OrganizationDto organization = organizationDbTester.insert();

    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .setParam(ORGANIZATION_KEY_PARAM, organization.getKey())
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_project_administrator() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .setParam(PROJECT_KEY_PARAM, project.getKey())
      .execute();
  }


  @Test
  public void throw_IllegalArgumentException_if_project_key_greater_than_400() {
    String longProjectKey = generateStringWithLength(401);
    userSession.logIn().addPermission(ADMINISTER, defaultOrganizationProvider.get().getUuid());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'project' length (401) is longer than the maximum authorized (400)");

    wsActionTester.newRequest()
      .setParam("project", longProjectKey)
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", "a_secret")
      .executeProtobuf(CreateWsResponse.class);
  }


  private static String generateStringWithLength(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append("x");
    }
    return sb.toString();
  }

}
