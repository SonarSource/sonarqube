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

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.component.ComponentFinder;
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
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.server.ws.KeyExamples.NAME_WEBHOOK_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.URL_WEBHOOK_EXAMPLE_001;

public class UpdateActionIT {

  private static final String DEFAULT_COMPLIANT_SECRET = "at_least_16_characters";

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = create();
  private final DbClient dbClient = db.getDbClient();
  private final WebhookDbTester webhookDbTester = db.webhooks();
  private final ComponentDbTester componentDbTester = db.components();
  private final Configuration configuration = mock(Configuration.class);
  private final NetworkInterfaceProvider networkInterfaceProvider = mock(NetworkInterfaceProvider.class);
  private final WebhookSupport webhookSupport = new WebhookSupport(userSession, configuration, networkInterfaceProvider);
  private final ComponentTypes componentTypes = mock(ComponentTypes.class);
  private final ComponentFinder componentFinder = new ComponentFinder(dbClient, componentTypes);
  private final UpdateAction underTest = new UpdateAction(dbClient, userSession, webhookSupport, componentFinder);
  private final WsActionTester wsActionTester = new WsActionTester(underTest);

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
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    TestResponse response = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(reloaded.get().getSecret()).isEqualTo(dto.getSecret());
  }

  @Test
  public void update_with_empty_secrets_removes_the_secret() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    TestResponse response = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", "")
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(reloaded.get().getSecret()).isNull();
  }

  @Test
  public void update_a_project_webhook_with_all_fields() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    TestResponse response = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", DEFAULT_COMPLIANT_SECRET)
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(reloaded.get().getSecret()).isEqualTo(DEFAULT_COMPLIANT_SECRET);
  }

  @Test
  public void update_a_global_webhook() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);

    TestResponse response = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", DEFAULT_COMPLIANT_SECRET)
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    Optional<WebhookDto> reloaded = webhookDbTester.selectWebhook(dto.getUuid());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getName()).isEqualTo(NAME_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getUrl()).isEqualTo(URL_WEBHOOK_EXAMPLE_001);
    assertThat(reloaded.get().getProjectUuid()).isNull();
    assertThat(reloaded.get().getSecret()).isEqualTo(DEFAULT_COMPLIANT_SECRET);
  }

  @Test
  public void fail_if_webhook_does_not_exist() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestRequest request = wsActionTester.newRequest()
      .setParam("webhook", "inexistent-webhook-uuid")
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No webhook with key 'inexistent-webhook-uuid'");
  }

  @Test
  public void fail_if_not_logged_in() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();
    userSession.anonymous();
    TestRequest request = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_no_permission_on_webhook_scope_project() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn();
    TestRequest request = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_no_permission_on_webhook_scope_global() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();
    userSession.logIn();
    TestRequest request = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_url_is_not_valid() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    TestRequest request = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", "htp://www.wrong-protocol.com/");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void handle_whenSecretIsTooShort_fail() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    TestRequest request = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", URL_WEBHOOK_EXAMPLE_001)
      .setParam("secret", "short");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_credential_in_url_is_have_a_wrong_format() {
    ProjectDto project = componentDbTester.insertPrivateProject().getProjectDto();
    WebhookDto dto = webhookDbTester.insertWebhook(project);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    TestRequest request = wsActionTester.newRequest()
      .setParam("webhook", dto.getUuid())
      .setParam("name", NAME_WEBHOOK_EXAMPLE_001)
      .setParam("url", "http://:www.wrong-protocol.com/");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

}
