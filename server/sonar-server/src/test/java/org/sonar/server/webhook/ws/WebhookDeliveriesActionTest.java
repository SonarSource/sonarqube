/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.webhook.WebhookDeliveryDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.webhook.WebhookDbTesting.newWebhookDeliveryDto;
import static org.sonar.test.JsonAssert.assertJson;

public class WebhookDeliveriesActionTest {

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
    ComponentFinder componentFinder = TestComponentFinder.from(db);
    WebhookDeliveriesAction underTest = new WebhookDeliveriesAction(dbClient, userSession, componentFinder);
    ws = new WsActionTester(underTest);
    project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setDbKey("my-project"));
  }

  @Test
  public void test_definition() {
    assertThat(ws.getDef().params()).extracting(WebService.Param::key).containsOnly("componentKey", "ceTaskId");
    assertThat(ws.getDef().isPost()).isFalse();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void throw_UnauthorizedException_if_anonymous() {
    expectedException.expect(UnauthorizedException.class);

    ws.newRequest().execute();
  }

  @Test
  public void search_by_component_and_return_no_records() {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("componentKey", project.getDbKey())
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);

    assertThat(response.getDeliveriesCount()).isEqualTo(0);
  }

  @Test
  public void search_by_task_and_return_no_records() {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("ceTaskId", "t1")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);

    assertThat(response.getDeliveriesCount()).isEqualTo(0);
  }

  @Test
  public void search_by_component_and_return_records_of_example() {
    WebhookDeliveryDto dto = newWebhookDeliveryDto()
      .setUuid("d1")
      .setComponentUuid(project.uuid())
      .setCeTaskUuid("task-1")
      .setName("Jenkins")
      .setUrl("http://jenkins")
      .setCreatedAt(1_500_000_000_000L)
      .setSuccess(true)
      .setDurationMs(10)
      .setHttpStatus(200);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    String json = ws.newRequest()
      .setParam("componentKey", project.getDbKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void search_by_task_and_return_records() {
    WebhookDeliveryDto dto1 = newWebhookDeliveryDto().setComponentUuid(project.uuid()).setCeTaskUuid("t1");
    WebhookDeliveryDto dto2 = newWebhookDeliveryDto().setComponentUuid(project.uuid()).setCeTaskUuid("t1");
    WebhookDeliveryDto dto3 = newWebhookDeliveryDto().setComponentUuid(project.uuid()).setCeTaskUuid("t2");
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto1);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto2);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto3);
    db.commit();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("ceTaskId", "t1")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);
    assertThat(response.getDeliveriesCount()).isEqualTo(2);
    assertThat(response.getDeliveriesList()).extracting(Webhooks.Delivery::getId).containsOnly(dto1.getUuid(), dto2.getUuid());
  }

  @Test
  public void search_by_component_and_throw_ForbiddenException_if_not_admin_of_project() {
    WebhookDeliveryDto dto = newWebhookDeliveryDto()
      .setComponentUuid(project.uuid());
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam("componentKey", project.getDbKey())
      .execute();
  }

  @Test
  public void search_by_task_and_throw_ForbiddenException_if_not_admin_of_project() {
    WebhookDeliveryDto dto = newWebhookDeliveryDto()
      .setComponentUuid(project.uuid());
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam("ceTaskId", dto.getCeTaskUuid())
      .execute();
  }

  @Test
  public void throw_IAE_if_both_component_and_task_parameters_are_set() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'ceTaskId' or 'componentKey' must be provided");

    ws.newRequest()
      .setParam("componentKey", project.getDbKey())
      .setParam("ceTaskId", "t1")
      .execute();
  }
}
