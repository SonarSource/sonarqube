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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDeliveryDbTester;
import org.sonar.db.webhook.WebhookDeliveryDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.webhook.WebhookDeliveryTesting.newDto;
import static org.sonar.test.JsonAssert.assertJson;

public class WebhookDeliveriesActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private WebhookDeliveryDbTester webhookDeliveryDbTester = db.webhookDelivery();

  private WsActionTester ws;
  private ProjectDto project;
  private ProjectDto otherProject;

  @Before
  public void setUp() {
    ComponentFinder componentFinder = TestComponentFinder.from(db);
    WebhookDeliveriesAction underTest = new WebhookDeliveriesAction(dbClient, userSession, componentFinder);
    ws = new WsActionTester(underTest);
    project = db.components().insertPrivateProject(c -> c.setKey("my-project")).getProjectDto();
    otherProject = db.components().insertPrivateProject(c -> c.setKey("other-project")).getProjectDto();
  }

  @Test
  public void test_definition() {
    assertThat(ws.getDef().params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("componentKey", "ceTaskId", "webhook", "p", "ps");
    assertThat(ws.getDef().isPost()).isFalse();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void throw_UnauthorizedException_if_anonymous() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void search_by_component_and_return_no_records() {
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("componentKey", project.getKey())
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);

    assertThat(response.getDeliveriesCount()).isZero();
  }

  @Test
  public void search_by_task_and_return_no_records() {
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("ceTaskId", "t1")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);

    assertThat(response.getDeliveriesCount()).isZero();
  }

  @Test
  public void search_by_webhook_and_return_no_records() {
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("webhook", "t1")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);

    assertThat(response.getDeliveriesCount()).isZero();
  }

  @Test
  public void search_by_component_and_return_records_of_example() {
    WebhookDeliveryDto dto = newDto()
      .setUuid("d1")
      .setProjectUuid(project.getUuid())
      .setCeTaskUuid("task-1")
      .setName("Jenkins")
      .setUrl("http://jenkins")
      .setCreatedAt(1_500_000_000_000L)
      .setSuccess(true)
      .setDurationMs(10)
      .setHttpStatus(200);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    String json = ws.newRequest()
      .setParam("componentKey", project.getKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void search_by_task_and_return_records() {
    WebhookDeliveryDto dto1 = newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t1");
    WebhookDeliveryDto dto2 = newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t1");
    WebhookDeliveryDto dto3 = newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t2");
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto1);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto2);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto3);
    db.commit();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("ceTaskId", "t1")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);
    assertThat(response.getDeliveriesCount()).isEqualTo(2);
    assertThat(response.getDeliveriesList()).extracting(Webhooks.Delivery::getId).containsOnly(dto1.getUuid(), dto2.getUuid());
  }

  @Test
  public void search_by_webhook_and_return_records() {
    WebhookDeliveryDto dto1 = newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t1").setWebhookUuid("wh-1-uuid");
    WebhookDeliveryDto dto2 = newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t1").setWebhookUuid("wh-1-uuid");
    WebhookDeliveryDto dto3 = newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t2").setWebhookUuid("wh-2-uuid");

    WebhookDeliveryDto dto4 = newDto().setProjectUuid(otherProject.getUuid()).setCeTaskUuid("t4").setWebhookUuid("wh-1-uuid");
    WebhookDeliveryDto dto5 = newDto().setProjectUuid(otherProject.getUuid()).setCeTaskUuid("t5").setWebhookUuid("wh-1-uuid");

    dbClient.webhookDeliveryDao().insert(db.getSession(), dto1);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto2);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto3);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto4);
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto5);
    db.commit();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project, otherProject);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("webhook", "wh-1-uuid")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);
    assertThat(response.getDeliveriesCount()).isEqualTo(4);
    assertThat(response.getDeliveriesList()).extracting(Webhooks.Delivery::getId)
      .containsOnly(dto1.getUuid(), dto2.getUuid(), dto4.getUuid(), dto5.getUuid());
    assertThat(response.getDeliveriesList()).extracting(Webhooks.Delivery::getId, Webhooks.Delivery::getComponentKey)
      .containsOnly(
        tuple(dto1.getUuid(), project.getKey()),
        tuple(dto2.getUuid(), project.getKey()),
        tuple(dto4.getUuid(), otherProject.getKey()),
        tuple(dto5.getUuid(), otherProject.getKey()));
  }

  @Test
  public void validate_default_pagination() {

    for (int i = 0; i < 15; i++) {
      webhookDeliveryDbTester.insert(newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t1").setWebhookUuid("wh-1-uuid"));
    }

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("webhook", "wh-1-uuid")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);

    assertThat(response.getDeliveriesCount()).isEqualTo(10);

  }

  @Test
  public void validate_pagination_first_page() {

    for (int i = 0; i < 12; i++) {
      webhookDeliveryDbTester.insert(newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t1").setWebhookUuid("wh-1-uuid"));
    }

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("webhook", "wh-1-uuid")
      .setParam("p", "1")
      .setParam("ps", "10")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);

    assertThat(response.getDeliveriesCount()).isEqualTo(10);
    assertThat(response.getPaging().getTotal()).isEqualTo(12);
    assertThat(response.getPaging().getPageIndex()).isOne();
  }

  @Test
  public void validate_pagination_last_page() {

    for (int i = 0; i < 12; i++) {
      webhookDeliveryDbTester.insert(newDto().setProjectUuid(project.getUuid()).setCeTaskUuid("t1").setWebhookUuid("wh-1-uuid"));
    }

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    Webhooks.DeliveriesWsResponse response = ws.newRequest()
      .setParam("webhook", "wh-1-uuid")
      .setParam("p", "2")
      .setParam("ps", "10")
      .executeProtobuf(Webhooks.DeliveriesWsResponse.class);

    assertThat(response.getDeliveriesCount()).isEqualTo(2);
    assertThat(response.getPaging().getTotal()).isEqualTo(12);
    assertThat(response.getPaging().getPageIndex()).isEqualTo(2);
  }

  @Test
  public void search_by_component_and_throw_ForbiddenException_if_not_admin_of_project() {
    WebhookDeliveryDto dto = newDto()
      .setProjectUuid(project.getUuid());
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.logIn().addProjectPermission(ProjectPermission.USER, project);

    TestRequest request = ws.newRequest()
      .setParam("componentKey", project.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void search_by_task_and_throw_ForbiddenException_if_not_admin_of_project() {
    WebhookDeliveryDto dto = newDto()
      .setProjectUuid(project.getUuid());
    dbClient.webhookDeliveryDao().insert(db.getSession(), dto);
    db.commit();
    userSession.logIn().addProjectPermission(ProjectPermission.USER, project);

    TestRequest request = ws.newRequest()
      .setParam("ceTaskId", dto.getCeTaskUuid());
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_IAE_if_both_component_and_task_parameters_are_set() {
    userSession.logIn();

    TestRequest request = ws.newRequest()
      .setParam("componentKey", project.getKey())
      .setParam("ceTaskId", "t1");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either 'ceTaskId' or 'componentKey' or 'webhook' must be provided");
  }

  @Test
  public void throw_IAE_if_both_component_and_webhook_are_set() {
    userSession.logIn();

    TestRequest request = ws.newRequest()
      .setParam("componentKey", project.getKey())
      .setParam("webhook", "wh-uuid");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either 'ceTaskId' or 'componentKey' or 'webhook' must be provided");
  }
}
