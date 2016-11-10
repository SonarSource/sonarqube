/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.webhook.WebhookDeliveryDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.webhook.WebhookDbTesting.newWebhookDeliveryDto;
import static org.sonar.test.JsonAssert.assertJson;

public class WebhookDeliveryActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private WsActionTester ws;
  private ComponentDto project;

  @Before
  public void setUp() {
    ComponentFinder componentFinder = new ComponentFinder(dbClient);
    WebhookDeliveryAction underTest = new WebhookDeliveryAction(dbClient, userSession, componentFinder);
    ws = new WsActionTester(underTest);
    project = db.components().insertComponent(newProjectDto().setKey("my-project"));
  }

  @Test
  public void test_definition() {
    assertThat(ws.getDef().params()).extracting(WebService.Param::key).containsOnly("deliveryId");
    assertThat(ws.getDef().isPost()).isFalse();
    assertThat(ws.getDef().isInternal()).isTrue();
    assertThat(ws.getDef().responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void throw_UnauthorizedException_if_anonymous() {
    expectedException.expect(UnauthorizedException.class);

    ws.newRequest().execute();
  }

  @Test
  public void return_404_if_delivery_does_not_exist() throws Exception {
    userSession.login();

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("deliveryId", "does_not_exist")
      .execute();
  }

  @Test
  public void load_the_delivery_of_example() throws Exception {
    WebhookDeliveryDto dto = newWebhookDeliveryDto()
      .setUuid("d1")
      .setComponentUuid(project.uuid())
      .setCeTaskUuid("task-1")
      .setName("Jenkins")
      .setUrl("http://jenkins")
      .setCreatedAt(1_500_000_000_000L)
      .setSuccess(true)
      .setDurationMs(10)
      .setHttpStatus(200)
      .setPayload("{\"status\"=\"SUCCESS\"}");
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.login().addProjectUuidPermissions(UserRole.ADMIN, project.uuid());

    String json = ws.newRequest()
      .setParam("deliveryId", dto.getUuid())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void return_delivery_that_failed_to_be_sent() throws Exception {
    WebhookDeliveryDto dto = newWebhookDeliveryDto()
      .setComponentUuid(project.uuid())
      .setSuccess(false)
      .setHttpStatus(null)
      .setDurationMs(null)
      .setErrorStacktrace("IOException -> can not connect");
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.login().addProjectUuidPermissions(UserRole.ADMIN, project.uuid());

    Webhooks.DeliveryWsResponse response = Webhooks.DeliveryWsResponse.parseFrom(ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("deliveryId", dto.getUuid())
      .execute()
      .getInputStream());

    Webhooks.Delivery actual = response.getDelivery();
    assertThat(actual.hasHttpStatus()).isFalse();
    assertThat(actual.hasDurationMs()).isFalse();
    assertThat(actual.getErrorStacktrace()).isEqualTo(dto.getErrorStacktrace());
  }

  @Test
  public void throw_ForbiddenException_if_not_admin_of_project() throws Exception {
    WebhookDeliveryDto dto = newWebhookDeliveryDto()
      .setComponentUuid(project.uuid());
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.login().addProjectUuidPermissions(UserRole.USER, project.uuid());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam("deliveryId", dto.getUuid())
      .execute();
  }
}
