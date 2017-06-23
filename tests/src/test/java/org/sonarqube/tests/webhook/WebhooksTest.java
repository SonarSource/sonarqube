/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.webhook;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category3Suite;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Webhooks;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.project.DeleteRequest;
import org.sonarqube.ws.client.setting.ResetRequest;
import org.sonarqube.ws.client.setting.SetRequest;
import org.sonarqube.ws.client.webhook.DeliveriesRequest;
import util.ItUtils;

import static java.util.Objects.requireNonNull;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.jsonToMap;
import static util.ItUtils.runProjectAnalysis;

public class WebhooksTest {

  private static final String PROJECT_KEY = "my-project";
  private static final String PROJECT_NAME = "My Project";
  private static final String GLOBAL_WEBHOOK_PROPERTY = "sonar.webhooks.global";
  private static final String PROJECT_WEBHOOK_PROPERTY = "sonar.webhooks.project";

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @ClassRule
  public static ExternalServer externalServer = new ExternalServer();

  private WsClient adminWs = ItUtils.newAdminWsClient(orchestrator);

  @Before
  public void setUp() throws Exception {
    externalServer.clear();
  }

  @Before
  @After
  public void reset() throws Exception {
    disableGlobalWebhooks();
    try {
      // delete project and related properties/webhook deliveries
      adminWs.projects().delete(DeleteRequest.builder().setKey(PROJECT_KEY).build());
    } catch (HttpException e) {
      // ignore because project may not exist
    }
  }

  @Test
  public void call_multiple_global_and_project_webhooks_when_analysis_is_done() {
    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_NAME);
    enableGlobalWebhooks(
      new Webhook("Jenkins", externalServer.urlFor("/jenkins")),
      new Webhook("HipChat", externalServer.urlFor("/hipchat")));
    enableProjectWebhooks(PROJECT_KEY,
      new Webhook("Burgr", externalServer.urlFor("/burgr")));

    analyseProject();

    // the same payload has been sent to three servers
    assertThat(externalServer.getPayloadRequests()).hasSize(3);
    PayloadRequest request = externalServer.getPayloadRequests().get(0);
    for (int i = 1; i < 3; i++) {
      PayloadRequest r = externalServer.getPayloadRequests().get(i);
      assertThat(request.getJson()).isEqualTo(r.getJson());
    }

    // verify HTTP headers
    assertThat(request.getHttpHeaders().get("X-SonarQube-Project")).isEqualTo(PROJECT_KEY);

    // verify content of payload
    Map<String, Object> payload = jsonToMap(request.getJson());
    assertThat(payload.get("status")).isEqualTo("SUCCESS");
    assertThat(payload.get("analysedAt")).isNotNull();
    Map<String, String> project = (Map<String, String>) payload.get("project");
    assertThat(project.get("key")).isEqualTo(PROJECT_KEY);
    assertThat(project.get("name")).isEqualTo(PROJECT_NAME);
    Map<String, Object> gate = (Map<String, Object>) payload.get("qualityGate");
    assertThat(gate.get("name")).isEqualTo("SonarQube way");
    assertThat(gate.get("status")).isEqualTo("OK");
    assertThat(gate.get("conditions")).isNotNull();

    // verify list of persisted deliveries (api/webhooks/deliveries)
    List<Webhooks.Delivery> deliveries = getPersistedDeliveries();
    assertThat(deliveries).hasSize(3);
    for (Webhooks.Delivery delivery : deliveries) {
      assertThatPersistedDeliveryIsValid(delivery);
      assertThat(delivery.getSuccess()).isTrue();
      assertThat(delivery.getHttpStatus()).isEqualTo(200);
      assertThat(delivery.getName()).isIn("Jenkins", "HipChat", "Burgr");
      assertThat(delivery.hasErrorStacktrace()).isFalse();
      // payload is available only in api/webhooks/delivery to avoid loading multiple DB CLOBs
      assertThat(delivery.hasPayload()).isFalse();
    }

    // verify detail of persisted delivery (api/webhooks/delivery)
    Webhooks.Delivery detail = getDetailOfPersistedDelivery(deliveries.get(0));
    assertThatPersistedDeliveryIsValid(detail);
    assertThat(detail.getPayload()).isEqualTo(request.getJson());
  }

  @Test
  public void persist_delivery_as_failed_if_external_server_returns_an_error_code() {
    enableGlobalWebhooks(
      new Webhook("Fail", externalServer.urlFor("/fail")),
      new Webhook("HipChat", externalServer.urlFor("/hipchat")));

    analyseProject();

    // all webhooks are called, even if one returns an error code
    assertThat(externalServer.getPayloadRequests()).hasSize(2);

    // verify persisted deliveries
    Webhooks.Delivery failedDelivery = getPersistedDeliveryByName("Fail");
    assertThatPersistedDeliveryIsValid(failedDelivery);
    assertThat(failedDelivery.getSuccess()).isFalse();
    assertThat(failedDelivery.getHttpStatus()).isEqualTo(500);

    Webhooks.Delivery successfulDelivery = getPersistedDeliveryByName("HipChat");
    assertThatPersistedDeliveryIsValid(successfulDelivery);
    assertThat(successfulDelivery.getSuccess()).isTrue();
    assertThat(successfulDelivery.getHttpStatus()).isEqualTo(200);
  }

  /**
   * Restrict calls to ten webhooks per type (global or project)
   */
  @Test
  public void do_not_become_a_denial_of_service_attacker() {
    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_NAME);

    List<Webhook> globalWebhooks = range(0, 15).mapToObj(i -> new Webhook("G" + i, externalServer.urlFor("/global"))).collect(Collectors.toList());
    enableGlobalWebhooks(globalWebhooks.toArray(new Webhook[globalWebhooks.size()]));
    List<Webhook> projectWebhooks = range(0, 15).mapToObj(i -> new Webhook("P" + i, externalServer.urlFor("/project"))).collect(Collectors.toList());
    enableProjectWebhooks(PROJECT_KEY, projectWebhooks.toArray(new Webhook[projectWebhooks.size()]));

    analyseProject();

    // only the first ten global webhooks and ten project webhooks are called
    assertThat(externalServer.getPayloadRequests()).hasSize(10 + 10);
    assertThat(externalServer.getPayloadRequestsOnPath("/global")).hasSize(10);
    assertThat(externalServer.getPayloadRequestsOnPath("/project")).hasSize(10);

    // verify persisted deliveries
    assertThat(getPersistedDeliveries()).hasSize(10 + 10);
  }

  @Test
  public void persist_delivery_as_failed_if_webhook_url_is_malformed() {
    enableGlobalWebhooks(new Webhook("Jenkins", "this_is_not_an_url"));

    analyseProject();

    assertThat(externalServer.getPayloadRequests()).isEmpty();

    // verify persisted deliveries
    Webhooks.Delivery delivery = getPersistedDeliveryByName("Jenkins");
    Webhooks.Delivery detail = getDetailOfPersistedDelivery(delivery);

    assertThat(detail.getSuccess()).isFalse();
    assertThat(detail.hasHttpStatus()).isFalse();
    assertThat(detail.hasDurationMs()).isFalse();
    assertThat(detail.getPayload()).isNotEmpty();
    assertThat(detail.getErrorStacktrace())
      .contains("java.lang.IllegalArgumentException")
      .contains("unexpected url")
      .contains("this_is_not_an_url");
  }

  @Test
  public void ignore_webhook_if_url_is_missing() {
    // property sets, as used to define webhooks, do
    // not allow to validate values yet
    enableGlobalWebhooks(new Webhook("Jenkins", null));

    analyseProject();

    assertThat(externalServer.getPayloadRequests()).isEmpty();
    assertThat(getPersistedDeliveries()).isEmpty();
  }

  private void analyseProject() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.projectKey", PROJECT_KEY,
      "sonar.projectName", PROJECT_NAME);
  }

  private List<Webhooks.Delivery> getPersistedDeliveries() {
    DeliveriesRequest deliveriesReq = DeliveriesRequest.builder().setComponentKey(PROJECT_KEY).build();
    return adminWs.webhooks().deliveries(deliveriesReq).getDeliveriesList();
  }

  private Webhooks.Delivery getPersistedDeliveryByName(String webhookName) {
    List<Webhooks.Delivery> deliveries = getPersistedDeliveries();
    return deliveries.stream().filter(d -> d.getName().equals(webhookName)).findFirst().get();
  }

  private Webhooks.Delivery getDetailOfPersistedDelivery(Webhooks.Delivery delivery) {
    Webhooks.Delivery detail = adminWs.webhooks().delivery(delivery.getId()).getDelivery();
    return requireNonNull(detail);
  }

  private void assertThatPersistedDeliveryIsValid(Webhooks.Delivery delivery) {
    assertThat(delivery.getId()).isNotEmpty();
    assertThat(delivery.getName()).isNotEmpty();
    assertThat(delivery.hasSuccess()).isTrue();
    assertThat(delivery.getHttpStatus()).isGreaterThanOrEqualTo(200);
    assertThat(delivery.getDurationMs()).isGreaterThanOrEqualTo(0);
    assertThat(delivery.getAt()).isNotEmpty();
    assertThat(delivery.getComponentKey()).isEqualTo(PROJECT_KEY);
    assertThat(delivery.getUrl()).startsWith(externalServer.urlFor("/"));
  }

  private void enableGlobalWebhooks(Webhook... webhooks) {
    enableWebhooks(null, GLOBAL_WEBHOOK_PROPERTY, webhooks);
  }

  private void enableProjectWebhooks(String projectKey, Webhook... webhooks) {
    enableWebhooks(projectKey, PROJECT_WEBHOOK_PROPERTY, webhooks);
  }

  private void enableWebhooks(@Nullable String projectKey, String property, Webhook... webhooks) {
    List<String> webhookIds = new ArrayList<>();
    for (int i = 0; i < webhooks.length; i++) {
      Webhook webhook = webhooks[i];
      String id = String.valueOf(i + 1);
      webhookIds.add(id);
      setProperty(projectKey, property + "." + id + ".name", webhook.name);
      setProperty(projectKey, property + "." + id + ".url", webhook.url);
    }
    setProperty(projectKey, property, StringUtils.join(webhookIds, ","));
  }

  private void disableGlobalWebhooks() {
    setProperty(null, GLOBAL_WEBHOOK_PROPERTY, null);
  }

  private void setProperty(@Nullable String componentKey, String key, @Nullable String value) {
    if (value == null) {
      ResetRequest req = ResetRequest.builder().setKeys(key).setComponent(componentKey).build();
      adminWs.settings().reset(req);
    } else {
      SetRequest req = SetRequest.builder().setKey(key).setValue(value).setComponent(componentKey).build();
      adminWs.settings().set(req);
    }
  }

  private static class Webhook {
    private final String name;
    private final String url;

    Webhook(@Nullable String name, @Nullable String url) {
      this.name = name;
      this.url = url;
    }
  }

}
