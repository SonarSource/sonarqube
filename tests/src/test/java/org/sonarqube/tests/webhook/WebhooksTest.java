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
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.Webhooks;
import org.sonarqube.ws.Webhooks.CreateWsResponse.Webhook;
import org.sonarqube.ws.client.issues.BulkChangeRequest;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.jsonToMap;
import static util.ItUtils.runProjectAnalysis;

public class WebhooksTest {

  @ClassRule
  public static Orchestrator orchestrator = WebhooksSuite.ORCHESTRATOR;

  @ClassRule
  public static ExternalServer externalServer = new ExternalServer();

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    externalServer.clear();
  }

  @Test
  public void call_multiple_global_and_project_webhooks_when_analysis_is_done() throws InterruptedException {
    Project project = tester.projects().provision();
    Webhook jenkins = tester.webhooks().generate(p -> p.setName("Jenkins").setUrl(externalServer.urlFor("/jenkins")));
    Webhook hipchat = tester.webhooks().generate(p -> p.setName("HipChat").setUrl(externalServer.urlFor("/hipchat")));
    Webhook burgr = tester.webhooks().generate(project, p -> p.setName("Burgr").setUrl(externalServer.urlFor("/burgr")));

    analyseProject(project);

    // the same payload has been sent to three servers
    externalServer.waitUntilAllWebHooksCalled(3);
    assertThat(externalServer.getPayloadRequests()).hasSize(3);
    PayloadRequest request = externalServer.getPayloadRequests().get(0);
    for (int i = 1; i < 3; i++) {
      PayloadRequest r = externalServer.getPayloadRequests().get(i);
      assertThat(request.getJson()).isEqualTo(r.getJson());
    }

    // verify HTTP headers
    assertThat(request.getHttpHeaders().get("X-SonarQube-Project")).isEqualTo(project.getKey());

    // verify content of payload
    Map<String, Object> payload = jsonToMap(request.getJson());
    assertThat(payload.get("status")).isEqualTo("SUCCESS");
    assertThat(payload.get("analysedAt")).isNotNull();
    Map<String, String> projectPayload = (Map<String, String>) payload.get("project");
    assertThat(projectPayload.get("key")).isEqualTo(project.getKey());
    assertThat(projectPayload.get("name")).isEqualTo(project.getName());
    assertThat(projectPayload.get("url")).isEqualTo(orchestrator.getServer().getUrl() + "/dashboard?id=" + project.getKey());
    Map<String, Object> gate = (Map<String, Object>) payload.get("qualityGate");
    assertThat(gate.get("name")).isEqualTo("Sonar way");
    assertThat(gate.get("status")).isEqualTo("OK");
    assertThat(gate.get("conditions")).isNotNull();

    // verify list of persisted deliveries (api/webhooks/deliveries)
    List<Webhooks.Delivery> deliveries = tester.webhooks().getPersistedDeliveries(project);
    assertThat(deliveries).hasSize(3);
    for (Webhooks.Delivery delivery : deliveries) {
      tester.webhooks().assertThatPersistedDeliveryIsValid(delivery, project, externalServer.urlFor("/"));
      assertThat(delivery.getSuccess()).isTrue();
      assertThat(delivery.getHttpStatus()).isEqualTo(200);
      assertThat(delivery.getName()).isIn(jenkins.getName(), hipchat.getName(), burgr.getName());
      assertThat(delivery.hasErrorStacktrace()).isFalse();
      // payload is available only in api/webhooks/delivery to avoid loading multiple DB CLOBs
      assertThat(delivery.hasPayload()).isFalse();
    }

    // verify detail of persisted delivery (api/webhooks/delivery)
    Webhooks.Delivery detail = tester.webhooks().getDetailOfPersistedDelivery(deliveries.get(0));
    tester.webhooks().assertThatPersistedDeliveryIsValid(detail, project, externalServer.urlFor("/"));
    assertThat(detail.getPayload()).isEqualTo(request.getJson());
  }

  @Test
  public void persist_delivery_as_failed_if_external_server_returns_an_error_code() {
    Project project = tester.projects().provision();
    Webhook fail = tester.webhooks().generate(p -> p.setName("Fail").setUrl(externalServer.urlFor("/fail")));
    Webhook hipchat = tester.webhooks().generate(p -> p.setName("HipChat").setUrl(externalServer.urlFor("/hipchat")));

    analyseProject(project);

    // all webhooks are called, even if one returns an error code
    assertThat(externalServer.getPayloadRequests()).hasSize(2);

    // verify persisted deliveries
    Webhooks.Delivery failedDelivery = tester.webhooks().getPersistedDeliveryByName(project, fail.getName());
    tester.webhooks().assertThatPersistedDeliveryIsValid(failedDelivery, project, fail.getUrl());
    assertThat(failedDelivery.getSuccess()).isFalse();
    assertThat(failedDelivery.getHttpStatus()).isEqualTo(500);

    Webhooks.Delivery successfulDelivery = tester.webhooks().getPersistedDeliveryByName(project, hipchat.getName());
    tester.webhooks().assertThatPersistedDeliveryIsValid(successfulDelivery, project, hipchat.getUrl());
    assertThat(successfulDelivery.getSuccess()).isTrue();
    assertThat(successfulDelivery.getHttpStatus()).isEqualTo(200);
  }

  @Test
  public void persist_delivery_as_failed_if_webhook_is_not_reachable() {
    Project project = tester.projects().provision();
    Webhook badUrl = tester.webhooks().generate(p -> p.setUrl("http://does_not_exist"));

    analyseProject(project);

    assertThat(externalServer.getPayloadRequests()).isEmpty();

    // verify persisted deliveries
    Webhooks.Delivery delivery = tester.webhooks().getPersistedDeliveryByName(project, badUrl.getName());
    Webhooks.Delivery detail = tester.webhooks().getDetailOfPersistedDelivery(delivery);

    assertThat(detail.getSuccess()).isFalse();
    assertThat(detail.hasHttpStatus()).isFalse();
    assertThat(detail.hasDurationMs()).isFalse();
    assertThat(detail.getPayload()).isNotEmpty();
    assertThat(detail.getErrorStacktrace())
      .contains("java.net.UnknownHostException")
      .contains("Name or service not known");
  }

  @Test
  public void send_webhook_on_issue_change() throws InterruptedException {
    Organization defaultOrganization = tester.organizations().getDefaultOrganization();
    Project project = tester.projects().provision();
    Webhook burgr = tester.webhooks().generate(project, p -> p.setName("Burgr").setUrl(externalServer.urlFor("/burgr")));

    // quality profile with one issue per line
    QualityProfile qualityProfile = tester.qProfiles().createXooProfile(defaultOrganization);
    tester.qProfiles().activateRule(qualityProfile, "xoo:OneIssuePerLine");
    tester.qProfiles().assignQProfileToProject(qualityProfile, project);
    // quality gate definition
    Qualitygates.CreateResponse qGate = tester.qGates().generate();
    tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(String.valueOf(qGate.getId()))
      .setMetric("reliability_rating").setOp("GT").setError("1"));
    tester.qGates().associateProject(qGate, project);
    // analyze project and clear first webhook

    analyseProject(project);
    externalServer.waitUntilAllWebHooksCalled(1);
    externalServer.clear();

    // change an issue to blocker bug, QG status goes from OK to ERROR, so webhook is called
    List<Issue> issues = tester.wsClient().issues().search(new SearchRequest()).getIssuesList();
    Issue firstIssue = issues.iterator().next();
    tester.wsClient().issues().bulkChange(new BulkChangeRequest().setIssues(singletonList(firstIssue.getKey()))
      .setSetSeverity(singletonList("BLOCKER"))
      .setSetType(singletonList("BUG")));
    externalServer.waitUntilAllWebHooksCalled(1);

    PayloadRequest request = externalServer.getPayloadRequests().get(0);
    assertThat(request.getHttpHeaders().get("X-SonarQube-Project")).isEqualTo(project.getKey());
    // verify content of payload
    Map<String, Object> payload = jsonToMap(request.getJson());
    assertThat(payload.get("status")).isEqualTo("SUCCESS");
    assertThat(payload.get("analysedAt")).isNotNull();
    Map<String, String> projectPayload = (Map<String, String>) payload.get("project");
    assertThat(projectPayload.get("key")).isEqualTo(project.getKey());
    assertThat(projectPayload.get("name")).isEqualTo(project.getName());
    assertThat(projectPayload.get("url")).isEqualTo(orchestrator.getServer().getUrl() + "/dashboard?id=" + project.getKey());
    Map<String, Object> gate = (Map<String, Object>) payload.get("qualityGate");
    assertThat(gate.get("name")).isEqualTo(qGate.getName());
    assertThat(gate.get("status")).isEqualTo("ERROR");
    assertThat(gate.get("conditions")).isNotNull();
    externalServer.clear();

    // change severity of issue, won't change the QG status, so no webhook called
    tester.wsClient().issues().bulkChange(new BulkChangeRequest().setIssues(singletonList(firstIssue.getKey()))
      .setSetSeverity(singletonList("MINOR")));
    externalServer.waitUntilAllWebHooksCalled(1);
    assertThat(externalServer.getPayloadRequests()).isEmpty();

    // resolve issue as won't fix, QG status goes to OK, so webhook called
    tester.wsClient().issues().bulkChange(new BulkChangeRequest().setIssues(singletonList(firstIssue.getKey()))
      .setDoTransition("wontfix"));
    externalServer.waitUntilAllWebHooksCalled(1);
    request = externalServer.getPayloadRequests().get(0);
    payload = jsonToMap(request.getJson());
    gate = (Map<String, Object>) payload.get("qualityGate");
    assertThat(gate.get("status")).isEqualTo("OK");
  }

  private void analyseProject(Project project) {
    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.projectKey", project.getKey(),
      "sonar.projectName", project.getName());
  }
}
