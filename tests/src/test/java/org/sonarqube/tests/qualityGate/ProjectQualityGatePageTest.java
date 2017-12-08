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
package org.sonarqube.tests.qualityGate;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.sonar.orchestrator.Orchestrator;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.qa.util.pageobjects.ProjectQualityGatePage;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.qualitygates.SelectRequest;

public class ProjectQualityGatePageTest {

  @ClassRule
  public static Orchestrator orchestrator = QualityGateSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of QualityGateSuite must disable organizations
    .disableOrganizations();

  @Before
  public void setUp() {
    tester.wsClient().wsConnector().call(new PostRequest("api/projects/create")
      .setParam("name", "Sample")
      .setParam("key", "sample"));
    defaultGate = qualityGateClient().list().defaultGate();
  }

  private QualityGate defaultGate;

  @After
  public void tearDown() {
    if (defaultGate != null) {
      qualityGateClient().setDefault(defaultGate.id());
    }
  }

  @Test
  public void should_display_default() {
    QualityGate customQualityGate = createCustomQualityGate("should_display_default");
    qualityGateClient().setDefault(customQualityGate.id());

    try {
      ProjectQualityGatePage page = openPage();
      SelenideElement selectedQualityGate = page.getSelectedQualityGate();
      selectedQualityGate.should(Condition.text("Default"));
      selectedQualityGate.should(Condition.text(customQualityGate.name()));
    } finally {
      qualityGateClient().destroy(customQualityGate.id());
    }
  }

  @Test
  public void should_display_custom() {
    QualityGate customQualityGate = createCustomQualityGate("should_display_custom");
    associateWithQualityGate(customQualityGate);

    ProjectQualityGatePage page = openPage();
    SelenideElement selectedQualityGate = page.getSelectedQualityGate();
    selectedQualityGate.shouldNot(Condition.text("Default"));
    selectedQualityGate.should(Condition.text(customQualityGate.name()));
  }

  @Test
  public void should_set_custom() {
    QualityGate customQualityGate = createCustomQualityGate("should_set_custom");

    ProjectQualityGatePage page = openPage();
    page.setQualityGate(customQualityGate.name());

    SelenideElement selectedQualityGate = page.getSelectedQualityGate();
    selectedQualityGate.should(Condition.text(customQualityGate.name()));
  }

  @Test
  public void should_set_default() {
    QualityGate customQualityGate = createCustomQualityGate("should_set_default");
    qualityGateClient().setDefault(customQualityGate.id());

    try {
      ProjectQualityGatePage page = openPage();
      page.setQualityGate(customQualityGate.name());

      SelenideElement selectedQualityGate = page.getSelectedQualityGate();
      selectedQualityGate.should(Condition.text("Default"));
      selectedQualityGate.should(Condition.text(customQualityGate.name()));
    } finally {
      qualityGateClient().destroy(customQualityGate.id());
    }
  }

  private ProjectQualityGatePage openPage() {
    tester.wsClient().users().skipOnboardingTutorial();
    Navigation navigation = tester.openBrowser().logIn().submitCredentials("admin");
    return navigation.openProjectQualityGate("sample");
  }

  private QualityGate createCustomQualityGate(String name) {
    return qualityGateClient().create(name);
  }

  private void associateWithQualityGate(QualityGate qualityGate) {
    tester.wsClient().qualitygates().select(new SelectRequest().setProjectKey("sample").setGateId(String.valueOf(qualityGate.id())));
  }

  private QualityGateClient qualityGateClient() {
    return orchestrator.getServer().adminWsClient().qualityGateClient();
  }
}
