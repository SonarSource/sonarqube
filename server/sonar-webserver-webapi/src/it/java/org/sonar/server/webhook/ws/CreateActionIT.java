/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.network.NetworkInterfaceProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Webhooks.CreateWsResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.db.DbTester.create;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.NAME_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.URL_PARAM;
import static org.sonar.server.ws.KeyExamples.NAME_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.URL_WEBHOOK_EXAMPLE_001;

public class CreateActionIT {

  private static final String DEFAULT_COMPLIANT_SECRET = "at_least_16_characters";

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = create();
  private final DbClient dbClient = db.getDbClient();
  private final WebhookDbTester webhookDbTester = db.webhooks();
  private final ComponentDbTester componentDbTester = db.components();
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final Configuration configuration = mock(Configuration.class);
  private final NetworkInterfaceProvider networkInterfaceProvider = mock(NetworkInterfaceProvider.class);
  private final WebhookSupport webhookSupport = new WebhookSupport(userSession, configuration, networkInterfaceProvider);
  private final ComponentTypes componentTypes = mock(ComponentTypes.class);
  private final ComponentFinder componentFinder = new ComponentFinder(dbClient, componentTypes);
  private final CreateAction underTest = new CreateAction(dbClient, userSession, uuidFactory, webhookSupport, componentFinder);
  private final WsActionTester wsActionTester = new WsActionTester(underTest);

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
        tuple("project", false),
        tuple("name", true),
        tuple("url", true),
        tuple("secret", false));
  }

  @Test
  public void create_a_webhook_with_400_length_project_key() {
    String longProjectKey = generateStringWithLength(400);
    ProjectDto project = componentDbTester.insertPrivateProject(componentDto -> componentDto.setKey(longProjectKey)).getProjectDto();

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("project", longProjectKey)
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", DEFAULT_COMPLIANT_SECRET)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getHasSecret()).isTrue();
  }

  @Test
  public void create_a_webhook_with_secret() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", DEFAULT_COMPLIANT_SECRET)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getHasSecret()).isTrue();

    assertThat(webhookDbTester.selectWebhook(response.getWebhook().getKey()))
      .isPresent()
      .hasValueSatisfying(reloaded -> {
        assertThat(reloaded.getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
        assertThat(reloaded.getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
        assertThat(reloaded.getProjectUuid()).isNull();
        assertThat(reloaded.getSecret()).isEqualTo(DEFAULT_COMPLIANT_SECRET);
      });
  }

  @Test
  public void create_a_global_webhook() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getHasSecret()).isFalse();
  }

  @Test
  public void create_a_webhook_on_project() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    CreateWsResponse response = wsActionTester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getWebhook()).isNotNull();
    assertThat(response.getWebhook().getKey()).isNotNull();
    assertThat(response.getWebhook().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(response.getWebhook().getHasSecret()).isFalse();
  }

  @Test
  public void fail_if_project_does_not_exist() {
    userSession.logIn();
    TestRequest request = wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, "nonexistent-project-uuid")
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'nonexistent-project-uuid' not found");
  }

  @Test
  public void fail_if_crossing_maximum_quantity_of_webhooks_on_this_project() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    for (int i = 0; i < 10; i++) {
      webhookDbTester.insertWebhook(project);
    }
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);
    TestRequest request = wsActionTester.newRequest()
      .setParam(PROJECT_KEY_PARAM, project.getKey())
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Maximum number of webhook reached for project '%s'", project.getKey()));
  }

  @Test
  public void fail_if_crossing_maximum_quantity_of_global_webhooks() {
    for (int i = 0; i < 10; i++) {
      webhookDbTester.insertGlobalWebhook();
    }
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestRequest request = wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Maximum number of global webhooks reached");
  }

  @Test
  public void fail_if_url_is_not_valid() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestRequest request = wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, "htp://www.wrong-protocol.com/");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void execute_whenSecretIsTooShort_fail() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestRequest request = wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", "short");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_credential_in_url_is_have_a_wrong_format() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestRequest request = wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, "http://:www.wrong-protocol.com/");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void return_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    TestRequest request = wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void throw_ForbiddenException_if_project_not_provided_but_user_is_not_administrator() {
    userSession.logIn();
    TestRequest request = wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class).hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_not_project_administrator() {
    ComponentDto project = componentDbTester.insertPrivateProject().getMainBranchComponent();
    userSession.logIn();
    TestRequest request = wsActionTester.newRequest()
      .setParam(NAME_PARAM, NAME_WEBHOOK_EXAMPLE_001)
      .setParam(URL_PARAM, URL_WEBHOOK_EXAMPLE_001)
      .setParam(PROJECT_KEY_PARAM, project.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_IllegalArgumentException_if_project_key_greater_than_400() {
    String longProjectKey = generateStringWithLength(401);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestRequest request = wsActionTester.newRequest()
      .setParam("project", longProjectKey)
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", DEFAULT_COMPLIANT_SECRET);

    assertThatThrownBy(() -> request.executeProtobuf(CreateWsResponse.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'project' length (401) is longer than the maximum authorized (400)");
  }

  private static String generateStringWithLength(int length) {
    return "x".repeat(Math.max(0, length));
  }

}
