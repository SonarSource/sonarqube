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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.WebhooksPage;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.Webhooks.CreateWsResponse.Webhook;

import static util.ItUtils.runProjectAnalysis;

public class WebhooksPageTest
{
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
  public void list_global_webhooks() {
    tester.webhooks().generate();
    Webhook webhook = tester.webhooks().generate();
    tester.wsClient().users().skipOnboardingTutorial();
    WebhooksPage webhooksPage = tester.openBrowser().logIn().submitCredentials("admin").openWebhooks();
    webhooksPage
      .countWebhooks(2)
      .hasWebhook(webhook.getUrl())
      .hasWebhook(webhook.getName());
  }

  @Test
  public void list_project_webhooks() {
    User user = tester.users().generateAdministratorOnDefaultOrganization();
    tester.wsClient().users().skipOnboardingTutorial();

    Project project = tester.projects().provision();
    analyseProject(project);

    Webhook webhook1 = tester.webhooks().generate(project);
    Webhook webhook2 = tester.webhooks().generate(project);

    WebhooksPage webhooksPage = tester.openBrowser().logIn().submitCredentials(user.getLogin()).openProjectWebhooks(project.getKey());
    webhooksPage
      .countWebhooks(2)
      .hasWebhook(webhook1.getUrl())
      .hasWebhook(webhook2.getUrl());
  }

  @Test
  public void create_new_webhook() {
    User user = tester.users().generateAdministratorOnDefaultOrganization();
    tester.wsClient().users().skipOnboardingTutorial();

    Project project = tester.projects().provision();
    analyseProject(project);

    WebhooksPage webhooksPage = tester.openBrowser().logIn().submitCredentials(user.getLogin()).openProjectWebhooks(project.getKey());
    webhooksPage
      .hasNoWebhooks()
      .createWebhook("my-webhook", "http://greg:pass@test.com")
      .countWebhooks(1)
      .hasWebhook("my-webhook")
      .hasWebhook("http://greg:pass@test.com")
      .hasNoLatestDelivery("my-webhook");
  }

  @Test
  public void prevent_webhook_creation() {
    tester.wsClient().users().skipOnboardingTutorial();

    Webhook webhook = tester.webhooks().generate();
    for (int i = 0; i < 9; i++) {
      tester.webhooks().generate();
    }

    WebhooksPage webhooksPage = tester.openBrowser().logIn().submitCredentials("admin").openWebhooks();
    webhooksPage
      .countWebhooks(10)
      .createIsDisabled()
      .deleteWebhook(webhook.getName())
      .countWebhooks(9)
      .createWebhook("my-new-webhook", "http://my-new-webhook.com");
  }

  @Test
  public void delete_webhook() {
    User user = tester.users().generateAdministratorOnDefaultOrganization();
    tester.wsClient().users().skipOnboardingTutorial();

    Project project = tester.projects().provision();
    analyseProject(project);

    tester.webhooks().generate(project);
    tester.webhooks().generate(project);
    Webhook webhook = tester.webhooks().generate(project);

    WebhooksPage webhooksPage = tester.openBrowser().logIn().submitCredentials(user.getLogin()).openProjectWebhooks(project.getKey());
    webhooksPage
      .countWebhooks(3)
      .deleteWebhook(webhook.getName())
      .countWebhooks(2);
  }

  @Test
  public void display_deliveries_payloads() throws InterruptedException {
    User user = tester.users().generateAdministratorOnDefaultOrganization();
    tester.wsClient().users().skipOnboardingTutorial();

    Project project = tester.projects().provision();
    Webhook webhook = tester.webhooks().generate(project, p -> p.setUrl(externalServer.urlFor("/test_deliveries")));

    analyseProject(project);
    analyseProject(project);

    externalServer.waitUntilAllWebHooksCalled(1);

    WebhooksPage webhooksPage = tester.openBrowser().logIn().submitCredentials(user.getLogin()).openProjectWebhooks(project.getKey());
    webhooksPage
      .countWebhooks(1)
      .hasWebhook(webhook.getUrl())
      .hasLatestDelivery(webhook.getName());

    WebhooksPage.DeliveriesForm deliveriesForm = webhooksPage.showDeliveries(webhook.getName());
    deliveriesForm
      .countDeliveries(2)
      .isSuccessFull(0)
      .payloadContains(0, "Response: 200", "\"status\": \"SUCCESS\"");
  }

  private void analyseProject(Project project) {
    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.projectKey", project.getKey(),
      "sonar.projectName", project.getName());
  }
}
