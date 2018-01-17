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
package org.sonarqube.tests.webhook;

import com.sonar.orchestrator.Orchestrator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category3Suite;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.Webhooks;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issues.BulkChangeRequest;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.projects.DeleteRequest;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;
import org.sonarqube.ws.client.settings.ResetRequest;
import org.sonarqube.ws.client.settings.SetRequest;
import org.sonarqube.ws.client.webhooks.DeliveriesRequest;
import org.sonarqube.ws.client.webhooks.DeliveryRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
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

  @Rule
  public Tester tester = new Tester(orchestrator);

  private WsClient adminWs = ItUtils.newAdminWsClient(orchestrator);

  @Before
  public void setUp() {
    externalServer.clear();
  }

  @Before
  @After
  public void reset() {
    disableGlobalWebhooks();
    try {
      // delete project and related properties/webhook deliveries
      adminWs.projects().delete(new DeleteRequest().setProject(PROJECT_KEY));
    } catch (HttpException e) {
      // ignore because project may not exist
    }
  }

  @Test
  public void call_multiple_global_and_project_webhooks_when_analysis_is_done() throws InterruptedException {
    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_NAME);
    enableGlobalWebhooks(
      new Webhook("Jenkins", externalServer.urlFor("/jenkins")),
      new Webhook("HipChat", externalServer.urlFor("/hipchat")));
    enableProjectWebhooks(PROJECT_KEY,
      new Webhook("Burgr", externalServer.urlFor("/burgr")));

    analyseProject();

    // the same payload has been sent to three servers
    waitUntilAllWebHooksCalled(3);
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
    assertThat(project.get("url")).isEqualTo(orchestrator.getServer().getUrl() + "/dashboard?id=" + PROJECT_KEY);
    Map<String, Object> gate = (Map<String, Object>) payload.get("qualityGate");
    assertThat(gate.get("name")).isEqualTo("Sonar way");
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
  public void do_not_become_a_denial_of_service_attacker() throws InterruptedException {
    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_NAME);

    List<Webhook> globalWebhooks = range(0, 15).mapToObj(i -> new Webhook("G" + i, externalServer.urlFor("/global"))).collect(Collectors.toList());
    enableGlobalWebhooks(globalWebhooks.toArray(new Webhook[globalWebhooks.size()]));
    List<Webhook> projectWebhooks = range(0, 15).mapToObj(i -> new Webhook("P" + i, externalServer.urlFor("/project"))).collect(Collectors.toList());
    enableProjectWebhooks(PROJECT_KEY, projectWebhooks.toArray(new Webhook[projectWebhooks.size()]));

    analyseProject();

    // only the first ten global webhooks and ten project webhooks are called
    waitUntilAllWebHooksCalled(10 + 10);
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
      .contains("Webhook URL is not valid: this_is_not_an_url");
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

  @Test
  public void send_webhook_on_issue_change() throws InterruptedException {
    Organization defaultOrganization = tester.organizations().getDefaultOrganization();
    Project wsProject = tester.projects().provision(r -> r.setProject(PROJECT_KEY).setName(PROJECT_NAME));
    enableProjectWebhooks(PROJECT_KEY, new Webhook("Burgr", externalServer.urlFor("/burgr")));
    // quality profile with one issue per line
    QualityProfile qualityProfile = tester.qProfiles().createXooProfile(defaultOrganization);
    tester.qProfiles().activateRule(qualityProfile, "xoo:OneIssuePerLine");
    tester.qProfiles().assignQProfileToProject(qualityProfile, wsProject);
    // quality gate definition
    Qualitygates.CreateResponse qGate = tester.qGates().generate();
    tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(String.valueOf(qGate.getId()))
      .setMetric("reliability_rating").setOp("GT").setError("1"));
    tester.qGates().associateProject(qGate, wsProject);
    // analyze project and clear first webhook
    analyseProject();
    waitUntilAllWebHooksCalled(1);
    externalServer.clear();

    // change an issue to blocker bug, QG status goes from OK to ERROR, so webhook is called
    List<Issue> issues = tester.wsClient().issues().search(new SearchRequest()).getIssuesList();
    Issue firstIssue = issues.iterator().next();
    tester.wsClient().issues().bulkChange(new BulkChangeRequest().setIssues(singletonList(firstIssue.getKey()))
      .setSetSeverity(singletonList("BLOCKER"))
      .setSetType(singletonList("BUG")));
    waitUntilAllWebHooksCalled(1);

    PayloadRequest request = externalServer.getPayloadRequests().get(0);
    assertThat(request.getHttpHeaders().get("X-SonarQube-Project")).isEqualTo(PROJECT_KEY);
    // verify content of payload
    Map<String, Object> payload = jsonToMap(request.getJson());
    assertThat(payload.get("status")).isEqualTo("SUCCESS");
    assertThat(payload.get("analysedAt")).isNotNull();
    Map<String, String> project = (Map<String, String>) payload.get("project");
    assertThat(project.get("key")).isEqualTo(PROJECT_KEY);
    assertThat(project.get("name")).isEqualTo(PROJECT_NAME);
    assertThat(project.get("url")).isEqualTo(orchestrator.getServer().getUrl() + "/dashboard?id=" + PROJECT_KEY);
    Map<String, Object> gate = (Map<String, Object>) payload.get("qualityGate");
    assertThat(gate.get("name")).isEqualTo(qGate.getName());
    assertThat(gate.get("status")).isEqualTo("ERROR");
    assertThat(gate.get("conditions")).isNotNull();
    externalServer.clear();

    // change severity of issue, won't change the QG status, so no webhook called
    tester.wsClient().issues().bulkChange(new BulkChangeRequest().setIssues(singletonList(firstIssue.getKey()))
      .setSetSeverity(singletonList("MINOR")));
    waitUntilAllWebHooksCalled(1);
    assertThat(externalServer.getPayloadRequests()).isEmpty();

    // resolve issue as won't fix, QG status goes to OK, so webhook called
    tester.wsClient().issues().bulkChange(new BulkChangeRequest().setIssues(singletonList(firstIssue.getKey()))
      .setDoTransition("wontfix"));
    waitUntilAllWebHooksCalled(1);
    request = externalServer.getPayloadRequests().get(0);
    payload = jsonToMap(request.getJson());
    gate = (Map<String, Object>) payload.get("qualityGate");
    assertThat(gate.get("status")).isEqualTo("OK");
  }

  private void analyseProject() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.projectKey", PROJECT_KEY,
      "sonar.projectName", PROJECT_NAME);
  }

  private List<Webhooks.Delivery> getPersistedDeliveries() {
    DeliveriesRequest deliveriesReq = new DeliveriesRequest().setComponentKey(PROJECT_KEY);
    return adminWs.webhooks().deliveries(deliveriesReq).getDeliveriesList();
  }

  private Webhooks.Delivery getPersistedDeliveryByName(String webhookName) {
    List<Webhooks.Delivery> deliveries = getPersistedDeliveries();
    return deliveries.stream().filter(d -> d.getName().equals(webhookName)).findFirst().get();
  }

  private Webhooks.Delivery getDetailOfPersistedDelivery(Webhooks.Delivery delivery) {
    Webhooks.Delivery detail = adminWs.webhooks().delivery(new DeliveryRequest().setDeliveryId(delivery.getId())).getDelivery();
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
      ResetRequest req = new ResetRequest().setKeys(Collections.singletonList(key)).setComponent(componentKey);
      adminWs.settings().reset(req);
    } else {
      SetRequest req = new SetRequest().setKey(key).setValue(value).setComponent(componentKey);
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

  /**
   * Wait up to 30 seconds
   */
  private static void waitUntilAllWebHooksCalled(int expectedNumberOfRequests) throws InterruptedException {
    for (int i = 0; i < 60; i++) {
      if (externalServer.getPayloadRequests().size() == expectedNumberOfRequests) {
        break;
      }
      Thread.sleep(500);
    }
  }

}
