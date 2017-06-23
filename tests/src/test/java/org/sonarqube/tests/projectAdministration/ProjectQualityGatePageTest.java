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
package org.sonarqube.tests.projectAdministration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category1Suite;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.qualitygate.SelectWsRequest;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.ProjectQualityGatePage;

import static util.ItUtils.newAdminWsClient;

public class ProjectQualityGatePageTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  private Navigation nav = Navigation.create(ORCHESTRATOR);

  private static WsClient wsClient;

  @BeforeClass
  public static void prepare() {
    wsClient = newAdminWsClient(ORCHESTRATOR);
  }

  @Before
  public void setUp() {
    ORCHESTRATOR.resetData();

    wsClient.wsConnector().call(new PostRequest("api/projects/create")
      .setParam("name", "Sample")
      .setParam("key", "sample"));
  }

  @Test
  public void should_display_default() {
    QualityGate customQualityGate = createCustomQualityGate("should_display_default");
    qualityGateClient().setDefault(customQualityGate.id());

    ProjectQualityGatePage page = openPage();
    SelenideElement selectedQualityGate = page.getSelectedQualityGate();
    selectedQualityGate.should(Condition.hasText("Default"));
    selectedQualityGate.should(Condition.hasText(customQualityGate.name()));
  }

  @Test
  public void should_display_custom() {
    QualityGate customQualityGate = createCustomQualityGate("should_display_custom");
    associateWithQualityGate(customQualityGate);

    ProjectQualityGatePage page = openPage();
    SelenideElement selectedQualityGate = page.getSelectedQualityGate();
    selectedQualityGate.shouldNot(Condition.hasText("Default"));
    selectedQualityGate.should(Condition.hasText(customQualityGate.name()));
  }

  @Test
  public void should_display_none() {
    qualityGateClient().unsetDefault();

    ProjectQualityGatePage page = openPage();
    page.assertNotSelected();
  }

  @Test
  public void should_set_custom() {
    QualityGate customQualityGate = createCustomQualityGate("should_set_custom");

    ProjectQualityGatePage page = openPage();
    page.setQualityGate(customQualityGate.name());

    SelenideElement selectedQualityGate = page.getSelectedQualityGate();
    selectedQualityGate.should(Condition.hasText(customQualityGate.name()));
  }

  @Test
  public void should_set_default() {
    QualityGate customQualityGate = createCustomQualityGate("should_set_default");
    qualityGateClient().setDefault(customQualityGate.id());

    ProjectQualityGatePage page = openPage();
    page.setQualityGate(customQualityGate.name());

    SelenideElement selectedQualityGate = page.getSelectedQualityGate();
    selectedQualityGate.should(Condition.hasText("Default"));
    selectedQualityGate.should(Condition.hasText(customQualityGate.name()));
  }

  @Test
  @Ignore("find a way to select None")
  public void should_set_none() {
    qualityGateClient().unsetDefault();
    QualityGate customQualityGate = createCustomQualityGate("should_set_none");
    associateWithQualityGate(customQualityGate);

    ProjectQualityGatePage page = openPage();
    page.setQualityGate("");

    page.assertNotSelected();
  }

  private ProjectQualityGatePage openPage() {
    nav.logIn().submitCredentials("admin", "admin");
    return nav.openProjectQualityGate("sample");
  }

  private static QualityGate createCustomQualityGate(String name) {
    return qualityGateClient().create(name);
  }

  private void associateWithQualityGate(QualityGate qualityGate) {
    wsClient.qualityGates().associateProject(new SelectWsRequest().setProjectKey("sample").setGateId(qualityGate.id()));
  }

  private static QualityGateClient qualityGateClient() {
    return ORCHESTRATOR.getServer().adminWsClient().qualityGateClient();
  }
}
