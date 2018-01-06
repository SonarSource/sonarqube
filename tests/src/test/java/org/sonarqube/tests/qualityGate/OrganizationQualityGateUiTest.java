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
package org.sonarqube.tests.qualityGate;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.sonar.orchestrator.Orchestrator;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.ProjectDashboardPage;
import org.sonarqube.qa.util.pageobjects.QualityGatePage;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import util.issue.IssueRule;

import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.restoreProfile;
import static util.ItUtils.runProjectAnalysis;

public class OrganizationQualityGateUiTest {
  @ClassRule
  public static Orchestrator orchestrator = OrganizationQualityGateSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Rule
  public IssueRule issueRule = IssueRule.from(orchestrator);

  private Organizations.Organization organization;
  private Users.CreateWsResponse.User user;
  private Users.CreateWsResponse.User gateAdmin;

  @Before
  public void setUp() {
    organization = tester.organizations().generate();
    gateAdmin = tester.users().generate();
    tester.organizations().addMember(organization, gateAdmin);
    tester.wsClient().permissions().addUser(new AddUserRequest()
      .setOrganization(organization.getKey())
      .setLogin(gateAdmin.getLogin())
      .setPermission("gateadmin"));
    user = tester.users().generate();
    tester.organizations().addMember(organization, user);
    restoreProfile(orchestrator, getClass().getResource("/issue/with-many-rules.xml"), organization.getKey());
  }

  @Test
  public void should_have_a_link_to_quality_gates() {
    tester.openBrowser()
      .logIn().submitCredentials(user.getLogin())
      .openQualityGates(organization.getKey());

    SelenideElement element = $(".navbar-context .navbar-tabs")
      .find(By.linkText("Quality Gates"))
      .should(Condition.exist);
    assertThat(element.attr("href")).endsWith("/organizations/" + organization.getKey() + "/quality_gates");
  }

  @Test
  public void should_display_available_quality_gates() {
    QualityGatePage page = tester.openBrowser()
      .logIn().submitCredentials(gateAdmin.getLogin())
      .openQualityGates(organization.getKey());
    page.countQualityGates(1).displayIntro();
  }

  @Test
  public void should_not_allow_random_user_to_create() {
    tester.openBrowser()
      .logIn().submitCredentials(user.getLogin())
      .openQualityGates(organization.getKey())
      .canNotCreateQG();
    tester.openBrowser()
      .logIn().submitCredentials(gateAdmin.getLogin())
      .openQualityGates(organization.getKey())
      .canCreateQG();
  }

  @Test
  public void quality_gate_link_on_project_dashboard_should_have_organization_context() {
    String project = tester.projects().provision(organization).getKey();
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.projectKey", project,
      "sonar.organization", organization.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin",
      "sonar.scm.disabled", "false",
      "sonar.scm.provider", "xoo");

    String link = "/organizations/" + organization.getKey() + "/quality_gates/show/1";
    ProjectDashboardPage page = tester.openBrowser()
      .logIn().submitCredentials(user.getLogin())
      .openProjectDashboard(project);
    page.hasQualityGateLink("Sonar way", link);
  }

  @Test
  public void should_redirect_to_display_quality_gate_detail() {
    QualityGatePage page = tester.openBrowser()
      .logIn().submitCredentials(user.getLogin())
      .openQualityGates(organization.getKey());
    page.countQualityGates(1).displayQualityGateDetail("Sonar way");
  }
}
